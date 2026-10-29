package com.projectpandora.app.data
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class LogEditDeadlineTest {
    @Test fun onlyCurrentShanghaiDateIsEditableAndHistoricalRangeIsReadOnly() {
        val reference=LocalDate.of(2026,10,24)
        val record=WorkLog(1,1,"2026-10-24","今日","submitted","2026-10-20T00:00:00Z","")
        assertTrue(canModifyLog(record,reference))
        assertFalse(canModifyLog(record.copy(logDate="2026-10-23",createdAt="2026-10-24T01:00:00Z"),reference))
        assertFalse(canModifyLog(record.copy(logDate="2026-10-25"),reference))
        assertFalse(canModifyLog(record.copy(logDate="2026-10-21",endDate="2026-10-24"),reference))
        val historical=record.copy(logDate=today().minusDays(3).toString(),endDate=today().toString())
        assertTrue(logEditorReadOnly(EditBuffer("s",1,1,today().toString(),"bypass","submitted"),listOf(historical)))
    }
}
