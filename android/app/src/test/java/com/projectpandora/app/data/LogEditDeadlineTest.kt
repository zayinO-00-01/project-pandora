package com.projectpandora.app.data
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
class LogEditDeadlineTest {
    @Test fun creationTimeControlsDeadlineRatherThanLogDateOrLastEdit() {
        val created=Instant.parse("2026-10-23T15:59:00Z")
        assertTrue(canModifyLog(created.toString(),created.plusSeconds(60)))
        assertTrue(canModifyLog(created.toString(),created.plusSeconds(86399)))
        assertFalse(canModifyLog(created.toString(),created.plusSeconds(86400)))
        assertFalse(canModifyLog(created.toString(),created.minusSeconds(1)))
        assertFalse(canModifyLog("",created))
        val oldDate=today().minusDays(3).toString()
        val record=WorkLog(1,1,oldDate,"补录","submitted",Instant.now().toString(),"",today().toString())
        assertFalse(logEditorReadOnly(EditBuffer("s",1,1,oldDate,"修改","submitted"),listOf(record)))
    }
}
