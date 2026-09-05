package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    /**
     * Schedules daily reminders for Shift 1 (06:45) and Shift 2 (18:45).
     */
    fun scheduleDailyReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Schedule Shift 1 Reminder (06:45 AM)
        scheduleAlarm(context, alarmManager, 1, 6, 45)

        // Schedule Shift 2 Reminder (18:45 PM / 06:45 PM)
        scheduleAlarm(context, alarmManager, 2, 18, 45)
    }

    private fun scheduleAlarm(context: Context, alarmManager: AlarmManager, requestId: Int, hour: Int, minute: Int) {
        val intent = Intent(context, AttendanceReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(TAG, "Scheduled alarm $requestId for ${hour}:${minute}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm $requestId: ${e.message}")
        }
    }
}
