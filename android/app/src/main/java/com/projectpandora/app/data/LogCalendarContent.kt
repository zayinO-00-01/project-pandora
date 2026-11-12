package com.projectpandora.app.data

import java.time.LocalDate

/** Same visible records power both the month cell and the expanded date list. */
data class LogCalendarContent(val logs: List<WorkLog>,val tasks: List<DispatchTask>,val labels: List<WorkLabel>)

fun logCalendarContent(date: LocalDate,userId: Long?,logs: List<WorkLog>,tasks: List<DispatchTask>,assignments: List<ContentLabels>): LogCalendarContent {
    val dayLogs=logs.filter {it.userId==userId && it.covers(date.toString())}
        .sortedByDescending {it.updatedAt}
    val dayTasks=orderedTasks(tasks.filter {it.assigneeId==userId && taskIsApproved(it) && !it.deleted && taskDate(it)==date})
    val assigned=assignments.associateBy {it.kind to it.contentId}
    fun tags(kind: String,id: Long,fallback: WorkLabel?)=assigned[kind to id]?.labels ?: listOfNotNull(fallback)
    val labels=(dayLogs.flatMap {tags("log",it.id,it.label)}+dayTasks.flatMap {tags("task",it.id,it.label)}).distinctBy {it.id}
    return LogCalendarContent(dayLogs,dayTasks,labels)
}
