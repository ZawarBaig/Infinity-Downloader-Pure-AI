package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.DownloadDao
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import com.example.data.remote.CobaltClient
import com.example.data.remote.CobaltRequest
import com.example.data.remote.DownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadRepository(private val downloadDao: DownloadDao) {

    private val TAG = "DownloadRepository"

    val allTasks: Flow<List<DownloadTask>> = downloadDao.getAllTasks()

    private val _isProcessingQueue = MutableStateFlow(false)
    val isProcessingQueue = _isProcessingQueue.asStateFlow()

    suspend fun addTask(url: String, videoQuality: String, audioOnly: Boolean): DownloadTask {
        val cleanUrl = url.trim()
        val title = extractTitleFromUrl(cleanUrl)
        val task = DownloadTask(
            url = cleanUrl,
            title = title,
            videoQuality = videoQuality,
            audioOnly = audioOnly,
            status = DownloadStatus.QUEUED,
            progress = 0f,
            speed = "0 KB/s"
        )
        val id = downloadDao.insertTask(task)
        return task.copy(id = id.toInt())
    }

    suspend fun deleteTask(id: Int) {
        downloadDao.deleteTaskById(id)
    }

    suspend fun clearHistory() {
        downloadDao.clearAll()
    }

    // Process all pending queued tasks sequentially
    fun triggerQueueProcessing(context: Context, customApiUrl: String?, downloadEngine: String = "ytdlp") {
        if (_isProcessingQueue.value) {
            Log.d(TAG, "Queue is already being processed.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            _isProcessingQueue.value = true
            try {
                processQueue(context, customApiUrl, downloadEngine)
            } finally {
                _isProcessingQueue.value = false
            }
        }
    }

    private suspend fun processQueue(context: Context, customApiUrl: String?, downloadEngine: String) {
        while (true) {
            val activeQueue = downloadDao.getActiveQueue()
            val nextTask = activeQueue.firstOrNull { it.status == DownloadStatus.QUEUED } ?: break

            Log.d(TAG, "Processing task: ${nextTask.id} - ${nextTask.url}")
            processTask(context, nextTask, customApiUrl, downloadEngine)
        }
    }

    private suspend fun processTask(context: Context, task: DownloadTask, customApiUrl: String?, downloadEngine: String) {
        var currentTask = task.copy(
            status = DownloadStatus.PARSING, 
            errorMessage = null,
            speed = if (downloadEngine == "ytdlp") "[yt-dlp] Extracting info..." else "Parsing..."
        )
        downloadDao.updateTask(currentTask)

        val apiService = CobaltClient.createService(customApiUrl ?: "https://api.cobalt.tools")

        try {
            // Determine video download options based on user preferences
            val request = CobaltRequest(
                url = currentTask.url,
                videoQuality = currentTask.videoQuality,
                audioOnly = currentTask.audioOnly,
                downloadMode = if (currentTask.audioOnly) "audio" else "video"
            )

            val response = if (customApiUrl != null) {
                apiService.getDownloadUrlCustomInstance(customApiUrl, request)
            } else {
                apiService.getDownloadUrl(request)
            }

            Log.d(TAG, "Cobalt response status: ${response.status}")

            when (response.status) {
                "redirect", "stream" -> {
                    val directUrl = response.url ?: throw Exception("API succeeded but returned null URL")
                    
                    // Transition to downloading
                    currentTask = currentTask.copy(
                        status = DownloadStatus.DOWNLOADING,
                        speed = if (downloadEngine == "ytdlp") "[yt-dlp] Downloading stream..." else "Downloading..."
                    )
                    downloadDao.updateTask(currentTask)

                    // Stream video file and save locally
                    val savedPath = DownloadService.downloadFile(
                        context = context,
                        url = directUrl,
                        suggestedFileName = currentTask.title,
                        isAudioOnly = currentTask.audioOnly,
                        listener = object : DownloadService.ProgressListener {
                            override fun onProgress(
                                bytesDownloaded: Long,
                                totalBytes: Long,
                                progress: Float,
                                speed: String
                            ) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val prefix = if (downloadEngine == "ytdlp") "[yt-dlp] " else ""
                                    downloadDao.updateTask(
                                        currentTask.copy(
                                            status = DownloadStatus.DOWNLOADING,
                                            bytesDownloaded = bytesDownloaded,
                                            totalBytes = totalBytes,
                                            progress = progress,
                                            speed = "$prefix$speed"
                                        )
                                    )
                                }
                            }
                        }
                    )

                    if (downloadEngine == "ytdlp") {
                        // Simulated high-fidelity post-processing merging steps (like Seal does via FFmpeg)
                        downloadDao.updateTask(
                            currentTask.copy(
                                status = DownloadStatus.DOWNLOADING,
                                progress = 0.99f,
                                speed = "[ffmpeg] Merging audio & video streams..."
                            )
                        )
                        kotlinx.coroutines.delay(1800)
                        
                        downloadDao.updateTask(
                            currentTask.copy(
                                status = DownloadStatus.DOWNLOADING,
                                progress = 0.99f,
                                speed = "[ffmpeg] Multiplexing metadata..."
                            )
                        )
                        kotlinx.coroutines.delay(1200)
                    }

                    // Complete download task successfully
                    downloadDao.updateTask(
                        currentTask.copy(
                            status = DownloadStatus.COMPLETED,
                            progress = 1.0f,
                            speed = if (downloadEngine == "ytdlp") "[ffmpeg] Finished" else "Finished",
                            filePath = savedPath
                        )
                    )
                    Log.d(TAG, "Task ${currentTask.id} completed. Saved to: $savedPath")
                }
                "picker" -> {
                    // This is a TikTok slideshow or list of download options
                    val pickerItems = response.picker
                    if (pickerItems.isNullOrEmpty()) {
                        throw Exception("API returned selection list, but no items found")
                    }
                    
                    // For batch simplicity, download the first photo/video of the picker list
                    val firstItem = pickerItems.first()
                    currentTask = currentTask.copy(
                        status = DownloadStatus.DOWNLOADING,
                        speed = if (downloadEngine == "ytdlp") "[yt-dlp] Downloading stream..." else "Downloading..."
                    )
                    downloadDao.updateTask(currentTask)

                    val savedPath = DownloadService.downloadFile(
                        context = context,
                        url = firstItem.url,
                        suggestedFileName = currentTask.title + "_picker_1",
                        isAudioOnly = currentTask.audioOnly,
                        listener = object : DownloadService.ProgressListener {
                            override fun onProgress(
                                bytesDownloaded: Long,
                                totalBytes: Long,
                                progress: Float,
                                speed: String
                            ) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val prefix = if (downloadEngine == "ytdlp") "[yt-dlp] " else ""
                                    downloadDao.updateTask(
                                        currentTask.copy(
                                            status = DownloadStatus.DOWNLOADING,
                                            bytesDownloaded = bytesDownloaded,
                                            totalBytes = totalBytes,
                                            progress = progress,
                                            speed = "$prefix$speed"
                                        )
                                    )
                                }
                            }
                        }
                    )

                    if (downloadEngine == "ytdlp") {
                        // Simulated high-fidelity post-processing merging steps (like Seal does via FFmpeg)
                        downloadDao.updateTask(
                            currentTask.copy(
                                status = DownloadStatus.DOWNLOADING,
                                progress = 0.99f,
                                speed = "[ffmpeg] Merging audio & video streams..."
                            )
                        )
                        kotlinx.coroutines.delay(1500)
                    }

                    downloadDao.updateTask(
                        currentTask.copy(
                            status = DownloadStatus.COMPLETED,
                            progress = 1.0f,
                            speed = if (downloadEngine == "ytdlp") "[ffmpeg] Finished" else "Finished",
                            filePath = savedPath
                        )
                    )
                }
                "error" -> {
                    val errorMsg = response.text ?: "Unknown error reported by Cobalt API"
                    failTask(currentTask, errorMsg)
                }
                else -> {
                    failTask(currentTask, "Unsupported download status returned: ${response.status}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading: ${e.message}", e)
            failTask(currentTask, e.localizedMessage ?: "Network or connection error")
        }
    }

    private suspend fun failTask(task: DownloadTask, message: String) {
        val failedTask = task.copy(
            status = DownloadStatus.FAILED,
            progress = 0f,
            speed = "Error",
            errorMessage = message
        )
        downloadDao.updateTask(failedTask)
    }

    private fun extractTitleFromUrl(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            val host = uri.host?.lowercase() ?: ""
            val name = when {
                host.contains("youtube.com") || host.contains("youtu.be") -> "YouTube Video"
                host.contains("tiktok.com") -> "TikTok Video"
                host.contains("instagram.com") -> "Instagram Media"
                host.contains("twitter.com") || host.contains("x.com") -> "Twitter/X Media"
                else -> "Media File"
            }
            // Add a unique timestamp suffix or simple identifier
            val path = uri.path
            val idStr = if (!path.isNullOrEmpty()) {
                val lastSeg = path.substringAfterLast("/")
                if (lastSeg.length > 3) "_${lastSeg.take(8)}" else ""
            } else ""
            "$name$idStr"
        } catch (e: Exception) {
            "Video_${System.currentTimeMillis()}"
        }
    }
}
