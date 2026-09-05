package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "attendance_records")
@JsonClass(generateAdapter = true)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nama: String,
    val shift: String,
    val waktu: String,
    val tanggal: String,
    val status: String,
    val isSynced: Boolean = false
)
