package com.projectpandora.app.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit

object TaskDispatchValidation {
    private val format=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT)
    val zone: ZoneId=ZoneId.of("Asia/Shanghai")
    fun earliestDeadlineMinute(reference: Instant=Instant.now()): LocalDateTime {
        val minute=reference.truncatedTo(ChronoUnit.MINUTES)
        return (if(reference==minute)minute else minute.plus(1,ChronoUnit.MINUTES)).atZone(zone).toLocalDateTime()
    }
    fun localTime(value: String?)=value?.let {runCatching {LocalDateTime.parse(it,format)}.getOrNull()}
    fun displayTime(value: String?)=value?.let {runCatching {Instant.parse(it).atZone(zone).format(format)}.getOrNull()}.orEmpty()
    fun timestamp(value: String)=LocalDateTime.parse(value,format).atZone(zone).toInstant().toString()
    fun needsOriginalSchedule(buffer: TaskDispatchBuffer)=
        buffer.taskId!=null && buffer.originalStartAt==null && buffer.originalDueAt==null
    fun restoreOriginalSchedule(buffer: TaskDispatchBuffer,task: DispatchTask): TaskDispatchBuffer =
        if(needsOriginalSchedule(buffer) && buffer.taskId==task.id)
            buffer.copy(originalStartAt=task.startAt,originalDueAt=task.dueAt)
        else buffer
    private fun editedTime(value: String?,original: String?): String? {
        val text=value?.trim()?.takeIf {it.isNotEmpty()} ?: return null
        return original?.takeIf {displayTime(it)==text} ?: timestamp(text)
    }
    fun input(buffer: TaskDispatchBuffer): TaskCreateInput = TaskCreateInput(buffer.title.trim(),buffer.detail.trim(),buffer.priority,buffer.assigneeId ?: 0,
        editedTime(buffer.dueDate,buffer.originalDueAt),buffer.labelId.takeIf {buffer.taskId==null},
        editedTime(buffer.startDate,buffer.originalStartAt),buffer.labelIds.takeIf {buffer.taskId==null})
    fun assigneeIds(buffer: TaskDispatchBuffer)=buffer.assigneeIds ?: listOfNotNull(buffer.assigneeId)
    fun error(buffer: TaskDispatchBuffer, members: List<UserSummary>): String? {
        val people=assigneeIds(buffer)
        if(people.isEmpty() || people.size>100)return "请选择1至100位责任人"
        if(people.any {id->id!=buffer.userId && members.none {it.id==id && it.role in AppRoles.business}})return "请选择当前公司的有效业务成员"
        if(buffer.assigneeId!=buffer.userId && members.none {it.id==buffer.assigneeId && it.role in AppRoles.business})return "请选择当前公司的业务成员（包括本人）"
        val value=try {input(buffer)} catch(_:Exception) {return "时间无效，请重新选择有效的日期和时间"}
        val requireTimes=buffer.taskId==null || value.dueAt!=buffer.originalDueAt || value.startAt!=buffer.originalStartAt
        return inputError(value,buffer.userId,requireTimes) ?: if(requireTimes)creationDateError(value) else null
    }
    fun assignees(session: Session,organization: Organization): List<UserSummary> =
        listOf(UserSummary(session.userId,session.username,session.displayName,session.role)) +
            (if(AppRoles.canDispatch(session.role))organization.assignees.filter {it.id!=session.userId && it.role in AppRoles.business} else emptyList())
    fun creationDateError(input: TaskCreateInput, reference: Instant=Instant.now()): String? {
        if(input.startAt.isNullOrBlank())return "请选择开始时间"
        val due=input.dueAt?.let {runCatching {Instant.parse(it)}.getOrNull()}
        return if(due!=null && due.isBefore(reference))"截止时间不能早于当前时间" else null
    }
    fun inputError(input: TaskCreateInput,userId: Long,requireTimes: Boolean=true): String? {
        if(input.title.isBlank())return "请填写任务标题"
        if(input.title.length>128)return "任务标题不能超过128字"
        if(input.detail.length>5000)return "任务详情不能超过5000字"
        if(input.priority !in setOf("low","normal","high"))return "请选择有效的优先级"
        if(input.assigneeId<=0)return "请选择公司业务成员"
        if(requireTimes && input.dueAt.isNullOrBlank())return "请选择截止时间"
        if(requireTimes && input.startAt.isNullOrBlank())return "请选择开始时间"
        val due=input.dueAt?.takeIf {it.isNotBlank()}?.let {
            runCatching {Instant.parse(it)}.getOrNull() ?: return "截止时间无效"
        }
        val start=input.startAt?.takeIf {it.isNotBlank()}?.let {
            runCatching {Instant.parse(it)}.getOrNull() ?: return "开始时间无效"
        }
        if(start!=null && due!=null && start.isAfter(due))return "开始时间不能晚于截止时间"
        return null
    }
}
