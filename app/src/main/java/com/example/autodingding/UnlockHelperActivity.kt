package com.example.autodingding

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.WindowManager

/**
 * 透明 Activity，用于在现代 Android 设备上正确唤醒屏幕并解除锁屏。
 * 替换 AlarmReceiver 中已废弃的 KeyguardLock.disableKeyguard() 方案。
 *
 * 流程：唤醒屏幕 → 显示在锁屏上方 → 解除锁屏 → 启动钉钉 → 广播打卡指令 → 自身 finish
 */
class UnlockHelperActivity : Activity() {

    companion object {
        private const val TAG = "UnlockHelperActivity"
        private const val WAKE_LOCK_TAG = "AutoDingDing:UnlockWakeLock"
        private const val WAKE_LOCK_TIMEOUT_MS = 10_000L
        private const val DINGDING_PACKAGE = "com.alibaba.android.rimet"
        private const val DINGDING_LAUNCH_DELAY_MS = 3000L
    }

    private var alarmType = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmType = intent.getIntExtra("type", -1)
        LogManager.d(TAG, "Started, alarmType=$alarmType")

        wakeUpScreen()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> dismissKeyguardModern()
            else -> dismissKeyguardLegacy()
        }
    }

    // ---- screen wake ----

    @Suppress("DEPRECATION")
    private fun wakeUpScreen() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                or PowerManager.ACQUIRE_CAUSES_WAKEUP
                or PowerManager.ON_AFTER_RELEASE,
            WAKE_LOCK_TAG
        )
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    // ---- keyguard dismissal ----

    private fun dismissKeyguardModern() {
        // 确保此 Activity 可以显示在锁屏上方
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        LogManager.d(TAG, "Requesting keyguard dismissal (API ${Build.VERSION.SDK_INT})")

        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                LogManager.i(TAG, "Keyguard dismissed")
                postKeyguardProceed()
            }

            override fun onDismissCancelled() {
                LogManager.w(TAG, "Keyguard dismiss cancelled, trying legacy fallback")
                dismissKeyguardLegacy()
            }

            override fun onDismissError() {
                LogManager.e(TAG, "Keyguard dismiss error, trying legacy fallback")
                dismissKeyguardLegacy()
            }
        })
    }

    @Suppress("DEPRECATION")
    private fun dismissKeyguardLegacy() {
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val lock = km.newKeyguardLock("AutoDingDing:LegacyUnlock")
            lock.disableKeyguard()
            LogManager.d(TAG, "Legacy dismissKeyguard attempted")
        } catch (e: Exception) {
            LogManager.w(TAG, "Legacy dismiss failed: ${e.message}")
        }
        postKeyguardProceed()
    }

    // ---- post-unlock workflow ----

    private fun postKeyguardProceed() {
        // 给系统一点时间完成锁屏到桌面的过渡
        Handler(Looper.getMainLooper()).postDelayed({
            launchDingDingAndCheckIn()
        }, 800L)
    }

    private fun launchDingDingAndCheckIn() {
        LogManager.d(TAG, "Launching DingDing and scheduling check-in")

        try {
            val intent = packageManager.getLaunchIntentForPackage(DINGDING_PACKAGE)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                LogManager.d(TAG, "DingDing launched")
            } else {
                LogManager.e(TAG, "DingDing not installed")
            }
        } catch (e: Exception) {
            LogManager.e(TAG, "Launch DingDing failed: ${e.message}")
        }

        // 等钉钉启动完成后广播打卡指令
        Handler(Looper.getMainLooper()).postDelayed({
            val broadcastIntent = Intent("com.example.autodingding.START_CHECK_IN")
            broadcastIntent.putExtra("type", alarmType)
            sendBroadcast(broadcastIntent)
            LogManager.d(TAG, "START_CHECK_IN broadcast sent, finishing")

            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, DINGDING_LAUNCH_DELAY_MS)
    }
}
