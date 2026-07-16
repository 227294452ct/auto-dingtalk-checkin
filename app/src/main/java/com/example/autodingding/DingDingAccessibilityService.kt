package com.example.autodingding

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat

class DingDingAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DingDingService"
        private const val PREF_NAME = "AutoDingDing"
        private const val CHECK_IN_TIMEOUT_MS = 60_000L
        private const val MAX_ATTEMPTS = 5
        private const val DELAY_PAGE_LOAD_MS = 2500L
        private const val DELAY_BUTTON_CLICK_MS = 2000L
        private const val DELAY_AFTER_BACK_MS = 1500L
        private const val RETRY_DELAY_MS = 2000L
        private const val DINGDING_PACKAGE = "com.alibaba.android.rimet"
        private const val NOTIFICATION_CHANNEL_ID = "check_in_failure"
        private const val NOTIFICATION_CHANNEL_NAME = "打卡失败通知"

        private const val TEXT_WORKBENCH = "工作台"
        private const val TEXT_MESSAGES = "消息"
        private const val TEXT_CONTACTS = "联系人"
        private const val TEXT_MY = "我的"
        private const val TEXT_CHECK_IN = "打卡"
        private const val TEXT_MORNING_CHECK_IN = "上班打卡"
        private const val TEXT_EVENING_CHECK_IN = "下班打卡"
        private const val TEXT_SIGN_IN = "签到"
        private const val TEXT_CHECK_IN_NOW = "立即打卡"
        private const val TEXT_SUCCESS = "打卡成功"
        private const val TEXT_SIGN_SUCCESS = "签到成功"
        private const val TEXT_ALREADY_CHECKED = "已打卡"
        private const val TEXT_CONFIRM = "确认"
        private const val TEXT_CONFIRM_ALT = "确定"
        private const val TEXT_BACK = "返回"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private var isCheckingIn = false
    private var checkInStartTime = 0L
    private var checkInType = -1
    private val handler = Handler(Looper.getMainLooper())
    private var checkInAttempts = 0

    private val checkInReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.autodingding.START_CHECK_IN") {
                checkInType = intent.getIntExtra("type", -1)
                startCheckIn()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        registerReceiver(checkInReceiver, IntentFilter("com.example.autodingding.START_CHECK_IN"))
        createNotificationChannel()
        LogManager.i(TAG, "Accessibility service created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isCheckingIn) return

        val pkg = event.packageName?.toString()
        if (pkg != DINGDING_PACKAGE) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handler.postDelayed({ performAutoCheckIn() }, DELAY_PAGE_LOAD_MS)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        stopCheckIn()
        try { unregisterReceiver(checkInReceiver) } catch (_: Exception) {}
        LogManager.i(TAG, "Accessibility service destroyed")
    }

    fun startCheckIn() {
        isCheckingIn = true
        checkInAttempts = 0
        checkInStartTime = System.currentTimeMillis()
        LogManager.i(TAG, "Check-in started: type=$checkInType")
        performAutoCheckIn()
    }

    fun stopCheckIn() {
        isCheckingIn = false
        checkInAttempts = 0
        checkInStartTime = 0L
        checkInType = -1
        handler.removeCallbacksAndMessages(null)
    }

    private fun performAutoCheckIn() {
        if (!isCheckingIn) return

        if (System.currentTimeMillis() - checkInStartTime > CHECK_IN_TIMEOUT_MS) {
            LogManager.w(TAG, "Check-in timed out")
            finishCheckIn(false, "超时")
            return
        }

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            retryCheckIn("无法获取窗口内容")
            return
        }

        try {
            when {
                isOnHomePage(rootNode) -> navigateToCheckIn(rootNode)
                isOnCheckInSuccessPage(rootNode) -> finishCheckIn(true, null)
                isOnCheckInPage(rootNode) -> clickCheckInButton(rootNode)
                else -> navigateToHome(rootNode)
            }
        } catch (e: Exception) {
            LogManager.e(TAG, "Auto check-in error: ${e.message}")
            retryCheckIn("异常: ${e.message}")
        } finally {
            rootNode.recycle()
        }
    }

    private fun isOnHomePage(node: AccessibilityNodeInfo): Boolean {
        return findNodeByText(node, TEXT_WORKBENCH) != null ||
                findNodeByText(node, TEXT_MESSAGES) != null ||
                findNodeByText(node, TEXT_CONTACTS) != null ||
                findNodeByText(node, TEXT_MY) != null
    }

    private fun isOnCheckInPage(node: AccessibilityNodeInfo): Boolean {
        return findNodeByText(node, TEXT_MORNING_CHECK_IN) != null ||
                findNodeByText(node, TEXT_EVENING_CHECK_IN) != null ||
                findNodeByText(node, TEXT_SIGN_IN) != null
    }

    private fun isOnCheckInSuccessPage(node: AccessibilityNodeInfo): Boolean {
        return findNodeByText(node, TEXT_SUCCESS) != null ||
                findNodeByText(node, TEXT_SIGN_SUCCESS) != null ||
                findNodeByText(node, TEXT_ALREADY_CHECKED) != null
    }

    private fun navigateToCheckIn(node: AccessibilityNodeInfo) {
        LogManager.d(TAG, "Navigating to check-in page")
        val workbenchNode = findNodeByText(node, TEXT_WORKBENCH)
        workbenchNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        workbenchNode?.recycle()

        handler.postDelayed({
            val root = rootInActiveWindow ?: return@postDelayed
            try {
                val checkInNode = findNodeByText(root, TEXT_CHECK_IN)
                checkInNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                checkInNode?.recycle()
            } finally {
                root.recycle()
            }
        }, DELAY_PAGE_LOAD_MS + 500L)
    }

    private fun clickCheckInButton(node: AccessibilityNodeInfo) {
        val targetText = when (checkInType) {
            0 -> TEXT_MORNING_CHECK_IN
            1 -> TEXT_EVENING_CHECK_IN
            else -> null
        }

        val checkInBtn = if (targetText != null) {
            findNodeByText(node, targetText)
        } else {
            null
        } ?: findNodeByText(node, TEXT_SIGN_IN)
          ?: findNodeByText(node, TEXT_CHECK_IN_NOW)

        if (checkInBtn == null) {
            retryCheckIn("未找到打卡按钮")
            return
        }

        LogManager.d(TAG, "Clicking check-in button: ${checkInBtn.text}")
        checkInBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        checkInBtn.recycle()

        handler.postDelayed({
            val root = rootInActiveWindow ?: return@postDelayed
            try {
                val confirmBtn = findNodeByText(root, TEXT_CONFIRM)
                    ?: findNodeByText(root, TEXT_CONFIRM_ALT)
                confirmBtn?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                confirmBtn?.recycle()
            } finally {
                root.recycle()
            }
        }, DELAY_BUTTON_CLICK_MS)

        handler.postDelayed({
            if (isCheckingIn) {
                checkInAttempts++
                if (checkInAttempts < MAX_ATTEMPTS) {
                    performAutoCheckIn()
                } else {
                    finishCheckIn(false, "重试${MAX_ATTEMPTS}次后仍未成功")
                }
            }
        }, DELAY_PAGE_LOAD_MS)
    }

    private fun navigateToHome(node: AccessibilityNodeInfo) {
        LogManager.d(TAG, "Navigating back to home")
        val backBtn = findNodeByContentDesc(node, TEXT_BACK)
            ?: findNodeByText(node, TEXT_BACK)
        if (backBtn != null) {
            backBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            backBtn.recycle()
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        handler.postDelayed({
            if (isCheckingIn) performAutoCheckIn()
        }, DELAY_AFTER_BACK_MS)
    }

    private fun finishCheckIn(success: Boolean, reason: String?) {
        if (!isCheckingIn) return
        isCheckingIn = false

        val now = System.currentTimeMillis()
        val typeName = when (checkInType) { 0 -> "上班" 1 -> "下班" else -> "未知" }

        if (success) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val dedupKey = when (checkInType) {
                0 -> "lastCheckInDate_morning"
                1 -> "lastCheckInDate_evening"
                else -> null
            }
            if (dedupKey != null) {
                sharedPreferences.edit().putString(dedupKey, today).apply()
            }

            CheckInRecordManager.save(this,
                CheckInRecordManager.CheckInRecord(
                    date = today,
                    scheduledTime = "",
                    type = typeName,
                    success = true,
                    actualTime = now,
                    failureReason = null
                )
            )

            sharedPreferences.edit().putLong("lastCheckInTime", now).apply()
            sendBroadcast(Intent("com.example.autodingding.CHECK_IN_SUCCESS"))
            LogManager.i(TAG, "Check-in succeeded: $typeName")
        } else {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            CheckInRecordManager.save(this,
                CheckInRecordManager.CheckInRecord(
                    date = today,
                    scheduledTime = "",
                    type = typeName,
                    success = false,
                    actualTime = now,
                    failureReason = reason
                )
            )
            LogManager.w(TAG, "Check-in failed: $reason")

            NotificationHelper.notifyFailure(this, typeName, reason ?: "未知原因")
        }

        checkInAttempts = 0
        checkInStartTime = 0L
        checkInType = -1
        handler.removeCallbacksAndMessages(null)
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun retryCheckIn(reason: String) {
        checkInAttempts++
        if (checkInAttempts < MAX_ATTEMPTS && isCheckingIn) {
            LogManager.d(TAG, "Retrying... attempt $checkInAttempts/$MAX_ATTEMPTS, reason: $reason")
            handler.postDelayed({ performAutoCheckIn() }, RETRY_DELAY_MS)
        } else {
            finishCheckIn(false, reason)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "打卡失败时发送通知"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun findNodeByText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.contains(text) == true) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, text)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeByContentDesc(node: AccessibilityNodeInfo?, desc: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.contentDescription?.contains(desc) == true) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByContentDesc(child, desc)
            if (result != null) return result
        }
        return null
    }
}
