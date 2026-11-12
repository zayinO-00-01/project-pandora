package com.projectpandora.app.data

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class LogCalendarContentTest {
    private val day=LocalDate.of(2026,11,12)
    private val red=WorkLabel(1,"产品","#FF0000")
    private val blue=WorkLabel(2,"客户","#0000FF")
    private val log=WorkLog(1,7,"2026-11-11","跨日历史日志","submitted","","",endDate="2026-11-12",label=red)
    private val task=DispatchTask(1,"任务","","normal","todo","2026-11-11T16:30:00Z",null,"",9,7,"主管","本人","","",label=blue)

    @Test fun selectedDayIncludesEveryOwnTaskAndHistoricalRangeLog() {
        val tasks=(1L..15L).map {task.copy(id=it)}
        val result=logCalendarContent(day,7,listOf(log,log.copy(id=2,userId=8)),tasks,emptyList())
        assertEquals(listOf(log),result.logs)
        assertEquals(15,result.tasks.size)
        assertEquals(listOf(red,blue),result.labels)
    }

    @Test fun calendarUsesShanghaiDatesAndOnlyApprovedOwnTasks() {
        val result=logCalendarContent(day,7,emptyList(),listOf(
            task,
            task.copy(id=2,dueAt="2026-11-12T16:00:00Z"),
            task.copy(id=3,assigneeId=8),
            task.copy(id=4,approvalStatus="pending"),
            task.copy(id=5,deleted=true)
        ),emptyList())
        assertEquals(listOf(task),result.tasks)
        assertTrue(result.logs.isEmpty())
    }

    @Test fun clearedAssignmentsOverrideLegacyLabelsAndDuplicateLabelsAppearOnce() {
        val result=logCalendarContent(day,7,listOf(log),listOf(task,task.copy(id=2)),listOf(
            ContentLabels("log",1,emptyList()),
            ContentLabels("task",1,listOf(red,blue)),
            ContentLabels("task",2,listOf(blue))
        ))
        assertEquals(listOf(red,blue),result.labels)
        assertTrue(logCalendarContent(day,7,listOf(log),emptyList(),listOf(ContentLabels("log",1,emptyList()))).labels.isEmpty())
    }
}
