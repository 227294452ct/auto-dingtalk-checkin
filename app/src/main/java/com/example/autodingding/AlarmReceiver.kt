package com.example.autodingding

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
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

        // 啟動解鎖 Activity，後續的啟動釘釘和廣播打卡指令都由它處理
        launchUnlockActivity(context, alarmType)

        rescheduleNext(context)
    }

    // ---- helpers ----

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

    private fun launchUnlockActivity(context: Context, alarmType: Int) {
        val unlockIntent = Intent(context, UnlockHelperActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("type", alarmType)
        }
        context.startActivity(unlockIntent)
        LogManager.d(TAG, "UnlockHelperActivity launched, type=$alarmType")
    }
}
