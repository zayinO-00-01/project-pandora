package com.projectpandora.app.data

import com.google.gson.Gson
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class PersonalWorkspaceApiTest {
    @Test fun todosUseAuthenticatedCrudAndTheServerGeneratedId() {
        MockWebServer().use {server->
            server.start()
            val api=ApiClient(server.url("/").toString())
            val me=Session(api.server,"staff-token",7,"STAFF","员工","staff")
            val body="""{"id":"server-id","title":"买书","date":"2026-11-12","completed":false}"""
            server.enqueue(MockResponse().setResponseCode(201).setBody(body))
            val localId="7b0b60dd-ab88-4de3-9671-4f15d7a582ae"
            val saved=api.saveTodo(me,DailyTodo(localId,"买书","2026-11-12"),true)
            assertEquals("server-id",saved.id)
            var request=server.takeRequest()
            assertEquals("POST",request.method);assertEquals("/api/v1/personal-todos",request.path)
            assertEquals("Bearer staff-token",request.getHeader("Authorization"))
            val create=Gson().fromJson(request.body.readUtf8(),com.google.gson.JsonObject::class.java)
            assertEquals("买书",create["title"].asString)
            assertEquals(localId,create["requestId"].asString)
            server.enqueue(MockResponse().setBody("[$body]"))
            assertEquals(listOf(saved),api.todos(me))
            assertEquals("GET",server.takeRequest().method)
            server.enqueue(MockResponse().setBody(body.replace("false","true")))
            assertTrue(api.saveTodo(me,saved.copy(completed=true),false,saved).completed)
            request=server.takeRequest()
            assertEquals("PUT",request.method);assertEquals("/api/v1/personal-todos/server-id",request.path)
            val update=Gson().fromJson(request.body.readUtf8(),com.google.gson.JsonObject::class.java)
            assertFalse(update.getAsJsonObject("base")["completed"].asBoolean)
            assertTrue(update["completed"].asBoolean)
            server.enqueue(MockResponse().setBody(body))
            api.deleteTodo(me,saved.id)
            assertEquals("DELETE",server.takeRequest().method)
        }
    }
    @Test fun ownProfileSendsOnlyTheEditablePersonalFields() {
        MockWebServer().use {server->
            server.start()
            val api=ApiClient(server.url("/").toString())
            val me=Session(api.server,"token",7,"STAFF","员工","staff")
            server.enqueue(MockResponse().setBody("""{"id":7,"username":"staff","role":"STAFF","displayName":"新姓名"}"""))
            val profile=api.updateOwnProfile(me,OwnProfileInput("新姓名","家人","010-12345678","地址"))
            assertEquals("新姓名",profile.displayName)
            val request=server.takeRequest()
            assertEquals("PUT",request.method);assertEquals("/api/v1/users/me",request.path)
            val fields=Gson().fromJson(request.body.readUtf8(),com.google.gson.JsonObject::class.java)
            assertEquals(setOf("displayName","emergencyContact","emergencyPhone","address"),fields.keySet())
        }
    }
}
