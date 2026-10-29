package com.projectpandora.app.data

import com.google.gson.JsonParser
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class DispatchContractTest {
    @Test fun organizationAndDispatchUseAuthenticatedContract() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"members":[{"id":3,"username":"staff","displayName":"员工","role":"STAFF"},{"id":8,"username":"unrelated","role":"DEPT_HEAD"}],"assignees":[{"id":3,"username":"staff","displayName":"员工","role":"STAFF"}],"manager":{"id":1,"username":"founder","role":"FOUNDER"}}"""))
            server.enqueue(MockResponse().setResponseCode(201).setBody(task))
            server.start()
            val api=ApiClient(server.url("/").toString())
            val me=Session(api.server,"leader-token",2,"FOUNDER",null,"lead")
            val organization=api.organization(me)
            assertEquals(listOf(3L,8L),organization.members.map {it.id})
            val members=organization.assignees
            assertEquals(listOf(3L),members.map {it.id})
            val result=api.createTask(me,TaskCreateInput("核对合同","核对条款","high",members.single().id,"2026-10-16T02:30:00Z"))
            assertEquals(19L,result.id)
            assertEquals(3L,result.assigneeId)
            assertEquals("todo",result.status)
            val org=server.takeRequest()
            assertEquals("GET",org.method)
            assertEquals("/api/v1/users/organization",org.path)
            val request=server.takeRequest()
            assertEquals("POST",request.method)
            assertEquals("/api/v1/tasks",request.path)
            assertEquals("Bearer leader-token",request.getHeader("Authorization"))
            val body=JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals(setOf("title","detail","priority","assigneeId","dueAt","labelId"),body.keySet())
            assertEquals("核对合同",body["title"].asString)
            assertEquals("high",body["priority"].asString)
            assertEquals(3L,body["assigneeId"].asLong)
            assertEquals("2026-10-16T02:30:00Z",body["dueAt"].asString)
        }
    }
    @Test fun readableMembersNeverFallbackToDispatchAssignees() {
        MockWebServer().use {server ->
            val readable="""{"id":8,"username":"unrelated","role":"DEPT_HEAD"}"""
            server.enqueue(MockResponse().setBody("""{"members":[$readable],"manager":null}"""))
            server.enqueue(MockResponse().setBody("""{"members":[$readable],"manager":null,"assignees":[]}"""))
            server.start()
            val api=ApiClient(server.url("/").toString())
            val me=Session(api.server,"founder-token",2,"FOUNDER",null,"founder")
            repeat(2) {
                val organization=api.organization(me)
                assertEquals(listOf(8L),organization.members.map {member->member.id})
                assertTrue(organization.assignees.isEmpty())
                val draft=TaskDispatchBuffer(api.server,me.userId,"任务",assigneeId=8)
                assertNotNull(TaskDispatchValidation.error(draft,organization.assignees))
            }
        }
    }
    @Test fun dispatchRejectsNonLeadersAndInvalidInputBeforeNetwork() {
        MockWebServer().use {server ->
            server.start()
            val api=ApiClient(server.url("/").toString())
            val input=TaskCreateInput("任务","","normal",3,null)
            for(role in listOf("ADMIN","LEADER")) {
                try {api.createTask(Session(api.server,"t",2,role,null,"u"),input);fail("accepted $role")}
                catch(e:ApiException) {assertEquals(403,e.status)}
            }
            val me=Session(api.server,"t",2,"FOUNDER",null,"f")
            for(bad in listOf(input.copy(title=" "),input.copy(title="x".repeat(129)),input.copy(detail="x".repeat(5001)),input.copy(priority="urgent"),input.copy(assigneeId=0),input.copy(dueAt="tomorrow"))) {
                try {api.createTask(me,bad);fail("invalid accepted")} catch(_:IllegalArgumentException) {}
            }
            assertEquals(0,server.requestCount)
        }
    }
    @Test fun nullableDeadlineAndServerFailureDoNotInventSuccess() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setBody(task))
            server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"组织关系已变更"}"""))
            server.enqueue(MockResponse().setBody(task.replace("\"assigneeId\":3","\"assigneeId\":99")))
            server.start()
            val api=ApiClient(server.url("/").toString())
            val me=Session(api.server,"t",2,"DEPT_HEAD",null,"d")
            val input=TaskCreateInput("任务","","normal",3,null)
            assertEquals(19L,api.createTask(me,input).id)
            val body=JsonParser.parseString(server.takeRequest().body.readUtf8()).asJsonObject
            assertTrue(body.has("dueAt"));assertTrue(body["dueAt"].isJsonNull)
            try {api.createTask(me,input);fail("permission failure accepted")} catch(e:ApiException) {assertEquals(403,e.status)}
            try {api.createTask(me,input);fail("wrong assignee accepted")} catch(e:ApiException) {assertEquals(502,e.status)}
        }
    }
    @Test fun dispatchDraftUsesShanghaiTimeAndActualDownwardMembers() {
        val member=UserSummary(3,"staff",null,"STAFF")
        val draft=TaskDispatchBuffer("server",2,"任务","详情","normal",3,"2026-10-16 10:30")
        assertNull(TaskDispatchValidation.error(draft,listOf(member)))
        assertEquals("2026-10-16T02:30:00Z",TaskDispatchValidation.input(draft).dueAt)
        assertNull(TaskDispatchValidation.input(draft.copy(dueDate="")).dueAt)
        assertNotNull(TaskDispatchValidation.error(draft,emptyList()))
        assertNotNull(TaskDispatchValidation.error(draft,listOf(member.copy(role="ADMIN"))))
        assertNotNull(TaskDispatchValidation.error(draft.copy(dueDate="2026-02-30 10:00"),listOf(member)))
        assertNull(TaskDispatchValidation.error(draft.copy(assigneeId=2),emptyList()))
    }
    @Test fun organizationRequestsAndReadOnlyMembersKeepExistingRoutes() {
        MockWebServer().use { server ->
            val request="""{"id":7,"requester":{"id":3,"username":"s","role":"STAFF"},"target":{"id":2,"username":"l","role":"TEAM_LEAD"},"reason":"协作","status":"pending","updatedAt":""}"""
            server.enqueue(MockResponse().setBody("[]"))
            server.enqueue(MockResponse().setBody(request))
            for(status in listOf("approved","denied","revoked"))server.enqueue(MockResponse().setBody(request.replace("pending",status)))
            server.enqueue(MockResponse().setBody("[]"));server.enqueue(MockResponse().setBody("[]"))
            server.start()
            val api=ApiClient(server.url("/").toString())
            val me=Session(api.server,"t",3,"STAFF",null,"s")
            api.viewRequests(me);api.requestView(me,2,"协作")
            for(status in listOf("approved","denied","revoked"))assertEquals(status,api.decideView(me,7,status).status)
            api.memberLogs(me,2);api.memberTasks(me,2)
            val paths=listOf("/view-requests","/view-requests","/view-requests/7","/view-requests/7","/view-requests/7","/logs?userId=2","/tasks?userId=2")
            paths.forEachIndexed {i,path ->
                val sent=server.takeRequest();assertEquals("/api/v1$path",sent.path)
                assertEquals(listOf("GET","POST","PUT","PUT","PUT","GET","GET")[i],sent.method)
                assertEquals("Bearer t",sent.getHeader("Authorization"))
                if(i==1)assertEquals("协作",JsonParser.parseString(sent.body.readUtf8()).asJsonObject["reason"].asString)
            }
        }
    }
    private val task="""{"id":19,"title":"核对合同","detail":"核对条款","priority":"high","status":"todo","dueAt":"2026-10-16T02:30:00Z","progress":0,"progressNote":"","createdBy":2,"assigneeId":3,"creatorName":"领导","assigneeName":"员工","createdAt":"","updatedAt":""}"""
}
