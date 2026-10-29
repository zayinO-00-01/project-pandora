package com.projectpandora.app.data

import java.time.LocalDate
import java.time.ZoneId

fun today(): LocalDate = LocalDate.now(ZoneId.of("Asia/Shanghai"))
data class Session(val server: String, val token: String, val userId: Long, val role: String, val displayName: String?, val username: String)
data class LoginResponse(val token: String, val role: String, val displayName: String?, val userId: Long)
data class MyProfile(val id: Long,val username: String,val displayName: String?,val role: String,val avatarData: String?)
data class ActionResult(val message: String)

data class UserSummary(val id: Long, val username: String, val displayName: String?, val role: String)
data class Profile(val id: Long, val username: String, val role: String, val displayName: String?, val managerId: Long?,
    val managerName: String?, val avatar: String?, val avatarImage: String?, val phone: String?, val idCard: String?, val birthDate: String?,
    val gender: String?, val hireDate: String?, val emergencyContact: String?, val emergencyPhone: String?, val address: String?)
data class Organization(val members: List<UserSummary> = emptyList(),val manager: UserSummary? = null, val assignees: List<UserSummary> = emptyList())
data class ViewRequest(val id: Long,val requester: UserSummary,val target: UserSummary,val reason: String,val status: String,val updatedAt: String)
data class WorkLog(val id: Long, val userId: Long, val logDate: String, val content: String, val status: String, val createdAt: String, val updatedAt: String, val endDate: String? = null, val label: WorkLabel? = null)
data class PanelItem(val id: Long, val title: String, val sortOrder: Int?, val myRemark: String?, val detail: String? = null)
data class IntegrityResponse(val ok: Boolean, val message: String?, val supportsDateRanges: Boolean = false)
data class PanelMap(val companyImportant: List<PanelItem> = emptyList(), val companyDispatch: List<PanelItem> = emptyList(), val personalTop: List<PanelItem> = emptyList(), val todayLogs: List<WorkLog> = emptyList())
data class AppNotification(val id: Long, val type: String, val title: String, val body: String, val taskId: Long?, val read: Boolean, val createdAt: String)
data class NotificationList(val items: List<AppNotification> = emptyList(), val unreadCount: Long = 0)
data class LogInput(val logDate: String, val content: String, val status: String, val endDate: String? = null,val labelId: Long? = null)
data class EditBuffer(val server: String, val userId: Long, val logId: Long?, val date: String, val content: String, val status: String, val endDate: String? = null,val labelId: Long? = null)

data class DispatchTask(val id: Long, val title: String, val detail: String, val priority: String,
    val status: String, val dueAt: String?, val progress: Int?, val progressNote: String,
    val createdBy: Long, val assigneeId: Long, val creatorName: String, val assigneeName: String,
    val createdAt: String, val updatedAt: String,
    val approvalStatus: String? = "approved", val approvalNote: String? = "",
    val reviewerId: Long? = null, val reviewedAt: String? = null,
    val reviewStatus: String? = "none", val reviewReason: String? = null, val reviewedBy: Long? = null,
    val lifecycleReviewedAt: String? = null, val deleted: Boolean = false, val pendingChange: String? = null,
    val label: WorkLabel? = null, val keywords: List<TaskKeyword>? = null)
data class TaskProgressInput(val progress: Int?, val progressNote: String, val status: String? = null)
data class TaskEditBuffer(val server: String, val userId: Long, val taskId: Long, val progress: String, val note: String, val status: String? = null)

data class TaskCreateInput(val title: String, val detail: String, val priority: String, val assigneeId: Long, val dueAt: String?,val labelId: Long? = null)
data class TaskDispatchBuffer(val server: String, val userId: Long, val title: String = "", val detail: String = "", val priority: String = "normal", val assigneeId: Long? = null, val dueDate: String = "",val taskId: Long? = null,val labelId: Long? = null)


data class PersonalPriorityWrite(val id: Long?, val title: String, val detail: String, val sortOrder: Int)
data class PersonalPrioritiesInput(val revision: Long, val items: List<PersonalPriorityWrite>)
data class PersonalPrioritySnapshot(val revision: Long, val items: List<PanelItem>)
data class CompanyNotice(val id: Long, val title: String, val content: String, val sortOrder: Int?, val enabled: Boolean, val updatedAt: String, val myNote: String?) {
    fun panel()=PanelItem(id,title,sortOrder,myNote,content)
}
data class CompanyRemarkInput(val remark: String)
/** PUT /company-notices/{id}/note 的返回：只包含调用者自己的备注。 */
data class NoticeNote(val id: Long, val myNote: String?)

data class WorkLabel(val id: Long,val name: String,val color: String,val active: Boolean = true)
data class TaskReview(val id: Long,val taskId: Long,val taskTitle: String,val applicantId: Long,val applicantName: String,val reviewerId: Long,val reviewerName: String,val operation: String,val payload: String,val status: String,val createdAt: String,val decidedAt: String?)
fun reviewOperation(value: String)=when(value) {"CREATE"->"创建任务";"EDIT"->"编辑任务";"DELETE"->"删除任务";"KEYWORD_EDIT"->"修改他人关键词";"KEYWORD_DELETE"->"删除他人关键词";else->"更新进度"}
fun reviewStatus(value: String?)=when(value) {"pending"->"待审核";"approved"->"已批准";"canceled"->"已撤回";else->"已拒绝"}

data class TaskKeyword(val id: Long,val text: String,val createdBy: Long)
