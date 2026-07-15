package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class DownloadViewModel(private val repository: DownloadRepository) : ViewModel() {

    // Inputs
    private val _inputUrl = MutableStateFlow("")
    val inputUrl = _inputUrl.asStateFlow()

    private val _batchUrls = MutableStateFlow("")
    val batchUrls = _batchUrls.asStateFlow()

    private val _selectedQuality = MutableStateFlow("720") // "max", "1080", "720", "480", "360"
    val selectedQuality = _selectedQuality.asStateFlow()

    private val _audioOnly = MutableStateFlow(false)
    val audioOnly = _audioOnly.asStateFlow()

    private val _customApiUrl = MutableStateFlow("https://api.cobalt.tools")
    val customApiUrl = _customApiUrl.asStateFlow()

    private val _downloadEngine = MutableStateFlow("ytdlp") // "ytdlp" or "cobalt"
    val downloadEngine = _downloadEngine.asStateFlow()

    private val _showBatchMode = MutableStateFlow(false)
    val showBatchMode = _showBatchMode.asStateFlow()

    // Task Streams
    val allTasks: StateFlow<List<DownloadTask>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeDownloads: StateFlow<List<DownloadTask>> = allTasks
        .map { tasks ->
            tasks.filter {
                it.status == DownloadStatus.QUEUED ||
                it.status == DownloadStatus.PARSING ||
                it.status == DownloadStatus.DOWNLOADING
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedDownloads: StateFlow<List<DownloadTask>> = allTasks
        .map { tasks ->
            tasks.filter {
                it.status == DownloadStatus.COMPLETED ||
                it.status == DownloadStatus.FAILED
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isProcessingQueue = repository.isProcessingQueue

    init {
        // Automatically start processing if there are queued items from a previous session
        // triggered via first UI frame
    }

    fun setInputUrl(url: String) {
        _inputUrl.value = url
    }

    fun setBatchUrls(urls: String) {
        _batchUrls.value = urls
    }

    fun setSelectedQuality(quality: String) {
        _selectedQuality.value = quality
    }

    fun setAudioOnly(audio: Boolean) {
        _audioOnly.value = audio
    }

    fun setCustomApiUrl(url: String) {
        _customApiUrl.value = url
    }

    fun setDownloadEngine(engine: String) {
        _downloadEngine.value = engine
    }

    fun toggleBatchMode() {
        _showBatchMode.value = !_showBatchMode.value
    }

    // Add a single task
    fun queueDownload(context: Context) {
        val url = _inputUrl.value.trim()
        if (url.isEmpty()) {
            Toast.makeText(context, "Please enter or paste a valid URL", Toast.LENGTH_SHORT).show()
            return
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(context, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            repository.addTask(
                url = url,
                videoQuality = _selectedQuality.value,
                audioOnly = _audioOnly.value
            )
            _inputUrl.value = "" // clear input
            Toast.makeText(context, "Added to download queue", Toast.LENGTH_SHORT).show()
            
            // Start queue processing
            val api = if (_customApiUrl.value.trim().isNotEmpty()) _customApiUrl.value.trim() else null
            repository.triggerQueueProcessing(context.applicationContext, api, _downloadEngine.value)
        }
    }

    // Add multiple tasks in a batch
    fun queueBatchDownloads(context: Context) {
        val urlsText = _batchUrls.value.trim()
        if (urlsText.isEmpty()) {
            Toast.makeText(context, "Please enter one or more URLs", Toast.LENGTH_SHORT).show()
            return
        }

        val lines = urlsText.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() && (it.startsWith("http://") || it.startsWith("https://")) }

        if (lines.isEmpty()) {
            Toast.makeText(context, "No valid HTTP/HTTPS URLs found", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            var addedCount = 0
            for (url in lines) {
                repository.addTask(
                    url = url,
                    videoQuality = _selectedQuality.value,
                    audioOnly = _audioOnly.value
                )
                addedCount++
            }
            _batchUrls.value = "" // clear input
            _showBatchMode.value = false // back to single mode
            Toast.makeText(context, "Added $addedCount tasks to queue", Toast.LENGTH_SHORT).show()

            // Start queue processing
            val api = if (_customApiUrl.value.trim().isNotEmpty()) _customApiUrl.value.trim() else null
            repository.triggerQueueProcessing(context.applicationContext, api, _downloadEngine.value)
        }
    }

    fun triggerQueueIfNeeded(context: Context) {
        if (!isProcessingQueue.value && activeDownloads.value.any { it.status == DownloadStatus.QUEUED }) {
            val api = if (_customApiUrl.value.trim().isNotEmpty()) _customApiUrl.value.trim() else null
            repository.triggerQueueProcessing(context.applicationContext, api, _downloadEngine.value)
        }
    }

    fun cancelTask(id: Int) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    fun clearTaskHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun playVideo(context: Context, task: DownloadTask) {
        val filePath = task.filePath ?: return
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (filePath.startsWith("content://")) {
                // MediaStore URI
                val uri = Uri.parse(filePath)
                intent.setDataAndType(uri, context.contentResolver.getType(uri) ?: "video/*")
            } else {
                // File Path (Legacy)
                val file = File(filePath)
                if (!file.exists()) {
                    Toast.makeText(context, "File does not exist or has been deleted", Toast.LENGTH_SHORT).show()
                    return
                }
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                intent.setDataAndType(uri, if (task.audioOnly) "audio/*" else "video/*")
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No application found to open this media file", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareVideo(context: Context, task: DownloadTask) {
        val filePath = task.filePath ?: return
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (task.audioOnly) "audio/*" else "video/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                intent.putExtra(Intent.EXTRA_STREAM, uri)
            } else {
                val file = File(filePath)
                if (!file.exists()) {
                    Toast.makeText(context, "File does not exist or has been deleted", Toast.LENGTH_SHORT).show()
                    return
                }
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                intent.putExtra(Intent.EXTRA_STREAM, uri)
            }

            context.startActivity(Intent.createChooser(intent, "Share Media File"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share media file", Toast.LENGTH_SHORT).show()
        }
    }
}

class DownloadViewModelFactory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DownloadViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
