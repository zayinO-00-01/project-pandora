package com.projectpandora.app.data

import org.junit.Assert.*
import org.junit.Test

class DutiesTest {
    private fun task(id: Long,creator: Long,assignee: Long,status: String="todo",due: String?=null)=
        DispatchTask(id,"任务$id","说明","normal",status,due,null,"",creator,assignee,"领导","员工","","")

    @Test fun staffOnlySeeOwnPendingTasksWithDatedTasksFirst() {
        val tasks=listOf(task(1,9,3),task(2,9,3,"doing","2026-10-17T10:00:00Z"),task(3,9,3,"done"),task(4,9,4))
        assertEquals(listOf(2L,1L),dutyTasks(tasks,3,false).map {it.id})
    }
    @Test fun leadersSeeTheirAssignmentsWithoutDroppingCompletedWork() {
        val tasks=listOf(task(1,3,9,"done"),task(2,3,8),task(3,7,3))
        assertEquals(listOf(2L,1L),dutyTasks(tasks,3,true).map {it.id})
    }
    @Test fun memberSearchStaysWithinAuthorizedList() {
        val members=listOf(UserSummary(1,"zhangsan","张三","STAFF"),UserSummary(2,"lisi","李四","STAFF"))
        assertEquals(listOf(members[0]),filterMembers(members," 张 "))
        assertEquals(listOf(members[1]),filterMembers(members,"LISI"))
        assertTrue(filterMembers(members,"王五").isEmpty())
        assertEquals(members,filterMembers(members,""))
    }
}
