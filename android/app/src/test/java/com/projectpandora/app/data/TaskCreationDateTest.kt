package com.projectpandora.app.data
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
class TaskCreationDateTest {
 @Test fun restoredLegacyDraftCanKeepAnExistingOverdueDeadlineAndMissingStart() {
  val original=DispatchTask(1,"旧任务","","normal","todo","2020-11-03T10:00:25Z",0,"",1,2,"a","b","","")
  val local=TaskDispatchBuffer("http://localhost",1,title="补充说明",assigneeId=1,dueDate="2020-11-03 18:00",taskId=1)
  val restored=TaskDispatchValidation.restoreOriginalSchedule(local,original)
  assertEquals(original.dueAt,TaskDispatchValidation.input(restored).dueAt)
  assertNull(TaskDispatchValidation.error(restored,emptyList()))
  assertNotNull(TaskDispatchValidation.error(restored.copy(dueDate="2020-11-04 18:00"),emptyList()))
 }
 @Test fun deadlineCannotBeEarlierThanTheCurrentTimeAndHistoricalStartRemainsAllowed() {
  val today=Instant.parse("2026-11-02T16:00:25Z")
  assertEquals("2026-11-03T00:01",TaskDispatchValidation.earliestDeadlineMinute(today).toString())
  val input=TaskCreateInput("任务","","normal",3,"2026-11-02T16:01:00Z",startAt="2026-11-02T16:00:00Z")
  assertEquals("请选择开始时间",TaskDispatchValidation.creationDateError(input.copy(startAt=null),today))
  assertNull(TaskDispatchValidation.creationDateError(input,today))
  assertEquals("截止时间不能早于当前时间",TaskDispatchValidation.creationDateError(input.copy(dueAt="2026-11-02T16:00:00Z"),today))
  assertNotNull(TaskDispatchValidation.creationDateError(input.copy(dueAt="2026-11-02T15:59:59Z"),today))
  assertNull(TaskDispatchValidation.creationDateError(input.copy(startAt="2026-10-01T01:00:00Z"),today))
  assertNull(TaskDispatchValidation.creationDateError(input.copy(startAt=input.dueAt),today))
  assertNull(TaskDispatchValidation.creationDateError(input.copy(dueAt="2026-11-05T10:00:00Z",startAt="2026-11-04T10:00:00Z"),today))
  assertNotNull(TaskDispatchValidation.creationDateError(input,Instant.parse("2026-11-03T02:00:00Z")))
 }
}
