package com.example.autodingding

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object NotificationHelper {

    private const val CHANNEL_ID = "check_in_failure"
    private const val NOTIFICATION_ID = 1001

    fun notifyFailure(context: Context, type: String, reason: String) {
        val title = "\u6253\u5361\u5931\u8D25"
        val message = "${type}\u6253\u5361\u5931\u8D25\uFF0C\u539F\u56E0\uFF1A${reason}"

        // 1. 系统通知
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT

        val pendingIntent = PendingIntent.getActivity(context, 0, clickIntent, flag)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)

        // 2. 短信通知（可选）
        val phone = context.getSharedPreferences("AutoDingDing", Context.MODE_PRIVATE)
            .getString("notification_phone", null)
        if (!phone.isNullOrBlank()) {
            sendSms(context, phone, "${title}: ${message}")
        }

        // 3. 邮件通知（可选）
        val email = context.getSharedPreferences("AutoDingDing", Context.MODE_PRIVATE)
            .getString("notification_email", null)
        if (!email.isNullOrBlank()) {
            sendEmail(context, email, title, message)
        }
    }

    private fun sendSms(context: Context, phone: String, message: String) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
            val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                context.getSystemService(SmsManager::class.java)
            else
                @Suppress("DEPRECATION") SmsManager.getDefault()
            sms.sendTextMessage(phone, null, message, null, null)
        } catch (_: Exception) {}
    }

    private fun sendEmail(context: Context, to: String, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:$to")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}