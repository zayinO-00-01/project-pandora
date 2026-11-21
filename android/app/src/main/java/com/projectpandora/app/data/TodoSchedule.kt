package com.projectpandora.app.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun DailyTodo.occursOn(day: LocalDate): Boolean {
    val start=startAt?.take(10)?.let(LocalDate::parse) ?: LocalDate.parse(date)
    val end=endAt?.take(10)?.let(LocalDate::parse) ?: start
    return !day.isBefore(start) && !day.isAfter(end)
}
fun DailyTodo.moveTo(day: LocalDate): DailyTodo {
    val offset=ChronoUnit.DAYS.between(LocalDate.parse(date),day)
    return copy(date=day.toString(),labelIds=null,
        startAt=startAt?.let {LocalDateTime.parse(it).plusDays(offset).toString()},
        endAt=endAt?.let {LocalDateTime.parse(it).plusDays(offset).toString()})
}
