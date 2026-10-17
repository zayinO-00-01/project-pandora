package com.projectpandora.app.data

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CalendarPeriodTest {
    @Test fun weekContainsMondayToSundayAcrossYearBoundary() {
        val dates=periodDates(LocalDate.of(2027,1,1),"week")
        assertEquals(7,dates.size)
        assertEquals(LocalDate.of(2026,12,28),dates.first())
        assertEquals(LocalDate.of(2027,1,3),dates.last())
        assertEquals(dates,periodDates(LocalDate.of(2027,1,3),"week"))
    }
    @Test fun dayContainsOnlySelectedDateAndMonthRetainsLeapDay() {
        val date=LocalDate.of(2028,2,29)
        assertEquals(listOf(date),periodDates(date,"day"))
        val dates=periodDates(date,"month")
        assertEquals(29,dates.size)
        assertEquals(LocalDate.of(2028,2,1),dates.first())
        assertEquals(date,dates.last())
    }
}
