package com.example.data.remote

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.util.Locale

object DownloadService {

    private val client: OkHttpClient = CobaltClient.defaultOkHttpClient

    interface ProgressListener {
        fun onProgress(bytesDownloaded: Long, totalBytes: Long, progress: Float, speed: String)
    }

    suspend fun downloadFile(
        context: Context,
        url: String,
        suggestedFileName: String?,
        isAudioOnly: Boolean,
        listener: ProgressListener
    ): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Server returned HTTP error code: ${response.code}")
        }

        val body = response.body ?: throw Exception("Response body is empty")
        val totalBytes = body.contentLength()
        val inputStream: InputStream = body.byteStream()

        // Extract filename from header, or URL, or use suggested name
        var fileName = suggestedFileName ?: getFileNameFromResponse(response, url)
        
        // Clean filename and ensure extension
        fileName = sanitizeFileName(fileName)
        val ext = if (isAudioOnly) "mp3" else "mp4"
        if (!fileName.endsWith(".$ext", ignoreCase = true)) {
            fileName = "$fileName.$ext"
        }

        val mimeType = if (isAudioOnly) "audio/mpeg" else "video/mp4"

        var outputStream: OutputStream? = null
        var fileUri: Uri? = null
        var localFile: File? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/InfinityDownloader")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val collectionUri = if (isAudioOnly) {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                fileUri = resolver.insert(collectionUri, contentValues) ?: throw Exception("Failed to insert MediaStore record")
                outputStream = resolver.openOutputStream(fileUri) ?: throw Exception("Failed to open output stream")
            } else {
                // Use Legacy Storage for older Android versions
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "InfinityDownloader"
                )
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                localFile = File(dir, fileName)
                outputStream = FileOutputStream(localFile)
            }

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var bytesDownloaded = 0L
            val startTime = System.currentTimeMillis()
            var lastUpdate = System.currentTimeMillis()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead

                val now = System.currentTimeMillis()
                // Update UI progress every 200ms to reduce database lock and UI performance lag
                if (now - lastUpdate >= 200 || bytesDownloaded == totalBytes) {
                    lastUpdate = now
                    val timeElapsedSec = (now - startTime) / 1000.0
                    val speedBytesPerSec = if (timeElapsedSec > 0) bytesDownloaded / timeElapsedSec else 0.0
                    val speedString = formatSpeed(speedBytesPerSec)
                    val progress = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
                    
                    listener.onProgress(bytesDownloaded, totalBytes, progress, speedString)
                }
            }

            outputStream.flush()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fileUri != null) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(fileUri, contentValues, null, null)
                
                // Scan the file to make it visible in MediaStore search immediately
                val realPath = getRealPathFromUri(context, fileUri)
                if (realPath != null) {
                    MediaScannerConnection.scanFile(context, arrayOf(realPath), arrayOf(mimeType), null)
                }
                return fileUri.toString()
            } else if (localFile != null) {
                // Scan the file
                MediaScannerConnection.scanFile(context, arrayOf(localFile.absolutePath), arrayOf(mimeType), null)
                return localFile.absolutePath
            } else {
                throw Exception("Failed to save downloaded file")
            }

        } catch (e: Exception) {
            // Clean up on failure
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fileUri != null) {
                    context.contentResolver.delete(fileUri, null, null)
                } else if (localFile != null && localFile.exists()) {
                    localFile.delete()
                }
            } catch (cleanupEx: Exception) {
                // ignore cleanup errors
            }
            throw e
        } finally {
            inputStream.close()
            outputStream?.close()
            body.close()
        }
    }

    private fun getFileNameFromResponse(response: Response, url: String): String {
        val contentDisposition = response.header("Content-Disposition")
        if (!contentDisposition.isNullOrEmpty()) {
            val filenamePattern = "filename\\*=UTF-8''(.+)".toRegex()
            val match = filenamePattern.find(contentDisposition)
            if (match != null) {
                return URLDecoder.decode(match.groupValues[1], "UTF-8")
            }
            val standardPattern = "filename=\"?([^\";]+)\"?".toRegex()
            val stdMatch = standardPattern.find(contentDisposition)
            if (stdMatch != null) {
                return stdMatch.groupValues[1]
            }
        }
        
        // Fallback to URL path segments
        try {
            val uri = Uri.parse(url)
            val path = uri.path
            if (!path.isNullOrEmpty()) {
                val lastSegment = path.substringAfterLast("/")
                if (lastSegment.isNotEmpty()) {
                    return URLDecoder.decode(lastSegment, "UTF-8")
                }
            }
        } catch (e: Exception) {
            // ignore parsing errors
        }

        return "video_${System.currentTimeMillis()}"
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun formatSpeed(bytesPerSec: Double): String {
        val kb = bytesPerSec / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f MB/s", mb)
        } else {
            String.format(Locale.getDefault(), "%.0f KB/s", kb)
        }
    }

    private fun getRealPathFromUri(context: Context, contentUri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val cursor = context.contentResolver.query(contentUri, projection, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                path = cursor.getString(columnIndex)
            }
            cursor.close()
        }
        return path
    }
}
