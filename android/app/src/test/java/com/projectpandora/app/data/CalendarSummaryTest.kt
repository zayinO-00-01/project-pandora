package com.projectpandora.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarSummaryTest {
    @Test fun calendarCellUsesOneLabelByTaskOrder() {
        val date=LocalDate.of(2026,10,24)
        val first=WorkLabel(1,"产品","#B9DAD0")
        val second=WorkLabel(2,"品牌","#F3CCA8")
        val base=DispatchTask(1,"任务","","normal","todo","2026-10-24T10:00:00Z",null,"",1,2,"主管","员工","","",first)
        val high=base.copy(id=2,priority="high",label=second)
        assertEquals(second,calendarDayLabel(date,listOf(base,high)))
        assertEquals(first,calendarDayLabel(date,listOf(base,base.copy(id=3,dueAt="2026-10-24T11:00:00Z",label=second))))
        assertEquals(first,calendarDayLabel(date,listOf(base,high.copy(status="done"))))
        assertEquals(first,calendarDayLabel(date,listOf(base,high.copy(label=null))))
        assertEquals(null,calendarDayLabel(date.plusDays(1),listOf(base,high)))
    }
    @Test fun calendarShowsDueTaskTitlesAndFirstNonblankLogLines() {
        val date=LocalDate.of(2026,10,17)
        val task=DispatchTask(1,"任务摘要","","normal","doing","2026-10-16T18:00:00Z",null,"反馈",1,2,"主管","员工","","")
        val log=WorkLog(2,2,"2026-10-17","\n日志摘要\n更多内容","submitted","","")
        assertEquals(listOf("任务摘要"),calendarSummaries(date,listOf(log),listOf(task)))
        assertEquals(emptyList<String>(),calendarSummaries(date.plusDays(1),listOf(log),listOf(task)))
    }
}
