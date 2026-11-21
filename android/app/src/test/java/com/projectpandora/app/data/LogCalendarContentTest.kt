package com.projectpandora.app.data

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class LogCalendarContentTest {
    private val day=LocalDate.of(2026,11,12)
    private val red=WorkLabel(1,"产品","#FF0000")
    private val blue=WorkLabel(2,"客户","#0000FF")
    private val log=WorkLog(1,7,"2026-11-11","跨日历史日志","submitted","","",endDate="2026-11-12",label=red)

    @Test fun selectedDayContainsOnlyOwnLogsIncludingHistoricalRanges() {
        val result=logCalendarContent(day,7,listOf(log,log.copy(id=2,userId=8),log.copy(id=3,logDate="2026-11-13",endDate=null)),emptyList())
        assertEquals(listOf(log),result.logs)
        assertEquals(listOf(red),result.labels)
    }
    @Test fun taskLabelAssignmentsCannotAppearInLogCalendarEvenWithSameId() {
        val result=logCalendarContent(day,7,listOf(log),listOf(ContentLabels("task",1,listOf(blue))))
        assertEquals(listOf(red),result.labels)
        assertTrue(logCalendarContent(day,7,emptyList(),listOf(ContentLabels("task",1,listOf(blue)))).labels.isEmpty())
    }
    @Test fun logAssignmentsOverrideLegacyLabelsAndDeduplicate() {
        val result=logCalendarContent(day,7,listOf(log,log.copy(id=2)),listOf(ContentLabels("log",1,listOf(red,blue)),ContentLabels("log",2,listOf(blue))))
        assertEquals(listOf(red,blue),result.labels)
        assertTrue(logCalendarContent(day,7,listOf(log),listOf(ContentLabels("log",1,emptyList()))).labels.isEmpty())
    }
}
