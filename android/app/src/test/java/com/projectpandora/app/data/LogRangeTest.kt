package com.projectpandora.app.data
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class LogRangeTest {
    @Test fun inclusiveRangeCrossesYearAndRejectsOutsideEndpoints() {
        val reference=LocalDate.of(2026,1,2)
        assertTrue(canWriteLogRange("2025-12-31","2026-01-02",reference))
        assertEquals(3L,logRangeDays("2025-12-31","2026-01-02"))
        assertFalse(canWriteLogRange("2025-12-29","2026-01-01",reference))
        assertFalse(canWriteLogRange("2026-01-02","2026-01-03",reference))
        assertFalse(canWriteLogRange("2026-01-02","2026-01-01",reference))
        assertFalse(canWriteLogRange("bad","2026-01-02",reference))
    }
    @Test fun allCoveredDatesFindOneRecordAndOldNullEndRemainsSingleDay() {
        val log=WorkLog(1,1,"2026-02-27","病假","submitted","","","2026-03-01")
        for(date in listOf("2026-02-27","2026-02-28","2026-03-01")) {
            assertTrue(log.covers(date))
            assertEquals(listOf("病假"),calendarSummaries(LocalDate.parse(date),listOf(log),emptyList()))
        }
        assertFalse(log.covers("2026-03-02"))
        assertFalse(log.copy(endDate=null).covers("2026-02-28"))
        val original=log.copy(logDate=today().minusDays(2).toString(),endDate=today().toString())
        assertTrue(logEditorReadOnly(EditBuffer("s",1,1,today().toString(),"bypass","submitted"),listOf(original)))
    }
    @Test fun oldBackendIsRejectedBeforePostingMultidayRecord() {
        MockWebServer().use {server->
            server.start();server.enqueue(MockResponse().setBody("""{"ok":true}"""))
            val api=ApiClient(server.url("/").toString());val session=Session(api.server,"t",1,"STAFF",null,"staff")
            try {api.save(session,null,LogInput(today().minusDays(2).toString(),"病假","submitted",today().toString()));fail("old service accepted range")}
            catch(e:ApiException) {assertEquals(409,e.status)}
            assertEquals(1,server.requestCount)
            assertEquals("GET",server.takeRequest().method)
        }
    }
}
