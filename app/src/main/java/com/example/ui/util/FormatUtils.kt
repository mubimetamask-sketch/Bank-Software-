package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.UploadCategory
import com.example.data.model.UploadStatus
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }

    fun formatSpeed(speedBps: Long): String {
        if (speedBps <= 0) return "0 KB/s"
        return "${formatBytes(speedBps)}/s"
    }

    fun formatEta(etaSeconds: Long): String {
        if (etaSeconds <= 0) return "Finishing..."
        if (etaSeconds < 60) return "${etaSeconds}s left"
        val minutes = etaSeconds / 60
        val seconds = etaSeconds % 60
        return "${minutes}m ${seconds}s left"
    }

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "-"
        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getCategoryIcon(categoryName: String): ImageVector {
        return when (categoryName) {
            UploadCategory.PAYMENT_CHEQUE.name -> Icons.Default.ReceiptLong
            UploadCategory.INVOICE_BILL.name -> Icons.AutoMirrored.Filled.Article
            UploadCategory.DATABASE_BACKUP.name -> Icons.Default.Backup
            UploadCategory.MEDIA.name -> Icons.Default.Image
            UploadCategory.DOCUMENT.name -> Icons.Default.Description
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }
    }

    fun getCategoryColor(categoryName: String): Color {
        return when (categoryName) {
            UploadCategory.PAYMENT_CHEQUE.name -> CloudAmber
            UploadCategory.INVOICE_BILL.name -> CloudBlue
            UploadCategory.DATABASE_BACKUP.name -> CloudPurple
            UploadCategory.MEDIA.name -> CloudCyan
            UploadCategory.DOCUMENT.name -> CloudEmerald
            else -> Color(0xFF64748B)
        }
    }

    fun getStatusColor(statusName: String): Color {
        return when (statusName) {
            UploadStatus.COMPLETED.name -> CloudEmerald
            UploadStatus.UPLOADING.name -> CloudBlue
            UploadStatus.PAUSED.name -> CloudAmber
            UploadStatus.QUEUED.name -> CloudCyan
            UploadStatus.FAILED.name -> CloudRose
            UploadStatus.CANCELLED.name -> Color(0xFF94A3B8)
            else -> Color(0xFF94A3B8)
        }
    }
}
