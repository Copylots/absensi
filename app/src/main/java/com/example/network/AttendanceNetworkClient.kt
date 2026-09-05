package com.example.network

import android.util.Log
import com.example.data.AttendanceRecord
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object AttendanceNetworkClient {
    private const val TAG = "AttendanceNetwork"
    private const val BASE_URL = "https://absensikaryawan-77f56-default-rtdb.firebaseio.com/absensi.json"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val recordAdapter = moshi.adapter(AttendanceRecord::class.java)

    /**
     * POSTs a single attendance record to Firebase.
     * Returns true if successful, false otherwise.
     */
    suspend fun postRecord(record: AttendanceRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            // Convert to JSON (excluding local Room id for Firebase payload)
            val payload = mapOf(
                "nama" to record.nama,
                "shift" to record.shift,
                "waktu" to record.waktu,
                "tanggal" to record.tanggal,
                "status" to record.status
            )
            val json = moshi.adapter(Map::class.java).toJson(payload)
            val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully posted record: ${record.nama}")
                    true
                } else {
                    Log.e(TAG, "Failed to post record. Code: ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting record: ${e.message}", e)
            false
        }
    }

    /**
     * GETs all attendance records from Firebase.
     * Handles both List and Map response shapes from Firebase dynamically.
     */
    suspend fun fetchRecords(): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(BASE_URL)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch records. Code: ${response.code}")
                    return@withContext emptyList()
                }

                val bodyString = response.body?.string() ?: return@withContext emptyList()
                if (bodyString.trim() == "null" || bodyString.isEmpty()) {
                    return@withContext emptyList()
                }

                val recordsList = mutableListOf<AttendanceRecord>()

                if (bodyString.trim().startsWith("[")) {
                    // It's a list response
                    val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                    val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
                    val rawList = adapter.fromJson(bodyString)
                    rawList?.forEach { map ->
                        val record = parseRecordFromMap(map)
                        if (record != null) {
                            recordsList.add(record)
                        }
                    }
                } else if (bodyString.trim().startsWith("{")) {
                    // It's a map response (ID -> Record)
                    val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Map::class.java)
                    val adapter = moshi.adapter<Map<String, Map<String, Any>>>(mapType)
                    val rawMap = adapter.fromJson(bodyString)
                    rawMap?.forEach { (_, map) ->
                        val record = parseRecordFromMap(map)
                        if (record != null) {
                            recordsList.add(record)
                        }
                    }
                }

                Log.d(TAG, "Successfully fetched ${recordsList.size} records")
                recordsList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching records: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseRecordFromMap(map: Map<String, Any>?): AttendanceRecord? {
        if (map == null) return null
        return try {
            AttendanceRecord(
                nama = map["nama"] as? String ?: "",
                shift = map["shift"] as? String ?: "",
                waktu = map["waktu"] as? String ?: "",
                tanggal = map["tanggal"] as? String ?: "",
                status = map["status"] as? String ?: "",
                isSynced = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing map to record: ${e.message}")
            null
        }
    }
}
