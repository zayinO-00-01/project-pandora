package com.projectpandora.app.data
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
class LogEditDeadlineTest {
    @Test fun anotherDraftDoesNotBlockOpeningReadOnlyHistory() {
        val created=Instant.parse("2026-10-23T15:59:59Z")
        val log=WorkLog(1,1,"2026-10-23","历史全文","submitted",created.toString(),created.toString())
        val draft=EditBuffer("http://localhost",1,null,"2026-10-24","未保存输入","draft")
        assertNull(logDraftForOpening(draft,log,created.plusSeconds(86400)))
        assertSame(draft,logDraftForOpening(draft,null,created.plusSeconds(86400)))
        assertSame(draft,logDraftForOpening(draft,log,created.plusSeconds(60)))
    }
    @Test fun exactCreationDeadlineIsExclusiveAndUpdateDoesNotExtendIt() {
        val created=Instant.parse("2026-10-23T15:59:59Z")
        val record=WorkLog(1,1,"2026-10-20","连续记录","submitted",created.toString(),created.plusSeconds(86399).toString(),"2026-10-23")
        assertTrue(canModifyLog(record,created))
        assertTrue(canModifyLog(record,created.plusSeconds(1)))
        assertTrue(canModifyLog(record,created.plusSeconds(86399)))
        assertFalse(canModifyLog(record,created.plusSeconds(86400)))
        assertFalse(canModifyLog(record,created.minusSeconds(1)))
        assertFalse(canModifyLog(record.copy(createdAt="invalid"),created))
        assertFalse(canModifyLog(record.copy(createdAt=""),created))
    }
}
