package com.projectpandora.app.data

import java.time.LocalDate

/** The month grid and selected-day list share the same log-only records. */
data class LogCalendarContent(val logs: List<WorkLog>,val labels: List<WorkLabel>)

fun logCalendarContent(date: LocalDate,userId: Long?,logs: List<WorkLog>,assignments: List<ContentLabels>): LogCalendarContent {
    val dayLogs=logs.filter {it.userId==userId && it.covers(date.toString())}
        .sortedByDescending {it.updatedAt}
    val assigned=assignments.filter {it.kind=="log"}.associateBy {it.contentId}
    val labels=dayLogs.flatMap {assigned[it.id]?.labels ?: listOfNotNull(it.label)}.distinctBy {it.id}
    return LogCalendarContent(dayLogs,labels)
}
