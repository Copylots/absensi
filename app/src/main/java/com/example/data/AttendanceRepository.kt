package com.example.data

import android.util.Log
import com.example.network.AttendanceNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AttendanceRepository(private val attendanceDao: AttendanceDao) {
    private val TAG = "AttendanceRepository"

    val allRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecordsFlow()

    /**
     * Inserts an attendance record locally.
     * Then immediately attempts to upload it to Firebase.
     */
    suspend fun insertRecord(record: AttendanceRecord): Boolean = withContext(Dispatchers.IO) {
        // 1. Insert into local Room database with isSynced = false
        val insertedId = attendanceDao.insertRecord(record)
        Log.d(TAG, "Inserted locally with ID: $insertedId")

        // 2. Try to upload to Firebase immediately
        val localRecord = record.copy(id = insertedId.toInt())
        val isSuccessful = AttendanceNetworkClient.postRecord(localRecord)
        if (isSuccessful) {
            attendanceDao.markAsSynced(insertedId.toInt())
            Log.d(TAG, "Successfully synced record $insertedId immediately")
            true
        } else {
            Log.d(TAG, "Failed to sync record $insertedId immediately; kept offline")
            false
        }
    }

    /**
     * Scans for any unsynced local records and attempts to upload them.
     */
    suspend fun syncUnsyncedRecords(): Int = withContext(Dispatchers.IO) {
        val unsynced = attendanceDao.getUnsyncedRecords()
        Log.d(TAG, "Found ${unsynced.size} unsynced records to upload")
        var syncCount = 0

        for (record in unsynced) {
            val isSuccessful = AttendanceNetworkClient.postRecord(record)
            if (isSuccessful) {
                attendanceDao.markAsSynced(record.id)
                syncCount++
            }
        }
        syncCount
    }

    /**
     * Refreshes local Room cache by fetching all records from Firebase and saving them.
     */
    suspend fun refreshRecords(): Boolean = withContext(Dispatchers.IO) {
        try {
            val remoteRecords = AttendanceNetworkClient.fetchRecords()
            if (remoteRecords.isNotEmpty()) {
                // To avoid duplicate/stale records from merging incorrectly,
                // we can optionally clear and insert, or use onConflict = REPLACE.
                // Since Firebase is our source of truth for remote records, let's merge them!
                // We don't want to lose unsynced local records, so we preserve them.
                val unsynced = attendanceDao.getUnsyncedRecords()
                
                attendanceDao.clearAll()
                
                // Re-insert unsynced records
                attendanceDao.insertRecords(unsynced)
                
                // Insert fetched remote records (which are already synced)
                val remoteToInsert = remoteRecords.map { it.copy(isSynced = true) }
                attendanceDao.insertRecords(remoteToInsert)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing records: ${e.message}")
            false
        }
    }
}
