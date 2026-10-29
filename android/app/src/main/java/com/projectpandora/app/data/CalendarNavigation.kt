package com.projectpandora.app.data

import java.time.LocalDate
import java.time.YearMonth

const val CALENDAR_FIRST_YEAR=1900
const val CALENDAR_LAST_YEAR=2100

/** Preserve the selected day, using the target month's last day when necessary. */
fun jumpCalendarDate(selected: LocalDate,year: Int,month: Int): LocalDate? {
    if(year !in CALENDAR_FIRST_YEAR..CALENDAR_LAST_YEAR || month !in 1..12)return null
    val target=YearMonth.of(year,month)
    return target.atDay(minOf(selected.dayOfMonth,target.lengthOfMonth()))
}
