package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UploadCategory
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewUploadBottomSheet(
    onDismiss: () -> Unit,
    onPickFiles: (UploadCategory) -> Unit,
    onSelectPreset: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(UploadCategory.DOCUMENT) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Title
            Text(
                text = "Direct Cloud Storage Upload",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Select files from your device or use fast business presets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Device File Picker Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_device_files_button")
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onDismiss()
                        onPickFiles(selectedCategory)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CloudBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = "Pick Files",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Browse Device Files",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Select PDFs, images, documents, or data backups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Presets Section
            Text(
                text = "Instant Business Presets (1-Tap Upload)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            PresetOptionRow(
                icon = Icons.Default.ReceiptLong,
                iconColor = CloudAmber,
                title = "Payment Cheque Slip",
                subtitle = "e.g. Al-Madina Traders CHQ-44812 (4.8 MB)",
                onClick = {
                    onDismiss()
                    onSelectPreset("CHEQUE_PAYMENT")
                }
            )

            PresetOptionRow(
                icon = Icons.AutoMirrored.Filled.Article,
                iconColor = CloudBlue,
                title = "Supplier Invoice / Bill",
                subtitle = "e.g. Delight Spices Spice Supply INV-908 (12.4 MB)",
                onClick = {
                    onDismiss()
                    onSelectPreset("SUPPLIER_INVOICE")
                }
            )

            PresetOptionRow(
                icon = Icons.Default.Backup,
                iconColor = CloudPurple,
                title = "Enterprise Database Backup",
                subtitle = "JSON export with payments & suppliers (8.2 MB)",
                onClick = {
                    onDismiss()
                    onSelectPreset("DATABASE_BACKUP")
                }
            )

            PresetOptionRow(
                icon = Icons.Default.Image,
                iconColor = CloudCyan,
                title = "High-Res Product Catalog",
                subtitle = "Media graphic supply asset (16.8 MB)",
                onClick = {
                    onDismiss()
                    onSelectPreset("PRODUCT_CATALOG")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PresetOptionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = "Upload",
                tint = CloudBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
