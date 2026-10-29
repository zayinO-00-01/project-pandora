package com.projectpandora.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarNavigationTest {
    @Test fun jumpPreservesDayWhenTargetMonthHasIt() {
        assertEquals(LocalDate.of(2035,10,23),jumpCalendarDate(LocalDate.of(2026,10,23),2035,10))
    }
    @Test fun jumpClampsJanuary31ToNonLeapFebruary28() {
        assertEquals(LocalDate.of(2027,2,28),jumpCalendarDate(LocalDate.of(2026,1,31),2027,2))
    }
    @Test fun jumpClampsJanuary31ToLeapFebruary29() {
        assertEquals(LocalDate.of(2028,2,29),jumpCalendarDate(LocalDate.of(2026,1,31),2028,2))
    }
    @Test fun jumpClampsLeapDayWhenChangingYear() {
        assertEquals(LocalDate.of(2029,2,28),jumpCalendarDate(LocalDate.of(2028,2,29),2029,2))
    }
    @Test fun jumpHandlesCenturyLeapRulesAtAllowedYearBoundary() {
        val selected=LocalDate.of(2026,1,31)
        assertEquals(LocalDate.of(1900,2,28),jumpCalendarDate(selected,1900,2))
        assertEquals(LocalDate.of(2000,2,29),jumpCalendarDate(selected,2000,2))
        assertEquals(LocalDate.of(2100,2,28),jumpCalendarDate(selected,2100,2))
    }
    @Test fun jumpClampsToThirtyDayMonth() {
        assertEquals(LocalDate.of(2026,4,30),jumpCalendarDate(LocalDate.of(2026,3,31),2026,4))
    }
    @Test fun jumpingToCurrentYearAndMonthKeepsSelectedDate() {
        val selected=LocalDate.of(2026,10,23)
        assertEquals(selected,jumpCalendarDate(selected,selected.year,selected.monthValue))
    }
    @Test fun jumpRejectsUnsupportedYearsAndMonths() {
        val selected=LocalDate.of(2026,10,23)
        listOf(Int.MIN_VALUE,0,1899,2101,Int.MAX_VALUE).forEach {assertNull(jumpCalendarDate(selected,it,10))}
        listOf(Int.MIN_VALUE,0,13,Int.MAX_VALUE).forEach {assertNull(jumpCalendarDate(selected,2026,it))}
    }
}
