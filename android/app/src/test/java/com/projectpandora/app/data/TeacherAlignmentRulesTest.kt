package com.projectpandora.app.data

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class TeacherAlignmentRulesTest {
    @Test fun freshBackfillAndDateRangeCanBeModifiedAcrossMidnight() {
        val created=Instant.now().minusSeconds(60).toString()
        val record=WorkLog(1,3,today().minusDays(3).toString(),"连续记录","submitted",created,created,today().toString())
        assertTrue(canModifyLog(record))
        assertFalse(logEditorReadOnly(EditBuffer("s",3,1,record.logDate,"修改","submitted",record.endDate),listOf(record)))
    }
    @Test fun staleTodayLogCannotBeReopenedByRecentUpdate() {
        val record=WorkLog(1,3,today().toString(),"旧记录","submitted",Instant.now().minusSeconds(25*60*60).toString(),Instant.now().toString())
        assertFalse(canModifyLog(record))
    }
    @Test fun everyBusinessRoleCanChooseAllActiveCompanyAssigneesIncludingSelf() {
        val members=listOf(UserSummary(3,"me",null,"STAFF"),UserSummary(9,"peer",null,"STAFF"),UserSummary(10,"founder",null,"FOUNDER"),UserSummary(11,"admin",null,"ADMIN"))
        for(role in AppRoles.business) {
            val session=Session("server","s",3,role,null,"me")
            assertTrue(AppRoles.canDispatch(role))
            assertEquals(listOf(3L,9L,10L),TaskDispatchValidation.assignees(session,Organization(assignees=members)).map {it.id})
        }
    }
    @Test fun staffCanCreateForAnotherCompanyMemberThroughTheApprovalContract() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setBody("""{"id":31,"title":"协作","detail":"","priority":"normal","status":"todo","createdBy":3,"assigneeId":9,"creatorName":"员工","assigneeName":"同事","createdAt":"","updatedAt":"","approvalStatus":"pending"}"""))
            server.start();val api=ApiClient(server.url("/").toString());val me=Session(api.server,"staff",3,"STAFF",null,"staff")
            assertEquals("pending",api.createTask(me,TaskCreateInput("协作","","normal",9,null)).approvalStatus)
            assertEquals("/api/v1/tasks",server.takeRequest().path)
        }
    }
    @Test fun calendarUsesTasksWhileDateRangeLogsStayOnTheLogPage() {
        val date=today()
        val task=DispatchTask(1,"任务","","normal","todo",date.atStartOfDay(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toString(),null,"",3,9,"员工","同事","","")
        val log=WorkLog(2,3,date.minusDays(2).toString(),"日志摘要","submitted","","",date.toString())
        assertEquals(listOf("任务"),calendarSummaries(date,listOf(log),listOf(task)))
    }
    @Test fun progressReviewKeepsEffectiveTaskUntilManagerDecisionAndCanBeWithdrawn() {
        MockWebServer().use {server->
            val effective="""{"id":31,"title":"创始人安排","detail":"","priority":"normal","status":"todo","progress":null,"progressNote":"旧说明","createdBy":1,"assigneeId":3,"creatorName":"创始人","assigneeName":"员工","approvalStatus":"approved"}"""
            val review="""{"id":41,"taskId":31,"taskTitle":"创始人安排","applicantId":3,"applicantName":"员工","reviewerId":2,"reviewerName":"当前上级","operation":"PROGRESS","payload":"{\"status\":\"doing\",\"progress\":null,\"progressNote\":\"新说明\"}","status":"pending","createdAt":"2026-10-29T01:00:00Z"}"""
            server.enqueue(MockResponse().setBody(effective))
            server.enqueue(MockResponse().setBody("[$review]"))
            server.enqueue(MockResponse().setBody(review.replace("\"status\":\"pending\"","\"status\":\"canceled\"")))
            server.start();val api=ApiClient(server.url("/").toString());val me=Session(api.server,"staff",3,"STAFF",null,"staff")
            val task=api.saveProgress(me,31,TaskProgressInput(null,"新说明","doing"))
            assertEquals("todo",task.status);assertEquals("旧说明",task.progressNote)
            val records=api.reviews(me)
            assertEquals("PROGRESS",records.single().operation)
            assertFalse(taskCanReportProgress(task,records))
            assertEquals("canceled",api.decideTask(me,41,"canceled").status)
            val sent=server.takeRequest();assertEquals("PUT",sent.method);assertEquals("/api/v1/tasks/31/progress",sent.path)
            val body=com.google.gson.JsonParser.parseString(sent.body.readUtf8()).asJsonObject
            assertEquals("doing",body["status"].asString);assertTrue(body["progress"].isJsonNull);assertEquals("新说明",body["progressNote"].asString)
            assertEquals("/api/v1/task-reviews",server.takeRequest().path)
            val canceled=server.takeRequest();assertEquals("PUT",canceled.method);assertEquals("/api/v1/task-reviews/41",canceled.path)
            assertEquals("canceled",com.google.gson.JsonParser.parseString(canceled.body.readUtf8()).asJsonObject["status"].asString)
            assertTrue(taskCanReportProgress(task,records.map {it.copy(status="canceled")}))
        }
    }
    @Test fun initialOrDefinitionWithdrawalUsesSeparatePostRoute() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setBody("""{"id":31,"approvalStatus":"rejected","approvalNote":"申请已撤回"}"""))
            server.start();val api=ApiClient(server.url("/").toString());val me=Session(api.server,"staff",3,"STAFF",null,"staff")
            val result=api.withdrawTask(me,31)
            assertEquals("rejected",result.approvalStatus);assertEquals("申请已撤回",result.approvalNote)
            val sent=server.takeRequest();assertEquals("POST",sent.method);assertEquals("/api/v1/tasks/31/withdraw",sent.path)
            assertEquals("Bearer staff",sent.getHeader("Authorization"));assertEquals(0L,sent.bodySize)
        }
    }

}
