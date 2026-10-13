package com.projectpandora.app.data

import org.junit.Assert.*
import org.junit.Test
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse

class ApiClientTest {
    @Test fun normalizeOriginAndRejectCredentialLeak() {
        assertEquals("http://192.168.1.10:8080/api/v1",ServerAddress.normalize("  http://192.168.1.10:8080/  "))
        assertEquals("https://example.com/api/v1",ServerAddress.normalize("https://example.com/api/v1/"))
        for(bad in listOf("file:///tmp","http://user:pass@example.com","http://example.com?token=x","http://example.com/other","missing-host","http://example.com#x")) {
            try {ServerAddress.normalize(bad);fail("accepted invalid URL: $bad")} catch(_:IllegalArgumentException) {}
        }
    }
    @Test fun validateContentAndShanghaiDate() {
        assertNull(LogValidation.error(today().toString(),"正常工作","draft"))
        assertNotNull(LogValidation.error(today().plusDays(1).toString(),"未来记录","draft"))
        assertNotNull(LogValidation.error("bad","记录","draft"))
        assertNotNull(LogValidation.error(today().toString(),"   ","draft"))
        assertNotNull(LogValidation.error(today().toString(),"x".repeat(5001),"submitted"))
    }
    @Test fun realHttpFlowUsesBearerAndSameLogId() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setBody("""{"token":"abc","userId":3,"role":"STAFF","displayName":"员工"}"""))
            server.enqueue(MockResponse().setResponseCode(201).setBody(log("draft")))
            server.enqueue(MockResponse().setBody(log("submitted")))
            server.enqueue(MockResponse().setBody(log("submitted")))
            server.enqueue(MockResponse().setBody("[${log("submitted")}]"))
            server.start()
            val api=ApiClient(server.url("/").toString())
            val me=api.login("staff","demo1234")
            val draft=api.save(me,null,LogInput(today().toString(),"草稿内容","draft"))
            assertEquals("draft",draft.status)
            assertEquals(7L,api.submit(me,draft.id).id)
            assertEquals(7L,api.save(me,draft.id,LogInput(today().toString(),"修改内容","submitted")).id)
            assertEquals(7L,api.logs(me).first().id)
            val paths=listOf("/api/v1/auth/login","/api/v1/logs","/api/v1/logs/7/submit","/api/v1/logs/7","/api/v1/logs")
            val methods=listOf("POST","POST","POST","PUT","GET")
            paths.forEachIndexed {i,path ->
                val request=server.takeRequest()
                assertEquals(path,request.path);assertEquals(methods[i],request.method)
                if(i==0)assertNull(request.getHeader("Authorization")) else assertEquals("Bearer abc",request.getHeader("Authorization"))
                if(i==1)assertTrue(request.body.readUtf8().contains("draft"))
                if(i==3)assertTrue(request.body.readUtf8().contains("修改内容"))
            }
        }
    }
    @Test fun apiErrorKeepsReadableStatus() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"登录已过期"}"""));server.start()
            try {ApiClient(server.url("/").toString()).login("staff","bad");fail("expected 401")}
            catch(e:ApiException) {assertEquals(401,e.status);assertEquals("登录已过期",e.message)}
        }
    }
    private fun log(state: String)="""{"id":7,"userId":3,"logDate":"${today()}","content":"修改内容","status":"$state","createdAt":"2026-10-08T01:00:00Z","updatedAt":"2026-10-08T02:00:00Z"}"""
}
