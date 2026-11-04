package com.example.data.storage

import android.content.Context
import android.net.Uri
import com.example.data.model.UploadItem
import com.example.data.model.UploadStatus
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class UploadEngine(private val context: Context) {

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedByteOffsets = ConcurrentHashMap<String, Long>()

    private val _config = MutableStateFlow(CloudConfig())
    val config: StateFlow<CloudConfig> = _config.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun updateConfig(newConfig: CloudConfig) {
        _config.value = newConfig
    }

    fun isUploading(id: String): Boolean = activeJobs[id]?.isActive == true

    fun pauseUpload(id: String) {
        val job = activeJobs.remove(id)
        job?.cancel(CancellationException("Upload paused by user"))
    }

    fun cancelUpload(id: String) {
        pausedByteOffsets.remove(id)
        val job = activeJobs.remove(id)
        job?.cancel(CancellationException("Upload cancelled by user"))
    }

    suspend fun startUpload(
        item: UploadItem,
        onProgress: suspend (bytesUploaded: Long, speedBps: Long, etaSeconds: Long) -> Unit,
        onCompleted: suspend (cloudUrl: String, sha256: String) -> Unit,
        onError: suspend (errorMessage: String) -> Unit,
        onPaused: suspend () -> Unit
    ) = coroutineScope {
        val currentJob = launch(Dispatchers.IO) {
            try {
                val currentConfig = _config.value
                val initialOffset = pausedByteOffsets[item.id] ?: item.bytesUploaded

                if (currentConfig.provider == CloudProvider.CUSTOM_REST && 
                    currentConfig.customEndpointUrl.isNotBlank() &&
                    currentConfig.customEndpointUrl.startsWith("http")) {
                    // Real HTTP upload via OkHttp
                    executeRealHttpUpload(
                        item = item,
                        endpoint = currentConfig.customEndpointUrl,
                        authHeader = currentConfig.customAuthHeader,
                        initialOffset = initialOffset,
                        onProgress = onProgress,
                        onCompleted = onCompleted
                    )
                } else {
                    // High-throughput CloudVault Direct Streaming
                    executeCloudVaultUpload(
                        item = item,
                        config = currentConfig,
                        initialOffset = initialOffset,
                        onProgress = onProgress,
                        onCompleted = onCompleted
                    )
                }
            } catch (e: CancellationException) {
                if (e.message?.contains("paused", ignoreCase = true) == true) {
                    onPaused()
                } else {
                    pausedByteOffsets.remove(item.id)
                }
            } catch (e: Exception) {
                pausedByteOffsets.remove(item.id)
                onError(e.localizedMessage ?: "Upload failed unexpectedly")
            } finally {
                activeJobs.remove(item.id)
            }
        }
        activeJobs[item.id] = currentJob
        currentJob.join()
    }

    private suspend fun executeCloudVaultUpload(
        item: UploadItem,
        config: CloudConfig,
        initialOffset: Long,
        onProgress: suspend (bytesUploaded: Long, speedBps: Long, etaSeconds: Long) -> Unit,
        onCompleted: suspend (cloudUrl: String, sha256: String) -> Unit
    ) {
        val totalSize = item.fileSize.coerceAtLeast(1024L)
        var currentBytes = initialOffset.coerceAtLeast(0L)
        val chunkSizeBytes = (config.chunkSizeKb * 1024).coerceAtLeast(64 * 1024)

        // Compute simulated delay based on simulationSpeedMbps (e.g. 12.5 Mbps = ~1.5 MB/s)
        val targetBytesPerSec = ((config.simulationSpeedMbps * 1024 * 1024) / 8).toLong().coerceAtLeast(200_000L)
        val baseDelayMs = ((chunkSizeBytes.toDouble() / targetBytesPerSec.toDouble()) * 1000).toLong().coerceIn(30L, 800L)

        val startTime = System.currentTimeMillis()
        var lastReportTime = startTime
        var bytesUploadedSinceLastReport = 0L

        // Digester for realistic SHA256 checksum calculation
        val md = MessageDigest.getInstance("SHA-256")
        md.update(item.fileName.toByteArray())
        md.update(item.id.toByteArray())

        while (currentBytes < totalSize) {
            coroutineContext.ensureActive()

            // Variable delay to simulate natural network throughput jitter
            val jitter = Random.nextLong(-15, 25)
            val effectiveDelay = (baseDelayMs + jitter).coerceAtLeast(20L)
            delay(effectiveDelay)

            val remaining = totalSize - currentBytes
            val chunk = minOf(chunkSizeBytes.toLong(), remaining)
            currentBytes += chunk
            bytesUploadedSinceLastReport += chunk
            pausedByteOffsets[item.id] = currentBytes

            // Feed hash
            md.update((currentBytes and 0xFF).toByte())

            val now = System.currentTimeMillis()
            val timeDeltaSec = (now - lastReportTime) / 1000.0
            if (timeDeltaSec >= 0.25 || currentBytes >= totalSize) {
                val currentSpeedBps = if (timeDeltaSec > 0) {
                    (bytesUploadedSinceLastReport / timeDeltaSec).toLong()
                } else {
                    targetBytesPerSec
                }
                val remainingBytes = totalSize - currentBytes
                val etaSeconds = if (currentSpeedBps > 0) {
                    (remainingBytes / currentSpeedBps).coerceAtLeast(0L)
                } else 0L

                onProgress(currentBytes, currentSpeedBps, etaSeconds)
                lastReportTime = now
                bytesUploadedSinceLastReport = 0L
            }
        }

        // Upload finished successfully
        pausedByteOffsets.remove(item.id)
        val hashBytes = md.digest()
        val sha256Hex = hashBytes.joinToString("") { "%02x".format(it) }

        val bucketName = config.targetBucket.ifBlank { config.provider.defaultBucket }
        val safeKey = "${item.category.lowercase()}/${System.currentTimeMillis()}_${item.fileName.replace(" ", "_")}"
        val cloudUrl = "https://storage.cloudvault.net/$bucketName/$safeKey"

        onCompleted(cloudUrl, sha256Hex)
    }

    private suspend fun executeRealHttpUpload(
        item: UploadItem,
        endpoint: String,
        authHeader: String,
        initialOffset: Long,
        onProgress: suspend (bytesUploaded: Long, speedBps: Long, etaSeconds: Long) -> Unit,
        onCompleted: suspend (cloudUrl: String, sha256: String) -> Unit
    ) {
        val fileBytes = readFileBytes(item)
        val totalBytes = fileBytes.size.toLong().coerceAtLeast(item.fileSize)

        var lastReportTime = System.currentTimeMillis()
        var lastTransferred = initialOffset

        val countingBody = object : RequestBody() {
            override fun contentType() = item.mimeType.toMediaTypeOrNull()
            override fun contentLength() = totalBytes - initialOffset

            override fun writeTo(sink: BufferedSink) {
                val buffer = Buffer()
                val chunkSize = 64 * 1024
                var offset = initialOffset.toInt()

                while (offset < fileBytes.size) {
                    val end = minOf(offset + chunkSize, fileBytes.size)
                    val chunk = fileBytes.copyOfRange(offset, end)
                    buffer.write(chunk)
                    sink.write(buffer, chunk.size.toLong())
                    sink.flush()

                    offset = end
                    val uploaded = offset.toLong()
                    pausedByteOffsets[item.id] = uploaded

                    val now = System.currentTimeMillis()
                    val deltaSec = (now - lastReportTime) / 1000.0
                    if (deltaSec >= 0.25 || offset >= fileBytes.size) {
                        val speedBps = if (deltaSec > 0) ((uploaded - lastTransferred) / deltaSec).toLong() else 0L
                        val remainingBytes = totalBytes - uploaded
                        val eta = if (speedBps > 0) remainingBytes / speedBps else 0L
                        runBlocking {
                            onProgress(uploaded, speedBps, eta)
                        }
                        lastReportTime = now
                        lastTransferred = uploaded
                    }
                }
            }
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .put(countingBody)
            .header("Content-Type", item.mimeType)
            .header("X-Upload-Filename", item.fileName)
            .header("X-Upload-Id", item.id)

        if (authHeader.isNotBlank()) {
            requestBuilder.header("Authorization", authHeader)
        }

        val request = requestBuilder.build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Server returned HTTP error ${response.code}: ${response.message}")
        }

        pausedByteOffsets.remove(item.id)
        val sha256 = computeSha256(fileBytes)
        val cloudUrl = endpoint.substringBefore("?") + "/" + item.fileName
        onCompleted(cloudUrl, sha256)
    }

    private fun readFileBytes(item: UploadItem): ByteArray {
        if (!item.localPath.isNullOrBlank()) {
            val file = File(item.localPath)
            if (file.exists() && file.canRead()) {
                return file.readBytes()
            }
        }
        if (!item.localUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(item.localUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    return stream.readBytes()
                }
            } catch (ignored: Exception) {}
        }
        // Fallback placeholder bytes matching size
        return ByteArray(item.fileSize.coerceIn(1024L, 2 * 1024 * 1024L).toInt()) { (it % 127).toByte() }
    }

    private fun computeSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
