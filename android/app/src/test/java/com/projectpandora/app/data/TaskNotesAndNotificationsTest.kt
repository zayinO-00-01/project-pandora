package com.projectpandora.app.data
import com.google.gson.JsonParser
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
class TaskNotesAndNotificationsTest {
 @Test fun taskNoteContractAndClearing() { MockWebServer().use { s ->
  s.enqueue(MockResponse().setBody("""{"taskId":42,"content":"","updatedAt":null}"""))
  s.enqueue(MockResponse().setBody("""{"taskId":42,"content":"私密","updatedAt":"now"}"""))
  s.enqueue(MockResponse().setBody("""{"taskId":42,"content":"","updatedAt":null}"""))
  s.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"无备注权限"}"""))
  s.start(); val api=ApiClient(s.url("/").toString()); val me=Session(api.server,"token",4,"STAFF",null,"staff")
  assertNull(api.taskNote(me,42).updatedAt); assertEquals("私密",api.saveTaskNote(me,42,"私密").content); assertEquals("",api.saveTaskNote(me,42,"").content)
  val get=s.takeRequest(); assertEquals("GET",get.method); assertEquals("/api/v1/tasks/42/note",get.path)
  val put=s.takeRequest(); assertEquals("PUT",put.method); assertEquals("Bearer token",put.getHeader("Authorization")); assertEquals("私密",JsonParser.parseString(put.body.readUtf8()).asJsonObject["content"].asString)
  assertEquals("",JsonParser.parseString(s.takeRequest().body.readUtf8()).asJsonObject["content"].asString)
  try {api.taskNote(me,43);fail("review access must not grant notes")} catch(e:ApiException) {assertEquals(403,e.status)}
  try {api.saveTaskNote(me,42,"字".repeat(2001));fail("too long")} catch(e:IllegalArgumentException) {assertTrue(e.message!!.contains("2000"))}; assertEquals(4,s.requestCount)
 } }
 @Test fun lateReadAndSavePreserveNewEdits() {
  val loaded=TaskNoteState(taskId=42,loading=true).edit("本机草稿").receive(TaskNote(42,"服务器旧值",null))
  assertEquals("本机草稿",loaded.content); assertTrue(loaded.dirty)
  val saved=loaded.copy(saving=true).edit("保存途中继续输入").saved(TaskNote(42,"本机草稿","now"),"本机草稿")
  assertEquals("保存途中继续输入",saved.content); assertTrue(saved.dirty); assertFalse(saved.saving)
  assertEquals(saved,saved.receive(TaskNote(99,"别的任务",null)))
 }
 @Test fun draftKeySeparatesServerAccountAndTask() {
  assertNotEquals(taskNoteDraftKey("https://a",4,42),taskNoteDraftKey("https://b",4,42))
  assertNotEquals(taskNoteDraftKey("https://a",4,42),taskNoteDraftKey("https://a",5,42))
  assertNotEquals(taskNoteDraftKey("https://a",4,42),taskNoteDraftKey("https://a",4,43))
 }
 @Test fun notificationReferenceAndLegacyRouting() {
  val n=AppNotification(1,"view-request","申请","",null,false,"now",88)
  assertEquals(NotificationDestination("organization",referenceId=88),notificationDestination(n))
  assertEquals(NotificationDestination("organization"),notificationDestination(n.copy(referenceId=null)))
  assertEquals(NotificationDestination("tasks",taskId=42),notificationDestination(n.copy(type="task-review",taskId=42)))
  val read=NotificationList(listOf(n),1).confirmRead(setOf(1)); assertEquals(0L,read.unreadCount); assertTrue(read.items.single().read); assertEquals(read,read.confirmRead(setOf(1)))
 }
 @Test fun acknowledgedBatchPreservesNewlyArrivedUnreadNotifications() {
  val original=AppNotification(1,"task-review","待审","",42,false,"now")
  val arrived=original.copy(id=2)
  val confirmed=NotificationList(listOf(original,arrived),2).confirmRead(setOf(1))
  assertTrue(confirmed.items[0].read); assertFalse(confirmed.items[1].read); assertEquals(1L,confirmed.unreadCount)
 }
 @Test fun detailLoadsWithoutListMembership() { MockWebServer().use {s ->
  s.enqueue(MockResponse().setBody("""{"id":42,"title":"审核详情","detail":"内容","priority":"normal","status":"todo","progressNote":"","createdBy":2,"assigneeId":3,"creatorName":"甲","assigneeName":"乙","createdAt":"now","updatedAt":"now"}"""))
  s.start(); val api=ApiClient(s.url("/").toString()); val me=Session(api.server,"token",4,"TEAM_LEAD",null,"manager")
  assertEquals("审核详情",api.task(me,42).title); assertEquals("/api/v1/tasks/42",s.takeRequest().path)
 } }
}
