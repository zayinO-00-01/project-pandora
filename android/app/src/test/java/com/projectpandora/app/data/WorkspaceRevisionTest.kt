package com.projectpandora.app.data
import org.junit.Assert.*
import org.junit.Test
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse

class WorkspaceRevisionTest {
    private fun task(id: Long,creator: Long,assignee: Long,status: String="todo")=DispatchTask(id,"任务$id","","normal",status,"2026-11-03T10:00:00Z",null,"",creator,assignee,"创建人","责任人","","")
    @Test fun directRelationIncludesCreatedAssignedAndCompletedButExcludesHierarchyOnlyAndDeleted() {
        val items=listOf(task(1,7,3),task(2,3,8,"done"),task(3,7,8),task(4,3,3),task(5,3,8).copy(deleted=true))
        assertEquals(setOf(1L,2L,4L),relatedTasks(items,3).map {it.id}.toSet())
        assertFalse(AppRoles.canReview("STAFF"))
        assertTrue(AppRoles.canReview("TEAM_LEAD"))
    }
    @Test fun directoryReadsSafeContactDtoWithAuthentication() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setBody("""[{"id":2,"username":"peer","displayName":"同事","role":"STAFF","managerId":1,"managerName":"上级","phone":"13800000000","avatar":null,"avatarData":null}]"""));server.start()
            val api=ApiClient(server.url("/").toString());val session=Session(api.server,"token",3,"STAFF",null,"me")
            assertEquals("同事",api.directory(session).single().displayName)
            val request=server.takeRequest();assertEquals("/api/v1/directory",request.path);assertEquals("Bearer token",request.getHeader("Authorization"))
        }
    }
}
