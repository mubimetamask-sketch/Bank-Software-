package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_items")
data class UploadItem(
    @PrimaryKey
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val localUri: String? = null,
    val localPath: String? = null,
    val category: String = UploadCategory.DOCUMENT.name,
    val status: String = UploadStatus.QUEUED.name,
    val bytesUploaded: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val etaSeconds: Long = 0L,
    val cloudUrl: String? = null,
    val cloudStorageKey: String? = null,
    val sha256Checksum: String? = null,
    val cloudBucket: String = "us-east-cloudvault-secure",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0
) {
    val progress: Float
        get() = if (fileSize > 0L) (bytesUploaded.toFloat() / fileSize.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    fun isTerminal(): Boolean {
        return status == UploadStatus.COMPLETED.name || 
               status == UploadStatus.CANCELLED.name
    }

    fun canPause(): Boolean {
        return status == UploadStatus.UPLOADING.name || status == UploadStatus.QUEUED.name
    }

    fun canResume(): Boolean {
        return status == UploadStatus.PAUSED.name || status == UploadStatus.FAILED.name
    }
}
