package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.db.UploadDao
import com.example.data.model.UploadCategory
import com.example.data.model.UploadItem
import com.example.data.model.UploadStatus
import com.example.data.storage.CloudConfig
import com.example.data.storage.UploadEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class UploadRepository(
    private val context: Context,
    private val dao: UploadDao,
    val uploadEngine: UploadEngine
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var queueWorkerJob: Job? = null

    val allUploads: Flow<List<UploadItem>> = dao.getAllUploads()
    val activeQueue: Flow<List<UploadItem>> = dao.getActiveQueue()
    val completedUploads: Flow<List<UploadItem>> = dao.getCompletedUploads()
    val totalUploadedBytes: Flow<Long?> = dao.getTotalUploadedBytes()
    val completedCount: Flow<Int> = dao.getCompletedCount()
    val config: StateFlow<CloudConfig> = uploadEngine.config

    init {
        startQueueWorker()
        seedInitialSamplesIfEmpty()
    }

    fun updateConfig(config: CloudConfig) {
        uploadEngine.updateConfig(config)
    }

    private fun startQueueWorker() {
        queueWorkerJob?.cancel()
        queueWorkerJob = repositoryScope.launch {
            while (isActive) {
                try {
                    val activeItems = dao.getActiveQueue().first()
                    val currentlyUploading = activeItems.filter { it.status == UploadStatus.UPLOADING.name }
                    val queuedItems = activeItems.filter { it.status == UploadStatus.QUEUED.name }

                    val maxConcurrent = uploadEngine.config.value.maxConcurrentUploads
                    val availableSlots = (maxConcurrent - currentlyUploading.size).coerceAtLeast(0)

                    if (availableSlots > 0 && queuedItems.isNotEmpty()) {
                        val toStart = queuedItems.take(availableSlots)
                        for (item in toStart) {
                            launchUploadTask(item)
                        }
                    }
                } catch (e: Exception) {
                    // Safe guard
                }
                delay(400)
            }
        }
    }

    private fun launchUploadTask(item: UploadItem) {
        repositoryScope.launch {
            dao.updateStatus(item.id, UploadStatus.UPLOADING.name)
            uploadEngine.startUpload(
                item = item,
                onProgress = { bytes, speed, eta ->
                    dao.updateProgress(item.id, bytes, speed, eta, UploadStatus.UPLOADING.name)
                },
                onCompleted = { cloudUrl, checksum ->
                    dao.markCompleted(
                        id = item.id,
                        bytes = item.fileSize,
                        completedAt = System.currentTimeMillis(),
                        cloudUrl = cloudUrl,
                        checksum = checksum
                    )
                },
                onError = { error ->
                    dao.markFailed(item.id, error)
                },
                onPaused = {
                    dao.updateStatus(item.id, UploadStatus.PAUSED.name)
                }
            )
        }
    }

    suspend fun enqueueFromUri(uri: Uri, customCategory: UploadCategory = UploadCategory.DOCUMENT): String {
        val resolver = context.contentResolver
        var fileName = "file_${System.currentTimeMillis()}"
        var fileSize = 1024L
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex).coerceAtLeast(1024L)
            }
        }

        // Cache file locally if accessible so uploads can stream smoothly
        val localCacheFile = File(context.cacheDir, "upload_${UUID.randomUUID()}_$fileName")
        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localCacheFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (ignored: Exception) {}

        val category = determineCategory(fileName, mimeType, customCategory)
        val id = UUID.randomUUID().toString()

        val uploadItem = UploadItem(
            id = id,
            fileName = fileName,
            fileSize = if (localCacheFile.exists() && localCacheFile.length() > 0) localCacheFile.length() else fileSize,
            mimeType = mimeType,
            localUri = uri.toString(),
            localPath = if (localCacheFile.exists()) localCacheFile.absolutePath else null,
            category = category.name,
            status = UploadStatus.QUEUED.name
        )

        dao.insert(uploadItem)
        return id
    }

    suspend fun enqueuePreset(type: String): String {
        val id = UUID.randomUUID().toString()
        val item: UploadItem = when (type) {
            "CHEQUE_PAYMENT" -> {
                val sampleContent = "CHEQUE_VOUCHER_PKR_45000_AL_MADINA_TRADERS_CHQ_44812_OCT_2026"
                val file = createTempSampleFile("Cheque_Slip_CHQ-44812.pdf", sampleContent, 4_850_000L)
                UploadItem(
                    id = id,
                    fileName = "Cheque_Slip_CHQ-44812.pdf",
                    fileSize = file.length(),
                    mimeType = "application/pdf",
                    localPath = file.absolutePath,
                    category = UploadCategory.PAYMENT_CHEQUE.name
                )
            }
            "SUPPLIER_INVOICE" -> {
                val sampleContent = "INVOICE_DELIGHT_CUISINE_SPICES_MASALA_SUPPLY_INV_90823"
                val file = createTempSampleFile("Invoice_Delight_Spices_INV908.pdf", sampleContent, 12_400_000L)
                UploadItem(
                    id = id,
                    fileName = "Invoice_Delight_Spices_INV908.pdf",
                    fileSize = file.length(),
                    mimeType = "application/pdf",
                    localPath = file.absolutePath,
                    category = UploadCategory.INVOICE_BILL.name
                )
            }
            "DATABASE_BACKUP" -> {
                val sampleContent = "{\"version\":\"2026.11\",\"suppliers\":3,\"payments\":18,\"backupTimestamp\":1793748697814}"
                val file = createTempSampleFile("Enterprise_Payments_Backup_2026.json", sampleContent, 8_200_000L)
                UploadItem(
                    id = id,
                    fileName = "Enterprise_Payments_Backup_2026.json",
                    fileSize = file.length(),
                    mimeType = "application/json",
                    localPath = file.absolutePath,
                    category = UploadCategory.DATABASE_BACKUP.name
                )
            }
            "PRODUCT_CATALOG" -> {
                val sampleContent = "PRODUCT_CATALOG_HIGH_RES_ASSETS_PKR_2026"
                val file = createTempSampleFile("Rawalpindi_Supply_Catalog.png", sampleContent, 16_800_000L)
                UploadItem(
                    id = id,
                    fileName = "Rawalpindi_Supply_Catalog.png",
                    fileSize = file.length(),
                    mimeType = "image/png",
                    localPath = file.absolutePath,
                    category = UploadCategory.MEDIA.name
                )
            }
            else -> {
                val sampleContent = "FINANCIAL_AUDIT_REPORT_ISLAMABAD_2026"
                val file = createTempSampleFile("Financial_Audit_Report.docx", sampleContent, 6_100_000L)
                UploadItem(
                    id = id,
                    fileName = "Financial_Audit_Report.docx",
                    fileSize = file.length(),
                    mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    localPath = file.absolutePath,
                    category = UploadCategory.DOCUMENT.name
                )
            }
        }
        dao.insert(item)
        return id
    }

    fun pauseUpload(id: String) {
        uploadEngine.pauseUpload(id)
        repositoryScope.launch {
            dao.updateStatus(id, UploadStatus.PAUSED.name)
        }
    }

    fun resumeUpload(id: String) {
        repositoryScope.launch {
            dao.updateStatus(id, UploadStatus.QUEUED.name)
        }
    }

    fun cancelUpload(id: String) {
        uploadEngine.cancelUpload(id)
        repositoryScope.launch {
            dao.updateStatus(id, UploadStatus.CANCELLED.name)
        }
    }

    fun retryUpload(id: String) {
        repositoryScope.launch {
            dao.updateProgress(id, 0L, 0L, 0L, UploadStatus.QUEUED.name)
        }
    }

    fun pauseAll() {
        repositoryScope.launch {
            val active = dao.getActiveQueue().first()
            for (item in active) {
                if (item.canPause()) {
                    pauseUpload(item.id)
                }
            }
        }
    }

    fun resumeAll() {
        repositoryScope.launch {
            val active = dao.getActiveQueue().first()
            for (item in active) {
                if (item.canResume()) {
                    resumeUpload(item.id)
                }
            }
        }
    }

    fun cancelAll() {
        repositoryScope.launch {
            val active = dao.getActiveQueue().first()
            for (item in active) {
                cancelUpload(item.id)
            }
        }
    }

    suspend fun deleteUpload(id: String) {
        uploadEngine.cancelUpload(id)
        dao.deleteById(id)
    }

    suspend fun clearCompleted() {
        dao.clearCompleted()
    }

    private fun determineCategory(
        fileName: String,
        mimeType: String,
        fallback: UploadCategory
    ): UploadCategory {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.contains("cheque") || lowerName.contains("slip") || lowerName.contains("chq") -> UploadCategory.PAYMENT_CHEQUE
            lowerName.contains("invoice") || lowerName.contains("bill") || lowerName.contains("receipt") -> UploadCategory.INVOICE_BILL
            lowerName.contains("backup") || lowerName.endsWith(".json") || lowerName.endsWith(".sql") -> UploadCategory.DATABASE_BACKUP
            mimeType.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".png") -> UploadCategory.MEDIA
            mimeType.contains("pdf") || lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx") -> UploadCategory.DOCUMENT
            else -> fallback
        }
    }

    private fun createTempSampleFile(name: String, sampleString: String, desiredSize: Long): File {
        val file = File(context.cacheDir, name)
        if (!file.exists() || file.length() < 1000) {
            FileOutputStream(file).use { fos ->
                val header = sampleString.toByteArray()
                fos.write(header)
                val remaining = desiredSize - header.size
                if (remaining > 0) {
                    val buffer = ByteArray(minOf(64 * 1024, remaining.toInt())) { 0x41.toByte() }
                    var written = 0L
                    while (written < remaining) {
                        val toWrite = minOf(buffer.size.toLong(), remaining - written).toInt()
                        fos.write(buffer, 0, toWrite)
                        written += toWrite
                    }
                }
            }
        }
        return file
    }

    private fun seedInitialSamplesIfEmpty() {
        repositoryScope.launch {
            val count = dao.getAllUploads().first().size
            if (count == 0) {
                // Seed 2 completed items to showcase the Cloud Vault on launch
                val completedItem1 = UploadItem(
                    id = "init_sample_1",
                    fileName = "Al_Madina_Traders_Cheque_CHQ-44812.pdf",
                    fileSize = 4_850_000L,
                    mimeType = "application/pdf",
                    category = UploadCategory.PAYMENT_CHEQUE.name,
                    status = UploadStatus.COMPLETED.name,
                    bytesUploaded = 4_850_000L,
                    completedAt = System.currentTimeMillis() - 7200000,
                    cloudUrl = "https://storage.cloudvault.net/us-east-cloudvault-secure/cheques/Al_Madina_Traders_Cheque_CHQ-44812.pdf",
                    sha256Checksum = "9e107d9d372bb6826bd81d3542a419d690a6e02611689fb2b2344ef6590fa0a2"
                )
                val completedItem2 = UploadItem(
                    id = "init_sample_2",
                    fileName = "Supplier_Payments_Ledger_2026.json",
                    fileSize = 3_120_000L,
                    mimeType = "application/json",
                    category = UploadCategory.DATABASE_BACKUP.name,
                    status = UploadStatus.COMPLETED.name,
                    bytesUploaded = 3_120_000L,
                    completedAt = System.currentTimeMillis() - 3600000,
                    cloudUrl = "https://storage.cloudvault.net/us-east-cloudvault-secure/backups/Supplier_Payments_Ledger_2026.json",
                    sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
                )
                dao.insertAll(listOf(completedItem1, completedItem2))
            }
        }
    }
}
