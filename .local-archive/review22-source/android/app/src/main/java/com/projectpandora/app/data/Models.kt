package com.projectpandora.app.data

import java.time.LocalDate
import java.time.ZoneId

fun today(): LocalDate = LocalDate.now(ZoneId.of("Asia/Shanghai"))
data class Session(val server: String, val token: String, val userId: Long, val role: String, val displayName: String?, val username: String)
data class LoginResponse(val token: String, val role: String, val displayName: String?, val userId: Long)
data class UserSummary(val id: Long, val username: String, val displayName: String?, val role: String)
data class WorkLog(val id: Long, val userId: Long, val logDate: String, val content: String, val status: String, val createdAt: String, val updatedAt: String)
data class PanelItem(val id: Long, val title: String, val sortOrder: Int?, val myRemark: String?)
data class PanelMap(val companyImportant: List<PanelItem> = emptyList(), val companyDispatch: List<PanelItem> = emptyList(), val personalTop: List<PanelItem> = emptyList(), val todayLogs: List<WorkLog> = emptyList())
data class LogInput(val logDate: String, val content: String, val status: String)
data class EditBuffer(val server: String, val userId: Long, val logId: Long?, val date: String, val content: String, val status: String)

data class DispatchTask(val id: Long, val title: String, val detail: String, val priority: String,
    val status: String, val dueAt: String?, val progress: Int, val progressNote: String,
    val createdBy: Long, val assigneeId: Long, val creatorName: String, val assigneeName: String,
    val createdAt: String, val updatedAt: String)
data class TaskProgressInput(val progress: Int, val progressNote: String)
data class TaskEditBuffer(val server: String, val userId: Long, val taskId: Long, val progress: String, val note: String)