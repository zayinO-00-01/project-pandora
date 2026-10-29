package com.projectpandora.app.data

import com.google.gson.JsonParser
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class SelfTaskApprovalTest {
    private val pending="""{"id":31,"title":"自行安排","detail":"整理资料","priority":"normal","status":"todo","dueAt":null,"progress":null,"progressNote":"","createdBy":3,"assigneeId":3,"creatorName":"员工","assigneeName":"员工","createdAt":"","updatedAt":"","approvalStatus":"pending","approvalNote":"","reviewerId":null,"reviewedAt":null}"""
    @Test fun employeeCanCreateOwnTaskButNotAssignAnotherPerson() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setResponseCode(201).setBody(pending));server.start()
            val api=ApiClient(server.url("/").toString());val me=Session(api.server,"s",3,"STAFF",null,"staff")
            val result=api.createTask(me,TaskCreateInput("自行安排","整理资料","normal",3,null))
            assertEquals("pending",result.approvalStatus)
            val sent=server.takeRequest();assertEquals("/api/v1/tasks",sent.path)
            assertEquals(3L,JsonParser.parseString(sent.body.readUtf8()).asJsonObject["assigneeId"].asLong)
            try {api.createTask(me,TaskCreateInput("越权安排","","normal",9,null));fail("employee dispatched to other user")}catch(e:ApiException){assertEquals(403,e.status)}
            assertEquals(1,server.requestCount)
        }
    }
    @Test fun selfDraftDoesNotDependOnHavingSubordinates() {
        val draft=TaskDispatchBuffer("server",3,"自行安排",assigneeId=3)
        assertNull(TaskDispatchValidation.error(draft,emptyList()))
        assertNull(TaskDispatchValidation.inputError(TaskDispatchValidation.input(draft),3))
        assertNotNull(TaskDispatchValidation.error(draft.copy(assigneeId=9),emptyList()))
    }
    @Test fun everyBusinessRoleKeepsSelfAndOnlyLeadersGetAuthorizedSubordinates() {
        val staff=UserSummary(9,"report","下级","STAFF")
        for(role in AppRoles.business) {
            val me=Session("server","s",3,role,null,"me")
            val assignees=TaskDispatchValidation.assignees(me,Organization(assignees=listOf(staff)))
            assertEquals(3L,assignees.first().id)
            assertEquals(if(role=="STAFF")listOf(3L) else listOf(3L,9L),assignees.map {it.id})
        }
    }
    @Test fun pendingAndRejectedAreNotExecutableOrShownAsStarted() {
        val parser=com.google.gson.Gson()
        val task=parser.fromJson(pending,DispatchTask::class.java)
        assertFalse(taskIsApproved(task));assertEquals("待上级审核",taskFeedbackLabel(task))
        assertFalse(taskIsApproved(task.copy(approvalStatus="rejected")))
        assertEquals("审核未通过",taskFeedbackLabel(task.copy(approvalStatus="rejected")))
        assertTrue(taskIsApproved(task.copy(approvalStatus="approved")))
        assertTrue(taskIsApproved(task.copy(approvalStatus=null)))
    }
    @Test fun managerReviewUsesSeparateAuthenticatedApprovalRoute() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setBody("[$pending]"))
            server.enqueue(MockResponse().setBody(pending.replace("\"approvalStatus\":\"pending\"","\"approvalStatus\":\"approved\"")))
            server.start();val api=ApiClient(server.url("/").toString());val manager=Session(api.server,"manager",2,"TEAM_LEAD",null,"team_lead")
            assertEquals(31L,api.pendingTaskApprovals(manager).single().id)
            assertEquals("approved",api.decideTaskApproval(manager,31,"approved","同意安排").approvalStatus)
            assertEquals("/api/v1/tasks/pending-approvals",server.takeRequest().path)
            val sent=server.takeRequest();assertEquals("PUT",sent.method);assertEquals("/api/v1/tasks/31/approval",sent.path)
            assertEquals("Bearer manager",sent.getHeader("Authorization"))
            val body=JsonParser.parseString(sent.body.readUtf8()).asJsonObject
            assertEquals("approved",body["decision"].asString);assertEquals("同意安排",body["note"].asString)
        }
    }
    @Test fun invalidApprovalOrNonLeaderNeverReachesNetwork() {
        MockWebServer().use {server->
            server.start();val api=ApiClient(server.url("/").toString())
            val staff=Session(api.server,"s",3,"STAFF",null,"staff")
            try{api.decideTaskApproval(staff,31,"approved","");fail("staff reviewed")}catch(e:ApiException){assertEquals(403,e.status)}
            val leader=staff.copy(role="TEAM_LEAD")
            try{api.decideTaskApproval(leader,31,"pending","");fail("invalid decision accepted")}catch(_:IllegalArgumentException){}
            try{api.decideTaskApproval(leader,31,"rejected","x".repeat(501));fail("long note accepted")}catch(_:IllegalArgumentException){}
            assertEquals(0,server.requestCount)
        }
    }
}