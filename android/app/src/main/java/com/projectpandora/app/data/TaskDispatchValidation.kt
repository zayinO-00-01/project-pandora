package com.projectpandora.app.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object TaskDispatchValidation {
    private val format=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT)
    fun input(buffer: TaskDispatchBuffer): TaskCreateInput = TaskCreateInput(buffer.title.trim(),buffer.detail.trim(),buffer.priority,buffer.assigneeId ?: 0,
        buffer.dueDate.trim().takeIf {it.isNotEmpty()}?.let {LocalDateTime.parse(it,format).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toString()},buffer.labelId)
    fun error(buffer: TaskDispatchBuffer, members: List<UserSummary>): String? {
        if(buffer.assigneeId!=buffer.userId && members.none {it.id==buffer.assigneeId && it.role in AppRoles.business})return "请选择当前公司的业务成员（包括本人）"
        val value=try {input(buffer)} catch(_:Exception) {return "截止时间无效，请重新选择日期和时间，或清除截止时间"}
        return inputError(value,buffer.userId)
    }
    fun assignees(session: Session,organization: Organization): List<UserSummary> =
        listOf(UserSummary(session.userId,session.username,session.displayName,session.role)) +
            (if(AppRoles.canDispatch(session.role))organization.assignees.filter {it.id!=session.userId && it.role in AppRoles.business} else emptyList())
    fun inputError(input: TaskCreateInput,userId: Long): String? {
        if(input.title.isBlank())return "请填写任务标题"
        if(input.title.length>128)return "任务标题不能超过128字"
        if(input.detail.length>5000)return "任务详情不能超过5000字"
        if(input.priority !in setOf("low","normal","high"))return "请选择有效的优先级"
        if(input.assigneeId<=0)return "请选择公司业务成员"
        if(input.dueAt!=null && runCatching {Instant.parse(input.dueAt)}.isFailure)return "截止时间无效"
        return null
    }
}
