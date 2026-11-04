package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.UploadCategory
import com.example.data.model.UploadItem
import com.example.data.repository.UploadRepository
import com.example.data.storage.CloudConfig
import com.example.data.storage.UploadEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UploadRepository

    init {
        val database = AppDatabase.getDatabase(application)
        val uploadEngine = UploadEngine(application)
        repository = UploadRepository(application, database.uploadDao(), uploadEngine)
    }

    val activeQueue: StateFlow<List<UploadItem>> = repository.activeQueue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedUploads: StateFlow<List<UploadItem>> = repository.completedUploads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalUploadedBytes: StateFlow<Long> = repository.totalUploadedBytes
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val completedCount: StateFlow<Int> = repository.completedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val config: StateFlow<CloudConfig> = repository.config

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(UploadCategory.ALL)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedItemForDetails = MutableStateFlow<UploadItem?>(null)
    val selectedItemForDetails = _selectedItemForDetails.asStateFlow()

    private val _showUploadSheet = MutableStateFlow(false)
    val showUploadSheet = _showUploadSheet.asStateFlow()

    val filteredCompletedFiles: StateFlow<List<UploadItem>> = combine(
        repository.completedUploads,
        _searchQuery,
        _selectedCategory
    ) { list, query, category ->
        list.filter { item ->
            val matchesCategory = (category == UploadCategory.ALL) || (item.category == category.name)
            val matchesQuery = query.isBlank() || 
                item.fileName.contains(query, ignoreCase = true) ||
                (item.cloudUrl?.contains(query, ignoreCase = true) == true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: UploadCategory) {
        _selectedCategory.value = category
    }

    fun setShowUploadSheet(show: Boolean) {
        _showUploadSheet.value = show
    }

    fun selectItemForDetails(item: UploadItem?) {
        _selectedItemForDetails.value = item
    }

    fun enqueueUri(uri: Uri, category: UploadCategory = UploadCategory.DOCUMENT) {
        viewModelScope.launch {
            repository.enqueueFromUri(uri, category)
        }
    }

    fun enqueuePreset(type: String) {
        viewModelScope.launch {
            repository.enqueuePreset(type)
        }
    }

    fun pauseUpload(id: String) {
        repository.pauseUpload(id)
    }

    fun resumeUpload(id: String) {
        repository.resumeUpload(id)
    }

    fun cancelUpload(id: String) {
        repository.cancelUpload(id)
    }

    fun retryUpload(id: String) {
        repository.retryUpload(id)
    }

    fun pauseAll() {
        repository.pauseAll()
    }

    fun resumeAll() {
        repository.resumeAll()
    }

    fun cancelAll() {
        repository.cancelAll()
    }

    fun deleteUpload(id: String) {
        viewModelScope.launch {
            repository.deleteUpload(id)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            repository.clearCompleted()
        }
    }

    fun updateConfig(config: CloudConfig) {
        repository.updateConfig(config)
    }
}
