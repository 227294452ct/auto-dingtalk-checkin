package com.example.autodingding

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnAccessibility: MaterialButton
    private lateinit var btnStart: MaterialButton
    private lateinit var btnMorningTime: MaterialButton
    private lateinit var btnEveningTime: MaterialButton
    private lateinit var btnHistory: MaterialButton
    private lateinit var btnSaveNotification: MaterialButton
    private lateinit var btnCheckInNow: MaterialButton
    private lateinit var btnLogViewer: MaterialButton
    private lateinit var tvStatus: TextView
    private lateinit var tvLastCheckIn: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var indicatorStatus: View
    private lateinit var etNotificationPhone: EditText
    private lateinit var etNotificationEmail: EditText
    private lateinit var sharedPreferences: SharedPreferences
    private var morningHour = 9
    private var morningMinute = 0
    private var eveningHour = 18
    private var eveningMinute = 0

    private val checkInSuccessReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateLastCheckIn()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("AutoDingDing", MODE_PRIVATE)

        btnAccessibility = findViewById(R.id.btn_accessibility)
        btnStart = findViewById(R.id.btn_start)
        btnMorningTime = findViewById(R.id.btn_morning_time)
        btnEveningTime = findViewById(R.id.btn_evening_time)
        btnHistory = findViewById(R.id.btn_history)
        btnSaveNotification = findViewById(R.id.btn_save_notification)
        btnCheckInNow = findViewById(R.id.btn_check_in_now)
        btnLogViewer = findViewById(R.id.btn_log_viewer)
        tvStatus = findViewById(R.id.tv_status)
        tvLastCheckIn = findViewById(R.id.tv_last_check_in)
        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status)
        indicatorStatus = findViewById(R.id.indicator_status)
        etNotificationPhone = findViewById(R.id.et_notification_phone)
        etNotificationEmail = findViewById(R.id.et_notification_email)

        morningHour = sharedPreferences.getInt("morningHour", 9)
        morningMinute = sharedPreferences.getInt("morningMinute", 0)
        eveningHour = sharedPreferences.getInt("eveningHour", 18)
        eveningMinute = sharedPreferences.getInt("eveningMinute", 0)

        updateTimeButtonTexts()
        updateLastCheckIn()
        updateStatus()
        updateAccessibilityButton()
        loadNotificationSettings()

        btnAccessibility.setOnClickListener {
            CheckInHelper.requestAccessibility(this)
        }

        btnMorningTime.setOnClickListener {
            showTimePicker(DayPeriod.MORNING)
        }

        btnEveningTime.setOnClickListener {
            showTimePicker(DayPeriod.EVENING)
        }

        btnStart.setOnClickListener {
            if (!CheckInHelper.isAccessibilityEnabled(this)) {
                CheckInHelper.requestAccessibility(this)
                return@setOnClickListener
            }

            if (isRunning()) {
                stopAutoCheckIn()
            } else {
                startAutoCheckIn()
            }
        }

        btnCheckInNow.setOnClickListener {
            if (!CheckInHelper.isAccessibilityEnabled(this)) {
                CheckInHelper.requestAccessibility(this)
                return@setOnClickListener
            }
            triggerManualCheckIn()
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnLogViewer.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        btnSaveNotification.setOnClickListener {
            saveNotificationSettings()
        }

        registerReceiver(checkInSuccessReceiver, IntentFilter("com.example.autodingding.CHECK_IN_SUCCESS"))
        LogManager.i("MainActivity", "MainActivity created")
    }

    private enum class DayPeriod { MORNING, EVENING }

    private fun triggerManualCheckIn() {
        val options = arrayOf(getString(R.string.morning_time), getString(R.string.evening_time))
        AlertDialog.Builder(this)
            .setTitle(R.string.check_in_now)
            .setItems(options) { _, which ->
                val type = which // 0=上班, 1=下班
                LogManager.i("MainActivity", "Manual check-in triggered: type=$type")
                CheckInHelper.launchDingDing(this)
                // 发送打卡广播，与 AlarmReceiver 保持一致
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val broadcastIntent = Intent("com.example.autodingding.START_CHECK_IN")
                    broadcastIntent.putExtra("type", type)
                    sendBroadcast(broadcastIntent)
                }, 3000L)
                Toast.makeText(this, R.string.manual_check_in_started, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTimePicker(period: DayPeriod) {
        val (hour, minute) = when (period) {
            DayPeriod.MORNING -> morningHour to morningMinute
            DayPeriod.EVENING -> eveningHour to eveningMinute
        }
        val title = if (period == DayPeriod.MORNING) "\u4E0A\u73ED\u65F6\u95F4" else "\u4E0B\u73ED\u65F6\u95F4"
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText(title)
            .build()
        picker.addOnPositiveButtonClickListener {
            val newHour = picker.hour
            val newMinute = picker.minute
            when (period) {
                DayPeriod.MORNING -> {
                    morningHour = newHour
                    morningMinute = newMinute
                }
                DayPeriod.EVENING -> {
                    eveningHour = newHour
                    eveningMinute = newMinute
                }
            }
            updateTimeButtonTexts()
        }
        picker.show(supportFragmentManager, "time_picker")
    }

    private fun updateTimeButtonTexts() {
        btnMorningTime.text = String.format("%02d:%02d", morningHour, morningMinute)
        btnEveningTime.text = String.format("%02d:%02d", eveningHour, eveningMinute)
    }

    private fun loadNotificationSettings() {
        val phone = sharedPreferences.getString("notification_phone", null)
        val email = sharedPreferences.getString("notification_email", null)
        if (!phone.isNullOrBlank()) etNotificationPhone.setText(phone)
        if (!email.isNullOrBlank()) etNotificationEmail.setText(email)
    }

    private fun saveNotificationSettings() {
        val phone = etNotificationPhone.text.toString().trim()
        val email = etNotificationEmail.text.toString().trim()
        sharedPreferences.edit()
            .putString("notification_phone", phone.ifBlank { null })
            .putString("notification_email", email.ifBlank { null })
            .apply()
        Toast.makeText(this, R.string.notification_saved, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateLastCheckIn()
        updateAccessibilityButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(checkInSuccessReceiver)
    }

    private fun isRunning(): Boolean = sharedPreferences.getBoolean("isRunning", false)

    private fun updateAccessibilityButton() {
        val enabled = CheckInHelper.isAccessibilityEnabled(this)
        tvAccessibilityStatus.text = if (enabled) {
            getString(R.string.status_enabled)
        } else {
            getString(R.string.status_not_enabled)
        }
        tvAccessibilityStatus.background = if (enabled) {
            getDrawable(R.drawable.badge_enabled_bg)
        } else {
            getDrawable(R.drawable.badge_disabled_bg)
        }
        btnAccessibility.text = if (enabled) {
            getString(R.string.accessibility_settings)
        } else {
            getString(R.string.enable_accessibility)
        }
    }

    private fun startAutoCheckIn() {
        sharedPreferences.edit()
            .putInt("morningHour", morningHour)
            .putInt("morningMinute", morningMinute)
            .putInt("eveningHour", eveningHour)
            .putInt("eveningMinute", eveningMinute)
            .putBoolean("isRunning", true)
            .apply()

        val intent = Intent(this, AutoCheckInService::class.java)
        intent.action = "START"
        startForegroundService(intent)

        LogManager.i("MainActivity", "Auto check-in started")
        updateStatus()
    }

    private fun stopAutoCheckIn() {
        sharedPreferences.edit()
            .putBoolean("isRunning", false)
            .apply()

        val intent = Intent(this, AutoCheckInService::class.java)
        intent.action = "STOP"
        startForegroundService(intent)

        LogManager.i("MainActivity", "Auto check-in stopped")
        updateStatus()
    }

    private fun updateStatus() {
        btnStart.text = if (isRunning()) getString(R.string.stop_check_in) else getString(R.string.start_check_in)
        tvStatus.text = if (isRunning()) getString(R.string.status_running) else getString(R.string.status_idle)
        tvStatus.setTextColor(if (isRunning()) getColor(R.color.teal_700) else getColor(R.color.text_primary))
        indicatorStatus.background = if (isRunning()) {
            getDrawable(R.drawable.dot_running)
        } else {
            getDrawable(R.drawable.dot_idle)
        }
        btnStart.icon = if (isRunning()) {
            getDrawable(R.drawable.ic_stop)
        } else {
            getDrawable(R.drawable.ic_play)
        }
    }

    private fun updateLastCheckIn() {
        val lastTime = sharedPreferences.getLong("lastCheckInTime", 0)
        if (lastTime > 0) {
            val date = Date(lastTime)
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            tvLastCheckIn.text = format.format(date)
        } else {
            tvLastCheckIn.text = getString(R.string.no_record)
        }
    }
}
