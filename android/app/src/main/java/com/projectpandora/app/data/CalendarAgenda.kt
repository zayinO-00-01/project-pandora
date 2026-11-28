package com.projectpandora.app.data

import java.time.LocalDate

internal data class CalendarTaskPreview(
    val id: Long, val title: String, val dueAt: String, val startAt: String?,
    val canReschedule: Boolean, val labels: List<WorkLabel>,
)
internal data class CalendarDaySummary(val date: String, val total: Long, val previews: List<CalendarTaskPreview>) {
    // Keep the existing two lines: two tasks, or one task plus the overflow control.
    val shown get() = previews.take(if(total>2)1 else 2)
    val remaining get() = total - shown.size
}
internal data class CalendarAgendaItem(val task: DispatchTask, val canReschedule: Boolean, val labels: List<WorkLabel>)
internal data class CalendarAgendaPage(val total: Long, val items: List<CalendarAgendaItem>, val nextPage: Int?) {
    fun append(page: CalendarAgendaPage) = page.copy(items = (items + page.items).distinctBy { it.task.id })
}

/** The month grid includes the adjacent-month cells it actually renders. */
internal fun companyCalendarDates(selected: LocalDate, mode: String): List<LocalDate> {
    if (mode == "day") return listOf(selected)
    if (mode == "week") {
        val sunday = selected.minusDays((selected.dayOfWeek.value % 7).toLong())
        return List(7) { sunday.plusDays(it.toLong()) }
    }
    val first = selected.withDayOfMonth(1)
    val leading = first.dayOfWeek.value % 7
    val start = first.minusDays(leading.toLong())
    val cells = ((leading + selected.lengthOfMonth() + 6) / 7) * 7
    return List(cells) { start.plusDays(it.toLong()) }
}
