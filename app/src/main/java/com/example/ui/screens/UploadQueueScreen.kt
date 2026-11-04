package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UploadItem
import com.example.data.model.UploadStatus
import com.example.data.storage.CloudConfig
import com.example.ui.components.UploadProgressCard
import com.example.ui.theme.*
import com.example.ui.util.FormatUtils

@Composable
fun UploadQueueScreen(
    activeQueue: List<UploadItem>,
    completedCount: Int,
    totalBytes: Long,
    config: CloudConfig,
    onOpenUploadSheet: () -> Unit,
    onPauseUpload: (String) -> Unit,
    onResumeUpload: (String) -> Unit,
    onCancelUpload: (String) -> Unit,
    onRetryUpload: (String) -> Unit,
    onDeleteUpload: (String) -> Unit,
    onSelectDetails: (UploadItem) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onCancelAll: () -> Unit,
    onSelectPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uploadingCount = activeQueue.count { it.status == UploadStatus.UPLOADING.name }
    val pausedCount = activeQueue.count { it.status == UploadStatus.PAUSED.name }
    val aggregateSpeed = activeQueue.filter { it.status == UploadStatus.UPLOADING.name }.sumOf { it.uploadSpeedBps }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Cloud Storage Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_upload_card")
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(CloudEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = config.provider.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CloudBlue.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Direct Stream",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CloudBlue
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Cloud Storage Uploader",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Transfer files directly with live chunk telemetry & checksums",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Stats Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatMetric(
                            label = "Active Queue",
                            value = "${activeQueue.size}",
                            tint = CloudBlue
                        )
                        StatMetric(
                            label = "Live Speed",
                            value = if (aggregateSpeed > 0) FormatUtils.formatSpeed(aggregateSpeed) else "Idle",
                            tint = if (aggregateSpeed > 0) CloudCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatMetric(
                            label = "Total Uploaded",
                            value = FormatUtils.formatBytes(totalBytes),
                            tint = CloudEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Upload CTA Button
                    Button(
                        onClick = onOpenUploadSheet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_files_cta")
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CloudBlue)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upload New File",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Fast Preset Quick Actions Strip
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Document Presets",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        QuickPresetChip(
                            label = "Cheque Slip (4.8MB)",
                            icon = Icons.Default.ReceiptLong,
                            color = CloudAmber,
                            onClick = { onSelectPreset("CHEQUE_PAYMENT") }
                        )
                    }
                    item {
                        QuickPresetChip(
                            label = "Supplier Bill (12MB)",
                            icon = Icons.Default.Description,
                            color = CloudBlue,
                            onClick = { onSelectPreset("SUPPLIER_INVOICE") }
                        )
                    }
                    item {
                        QuickPresetChip(
                            label = "DB Backup (8MB)",
                            icon = Icons.Default.Backup,
                            color = CloudPurple,
                            onClick = { onSelectPreset("DATABASE_BACKUP") }
                        )
                    }
                    item {
                        QuickPresetChip(
                            label = "Catalog Image (16MB)",
                            icon = Icons.Default.Image,
                            color = CloudCyan,
                            onClick = { onSelectPreset("PRODUCT_CATALOG") }
                        )
                    }
                }
            }
        }

        // Active Queue Header & Batch Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upload Queue (${activeQueue.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (activeQueue.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (uploadingCount > 0) {
                            TextButton(onClick = onPauseAll) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause All", style = MaterialTheme.typography.labelSmall)
                            }
                        } else if (pausedCount > 0) {
                            TextButton(onClick = onResumeAll) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume All", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        TextButton(onClick = onCancelAll) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall, color = CloudRose)
                        }
                    }
                }
            }
        }

        // Empty Queue State
        if (activeQueue.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CloudBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = CloudBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "All Transfers Completed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No files currently uploading. Pick documents or media to start direct cloud transfer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Queue Items
            items(items = activeQueue, key = { it.id }) { item ->
                UploadProgressCard(
                    item = item,
                    onPause = onPauseUpload,
                    onResume = onResumeUpload,
                    onCancel = onCancelUpload,
                    onRetry = onRetryUpload,
                    onDelete = onDeleteUpload,
                    onDetails = onSelectDetails
                )
            }
        }
    }
}

@Composable
private fun StatMetric(label: String, value: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickPresetChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
