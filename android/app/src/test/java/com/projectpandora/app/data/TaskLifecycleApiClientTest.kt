package com.projectpandora.app.data

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class TaskLifecycleApiClientTest {
    private val task="""{"id":31,"title":"任务","detail":"说明","priority":"normal","status":"doing","progress":null,"progressNote":"待验收","createdBy":3,"assigneeId":3,"creatorName":"员工","assigneeName":"员工","createdAt":"","updatedAt":"","approvalStatus":"approved","reviewerId":2,"reviewedAt":"2026-10-20T01:00:00Z","reviewStatus":"pending_done","reviewedBy":1,"lifecycleReviewedAt":"2026-10-22T01:00:00Z","deleted":false}"""
    @Test fun initialApprovalAndLifecycleAuditStayDistinctAcrossReviewRoutes() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setBody("[$task]"))
            server.enqueue(MockResponse().setBody(task.replace("pending_done","approved")))
            server.enqueue(MockResponse().setBody(task.replace("pending_done","rejected")))
            server.start()
            val api=ApiClient(server.url("/").toString());val me=Session(api.server,"manager",2,"TEAM_LEAD",null,"leader")
            val pending=api.taskReviews(me).single()
            assertTrue(taskIsApproved(pending));assertTrue(isPendingReview(pending.reviewStatus))
            assertEquals("2026-10-20T01:00:00Z",pending.reviewedAt)
            assertEquals("2026-10-22T01:00:00Z",pending.lifecycleReviewedAt)
            assertEquals(2L,pending.reviewerId);assertEquals(1L,pending.reviewedBy)
            assertEquals("approved",api.approveTask(me,31,"验收通过").reviewStatus)
            assertEquals("rejected",api.rejectTask(me,31,"请补充结果").reviewStatus)
            val reviews=server.takeRequest();assertEquals("/api/v1/tasks/reviews",reviews.path)
            val approve=server.takeRequest();assertEquals("POST",approve.method);assertEquals("/api/v1/tasks/31/approve",approve.path)
            assertEquals("Bearer manager",approve.getHeader("Authorization"))
            val reject=server.takeRequest();assertEquals("POST",reject.method);assertEquals("/api/v1/tasks/31/reject",reject.path)
        }
    }
    @Test fun pendingLifecycleFreezesProgressButCompletedTaskCanReopen() {
        val pending=com.google.gson.Gson().fromJson(task,DispatchTask::class.java)
        assertFalse(taskCanReportProgress(pending))
        assertFalse(taskCanReportProgress(pending.copy(reviewStatus="pending_change")))
        assertFalse(taskCanReportProgress(pending.copy(reviewStatus="pending_delete")))
        assertTrue(taskCanReportProgress(pending.copy(reviewStatus="approved",status="done")))
        assertFalse(taskCanReportProgress(pending.copy(approvalStatus="pending")))
    }
    @Test fun rejectionRequiresAReasonBeforeSendingRequest() {
        MockWebServer().use {server->
            server.start();val api=ApiClient(server.url("/").toString());val me=Session(api.server,"manager",2,"TEAM_LEAD",null,"leader")
            try {api.rejectTask(me,31,"   ");fail("empty reason accepted")} catch(_:IllegalArgumentException) {}
            assertEquals(0,server.requestCount)
        }
    }
}
