package com.projectpandora.app.data

import org.junit.Assert.*
import org.junit.Test

class PriorityOrderingTest {
    private fun task(id: Long,priority: String="normal",due: String?=null,status: String="todo")=
        DispatchTask(id,"任务$id","",priority,status,due,null,"",3,7,"领导","员工","","")

    @Test fun unfinishedThenPriorityThenDeadlineWithCompletedLast() {
        val tasks=listOf(task(1,"low","2026-10-01T00:00:00Z"),task(2,"normal","2026-10-02T00:00:00Z"),
            task(3,"high"),task(4,"high","2026-10-03T00:00:00Z"),task(5,"high","2026-09-01T00:00:00Z","done"))
        assertEquals(listOf(4L,3L,2L,1L,5L),orderedTasks(tasks).map {it.id})
        assertEquals(listOf(4L,3L,2L,1L),dutyTasks(tasks,7,false).map {it.id})
    }
    @Test fun deadlineUsesInstantAndMissingDatesHaveStableIdOrder() {
        val tasks=listOf(task(5,due="bad-date"),task(4),task(2,due="2026-10-17T10:00:00+08:00"),task(1,due="2026-10-17T01:30:00Z"))
        assertEquals(listOf(1L,2L,4L,5L),orderedTasks(tasks).map {it.id})
        assertEquals(1,taskPriorityRank("unknown"))
    }
    @Test fun tenthCanMoveBeforeNinthWithoutMutatingOriginal() {
        val items=(1L..10L).map {PanelItem(it,"事项$it",(it-1).toInt(),null,"说明$it")}
        val moved=reorderedPriorities(items,10,9)
        assertEquals(listOf(10L,9L),moved.takeLast(2).map {it.id})
        assertEquals((0..9).toList(),moved.map {it.sortOrder})
        assertEquals((1L..10L).toList(),items.map {it.id})
        assertEquals("说明10",moved[8].detail)
        assertEquals(items,reorderedPriorities(items,-1,9))
        assertEquals(items,reorderedPriorities(items,10,10))
    }
}
