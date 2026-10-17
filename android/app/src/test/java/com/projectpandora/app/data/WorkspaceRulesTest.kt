package com.projectpandora.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse

class WorkspaceRulesTest {
    @Test fun logWindowIncludesThreeDaysAgoAndTodayAcrossYears() {
        val reference=LocalDate.of(2026,1,3)
        assertTrue(canWriteLog("2025-12-31",reference))
        assertTrue(canWriteLog("2026-01-03",reference))
        assertFalse(canWriteLog("2025-12-30",reference))
        assertFalse(canWriteLog("2026-01-04",reference))
        assertFalse(canWriteLog("2026-02-30",reference))
        assertFalse(canWriteLog("bad-date",reference))
    }
    @Test fun expiredOriginalCannotBeUnlockedByChangingBufferDate() {
        val old=today().minusDays(8).toString()
        val log=WorkLog(91,1,old,"history","submitted","","")
        val buffer=EditBuffer("http://server",1,91,today().toString(),"changed","submitted")
        assertTrue(logEditorReadOnly(buffer,listOf(log)))
        assertFalse(logEditorReadOnly(buffer.copy(logId=null),emptyList()))
        assertFalse(logEditorReadOnly(buffer,listOf(log.copy(logDate=today().toString()))))
    }
    @Test fun missingOriginalLogCannotUnlockRestoredEditor() {
        val buffer=EditBuffer("http://server",1,91,today().toString(),"changed","submitted")
        assertTrue(logEditorReadOnly(buffer,emptyList()))
    }
    @Test fun periodsHandleLeapMonthAndWeekCrossingYear() {
        val leap=periodDates(LocalDate.of(2024,2,15),"month")
        assertEquals(29,leap.size)
        assertEquals(LocalDate.of(2024,2,29),leap.last())
        val week=periodDates(LocalDate.of(2026,1,1),"week")
        assertEquals(LocalDate.of(2025,12,29),week.first())
        assertEquals(LocalDate.of(2026,1,4),week.last())
        assertEquals(listOf(LocalDate.of(2026,10,10)),periodDates(LocalDate.of(2026,10,10),"day"))
    }
    @Test fun onlyFourBusinessRolesLoginAndLegacyRoleIsRejected() {
        MockWebServer().use {server->
            server.start()
            val api=ApiClient(server.url("/").toString())
            for(role in listOf("STAFF","TEAM_LEAD","DEPT_HEAD","FOUNDER")) {
                server.enqueue(MockResponse().setBody("""{"token":"t","userId":1,"role":"$role"}"""))
                assertEquals(role,api.login("user","pass").role)
            }
            server.enqueue(MockResponse().setBody("""{"token":"t","userId":1,"role":"ADMIN"}"""))
            try {api.login("admin","pass");fail("admin accepted")} catch(e:ApiException) {assertEquals(403,e.status)}
            server.enqueue(MockResponse().setBody("""{"token":"t","userId":1,"role":"LEADER"}"""))
            try {api.login("legacy","pass");fail("legacy role accepted")} catch(e:ApiException) {assertEquals(502,e.status)}
            server.enqueue(MockResponse().setBody("""{"token":"t","userId":1,"role":"UNKNOWN"}"""))
            try {api.login("user","pass");fail("unknown accepted")} catch(e:ApiException) {assertEquals(502,e.status)}
        }
    }
    @Test fun integrityUsesAuthenticatedEndpointAndPreservesFailure() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setBody("""{"ok":false,"message":"检查失败"}"""))
            server.start()
            val api=ApiClient(server.url("/").toString())
            val result=api.integrity(Session(api.server,"token",3,"STAFF",null,"staff"))
            assertFalse(result.ok)
            val request=server.takeRequest()
            assertEquals("/api/v1/logs/integrity",request.path)
            assertEquals("Bearer token",request.getHeader("Authorization"))
        }
    }
    @Test fun taskDateUsesShanghaiAndNullIsNotAnInventedDate() {
        val task=DispatchTask(1,"t","","normal","todo","2026-10-09T18:00:00Z",0,"",1,2,"a","b","","")
        assertEquals(LocalDate.of(2026,10,10),taskDate(task))
        assertNull(taskDate(task.copy(dueAt=null)))
        assertNull(taskDate(task.copy(dueAt="invalid")))
    }
    @Test fun historicalPercentagesNeverAppearInTaskLabels() {
        val task=DispatchTask(1,"t","","normal","doing",null,40,"已联系客户",1,2,"a","b","","")
        for(progress in listOf(null,0,40,100)) {
            assertEquals("进行中",taskFeedbackLabel(task.copy(progress=progress)))
            assertEquals("已完成",taskFeedbackLabel(task.copy(status="done",progress=progress)))
        }
    }
}
