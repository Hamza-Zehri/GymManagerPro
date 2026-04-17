package com.hamzaasad.gymmanagerpro.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfDisplay = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    private val sdfYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun todayString(): String = sdfDate.format(Date())
    fun currentYearMonth(): String = sdfYearMonth.format(Date())
    fun displayDate(millis: Long): String = sdfDisplay.format(Date(millis))
    fun displayDate(dateStr: String): String = runCatching {
        sdfDisplay.format(sdfDate.parse(dateStr)!!)
    }.getOrDefault(dateStr)
    fun displayTime(millis: Long): String = sdfTime.format(Date(millis))
    fun millisFromDateStr(dateStr: String): Long = runCatching {
        sdfDate.parse(dateStr)!!.time
    }.getOrDefault(System.currentTimeMillis())
    fun daysUntil(millis: Long): Long = ((millis - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
    fun isExpired(millis: Long): Boolean = millis < System.currentTimeMillis()

    fun addMonths(millis: Long, months: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.add(Calendar.MONTH, months)
        return cal.timeInMillis
    }

    fun addDays(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }
}
