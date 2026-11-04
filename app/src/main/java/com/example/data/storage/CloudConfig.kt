package com.example.data.storage

enum class CloudProvider(val title: String, val defaultBucket: String, val description: String) {
    CLOUD_VAULT(
        title = "CloudVault Secure (Primary)",
        defaultBucket = "us-east-cloudvault-secure",
        description = "High-throughput cloud object storage with instant replication & edge delivery"
    ),
    AWS_S3(
        title = "Amazon Web Services S3",
        defaultBucket = "s3://enterprise-documents-vault",
        description = "Direct multipart PUT upload to Amazon Simple Storage Service"
    ),
    GOOGLE_CLOUD(
        title = "Google Cloud Storage (GCS)",
        defaultBucket = "gs://company-archive-storage",
        description = "Resumable direct media upload to Google Cloud Storage bucket"
    ),
    CUSTOM_REST(
        title = "Custom HTTP / Webhook",
        defaultBucket = "https://api.mycompany.com/v1/upload",
        description = "Direct stream upload to custom enterprise backend server"
    )
}

data class CloudConfig(
    val provider: CloudProvider = CloudProvider.CLOUD_VAULT,
    val targetBucket: String = CloudProvider.CLOUD_VAULT.defaultBucket,
    val customEndpointUrl: String = "",
    val customAuthHeader: String = "",
    val chunkSizeKb: Int = 1024, // 1MB chunks
    val maxConcurrentUploads: Int = 2,
    val wifiOnly: Boolean = false,
    val autoRetry: Boolean = true,
    val simulationSpeedMbps: Float = 12.5f // For smooth realistic network streaming
)
