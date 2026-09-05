package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY waktu DESC")
    fun getAllRecordsFlow(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY waktu DESC")
    suspend fun getAllRecords(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE isSynced = 0")
    suspend fun getUnsyncedRecords(): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecord>)

    @Query("UPDATE attendance_records SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}
