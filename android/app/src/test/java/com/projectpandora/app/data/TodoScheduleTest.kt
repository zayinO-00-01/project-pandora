package com.projectpandora.app.data
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class TodoScheduleTest {
 @Test fun scheduleIncludesBothEndsAndMovesWithoutChangingDuration() {
  val todo=DailyTodo("1","安排","2026-11-21",startAt="2026-11-21T09:00",endAt="2026-11-23T10:00")
  assertTrue(todo.occursOn(LocalDate.parse("2026-11-22")))
  assertFalse(todo.occursOn(LocalDate.parse("2026-11-24")))
  val moved=todo.moveTo(LocalDate.parse("2026-11-22"))
  assertEquals("2026-11-24T10:00",moved.endAt)
  assertTrue(moved.occursOn(LocalDate.parse("2026-11-24")))
  assertTrue(DailyTodo("2","旧事项","2026-11-21").occursOn(LocalDate.parse("2026-11-21")))
 }
}
