package com.projectpandora.app.data

import com.google.gson.JsonParser
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TodoScheduleTest {
    @Test fun calendarsIncludeEachDayOfTheScheduleAndKeepDateOnlyItems() {
        val item=DailyTodo("1","Travel","2026-11-19")
        val start="2026-11-19T20:00"
        val end="2026-11-21T09:30"
        val anchor=LocalDate.parse(item.date)
        assertTrue(anchor in item.scheduleDates())
        assertFalse(anchor.plusDays(1) in item.scheduleDates())
        assertFalse(anchor.plusDays(1) in item.copy(startAt=start).scheduleDates())
        for(scheduled in listOf(item.copy(endAt=end),item.copy(startAt=start,endAt=end))) {
            assertTrue(anchor in scheduled.scheduleDates())
            assertTrue(anchor.plusDays(1) in scheduled.scheduleDates())
            assertTrue(anchor.plusDays(2) in scheduled.scheduleDates())
            assertFalse(anchor.minusDays(1) in scheduled.scheduleDates())
            assertFalse(anchor.plusDays(3) in scheduled.scheduleDates())
        }
    }

    @Test fun movingFromTheMiddleOfASpanShiftsTheEntireSchedule() {
        val item=DailyTodo("1","Travel","2026-11-19",completed=true,labelIds=listOf(12),
            startAt="2026-11-19T20:00:01",endAt="2026-11-21T09:30:02")
        val moved=item.moveTo(LocalDate.parse("2026-12-01"),fromDay=LocalDate.parse("2026-11-20"))
        assertEquals("2026-11-30",moved.date)
        assertEquals("2026-11-30T20:00:01",moved.startAt)
        assertEquals("2026-12-02T09:30:02",moved.endAt)
        assertTrue(moved.completed)
        assertNull(moved.labelIds)
        val endOnly=item.copy(startAt=null).moveTo(LocalDate.parse("2026-12-01"))
        assertNull(endOnly.startAt)
        assertEquals("2026-12-03T09:30:02",endOnly.endAt)
    }

    @Test fun clearingOneEndpointSendsExplicitNullAndTheOriginalTimes() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setBody("""{"id":"1","title":"Travel","date":"2026-11-19","completed":false,"labels":[],"startAt":null,"endAt":"2026-11-21T09:30:02"}"""))
            server.start()
            val api=ApiClient(server.url("/").toString())
            val me=Session(api.server,"token",1,"STAFF",null,"staff")
            val original=DailyTodo("1","Travel","2026-11-19",startAt="2026-11-19T20:00:01",endAt="2026-11-21T09:30:02")
            val saved=api.saveTodo(me,original.copy(startAt=null),false,original)
            assertNull(saved.startAt)
            assertEquals(original.endAt,saved.endAt)
            val request=server.takeRequest()
            assertEquals("PUT",request.method)
            assertEquals("/api/v1/personal-todos/1",request.path)
            val body=JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertTrue(body["startAt"].isJsonNull)
            assertEquals(original.endAt,body["endAt"].asString)
            assertEquals(original.startAt,body["base"].asJsonObject["startAt"].asString)
        }
    }
}
