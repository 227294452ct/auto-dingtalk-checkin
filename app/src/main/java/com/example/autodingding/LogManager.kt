package com.example.autodingding

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {

    private const val MAX_ENTRIES = 500
    private val entries = mutableListOf<LogEntry>()
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val level: String,
        val tag: String,
        val message: String
    ) {
        fun formattedTime(): String = dateFormat.format(Date(timestamp))

        fun shortLabel(): String = when (level) {
            "V" -> "V"
            "D" -> "D"
            "I" -> "I"
            "W" -> "W"
            "E" -> "E"
            else -> level
        }
    }

    @Synchronized
    fun getAll(): List<LogEntry> = entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
    }

    @Synchronized
    private fun add(entry: LogEntry) {
        if (entries.size >= MAX_ENTRIES) {
            entries.removeAt(0)
        }
        entries.add(entry)
    }

    fun v(tag: String, message: String) {
        Log.v(tag, message)
        add(LogEntry(level = "V", tag = tag, message = message))
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        add(LogEntry(level = "D", tag = tag, message = message))
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        add(LogEntry(level = "I", tag = tag, message = message))
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        add(LogEntry(level = "W", tag = tag, message = message))
    }

    fun e(tag: String, message: String) {
        Log.e(tag, message)
        add(LogEntry(level = "E", tag = tag, message = message))
    }
}
