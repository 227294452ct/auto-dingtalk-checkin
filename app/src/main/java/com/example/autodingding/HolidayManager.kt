package com.example.autodingding

import java.util.*

object HolidayManager {

    // 2026年中国法定节假日（预估，以国务院公告为准）
    private val HOLIDAYS_2026 = setOf(
        // 元旦 1月1日-3日
        "2026-01-01", "2026-01-02", "2026-01-03",
        // 春节 2月17日-23日（正月初一为2月17日）
        "2026-02-16", "2026-02-17", "2026-02-18", "2026-02-19",
        "2026-02-20", "2026-02-21", "2026-02-22",
        // 清明节 4月5日-7日
        "2026-04-05", "2026-04-06", "2026-04-07",
        // 劳动节 5月1日-5日
        "2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05",
        // 端午节 6月19日-21日
        "2026-06-19", "2026-06-20", "2026-06-21",
        // 中秋节 9月25日-27日
        "2026-09-25", "2026-09-26", "2026-09-27",
        // 国庆节 10月1日-7日
        "2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04",
        "2026-10-05", "2026-10-06", "2026-10-07"
    )

    fun isWorkday(): Boolean {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // 周六(7)、周日(1) 休息
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }
        // 检查是否为法定节假日
        val today = String.format("%tF", cal) // yyyy-MM-dd
        // 注：调休上班日暂不处理，极端情况可后续添加
        return today !in HOLIDAYS_2026
    }
}