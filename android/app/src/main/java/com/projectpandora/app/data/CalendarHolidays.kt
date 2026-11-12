package com.projectpandora.app.data

import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.sin

// 2026 national holiday schedule. Add later years only after checking an official notice.
// Festival dates are calculated separately for every year; they do not imply a day off.
private val holidayLabels = buildMap<LocalDate,String> {
    fun holiday(year: Int, month: Int, days: IntRange, name: String) {
        days.forEach { put(LocalDate.of(year,month,it),name) }
    }
    holiday(2026,1,1..3,"元旦")
    holiday(2026,2,15..23,"春节")
    holiday(2026,4,4..6,"清明节")
    holiday(2026,5,1..5,"劳动节")
    holiday(2026,6,19..21,"端午节")
    holiday(2026,9,25..27,"中秋节")
    holiday(2026,10,1..7,"国庆节")
    listOf("2026-01-04","2026-02-14","2026-02-28","2026-05-09","2026-09-20","2026-10-10").forEach {
        put(LocalDate.parse(it),"调休上班")
    }
}

fun holidayScheduleLabel(date: LocalDate): String? = holidayLabels[date]

fun holidayLabel(date: LocalDate): String? = if(holidayScheduleLabel(date)=="调休上班") "调休上班"
    else festivalName(date) ?: holidayScheduleLabel(date)

private val beijingOffset=ZoneOffset.ofHours(8)

fun festivalName(date: LocalDate): String? {
    when {
        date.monthValue==1 && date.dayOfMonth==1 -> return "元旦"
        date.monthValue==5 && date.dayOfMonth in 1..2 -> return "劳动节"
        date.monthValue==10 && date.dayOfMonth in 1..3 -> return "国庆节"
        date.monthValue==4 && date.dayOfMonth in 3..6 && isQingming(date) -> return "清明节"
    }
    val lunar=Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"),ULocale("zh_CN@calendar=chinese"))
    lunar.timeInMillis=date.atTime(12,0).toInstant(beijingOffset).toEpochMilli()
    if(lunar.get(Calendar.IS_LEAP_MONTH)!=0)return null
    val month=lunar.get(Calendar.MONTH)+1
    val day=lunar.get(Calendar.DAY_OF_MONTH)
    return when {
        month==1 && day in 1..3 -> "春节"
        month==5 && day==5 -> "端午节"
        month==8 && day==15 -> "中秋节"
        month==12 && day==lunar.getActualMaximum(Calendar.DAY_OF_MONTH) -> "除夕"
        else -> null
    }
}

// Qingming starts when the apparent solar longitude reaches 15 degrees.
private fun isQingming(date: LocalDate): Boolean = solarLongitude(date)<15 && solarLongitude(date.plusDays(1))>=15

private fun solarLongitude(date: LocalDate): Double {
    val julianDay=date.atStartOfDay().toInstant(beijingOffset).epochSecond/86400.0+2440587.5
    val t=(julianDay-2451545.0)/36525.0
    val meanLongitude=(280.46646+t*(36000.76983+t*.0003032))%360
    val anomaly=Math.toRadians(357.52911+t*(35999.05029-.0001537*t))
    val center=sin(anomaly)*(1.914602-t*(.004817+.000014*t))+
        sin(2*anomaly)*(.019993-.000101*t)+sin(3*anomaly)*.000289
    return (meanLongitude+center-.00569-.00478*sin(Math.toRadians(125.04-1934.136*t))+360)%360
}
