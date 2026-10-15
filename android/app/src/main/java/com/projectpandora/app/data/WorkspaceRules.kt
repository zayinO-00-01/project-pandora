package com.projectpandora.app.data

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.time.Instant
import java.time.ZoneId

fun canWriteLog(date: String,reference: LocalDate=today()): Boolean = runCatching {
    val parsed=LocalDate.parse(date)
    parsed>=reference.minusDays(7) && parsed<=reference
}.getOrDefault(false)

fun logEditorReadOnly(buffer: EditBuffer,logs: List<WorkLog>): Boolean = buffer.logId!=null &&
    !canWriteLog(logs.firstOrNull {it.id==buffer.logId}?.logDate ?: "")

object AppRoles {
    fun canViewDown(role: String)=role in setOf("FOUNDER","DEPT_HEAD","TEAM_LEAD")
    fun canDispatch(role: String)=canViewDown(role)
    fun homeTitle(role: String)=when(role) {"FOUNDER"->"公司总览";"DEPT_HEAD"->"部门总览";"TEAM_LEAD"->"团队总览";else->"我的工作"}
    fun scopeLabel(role: String)=when(role) {"FOUNDER"->"公司组织";"DEPT_HEAD"->"部门成员";"TEAM_LEAD"->"团队成员";else->"查看上级"}
    val business=setOf("STAFF","TEAM_LEAD","DEPT_HEAD","FOUNDER")
    fun label(role: String)=when(role) {
        "STAFF"->"员工";"TEAM_LEAD"->"团队长";"DEPT_HEAD"->"部门老总";"FOUNDER"->"创始人"
        "ADMIN"->"系统管理员";else->"未知角色"
    }
}
fun periodDates(date: LocalDate, mode: String): List<LocalDate> {
    val first=when(mode) {"week"->date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));"month"->date.withDayOfMonth(1);else->date}
    val count=when(mode) {"week"->7;"month"->date.lengthOfMonth();else->1}
    return (0 until count).map {first.plusDays(it.toLong())}
}
fun taskDate(task: DispatchTask): LocalDate?=task.dueAt?.let {
    runCatching {Instant.parse(it).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate()}.getOrNull()
}
