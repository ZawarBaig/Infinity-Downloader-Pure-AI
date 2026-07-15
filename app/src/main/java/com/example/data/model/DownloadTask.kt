package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

enum class DownloadStatus {
    QUEUED,      // Added to download queue
    PARSING,     // Querying cobalt api for download link
    DOWNLOADING, // Downloading file in progress
    COMPLETED,   // Saved to external storage
    FAILED       // Encountered error
}

@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val videoQuality: String = "720",
    val audioOnly: Boolean = false,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f, // 0.0 to 1.0
    val speed: String = "0 KB/s",
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
