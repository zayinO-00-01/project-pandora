package com.projectpandora.app.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object TaskDispatchValidation {
    private val format=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT)
    fun input(buffer: TaskDispatchBuffer): TaskCreateInput = TaskCreateInput(buffer.title.trim(),buffer.detail.trim(),buffer.priority,buffer.assigneeId ?: 0,
        buffer.dueDate.trim().takeIf {it.isNotEmpty()}?.let {LocalDateTime.parse(it,format).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toString()})
    fun error(buffer: TaskDispatchBuffer, members: List<UserSummary>): String? {
        if(members.none {it.id==buffer.assigneeId && it.id!=buffer.userId && it.role in AppRoles.business})return "请从当前授权下级中选择责任人"
        val value=try {input(buffer)} catch(_:Exception) {return "截止时间请按 yyyy-MM-dd HH:mm 填写上海时间，或留空"}
        return inputError(value,buffer.userId)
    }
    fun inputError(input: TaskCreateInput,userId: Long): String? {
        if(input.title.isBlank())return "请填写任务标题"
        if(input.title.length>128)return "任务标题不能超过128字"
        if(input.detail.length>5000)return "任务详情不能超过5000字"
        if(input.priority !in setOf("low","normal","high"))return "请选择有效的优先级"
        if(input.assigneeId<=0 || input.assigneeId==userId)return "请选择授权下级作为责任人"
        if(input.dueAt!=null && runCatching {Instant.parse(input.dueAt)}.isFailure)return "截止时间无效"
        return null
    }
}
