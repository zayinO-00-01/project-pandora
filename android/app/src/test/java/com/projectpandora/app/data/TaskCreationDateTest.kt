package com.projectpandora.app.data
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class TaskCreationDateTest {
 @Test fun shanghaiMidnightIsInclusiveAndStartIsAlsoChecked() {
  val today=LocalDate.of(2026,11,3)
  val input=TaskCreateInput("任务","","normal",3,"2026-10-31T16:00:00Z")
  assertNull(TaskDispatchValidation.creationDateError(input,today))
  assertNotNull(TaskDispatchValidation.creationDateError(input.copy(dueAt="2026-10-31T15:59:59Z"),today))
  assertNotNull(TaskDispatchValidation.creationDateError(input.copy(startAt="2026-10-30T16:00:00Z"),today))
  assertNull(TaskDispatchValidation.creationDateError(input.copy(startAt=input.dueAt),today))
 }
}
