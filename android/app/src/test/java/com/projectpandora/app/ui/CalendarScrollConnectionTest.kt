package com.projectpandora.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CalendarScrollConnectionTest {
    private class Calendar {
        var atTop=false
        var canResize=true
        var starts=0
        val deltas=mutableListOf<Float>()
        val releases=mutableListOf<Float>()
        val scroll=CalendarScrollConnection(
            isAtTop={atTop},
            startResize={starts++;canResize},
            resize={deltas+=it},
            finishResize={releases+=it}
        )
    }
    private val touch=NestedScrollSource.UserInput

    @Test fun pullingDownWithinTaskListScrollsContentWithoutResizingOrJumping()=runBlocking {
        val calendar=Calendar()
        assertEquals(Offset.Zero,calendar.scroll.onPreScroll(Offset(0f,60f),touch))
        assertEquals(Offset.Zero,calendar.scroll.onPostScroll(Offset(0f,60f),Offset.Zero,touch))
        assertEquals(Velocity.Zero,calendar.scroll.onPreFling(Velocity(0f,250f)))
        assertEquals(0,calendar.starts)
        assertTrue(calendar.deltas.isEmpty())
        assertTrue(calendar.releases.isEmpty())
    }

    @Test fun samePullExpandsOnlyByDistanceLeftAfterListReachesTop()=runBlocking {
        val calendar=Calendar()
        assertEquals(Offset.Zero,calendar.scroll.onPreScroll(Offset(0f,60f),touch))
        calendar.atTop=true // The list consumes 45px to reach its start.
        assertEquals(Offset(0f,15f),calendar.scroll.onPostScroll(Offset(0f,45f),Offset(0f,15f),touch))
        assertEquals(Offset(0f,20f),calendar.scroll.onPreScroll(Offset(0f,20f),touch))
        assertEquals(listOf(15f,20f),calendar.deltas)
        assertEquals(1,calendar.starts)
        assertEquals(Velocity(0f,300f),calendar.scroll.onPreFling(Velocity(0f,300f)))
        assertEquals(listOf(300f),calendar.releases)
        assertEquals(Velocity.Zero,calendar.scroll.onPreFling(Velocity(0f,300f)))
    }

    @Test fun scrollingUpWithinTaskListDoesNotCollapseCalendar() {
        val calendar=Calendar()
        assertEquals(Offset.Zero,calendar.scroll.onPreScroll(Offset(0f,-50f),touch))
        assertTrue(calendar.deltas.isEmpty())
    }

    @Test fun upwardPullAtTopStillCollapsesAndCanReverseBeforeRelease()=runBlocking {
        val calendar=Calendar().apply {atTop=true}
        assertEquals(Offset(0f,-50f),calendar.scroll.onPreScroll(Offset(10f,-50f),touch))
        assertEquals(Offset(0f,15f),calendar.scroll.onPreScroll(Offset(0f,15f),touch))
        assertEquals(listOf(-50f,15f),calendar.deltas)
        calendar.scroll.onPreFling(Velocity.Zero)
        assertEquals(listOf(0f),calendar.releases)
        assertEquals(1,calendar.starts)
    }

    @Test fun dayViewOrCalendarSizeLimitLeavesScrollingToList()=runBlocking {
        val calendar=Calendar().apply {atTop=true;canResize=false}
        assertEquals(Offset.Zero,calendar.scroll.onPreScroll(Offset(0f,-50f),touch))
        assertEquals(Offset.Zero,calendar.scroll.onPostScroll(Offset.Zero,Offset(0f,50f),touch))
        assertTrue(calendar.deltas.isEmpty())
        assertEquals(Velocity.Zero,calendar.scroll.onPreFling(Velocity(0f,300f)))
    }

    @Test fun flingOrProgrammaticScrollDoesNotExpandCalendar() {
        val calendar=Calendar().apply {atTop=true}
        assertEquals(Offset.Zero,calendar.scroll.onPreScroll(Offset(0f,-50f),NestedScrollSource.SideEffect))
        assertEquals(Offset.Zero,calendar.scroll.onPostScroll(Offset.Zero,Offset(0f,50f),NestedScrollSource.SideEffect))
        assertEquals(0,calendar.starts)
        assertTrue(calendar.deltas.isEmpty())
    }
}
