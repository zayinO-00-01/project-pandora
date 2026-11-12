package com.projectpandora.app.data

import java.time.LocalDate

// State Council holiday notices for 2026 and 2027.
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
    holiday(2027,1,1..3,"元旦")
    holiday(2027,2,5..13,"春节")
    holiday(2027,4,3..5,"清明节")
    holiday(2027,5,1..5,"劳动节")
    holiday(2027,6,9..9,"端午节")
    holiday(2027,9,15..15,"中秋节")
    holiday(2027,10,1..7,"国庆节")
    listOf("2026-01-04","2026-02-14","2026-02-28","2026-05-09","2026-09-20","2026-10-10",
        "2027-01-31","2027-02-14","2027-05-08","2027-09-26","2027-10-09").forEach {
        put(LocalDate.parse(it),"调休上班")
    }
}

fun holidayLabel(date: LocalDate): String? = holidayLabels[date]
