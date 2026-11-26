package com.projectpandora.app.data
import org.junit.Assert.*
import org.junit.Test
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import com.google.gson.JsonParser
class BatchDispatchTest {
 @Test fun oldDraftsKeepSingleAssigneeAndNewDraftsKeepAllSelectedPeople() {
 val draft=TaskDispatchBuffer("s",3,title="任务",assigneeId=3,dueDate="2090-12-01 18:00")
 assertEquals(listOf(3L),TaskDispatchValidation.assigneeIds(draft))
 assertEquals(listOf(3L,4L),TaskDispatchValidation.assigneeIds(draft.copy(assigneeIds=listOf(3,4))))
 assertNotNull(TaskDispatchValidation.error(draft.copy(assigneeIds=emptyList()),emptyList()))
 }
 @Test fun selectionUsesOnlyBusinessContactsFromDirectoryWithoutInventingPeople() {
 val staff=DirectoryContact(3,"staff","员工","STAFF",1,"上级",null,null,null)
 val admin=staff.copy(id=7,username="admin",role="ADMIN")
 assertEquals(listOf(UserSummary(3,"staff","员工","STAFF")),directoryAssignees(listOf(staff,admin,staff)))
 assertTrue(directoryAssignees(emptyList()).isEmpty())
 }
 @Test fun submitsAllPeopleInOneBatchRequest() {
 MockWebServer().use {server->
 server.enqueue(MockResponse().setBody("""[{"id":1,"assigneeId":3},{"id":2,"assigneeId":4}]"""));server.start()
 val api=ApiClient(server.url("/").toString());val me=Session(api.server,"t",3,"STAFF",null,"staff")
 val input=TaskCreateInput("任务","","normal",3,"2090-12-01T10:00:00Z",startAt="2090-12-01T09:00:00Z")
 assertEquals(2,api.createTasks(me,listOf(input,input.copy(assigneeId=4))).size)
 val request=server.takeRequest();assertEquals("/api/v1/tasks/batch",request.path);assertEquals(2,JsonParser.parseString(request.body.readUtf8()).asJsonObject["tasks"].asJsonArray.size())
 }
 }
 @Test fun createsOneSharedTaskAndRecognizesEveryResponsiblePerson() {
  MockWebServer().use {server->
   server.enqueue(MockResponse().setBody("""{"id":1,"title":"共享任务","status":"todo","priority":"normal","createdBy":3,"assigneeId":3,"assigneeIds":[3,4]}"""))
   server.start()
   val api=ApiClient(server.url("/").toString());val me=Session(api.server,"t",3,"STAFF",null,"staff")
   val draft=TaskDispatchBuffer(api.server,3,title="共享任务",assigneeId=3,assigneeIds=listOf(3,4),
       dueDate="2090-12-01 18:00",startDate="2090-12-01 17:00")
   val result=api.createTask(me,TaskDispatchValidation.input(draft))
   assertTrue(result.isResponsible(4))
   assertFalse(result.isResponsible(5))
   assertEquals(listOf(result),relatedTasks(listOf(result),4))
   val request=server.takeRequest()
   assertEquals("/api/v1/tasks",request.path)
   assertEquals(listOf(3L,4L),JsonParser.parseString(request.body.readUtf8()).asJsonObject["assigneeIds"].asJsonArray.map {it.asLong})
   assertEquals(1,server.requestCount)
  }
 }
 @Test fun refusesPartialSharedAssigneeResponseAndKeepsLegacySingleResponse() {
  MockWebServer().use {server->
   val legacy="""{"id":1,"createdBy":3,"assigneeId":3}"""
   server.enqueue(MockResponse().setBody(legacy))
   server.enqueue(MockResponse().setBody(legacy))
   server.start()
   val api=ApiClient(server.url("/").toString());val me=Session(api.server,"t",3,"STAFF",null,"staff")
   val input=TaskCreateInput("任务","","normal",3,"2090-12-01T10:00:00Z",startAt="2090-12-01T09:00:00Z")
   assertTrue(api.createTask(me,input).isResponsible(3))
   try {api.createTask(me,input.copy(assigneeIds=listOf(3,4)));fail("accepted a partial result")}
   catch(e:ApiException) {assertEquals(502,e.status)}
  }
 }
}
