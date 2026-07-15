package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DownloadTask
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<DownloadTask>>

    @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY createdAt ASC")
    fun getTasksByStatus(status: DownloadStatus): Flow<List<DownloadTask>>

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): DownloadTask?

    @Query("SELECT * FROM download_tasks WHERE status IN ('QUEUED', 'PARSING', 'DOWNLOADING') ORDER BY createdAt ASC")
    suspend fun getActiveQueue(): List<DownloadTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTask): Long

    @Update
    suspend fun updateTask(task: DownloadTask)

    @Delete
    suspend fun deleteTask(task: DownloadTask)

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    @Query("DELETE FROM download_tasks")
    suspend fun clearAll()
}
