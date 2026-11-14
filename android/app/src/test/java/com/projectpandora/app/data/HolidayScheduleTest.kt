package com.projectpandora.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HolidayScheduleTest {
    private val data="""{"year":2027,"papers":["https://www.gov.cn/example.htm"],"days":[
        {"name":"春节","date":"2027-02-05","isOffDay":true},
        {"name":"春节","date":"2027-02-14","isOffDay":false}
    ]}"""

    @Test fun annualNoticeDifferentiatesHolidayAndMakeUpWork() {
        val days=parseHolidaySchedule(data,2027)
        assertEquals(mapOf(LocalDate.of(2027,2,5) to "春节",LocalDate.of(2027,2,14) to "调休上班"),days)
    }

    @Test(expected=IllegalArgumentException::class)
    fun wrongYearIsRejected() { parseHolidaySchedule(data,2028) }
}
