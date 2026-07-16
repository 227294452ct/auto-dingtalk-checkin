package com.example.autodingding

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.*

class AutoCheckInService : android.app.Service() {

    companion object {
        private const val TAG = "AutoCheckInService"
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_RESCHEDULE = "RESCHEDULE"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alarmManager: AlarmManager

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("AutoDingDing", MODE_PRIVATE)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_START -> {
                    startForegroundService()
                    scheduleAlarms()
                }
                ACTION_STOP -> {
                    cancelAlarms()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                ACTION_RESCHEDULE -> {
                    scheduleAlarms()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "AutoCheckInChannel")
            .setContentTitle(getString(R.string.service_channel_name))
            .setContentText(getString(R.string.status_running))
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "AutoCheckInChannel",
                getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.service_channel_description)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleAlarms() {
        cancelAlarms()

        val morningHour = sharedPreferences.getInt("morningHour", 9)
        val morningMinute = sharedPreferences.getInt("morningMinute", 0)
        val eveningHour = sharedPreferences.getInt("eveningHour", 18)
        val eveningMinute = sharedPreferences.getInt("eveningMinute", 0)

        scheduleAlarm(morningHour, morningMinute, 0)
        scheduleAlarm(eveningHour, eveningMinute, 1)

        LogManager.d(TAG, "Alarms scheduled: $morningHour:$morningMinute, $eveningHour:$eveningMinute")
    }

    private fun scheduleAlarm(hour: Int, minute: Int, requestCode: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("type", requestCode)
        }

        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, flag)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            @Suppress("DEPRECATION")
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarms() {
        for (requestCode in 0..1) {
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("type", requestCode)
            }
            val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, flag)
            alarmManager.cancel(pendingIntent)
        }
    }
}
