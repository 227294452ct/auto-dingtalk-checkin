package com.example.autodingding

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object CheckInRecordManager {

    private const val PREF_NAME = "AutoDingDing"
    private const val KEY_RECORDS = "check_in_records"

    data class CheckInRecord(
        val id: Long = System.currentTimeMillis(),
        val date: String,
        val scheduledTime: String,
        val type: String,
        val success: Boolean,
        val actualTime: Long,
        val failureReason: String? = null
    )

    fun save(context: Context, record: CheckInRecord) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val records = getAllRecords(context).toMutableList()
        records.add(0, record)
        // 只保留最近 200 条
        val trimmed = if (records.size > 200) records.subList(0, 200) else records
        val jsonArray = JSONArray()
        for (r in trimmed) {
            jsonArray.put(recordToJson(r))
        }
        prefs.edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
    }

    fun getAll(context: Context): List<CheckInRecord> = getAllRecords(context)

    private fun getAllRecords(context: Context): List<CheckInRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        val records = mutableListOf<CheckInRecord>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                records.add(jsonToRecord(jsonArray.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return records
    }

    private fun recordToJson(r: CheckInRecord): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("date", r.date)
        put("scheduledTime", r.scheduledTime)
        put("type", r.type)
        put("success", r.success)
        put("actualTime", r.actualTime)
        if (r.failureReason != null) put("failureReason", r.failureReason)
    }

    private fun jsonToRecord(json: JSONObject): CheckInRecord = CheckInRecord(
        id = json.optLong("id", 0),
        date = json.optString("date", ""),
        scheduledTime = json.optString("scheduledTime", ""),
        type = json.optString("type", ""),
        success = json.optBoolean("success", false),
        actualTime = json.optLong("actualTime", 0),
        failureReason = json.optString("failureReason", null)
    )

    fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return "--"
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDate(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return dateStr
            SimpleDateFormat("MM\u6708dd\u65E5", Locale.getDefault()).format(date)
        } catch (_: Exception) { dateStr }
    }
}