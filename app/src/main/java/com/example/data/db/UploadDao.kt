package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UploadItem
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadDao {
    @Query("SELECT * FROM upload_items ORDER BY createdAt DESC")
    fun getAllUploads(): Flow<List<UploadItem>>

    @Query("SELECT * FROM upload_items WHERE status IN ('QUEUED', 'UPLOADING', 'PAUSED') ORDER BY createdAt DESC")
    fun getActiveQueue(): Flow<List<UploadItem>>

    @Query("SELECT * FROM upload_items WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedUploads(): Flow<List<UploadItem>>

    @Query("SELECT * FROM upload_items WHERE id = :id LIMIT 1")
    suspend fun getUploadById(id: String): UploadItem?

    @Query("SELECT * FROM upload_items WHERE id = :id LIMIT 1")
    fun getUploadFlowById(id: String): Flow<UploadItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: UploadItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<UploadItem>)

    @Update
    suspend fun update(item: UploadItem)

    @Query("UPDATE upload_items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE upload_items SET bytesUploaded = :bytes, uploadSpeedBps = :speed, etaSeconds = :eta, status = :status WHERE id = :id")
    suspend fun updateProgress(id: String, bytes: Long, speed: Long, eta: Long, status: String)

    @Query("UPDATE upload_items SET bytesUploaded = :bytes, status = 'COMPLETED', completedAt = :completedAt, cloudUrl = :cloudUrl, sha256Checksum = :checksum WHERE id = :id")
    suspend fun markCompleted(id: String, bytes: Long, completedAt: Long, cloudUrl: String, checksum: String)

    @Query("UPDATE upload_items SET status = 'FAILED', errorMessage = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    @Delete
    suspend fun delete(item: UploadItem)

    @Query("DELETE FROM upload_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM upload_items WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()

    @Query("DELETE FROM upload_items")
    suspend fun deleteAll()

    @Query("SELECT SUM(fileSize) FROM upload_items WHERE status = 'COMPLETED'")
    fun getTotalUploadedBytes(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM upload_items WHERE status = 'COMPLETED'")
    fun getCompletedCount(): Flow<Int>
}
