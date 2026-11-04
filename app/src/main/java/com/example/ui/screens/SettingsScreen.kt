package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.storage.CloudConfig
import com.example.data.storage.CloudProvider
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: CloudConfig,
    onSaveConfig: (CloudConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedProvider by remember(config) { mutableStateOf(config.provider) }
    var targetBucket by remember(config) { mutableStateOf(config.targetBucket) }
    var customEndpointUrl by remember(config) { mutableStateOf(config.customEndpointUrl) }
    var customAuthHeader by remember(config) { mutableStateOf(config.customAuthHeader) }
    var chunkSizeKb by remember(config) { mutableStateOf(config.chunkSizeKb) }
    var maxConcurrent by remember(config) { mutableStateOf(config.maxConcurrentUploads) }
    var wifiOnly by remember(config) { mutableStateOf(config.wifiOnly) }
    var autoRetry by remember(config) { mutableStateOf(config.autoRetry) }

    var expandedProviderMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Cloud Provider Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Cloud Storage Target",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configure where your files are uploaded directly",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedProviderMenu,
                    onExpandedChange = { expandedProviderMenu = !expandedProviderMenu }
                ) {
                    OutlinedTextField(
                        value = selectedProvider.title,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cloud Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProviderMenu) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("provider_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedProviderMenu,
                        onDismissRequest = { expandedProviderMenu = false }
                    ) {
                        CloudProvider.values().forEach { provider ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(provider.title, fontWeight = FontWeight.Bold)
                                        Text(provider.description, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    selectedProvider = provider
                                    if (targetBucket.isBlank() || targetBucket == config.provider.defaultBucket) {
                                        targetBucket = provider.defaultBucket
                                    }
                                    expandedProviderMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetBucket,
                    onValueChange = { targetBucket = it },
                    label = { Text("Storage Bucket / Container Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (selectedProvider == CloudProvider.CUSTOM_REST) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customEndpointUrl,
                        onValueChange = { customEndpointUrl = it },
                        label = { Text("Custom REST Upload Endpoint (HTTP/HTTPS)") },
                        placeholder = { Text("https://my-server.com/api/upload") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customAuthHeader,
                        onValueChange = { customAuthHeader = it },
                        label = { Text("Authorization Header (Optional)") },
                        placeholder = { Text("Bearer eyJhbGciOi...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Upload Performance & Chunking
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Transfer Telemetry & Chunking",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Chunk Size
                Text(
                    text = "Upload Chunk Size: ${chunkSizeKb} KB (${chunkSizeKb / 1024.0} MB)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Slider(
                    value = chunkSizeKb.toFloat(),
                    onValueChange = { chunkSizeKb = it.toInt() },
                    valueRange = 256f..4096f,
                    steps = 14,
                    colors = SliderDefaults.colors(thumbColor = CloudBlue, activeTrackColor = CloudBlue)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Max Concurrent Uploads
                Text(
                    text = "Max Concurrent Streams: $maxConcurrent files",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Slider(
                    value = maxConcurrent.toFloat(),
                    onValueChange = { maxConcurrent = it.toInt() },
                    valueRange = 1f..4f,
                    steps = 2,
                    colors = SliderDefaults.colors(thumbColor = CloudBlue, activeTrackColor = CloudBlue)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Wi-Fi Only Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wi-Fi Only Mode", fontWeight = FontWeight.SemiBold)
                        Text("Avoid consuming mobile cellular data", style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = wifiOnly, onCheckedChange = { wifiOnly = it })
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Retry Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Automatic Resumption & Retry", fontWeight = FontWeight.SemiBold)
                        Text("Resume dropped chunks upon reconnection", style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = autoRetry, onCheckedChange = { autoRetry = it })
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                val updated = config.copy(
                    provider = selectedProvider,
                    targetBucket = targetBucket,
                    customEndpointUrl = customEndpointUrl,
                    customAuthHeader = customAuthHeader,
                    chunkSizeKb = chunkSizeKb,
                    maxConcurrentUploads = maxConcurrent,
                    wifiOnly = wifiOnly,
                    autoRetry = autoRetry
                )
                onSaveConfig(updated)
                Toast.makeText(context, "Storage configuration saved!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_settings_button")
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CloudBlue)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Storage Settings", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
