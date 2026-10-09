package com.projectpandora.app.data

import com.google.gson.JsonParser
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class TaskApiClientTest {
    @Test fun tasksAndFeedbackUseRealHttpAndStableIdentity() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("[${task(0, "todo", "")}]"))
            server.enqueue(MockResponse().setBody(task(50, "doing", "已完成一半")))
            server.enqueue(MockResponse().setBody(task(100, "done", "已验收")))
            server.start()
            val api = ApiClient(server.url("/").toString())
            val me = Session(api.server, "staff-token", 3, "STAFF", "员工", "staff")
            val initial = api.tasks(me).single()
            assertEquals(19L, initial.id)
            assertEquals(3L, initial.assigneeId)
            assertEquals("负责人", initial.assigneeName)
            assertEquals("2026-10-10T15:59:59Z", initial.dueAt)
            val doing = api.saveProgress(me, initial.id, TaskProgressInput(50, "已完成一半"))
            assertEquals(initial.id, doing.id)
            assertEquals("doing", doing.status)
            val done = api.saveProgress(me, initial.id, TaskProgressInput(100, "已验收"))
            assertEquals(initial.id, done.id)
            assertEquals(100, done.progress)
            assertEquals("done", done.status)
            assertEquals("已验收", done.progressNote)
            repeat(3) { index ->
                val request = server.takeRequest()
                assertEquals("Bearer staff-token", request.getHeader("Authorization"))
                assertEquals(if(index == 0) "GET" else "PUT", request.method)
                assertEquals(if(index == 0) "/api/v1/tasks" else "/api/v1/tasks/19/progress", request.path)
                if(index > 0) {
                    val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
                    assertEquals(setOf("progress", "progressNote"), body.keySet())
                    assertEquals(if(index == 1) 50 else 100, body["progress"].asInt)
                    assertEquals(if(index == 1) "已完成一半" else "已验收", body["progressNote"].asString)
                }
            }
        }
    }
    @Test fun taskProgressRejectsInvalidInputBeforeNetwork() {
        assertNull(TaskValidation.error("0", "已开始准备"))
        assertNull(TaskValidation.error("100", "完成"))
        for(value in listOf("", "1.5", "-1", "101", "2147483648")) assertNotNull(TaskValidation.error(value, "说明"))
        assertNotNull(TaskValidation.error("50", "  "))
        assertNotNull(TaskValidation.error("50", "x".repeat(2001)))
        MockWebServer().use { server ->
            server.start()
            val api = ApiClient(server.url("/").toString())
            val me = Session(api.server, "token", 3, "STAFF", null, "staff")
            try { api.saveProgress(me, 19, TaskProgressInput(101, "说明")); fail("invalid progress accepted") }
            catch(_: IllegalArgumentException) {}
            assertEquals(0, server.requestCount)
        }
    }
    @Test fun taskErrorsKeepAuthorizationStatus() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"只有责任人可反馈"}"""))
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"登录已过期"}"""))
            server.start()
            val api = ApiClient(server.url("/").toString())
            val me = Session(api.server, "token", 3, "STAFF", null, "staff")
            try { api.saveProgress(me, 19, TaskProgressInput(50, "说明")); fail("expected 403") }
            catch(e: ApiException) { assertEquals(403, e.status); assertEquals("只有责任人可反馈", e.message) }
            try { api.tasks(me); fail("expected 401") }
            catch(e: ApiException) { assertEquals(401, e.status) }
        }
    }
    private fun task(progress: Int, status: String, note: String) = """{"id":19,"title":"核对合同","detail":"核对条款","priority":"high","status":"$status","dueAt":"2026-10-10T15:59:59Z","progress":$progress,"progressNote":"$note","createdBy":2,"assigneeId":3,"creatorName":"领导","assigneeName":"负责人","createdAt":"2026-10-09T01:00:00Z","updatedAt":"2026-10-09T02:00:00Z"}"""
}