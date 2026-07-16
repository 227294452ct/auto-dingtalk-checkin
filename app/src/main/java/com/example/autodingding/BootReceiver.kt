package com.example.autodingding

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPreferences = context.getSharedPreferences("AutoDingDing", Context.MODE_PRIVATE)
            val isRunning = sharedPreferences.getBoolean("isRunning", false)

            if (isRunning) {
                val serviceIntent = Intent(context, AutoCheckInService::class.java).apply {
                    action = AutoCheckInService.ACTION_START
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
