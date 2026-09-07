package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "scanned_texts")
data class ScannedText(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val scannedFrom: String = "Camera" // "Camera" or "Gallery" or "Manual"
)
