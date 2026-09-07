package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedTextDao {
    @Query("SELECT * FROM scanned_texts ORDER BY timestamp DESC")
    fun getAllScannedTexts(): Flow<List<ScannedText>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedText(scannedText: ScannedText)

    @Delete
    suspend fun deleteScannedText(scannedText: ScannedText)

    @Query("DELETE FROM scanned_texts WHERE id = :id")
    suspend fun deleteScannedTextById(id: String)

    @Query("DELETE FROM scanned_texts")
    suspend fun deleteAllScannedTexts()
}
