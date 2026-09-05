package com.example

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainApplication : Application() {
    private val TAG = "MainApplication"
    private val applicationScope = CoroutineScope(SupervisorJob())

    lateinit var database: AppDatabase
        private set

    lateinit var repository: AttendanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Initializing MainApplication")

        // 1. Initialize Room Database and Repository
        database = AppDatabase.getDatabase(this)
        repository = AttendanceRepository(database.attendanceDao())

        // 2. Schedule Local Shift Reminder Alarms
        ReminderScheduler.scheduleDailyReminders(this)

        // 3. Register Network Connectivity Monitoring for Auto-Sync
        registerNetworkMonitoring()
    }

    private fun registerNetworkMonitoring() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "Internet is available! Triggering automatic sync of unsynced records...")
                applicationScope.launch {
                    try {
                        val syncedCount = repository.syncUnsyncedRecords()
                        if (syncedCount > 0) {
                            Log.d(TAG, "Auto-sync completed. Synced $syncedCount records offline -> online.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Auto-sync failed: ${e.message}")
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "Internet connection lost; app is currently running in offline mode.")
            }
        })
    }
}
