package com.projectpandora.app.data

import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.time.Instant
import java.time.ZoneId

fun canWriteLog(date: String,reference: LocalDate=today()): Boolean = runCatching {
    val parsed=LocalDate.parse(date)
    parsed>=reference.minusDays(3) && parsed<=reference
}.getOrDefault(false)

fun canModifyLog(date: String,reference: LocalDate=today()): Boolean = date==reference.toString()

fun logEditorReadOnly(buffer: EditBuffer,logs: List<WorkLog>): Boolean = buffer.logId!=null &&
    !canModifyLog(logs.firstOrNull {it.id==buffer.logId}?.logDate ?: "")

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

// Used only when upgrading older percentage-only feedback or local drafts.
fun legacyTaskStatus(progress: Int?): String=when(progress) {0,null->"todo";100->"done";else->"doing"}
fun taskStatusLabel(status: String)=when(status) {"doing"->"进行中";"done"->"已完成";else->"待开始"}
fun taskFeedbackLabel(task: DispatchTask)=taskStatusLabel(task.status)

/** Staff see only their pending work; leaders see their authored assignments, including completed ones. */
fun dutyTasks(tasks: List<DispatchTask>,userId: Long,leader: Boolean): List<DispatchTask> =
    orderedTasks(tasks.filter {if(leader)it.createdBy==userId else it.assigneeId==userId && it.status!="done"})

fun taskPriorityRank(priority: String)=when(priority) {"high"->0;"low"->2;else->1}
fun taskPriorityLabel(priority: String)=when(priority) {"high"->"高优先级";"low"->"低优先级";else->"普通优先级"}
fun orderedTasks(tasks: List<DispatchTask>): List<DispatchTask> = tasks.sortedWith(
    compareBy<DispatchTask> {it.status=="done"}.thenBy {taskPriorityRank(it.priority)}
        .thenBy {it.dueAt?.let {due->runCatching {Instant.parse(due)}.getOrNull()} ?: Instant.MAX}.thenBy {it.id})

fun reorderedPriorities(items: List<PanelItem>,id: Long,targetId: Long): List<PanelItem> {
    val from=items.indexOfFirst {it.id==id};val to=items.indexOfFirst {it.id==targetId}
    if(from<0 || to<0 || from==to)return items
    return items.toMutableList().apply {add(to,removeAt(from))}.mapIndexed {index,item->item.copy(sortOrder=index)}
}
