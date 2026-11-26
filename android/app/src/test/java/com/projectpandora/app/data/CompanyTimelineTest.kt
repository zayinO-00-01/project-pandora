package com.projectpandora.app.data

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CompanyTimelineTest {
    private val day=LocalDate.of(2026,11,26)

    @Test fun clipsSpanningTasksToShanghaiDayAndKeepsDueOnlyTasks() {
        val spanning=task(1,"2026-11-25T15:00:00Z","2026-11-25T17:00:00Z")
        val dueOnly=task(2,null,"2026-11-26T04:30:00Z")
        val previousDay=task(3,null,"2026-11-25T15:59:00Z")
        val pending=task(4,null,"2026-11-25T16:00:00Z").copy(approvalStatus="pending")
        val items=companyTimeline(day,listOf(spanning,dueOnly,previousDay,pending))
        assertEquals(listOf(1L,2L),items.map {it.task.id})
        assertEquals(0,items[0].startMinute)
        assertEquals(60,items[0].endMinute)
        assertEquals(750,items[1].startMinute)
    }

    @Test fun overlappingBlocksGetSeparateLanesAndLaterBlocksReuseThem() {
        val items=companyTimeline(day,listOf(
            task(1,"2026-11-26T01:00:00Z","2026-11-26T02:00:00Z"),
            task(2,"2026-11-26T01:30:00Z","2026-11-26T02:30:00Z"),
            task(3,"2026-11-26T03:00:00Z","2026-11-26T03:00:00Z")
        ))
        assertEquals(listOf(0,1,0),items.map {it.lane})
        assertEquals(listOf(2,2,1),items.map {it.laneCount})
        assertTrue(items.last().endMinute>items.last().startMinute)
    }

    private fun task(id: Long,start: String?,due: String) =
        DispatchTask(id,"任务","","normal","todo",due,0,"",9,7,"主管","本人","","",startAt=start)
}
