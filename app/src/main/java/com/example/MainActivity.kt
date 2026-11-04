package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UploadCategory
import com.example.ui.UploadViewModel
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.NewUploadBottomSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CloudVaultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UploadQueueScreen
import com.example.ui.theme.CloudBlue
import com.example.ui.theme.MyApplicationTheme

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    QUEUE("Queue", Icons.Default.CloudUpload),
    VAULT("Vault", Icons.Default.FolderShared),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CloudDropApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDropApp(viewModel: UploadViewModel = viewModel()) {
    var currentTab by remember { mutableStateOf(AppTab.QUEUE) }

    val activeQueue by viewModel.activeQueue.collectAsStateWithLifecycle()
    val completedUploads by viewModel.completedUploads.collectAsStateWithLifecycle()
    val filteredFiles by viewModel.filteredCompletedFiles.collectAsStateWithLifecycle()
    val totalBytes by viewModel.totalUploadedBytes.collectAsStateWithLifecycle()
    val completedCount by viewModel.completedCount.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedItemForDetails by viewModel.selectedItemForDetails.collectAsStateWithLifecycle()
    val showUploadSheet by viewModel.showUploadSheet.collectAsStateWithLifecycle()

    var pendingUploadCategory by remember { mutableStateOf(UploadCategory.DOCUMENT) }

    // Multi-Document File Picker via Android Storage Access Framework
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                viewModel.enqueueUri(uri, pendingUploadCategory)
            }
        }
    }

    // Single Document fallback launcher
    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                viewModel.enqueueUri(uri, pendingUploadCategory)
            }
        }
    }

    BackHandler(enabled = currentTab != AppTab.QUEUE) {
        currentTab = AppTab.QUEUE
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = CloudBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (currentTab) {
                                AppTab.QUEUE -> "CloudDrop"
                                AppTab.VAULT -> "Cloud Vault"
                                AppTab.ANALYTICS -> "Storage Analytics"
                                AppTab.SETTINGS -> "Cloud Settings"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    if (currentTab == AppTab.QUEUE && activeQueue.isNotEmpty()) {
                        Badge(
                            containerColor = CloudBlue,
                            contentColor = Color.White,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = "${activeQueue.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (tab == AppTab.QUEUE && activeQueue.isNotEmpty()) {
                                        Badge(containerColor = CloudBlue) {
                                            Text("${activeQueue.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CloudBlue,
                            selectedTextColor = CloudBlue,
                            indicatorColor = CloudBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == AppTab.QUEUE || currentTab == AppTab.VAULT) {
                FloatingActionButton(
                    onClick = { viewModel.setShowUploadSheet(true) },
                    containerColor = CloudBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_upload_file")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Upload Files")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.QUEUE -> {
                    UploadQueueScreen(
                        activeQueue = activeQueue,
                        completedCount = completedCount,
                        totalBytes = totalBytes,
                        config = config,
                        onOpenUploadSheet = { viewModel.setShowUploadSheet(true) },
                        onPauseUpload = viewModel::pauseUpload,
                        onResumeUpload = viewModel::resumeUpload,
                        onCancelUpload = viewModel::cancelUpload,
                        onRetryUpload = viewModel::retryUpload,
                        onDeleteUpload = viewModel::deleteUpload,
                        onSelectDetails = viewModel::selectItemForDetails,
                        onPauseAll = viewModel::pauseAll,
                        onResumeAll = viewModel::resumeAll,
                        onCancelAll = viewModel::cancelAll,
                        onSelectPreset = viewModel::enqueuePreset
                    )
                }
                AppTab.VAULT -> {
                    CloudVaultScreen(
                        files = filteredFiles,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        onSearchChange = viewModel::setSearchQuery,
                        onCategorySelect = viewModel::setSelectedCategory,
                        onSelectDetails = viewModel::selectItemForDetails,
                        onDeleteFile = viewModel::deleteUpload,
                        onClearAllCompleted = viewModel::clearCompleted
                    )
                }
                AppTab.ANALYTICS -> {
                    AnalyticsScreen(
                        completedFiles = completedUploads,
                        totalUploadedBytes = totalBytes
                    )
                }
                AppTab.SETTINGS -> {
                    SettingsScreen(
                        config = config,
                        onSaveConfig = viewModel::updateConfig
                    )
                }
            }
        }
    }

    // File Details Dialog
    selectedItemForDetails?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { viewModel.selectItemForDetails(null) },
            onDelete = { id ->
                viewModel.deleteUpload(id)
                viewModel.selectItemForDetails(null)
            }
        )
    }

    // New Upload Bottom Sheet
    if (showUploadSheet) {
        NewUploadBottomSheet(
            onDismiss = { viewModel.setShowUploadSheet(false) },
            onPickFiles = { category ->
                pendingUploadCategory = category
                try {
                    filePickerLauncher.launch(arrayOf("*/*"))
                } catch (e: Exception) {
                    singlePickerLauncher.launch("*/*")
                }
            },
            onSelectPreset = { presetType ->
                viewModel.enqueuePreset(presetType)
            }
        )
    }
}
