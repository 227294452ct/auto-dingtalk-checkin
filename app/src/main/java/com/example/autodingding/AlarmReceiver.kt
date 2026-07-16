package com.example.autodingding

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val DINGDING_LAUNCH_DELAY_MS = 3000L
        private const val WAKE_LOCK_TAG = "AutoDingDing:AlarmWakeLock"
        private const val WAKE_LOCK_TIMEOUT_MS = 10_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        LogManager.d(TAG, "Alarm received")
        val alarmType = intent.getIntExtra("type", -1)

        if (!HolidayManager.isWorkday()) {
            LogManager.d(TAG, "Non-workday, skipping check-in")
            rescheduleNext(context)
            return
        }

        if (isAlreadyCheckedInToday(context, alarmType)) {
            LogManager.d(TAG, "Already checked in today for type $alarmType, skipping")
            rescheduleNext(context)
            return
        }

        if (!CheckInHelper.isAccessibilityEnabled(context)) {
            LogManager.e(TAG, "Accessibility not enabled")
            rescheduleNext(context)
            return
        }

        wakeAndUnlock(context)

        val launched = CheckInHelper.launchDingDing(context)
        if (!launched) {
            LogManager.e(TAG, "Failed to launch DingDing")
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val broadcastIntent = Intent("com.example.autodingding.START_CHECK_IN")
            broadcastIntent.putExtra("type", alarmType)
            context.sendBroadcast(broadcastIntent)
        }, DINGDING_LAUNCH_DELAY_MS)

        rescheduleNext(context)
    }

    private fun rescheduleNext(context: Context) {
        val rescheduleIntent = Intent(context, AutoCheckInService::class.java).apply {
            action = AutoCheckInService.ACTION_RESCHEDULE
        }
        context.startForegroundService(rescheduleIntent)
    }

    private fun isAlreadyCheckedInToday(context: Context, type: Int): Boolean {
        val prefs = context.getSharedPreferences("AutoDingDing", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val key = when (type) {
            0 -> "lastCheckInDate_morning"
            1 -> "lastCheckInDate_evening"
            else -> return false
        }
        val storedDate = prefs.getString(key, null)
        return storedDate == today
    }

    @Suppress("DEPRECATION")
    private fun wakeAndUnlock(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            WAKE_LOCK_TAG
        )
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)

        try {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val lock = km.newKeyguardLock(WAKE_LOCK_TAG)
            lock.disableKeyguard()
        } catch (e: Exception) {
            LogManager.w(TAG, "Failed to dismiss keyguard: ${e.message}")
        }

        LogManager.d(TAG, "Screen woke and keyguard dismissed")
    }
}
