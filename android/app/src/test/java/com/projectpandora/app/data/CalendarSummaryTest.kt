package com.projectpandora.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarSummaryTest {
    @Test fun calendarShowsDueTaskTitlesAndFirstNonblankLogLines() {
        val date=LocalDate.of(2026,10,17)
        val task=DispatchTask(1,"任务摘要","","normal","doing","2026-10-16T18:00:00Z",null,"反馈",1,2,"主管","员工","","")
        val log=WorkLog(2,2,"2026-10-17","\n日志摘要\n更多内容","submitted","","")
        assertEquals(listOf("任务摘要","日志摘要"),calendarSummaries(date,listOf(log),listOf(task)))
        assertEquals(emptyList<String>(),calendarSummaries(date.plusDays(1),listOf(log),listOf(task)))
    }
}
