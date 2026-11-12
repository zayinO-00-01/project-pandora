package com.projectpandora.app.data

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class HolidayScheduleTest {
    @Test fun known2026ScheduleIncludesRestAndAdjustedWorkdays() {
        assertEquals("春节",holidayScheduleLabel(LocalDate.of(2026,2,23)))
        assertEquals("调休上班",holidayScheduleLabel(LocalDate.of(2026,2,28)))
        assertEquals("中秋节",holidayScheduleLabel(LocalDate.of(2026,9,27)))
        assertNull(holidayScheduleLabel(LocalDate.of(2026,11,12)))
    }
    @Test fun unverifiedFutureScheduleDoesNotClaimAnOfficialRestOrWorkday() {
        assertNull(holidayScheduleLabel(LocalDate.of(2027,1,31)))
        assertNull(holidayScheduleLabel(LocalDate.of(2027,2,5)))
        assertNull(holidayScheduleLabel(LocalDate.of(2028,10,1)))
    }
}
