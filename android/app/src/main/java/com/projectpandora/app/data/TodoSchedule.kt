package com.projectpandora.app.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun DailyTodo.scheduleDates(): ClosedRange<LocalDate> {
    val anchor=LocalDate.parse(date)
    return anchor..(endAt?.let {LocalDateTime.parse(it).toLocalDate()} ?: anchor)
}

fun DailyTodo.moveTo(day: LocalDate,fromDay: LocalDate=LocalDate.parse(date)): DailyTodo {
    val offset=ChronoUnit.DAYS.between(fromDay,day)
    return copy(date=LocalDate.parse(date).plusDays(offset).toString(),labelIds=null,
        startAt=startAt?.let {LocalDateTime.parse(it).plusDays(offset).toString()},
        endAt=endAt?.let {LocalDateTime.parse(it).plusDays(offset).toString()})
}

fun DailyTodo.hasValidSchedule(): Boolean {
    val anchor=LocalDate.parse(date)
    val start=startAt?.let(LocalDateTime::parse)
    val end=endAt?.let(LocalDateTime::parse)
    return (start==null || start.toLocalDate()==anchor) &&
        (end==null || !end.isBefore(start ?: anchor.atStartOfDay()))
}
