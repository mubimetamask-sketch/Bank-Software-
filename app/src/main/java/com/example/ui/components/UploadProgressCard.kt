package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UploadItem
import com.example.data.model.UploadStatus
import com.example.ui.theme.*
import com.example.ui.util.FormatUtils

@Composable
fun UploadProgressCard(
    item: UploadItem,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDetails: (UploadItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = FormatUtils.getCategoryColor(item.category)
    val statusColor = FormatUtils.getStatusColor(item.status)
    val animatedProgress by animateFloatAsState(
        targetValue = item.progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "progress_anim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upload_card_${item.id}")
            .clip(RoundedCornerShape(16.dp))
            .clickable { onDetails(item) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon + File Details + Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FormatUtils.getCategoryIcon(item.category),
                        contentDescription = item.category,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // File Name & Status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.status.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        )
                        Text(
                            text = " • ${FormatUtils.formatBytes(item.fileSize)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Primary Action Button (Pause, Resume, Retry)
                when (item.status) {
                    UploadStatus.UPLOADING.name -> {
                        IconButton(
                            onClick = { onPause(item.id) },
                            modifier = Modifier
                                .testTag("pause_button_${item.id}")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PauseCircleFilled,
                                contentDescription = "Pause",
                                tint = CloudAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    UploadStatus.PAUSED.name -> {
                        IconButton(
                            onClick = { onResume(item.id) },
                            modifier = Modifier
                                .testTag("resume_button_${item.id}")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = "Resume",
                                tint = CloudBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    UploadStatus.FAILED.name -> {
                        IconButton(
                            onClick = { onRetry(item.id) },
                            modifier = Modifier
                                .testTag("retry_button_${item.id}")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Retry",
                                tint = CloudBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    UploadStatus.QUEUED.name -> {
                        IconButton(
                            onClick = { onPause(item.id) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Queued",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    UploadStatus.COMPLETED.name -> {
                        IconButton(
                            onClick = { onDetails(item) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed Details",
                                tint = CloudEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Cancel / Delete Menu
                IconButton(
                    onClick = {
                        if (item.status == UploadStatus.COMPLETED.name || item.status == UploadStatus.CANCELLED.name) {
                            onDelete(item.id)
                        } else {
                            onCancel(item.id)
                        }
                    },
                    modifier = Modifier
                        .testTag("cancel_button_${item.id}")
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = if (item.status == UploadStatus.COMPLETED.name) Icons.Default.DeleteOutline else Icons.Default.Close,
                        contentDescription = "Cancel or Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar Section
            if (item.status == UploadStatus.UPLOADING.name || item.status == UploadStatus.PAUSED.name || item.status == UploadStatus.QUEUED.name) {
                // Progress Bar with active gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CloudBlue, CloudCyan)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Analytics: Uploaded / Total, Percentage, Speed, ETA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${FormatUtils.formatBytes(item.bytesUploaded)} / ${FormatUtils.formatBytes(item.fileSize)} (${item.progressPercent}%)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (item.status == UploadStatus.UPLOADING.name && item.uploadSpeedBps > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚡ ${FormatUtils.formatSpeed(item.uploadSpeedBps)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CloudBlue
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${FormatUtils.formatEta(item.etaSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (item.status == UploadStatus.PAUSED.name) {
                        Text(
                            text = "Paused",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CloudAmber
                            )
                        )
                    } else if (item.status == UploadStatus.QUEUED.name) {
                        Text(
                            text = "Waiting in queue...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (item.status == UploadStatus.COMPLETED.name) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "100% Uploaded to Cloud",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = CloudEmerald
                        )
                    )
                    Text(
                        text = FormatUtils.formatDate(item.completedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (item.status == UploadStatus.FAILED.name) {
                Text(
                    text = "Error: ${item.errorMessage ?: "Upload failed"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = CloudRose),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
