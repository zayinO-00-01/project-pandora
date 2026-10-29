package com.projectpandora.app.data

/** Private note only; task progress and review payloads never include this content. */
data class TaskNote(val taskId: Long,val content: String,val updatedAt: String?)
data class TaskNoteState(val taskId: Long? = null,val content: String = "",val updatedAt: String? = null,
    val dirty: Boolean = false,val loading: Boolean = false,val loaded: Boolean = false,val saving: Boolean = false,
    val error: String? = null,val savedMessage: String? = null) {
    fun edit(value: String)=copy(content=value,dirty=true,error=null,savedMessage=null)
    fun receive(note: TaskNote): TaskNoteState = if(note.taskId!=taskId)this else copy(
        content=if(dirty)content else note.content,updatedAt=note.updatedAt,loading=false,loaded=true,error=null)
    fun saved(note: TaskNote,submitted: String): TaskNoteState = if(note.taskId!=taskId)this else copy(
        content=if(content==submitted)note.content else content,dirty=content!=submitted,
        updatedAt=note.updatedAt,saving=false,loaded=true,error=null,savedMessage="备注已保存")
}
fun taskNoteDraftKey(server: String,userId: Long,taskId: Long)="task-note:${server.length}:$server:$userId:$taskId"

data class NotificationDestination(val screen: String,val taskId: Long? = null,val referenceId: Long? = null)
fun notificationDestination(n: AppNotification): NotificationDestination = when {
    n.type=="view-request" -> NotificationDestination("organization",referenceId=n.referenceId)
    n.taskId!=null -> NotificationDestination("tasks",taskId=n.taskId)
    else -> NotificationDestination("inbox")
}
/** Called only after the server confirms the mutation. Repeated acknowledgements are idempotent. */
fun NotificationList.confirmRead(ids: Set<Long>): NotificationList {
    val changed=items.count {!it.read && it.id in ids}
    return copy(items=items.map {if(it.id in ids)it.copy(read=true) else it},unreadCount=(unreadCount-changed).coerceAtLeast(0))
}
