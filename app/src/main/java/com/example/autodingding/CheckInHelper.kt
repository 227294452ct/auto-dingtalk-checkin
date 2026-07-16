package com.example.autodingding

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings

object CheckInHelper {

    private const val DINGDING_PACKAGE = "com.alibaba.android.rimet"
    private const val LAUNCH_DELAY_MS = 3000L

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        val packageName = context.packageName
        val serviceName = "$packageName/${DingDingAccessibilityService::class.java.name}"

        return enabledServices?.contains(serviceName) == true
    }

    fun requestAccessibility(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun startCheckIn(context: Context) {
        launchDingDing(context)
        Handler(Looper.getMainLooper()).postDelayed({
            val broadcastIntent = Intent("com.example.autodingding.START_CHECK_IN")
            context.sendBroadcast(broadcastIntent)
        }, LAUNCH_DELAY_MS)
    }

    fun launchDingDing(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(DINGDING_PACKAGE)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
