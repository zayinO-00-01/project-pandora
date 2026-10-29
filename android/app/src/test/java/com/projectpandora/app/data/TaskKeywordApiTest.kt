package com.projectpandora.app.data

import com.google.gson.JsonParser
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class TaskKeywordApiTest {
    @Test fun keywordsUseTaskScopeAndRespectReviewResponse() {
        MockWebServer().use {server->
            repeat(3){server.enqueue(MockResponse().setBody("""{"message":"关键词变更已提交审核"}"""))}
            server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"仅参与人可维护"}"""))
            server.start()
            val api=ApiClient(server.url("/").toString());val me=Session(api.server,"token",4,"STAFF",null,"staff")
            assertTrue(api.saveKeyword(me,10,null,"需求").message.contains("审核"))
            api.saveKeyword(me,10,20,"联调");api.deleteKeyword(me,10,20)
            val add=server.takeRequest();assertEquals("POST",add.method);assertEquals("/api/v1/tasks/10/keywords",add.path)
            assertEquals("Bearer token",add.getHeader("Authorization"));assertEquals("需求",JsonParser.parseString(add.body.readUtf8()).asJsonObject["text"].asString)
            val edit=server.takeRequest();assertEquals("PUT",edit.method);assertEquals("/api/v1/tasks/10/keywords/20",edit.path)
            assertEquals("DELETE",server.takeRequest().method)
            try {api.saveKeyword(me,11,null,"越权");fail("accepted unauthorized participant")} catch(e:ApiException){assertEquals(403,e.status)}
        }
    }
}
