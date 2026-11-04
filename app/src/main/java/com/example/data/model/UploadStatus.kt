package com.example.data.model

enum class UploadStatus {
    QUEUED,
    UPLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class UploadCategory(val displayName: String, val iconName: String) {
    ALL("All Files", "folder"),
    PAYMENT_CHEQUE("Cheques & Slips", "receipt"),
    INVOICE_BILL("Invoices & Bills", "description"),
    DATABASE_BACKUP("Database Backups", "backup"),
    DOCUMENT("Documents & PDFs", "article"),
    MEDIA("Images & Media", "image"),
    OTHER("Other Files", "attachment")
}
