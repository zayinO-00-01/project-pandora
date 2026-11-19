package com.projectpandora.app.data

import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.sin
import kotlinx.coroutines.*

private val holidayYears = androidx.compose.runtime.mutableStateMapOf<Int, Map<LocalDate,String>>()

internal fun parseHolidaySchedule(json: String, year: Int): Map<LocalDate,String> {
    val data=com.google.gson.JsonParser.parseString(json).asJsonObject
    require(data["year"].asInt==year && data.getAsJsonArray("papers").any {
        java.net.URI(it.asString).let {uri->uri.scheme=="https" && uri.host?.endsWith(".gov.cn")==true}
    })
    return data.getAsJsonArray("days").associate {entry->
        val day=entry.asJsonObject
        val date=LocalDate.parse(day["date"].asString)
        require(kotlin.math.abs(date.year-year)<=1)
        date to if(day["isOffDay"].asBoolean)day["name"].asString else "调休上班"
    }.also {require(it.isNotEmpty())}
}

@androidx.compose.runtime.Composable
fun LoadHolidaySchedules(years: Set<Int>) {
    val context=androidx.compose.ui.platform.LocalContext.current.applicationContext
    androidx.compose.runtime.LaunchedEffect(years) {
        val prefs=context.getSharedPreferences("holiday-cn",0)
        kotlinx.coroutines.coroutineScope {
            years.forEach {year->launch {
                val cached=prefs.getString("$year",null) ?: withContext(Dispatchers.IO) {
                    runCatching {context.assets.open("holidays/$year.json").bufferedReader(Charsets.UTF_8).use {it.readText()}}.getOrNull()
                }
                if(cached!=null && !holidayYears.containsKey(year))
                    runCatching {parseHolidaySchedule(cached,year)}.getOrNull()?.let {holidayYears[year]=it}
                if(holidayYears.containsKey(year) && System.currentTimeMillis()-prefs.getLong("$year.checked",0)<86_400_000L)return@launch
                val downloaded=withContext(Dispatchers.IO) {
                    listOf(
                        "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/$year.json",
                        "https://cdn.jsdelivr.net/gh/NateScarlet/holiday-cn@master/$year.json"
                    ).firstNotNullOfOrNull {url->runCatching {
                        val connection=java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        try {
                            connection.connectTimeout=5000
                            connection.readTimeout=5000
                            val json=connection.inputStream.bufferedReader(Charsets.UTF_8).use {it.readText()}
                            json to parseHolidaySchedule(json,year)
                        } finally {connection.disconnect()}
                    }.getOrNull()}
                }
                downloaded?.let {(json,dates)->
                    prefs.edit().putString("$year",json).putLong("$year.checked",System.currentTimeMillis()).apply()
                    holidayYears[year]=dates
                }
            }}
        }
    }
}

fun holidayScheduleLabel(date: LocalDate): String? =
    holidayYears[date.year]?.get(date) ?: holidayYears[date.year+1]?.get(date) ?: holidayYears[date.year-1]?.get(date)

fun holidayLabel(date: LocalDate): String? = holidayScheduleLabel(date) ?: festivalName(date)

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
