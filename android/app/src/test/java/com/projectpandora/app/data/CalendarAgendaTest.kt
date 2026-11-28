package com.projectpandora.app.data

import java.time.LocalDate
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class CalendarAgendaTest {
    @Test fun summaryMatchesGridCellsAndBusyDayLoadsSeparatePages() {
        MockWebServer().use { server ->
            val selected = LocalDate.of(2026, 1, 1)
            val dates = companyCalendarDates(selected,"month")
            assertEquals(LocalDate.of(2025,12,28),dates.first())
            assertEquals(LocalDate.of(2026,1,31),dates.last())
            val chip = """{"id":1,"title":"Day 1","dueAt":"2026-01-01T01:00:00Z","canReschedule":true,"labels":[]}"""
            server.enqueue(MockResponse().setBody("""[{"date":"2026-01-01","total":10000,"previews":[$chip]}]"""))
            server.enqueue(MockResponse().setBody("""{"total":10000,"items":[${entry(1)}],"nextPage":1}"""))
            server.enqueue(MockResponse().setBody("""{"total":9999,"items":[${entry(1)},${entry(2)}],"nextPage":2}"""))
            server.start()
            val api = ApiClient(server.url("/").toString())
            val session = Session(api.server,"calendar-token",3,"STAFF",null,"staff")
            val day = api.calendarSummary(session,dates.first(),dates.last()).single()
            assertEquals(9999L,day.remaining)
            assertEquals(1L,day.shown.single().id)
            val first = api.calendarAgenda(session,selected,0)
            assertEquals(1,first.nextPage)
            val second = api.calendarAgenda(session,selected,requireNotNull(first.nextPage))
            val agenda = first.append(second)
            assertEquals(listOf(1L,2L),agenda.items.map {it.task.id})
            assertEquals(2,agenda.nextPage)
            val summaryRequest = server.takeRequest()
            assertEquals("/api/v1/tasks/calendar?from=2025-12-28&to=2026-01-31",summaryRequest.path)
            assertEquals("Bearer calendar-token",summaryRequest.getHeader("Authorization"))
            assertEquals("/api/v1/tasks/calendar/day?date=2026-01-01&page=0",server.takeRequest().path)
            assertEquals("/api/v1/tasks/calendar/day?date=2026-01-01&page=1",server.takeRequest().path)
        }
    }

    private fun entry(id: Long) = """{"task":{"id":$id,"title":"Task","detail":"","priority":"normal","status":"todo","dueAt":"2026-01-01T01:00:00Z","progressNote":"","createdBy":3,"assigneeId":3,"creatorName":"Staff","assigneeName":"Staff","createdAt":"now","updatedAt":"now"},"canReschedule":true,"labels":[]}"""
}
