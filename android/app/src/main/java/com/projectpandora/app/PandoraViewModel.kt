package com.projectpandora.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.projectpandora.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Owns server-backed UI state. Every request captures its session and generation. */
data class AppState(
    val session: Session? = null, val server: String = "", val username: String = "staff",
    val developmentPreview: Boolean = false,
    val busyLogin: Boolean = false, val loading: Boolean = false, val saving: Boolean = false,
    val logs: List<WorkLog> = emptyList(), val panels: PanelMap? = null,
    val tasks: List<DispatchTask> = emptyList(), val taskLoading: Boolean = false,
    val taskError: String? = null, val selectedTaskId: Long? = null,
    val organization: Organization? = null,val viewRequests: List<ViewRequest> = emptyList(),val organizationLoading: Boolean = false,
    val organizationError: String? = null,val member: UserSummary? = null,val memberLogs: List<WorkLog> = emptyList(),val memberTasks: List<DispatchTask> = emptyList(),
    val dispatchEditor: TaskDispatchBuffer? = null, val dispatchOpen: Boolean = false,
    val dispatchMembers: List<UserSummary> = emptyList(), val dispatchLoading: Boolean = false, val dispatchError: String? = null,
    val taskEditor: TaskEditBuffer? = null, val taskEditorError: String? = null,
    val logError: String? = null, val panelError: String? = null, val authError: String? = null,
    val editor: EditBuffer? = null, val editorError: String? = null,
    val screen: String = "home", val selectedDate: String = today().toString(),
    val notice: String? = null,
    val checkingIntegrity: Boolean = false, val integrityDate: String? = null, val integrityError: String? = null
)
class PandoraViewModel(app: Application): AndroidViewModel(app) {
    private val store = SessionStore(app)
    private val mutable = MutableStateFlow(AppState(server=store.server(), username=store.username()))
    val state = mutable.asStateFlow()
    private var refreshJob: Job? = null
    private var generation = 0
    private var organizationJob: Job? = null
    private var dispatchJob: Job? = null
    init { store.session()?.let { restore(it) } }
    private fun api(session: Session): ApiClient {
        check(!state.value.developmentPreview) {"开发预览不提交数据，请退出预览并登录真实账号。"}
        return ApiClient(session.server)
    }
    fun enterDevelopment() {
        if(!BuildConfig.DEBUG || state.value.busyLogin || state.value.session!=null)return
        generation++;refreshJob?.cancel();organizationJob?.cancel();dispatchJob?.cancel()
        // This local identity is never stored as a login or sent to any server.
        val preview=Session("本地开发预览","",-1,"STAFF","开发预览","developer")
        mutable.value=AppState(session=preview,server=state.value.server,username=state.value.username,
            developmentPreview=true,panels=PanelMap(),integrityDate=today().toString(),
            notice="开发预览：可查看页面及编辑布局，真实数据和保存需登录。")
    }
    fun loadOrganization() {
        val me=state.value.session ?: return
        organizationJob?.cancel();dispatchJob?.cancel()
        if(state.value.developmentPreview) {mutable.update {it.copy(organizationError="开发预览没有真实组织关系，请登录查看。")};return}
        val gen=generation
        mutable.update {it.copy(organizationLoading=true,organizationError=null,member=null,memberLogs=emptyList(),memberTasks=emptyList())}
        organizationJob=viewModelScope.launch {
            try {
                val org=withContext(Dispatchers.IO) {api(me).organization(me)}
                val requests=withContext(Dispatchers.IO) {api(me).viewRequests(me)}
                if(gen==generation)mutable.update {it.copy(organization=org,viewRequests=requests,organizationLoading=false)}
            } catch(e:CancellationException) {throw e}
              catch(e:Exception) {if(gen==generation)mutable.update {it.copy(organizationLoading=false,organization=null,viewRequests=emptyList(),organizationError=if(e is ApiException && e.status==404)"当前服务尚未开放组织与查看申请接口，请更新服务端。" else message(e))}}
        }
    }
    fun openMember(member: UserSummary) {
        val me=state.value.session ?: return
        val gen=generation
        organizationJob?.cancel();dispatchJob?.cancel()
        mutable.update {it.copy(member=member,organizationLoading=true,organizationError=null,memberLogs=emptyList(),memberTasks=emptyList())}
        organizationJob=viewModelScope.launch {
            try {
                val logs=withContext(Dispatchers.IO) {api(me).memberLogs(me,member.id)}
                val tasks=withContext(Dispatchers.IO) {api(me).memberTasks(me,member.id)}
                if(gen==generation)mutable.update {it.copy(memberLogs=logs.filter {log->log.status=="submitted"},memberTasks=tasks,organizationLoading=false)}
            } catch(e:CancellationException) {throw e}
              catch(e:Exception) {if(gen==generation)mutable.update {it.copy(organizationLoading=false,organizationError=message(e))}}
        }
    }
    fun requestView(targetId: Long,reason: String)=viewRequestAction {api(it).requestView(it,targetId,reason)}
    fun decideView(id: Long,status: String)=viewRequestAction {api(it).decideView(it,id,status)}
    private fun viewRequestAction(action: (Session)->ViewRequest) {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview)return
        val gen=generation
        mutable.update {it.copy(saving=true,organizationError=null)}
        viewModelScope.launch {
            try {withContext(Dispatchers.IO) {action(me)};if(gen==generation) {mutable.update {it.copy(saving=false)};loadOrganization()}}
            catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation)mutable.update {it.copy(saving=false,organizationError=message(e))}}
        }
    }
    private fun restore(old: Session) {
        if(old.role !in AppRoles.business) {store.clearSession();mutable.update {it.copy(authError="系统管理员请使用 Web 管理端")};return}
        mutable.update { it.copy(busyLogin=true) }
        viewModelScope.launch {
            try {
                val restored=withContext(Dispatchers.IO) {
                    try {
                        val me=api(old).users(old).firstOrNull {it.id==old.userId} ?: throw ApiException(401,"账号已失效，请重新登录")
                        old.copy(role=me.role,displayName=me.displayName)
                    } catch(e: ApiException) {
                        if(e.status!=404)throw e
                        // main has no business member-list endpoint; validate the token before restoring.
                        val check=api(old).integrity(old)
                        if(!check.ok)throw ApiException(401,"账号校验失败，请重新登录")
                        old
                    }
                }
                activate(restored)
            } catch (e: CancellationException) { throw e }
              catch (e: Exception) {
                if(e is ApiException && e.status==401)store.clearSession()
                mutable.update {it.copy(authError=message(e),busyLogin=false)}
            }
        }
    }
    fun login(server: String, username: String, password: String) {
        if(state.value.busyLogin)return
        val normalized=try {ServerAddress.normalize(server)} catch(e: IllegalArgumentException) {
            mutable.update {it.copy(authError=e.message)};return
        }
        if(username.isBlank() || password.isBlank()) {
            mutable.update {it.copy(authError="请输入账号和密码")};return
        }
        mutable.update {it.copy(busyLogin=true,authError=null,server=server,username=username.trim())}
        viewModelScope.launch {
            try {activate(withContext(Dispatchers.IO) { ApiClient(normalized).login(username.trim(),password) })}
            catch(e: Exception) {mutable.update {it.copy(busyLogin=false,authError=message(e))}}
        }
    }
    private fun activate(session: Session) {
        if(session.role !in AppRoles.business) {
            store.clearSession()
            mutable.value=AppState(server=session.server,username=session.username,authError="此账号不能进入 App 工作空间。系统管理员请使用 Web 管理端。")
            return
        }
        generation++;refreshJob?.cancel();organizationJob?.cancel();dispatchJob?.cancel();store.save(session)
        val pending=store.buffer(session)
        val taskPending=store.taskBuffer(session)
        val dispatchPending=if(AppRoles.canDispatch(session.role))store.dispatchBuffer(session) else null
        mutable.value=AppState(session=session,server=session.server,username=session.username,
            editor=pending,taskEditor=taskPending,selectedTaskId=taskPending?.taskId,dispatchEditor=dispatchPending,
            screen=if(pending!=null)"logs" else if(taskPending!=null || dispatchPending!=null)"tasks" else "home",
            notice=if(pending==null && taskPending==null && dispatchPending==null)null else "已恢复上次未保存的内容，请检查后保存。")
        refresh()
        if(pending!=null)checkIntegrity()
    }
    fun logout() {
        if(state.value.saving)return
        generation++;refreshJob?.cancel();organizationJob?.cancel();dispatchJob?.cancel()
        if(!state.value.developmentPreview)store.clearSession()
        mutable.value=AppState(server=state.value.server,username=state.value.username)
    }
    fun screen(name: String) {
        if(state.value.saving || state.value.editor!=null)return
        mutable.update {it.copy(screen=name,notice=null)}
        if(name=="organization")loadOrganization()
        if(name=="home" || name=="tasks")refresh()
        if(name=="logs")checkIntegrity()
    }
    fun openDispatch() {
        val current=state.value
        val me=current.session ?: return
        if(current.saving || !AppRoles.canDispatch(me.role))return
        if(current.developmentPreview) {mutable.update {it.copy(notice="请登录真实业务账号后派发任务。")};return}
        val draft=current.dispatchEditor ?: TaskDispatchBuffer(me.server,me.userId)
        store.saveDispatchBuffer(draft)
        mutable.update {it.copy(dispatchEditor=draft,dispatchOpen=true,dispatchError=null,selectedTaskId=null,screen="tasks",notice=null)}
        loadDispatchMembers()
    }
    fun loadDispatchMembers() {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview || !AppRoles.canDispatch(me.role))return
        dispatchJob?.cancel()
        val gen=generation
        mutable.update {it.copy(dispatchLoading=true,dispatchError=null,dispatchMembers=emptyList())}
        dispatchJob=viewModelScope.launch {
            try {
                val members=withContext(Dispatchers.IO) {api(me).organization(me).assignees}
                    .filter {it.id!=me.userId && it.role in AppRoles.business}
                if(gen==generation)mutable.update {it.copy(dispatchMembers=members,dispatchLoading=false)}
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {
                if(gen==generation) {
                    if(e is ApiException && e.status==401)failure(e,false)
                    else mutable.update {it.copy(dispatchLoading=false,dispatchError=message(e))}
                }
            }
        }
    }
    fun closeDispatch() {if(!state.value.saving)mutable.update {it.copy(dispatchOpen=false)}}
    fun changeDispatch(title: String? = null,detail: String? = null,priority: String? = null,assigneeId: Long? = null,dueDate: String? = null) {
        if(state.value.saving)return
        val draft=state.value.dispatchEditor ?: return
        val updated=draft.copy(title=title ?: draft.title,detail=detail ?: draft.detail,priority=priority ?: draft.priority,
            assigneeId=assigneeId ?: draft.assigneeId,dueDate=dueDate ?: draft.dueDate)
        store.saveDispatchBuffer(updated)
        mutable.update {it.copy(dispatchEditor=updated,dispatchError=null)}
    }
    fun discardDispatch() {
        if(state.value.saving)return
        state.value.session?.let {store.clearDispatchBuffer(it)}
        mutable.update {it.copy(dispatchEditor=null,dispatchOpen=false,dispatchError=null)}
    }
    fun createTask() {
        val current=state.value
        val me=current.session ?: return
        val draft=current.dispatchEditor ?: return
        if(current.saving || current.dispatchLoading || current.developmentPreview || !AppRoles.canDispatch(me.role))return
        TaskDispatchValidation.error(draft,current.dispatchMembers)?.let {error->mutable.update {it.copy(dispatchError=error)};return}
        val gen=generation
        mutable.update {it.copy(saving=true,dispatchError=null,notice=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {
                    val members=api(me).organization(me).assignees
                    TaskDispatchValidation.error(draft,members)?.let {throw IllegalArgumentException(it)}
                    api(me).createTask(me,TaskDispatchValidation.input(draft))
                }
                if(gen!=generation || state.value.session!=me)return@launch
                store.clearDispatchBuffer(me)
                mutable.update {it.copy(saving=false,dispatchEditor=null,dispatchOpen=false,dispatchError=null,
                    tasks=listOf(result)+it.tasks.filterNot {old->old.id==result.id},selectedTaskId=result.id,notice="任务已派发，责任人刷新后即可查看。")}
                refresh()
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {
                if(gen==generation && state.value.session==me) {
                    if(e is ApiException && e.status==401)failure(e,false)
                    else mutable.update {it.copy(saving=false,dispatchError=message(e))}
                }
            }
        }
    }
    fun openTask(id: Long) {
        if(state.value.saving || state.value.editor!=null)return
        mutable.update {it.copy(screen="tasks",selectedTaskId=id,notice=null)}
        refresh()
    }
    fun closeTask() {
        if(state.value.saving)return
        // The task buffer stays available when navigating or signing out.
        mutable.update {it.copy(selectedTaskId=null,taskEditorError=null)}
    }
    fun editTask(task: DispatchTask) {
        val me=state.value.session ?: return
        if(state.value.saving || task.assigneeId!=me.userId)return
        val pending=state.value.taskEditor
        if(pending!=null && pending.taskId!=task.id)return
        val buffer=pending ?: TaskEditBuffer(me.server,me.userId,task.id,task.progress.toString(),task.progressNote)
        store.saveTaskBuffer(buffer)
        mutable.update {it.copy(taskEditor=buffer,taskEditorError=null,selectedTaskId=task.id,screen="tasks",notice=null)}
    }
    fun changeTaskEditor(progress: String? = null, note: String? = null) {
        if(state.value.saving)return
        val current=state.value.taskEditor ?: return
        val buffer=current.copy(progress=progress ?: current.progress,note=note ?: current.note)
        store.saveTaskBuffer(buffer)
        mutable.update {it.copy(taskEditor=buffer,taskEditorError=null)}
    }
    fun discardTaskEditor() {
        if(state.value.saving)return
        state.value.session?.let {store.clearTaskBuffer(it)}
        mutable.update {it.copy(taskEditor=null,taskEditorError=null)}
    }
    fun saveTaskProgress() {
        val current=state.value
        val me=current.session ?: return
        val buffer=current.taskEditor ?: return
        if(current.saving || current.selectedTaskId!=buffer.taskId)return
        val task=current.tasks.firstOrNull {it.id==buffer.taskId}
        if(task==null || task.assigneeId!=me.userId) {
            mutable.update {it.copy(taskEditorError="请刷新任务并确认你是当前责任人后再提交。")};return
        }
        val validation=TaskValidation.error(buffer.progress,buffer.note)
        if(validation!=null) {mutable.update {it.copy(taskEditorError=validation)};return}
        val gen=generation
        refreshJob?.cancel()
        mutable.update {it.copy(saving=true,loading=false,taskLoading=false,taskEditorError=null,notice=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {
                    api(me).saveProgress(me,buffer.taskId,TaskProgressInput(buffer.progress.toInt(),buffer.note.trim()))
                }
                if(gen!=generation || state.value.session!=me)return@launch
                store.clearTaskBuffer(me)
                mutable.update {it.copy(saving=false,taskEditor=null,taskEditorError=null,
                    tasks=it.tasks.map {old->if(old.id==result.id)result else old},
                    notice="进度已保存，领导刷新即可看到最新反馈。")}
                refresh()
            } catch(e: CancellationException) {throw e}
              catch(e: Exception) {if(gen==generation && state.value.session==me)failure(e,false,true)}
        }
    }
    fun selectDate(date: String) {mutable.update {it.copy(selectedDate=date)}}
    fun edit(log: WorkLog? = null) {
        if(state.value.saving)return
        if((log==null || canWriteLog(log.logDate)) && state.value.integrityDate!=today().toString()) {checkIntegrity {edit(log)};return}
        val me=state.value.session ?: return
        val buffer=EditBuffer(me.server,me.userId,log?.id,log?.logDate ?: today().toString(),log?.content ?: "",log?.status ?: "draft")
        if(!state.value.developmentPreview)store.saveBuffer(buffer)
        mutable.update {it.copy(editor=buffer,editorError=null,notice=null,screen="logs")}
    }
    fun changeEditor(date: String? = null, content: String? = null) {
        state.value.editor?.let {if(logEditorReadOnly(it,state.value.logs))return}
        if(state.value.saving)return
        val current=state.value.editor ?: return
        val buffer=current.copy(date=date ?: current.date,content=content ?: current.content)
        if(!state.value.developmentPreview)store.saveBuffer(buffer)
        mutable.update {it.copy(editor=buffer,editorError=null)}
    }
    fun discardEditor() {
        if(state.value.saving)return
        state.value.session?.let {store.clearBuffer(it)}
        mutable.update {it.copy(editor=null,editorError=null,notice=null)}
    }
    fun save(status: String) {
        val current=state.value
        if(current.saving)return
        current.editor?.let {if(logEditorReadOnly(it,current.logs)) {mutable.update {s->s.copy(editorError="该日志已超出可修改日期，仅供查看")};return}}
        if(current.integrityDate!=today().toString()) {checkIntegrity {save(status)};return}
        val me=current.session ?: return
        val buffer=current.editor ?: return
        val validation=LogValidation.error(buffer.date,buffer.content,status)
        if(validation!=null) {mutable.update {it.copy(editorError=validation)};return}
        if((buffer.status=="submitted" || current.logs.any {it.id==buffer.logId && it.status=="submitted"}) && status=="draft") {mutable.update {it.copy(editorError="已提交日志不能改为草稿，请保存已提交修改。")};return}
        val gen=generation
        mutable.update {it.copy(saving=true,editorError=null,notice=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {api(me).save(me,buffer.logId,LogInput(buffer.date,buffer.content.trim(),status))}
                if(gen!=generation)return@launch
                store.clearBuffer(me)
                mutable.update {it.copy(saving=false,editor=null,editorError=null,selectedDate=result.logDate,
                    logs=(it.logs.filterNot {old->old.id==result.id}+result).sortedByDescending {log->log.createdAt},
                    notice=if(result.status=="draft")"草稿已保存，仅你自己可见。" else "日志已保存，领导刷新即可看到最新内容。")}
                refresh()
            } catch(e: Exception) {
                if(gen!=generation)return@launch
                failure(e,true)
            }
        }
    }
    fun submit(log: WorkLog) {
        if(state.value.saving)return
        if(!canWriteLog(log.logDate)) {mutable.update {it.copy(notice="该日志已超出可修改日期，仅供查看")};return}
        if(state.value.integrityDate!=today().toString()) {checkIntegrity {submit(log)};return}
        val me=state.value.session ?: return
        val gen=generation
        mutable.update {it.copy(saving=true,notice=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {api(me).submit(me,log.id)}
                if(gen!=generation)return@launch
                mutable.update {it.copy(saving=false,logs=it.logs.map {old->if(old.id==result.id)result else old},notice="日志已提交，领导可以查看。")}
                refresh()
            } catch(e: Exception) {if(gen==generation)failure(e,false)}
        }
    }
    fun refresh() {
        if(state.value.developmentPreview)return
        if(state.value.saving)return
        val me=state.value.session ?: return
        refreshJob?.cancel()
        val gen=generation
        mutable.update {it.copy(loading=true,taskLoading=true,logError=null,panelError=null,taskError=null)}
        refreshJob=viewModelScope.launch {
            val logResult=withContext(Dispatchers.IO) {runCatching {api(me).logs(me)}}
            ensureActive()
            if(gen!=generation)return@launch
            if((logResult.exceptionOrNull() as? ApiException)?.status==401) {failure(logResult.exceptionOrNull()!!,false);return@launch}
            logResult.onSuccess {logs-> mutable.update {it.copy(logs=logs)} }
                .onFailure {e->mutable.update {it.copy(logError=message(e))}}
            val panelResult=withContext(Dispatchers.IO) {runCatching {api(me).panels(me)}}
            ensureActive()
            if(gen!=generation)return@launch
            if((panelResult.exceptionOrNull() as? ApiException)?.status==401) {failure(panelResult.exceptionOrNull()!!,false);return@launch}
            panelResult.onSuccess {panels-> mutable.update {it.copy(panels=panels)} }
                .onFailure {e->mutable.update {it.copy(panelError=message(e))}}
            val taskResult=withContext(Dispatchers.IO) {runCatching {api(me).tasks(me)}}
            ensureActive()
            if(gen!=generation)return@launch
            if((taskResult.exceptionOrNull() as? ApiException)?.status==401) {failure(taskResult.exceptionOrNull()!!,false);return@launch}
            taskResult.onSuccess {tasks->mutable.update {it.copy(tasks=tasks)}}
                .onFailure {e->mutable.update {it.copy(taskError=message(e))}}
            mutable.update {it.copy(loading=false,taskLoading=false)}
        }
    }
    fun checkIntegrity(after: ()->Unit = {}) {
        if(state.value.developmentPreview) {
            mutable.update {it.copy(integrityDate=today().toString())}
            after();return
        }
        val current=state.value
        val me=current.session ?: return
        if(current.integrityDate==today().toString()) {after();return}
        if(current.checkingIntegrity)return
        val gen=generation
        mutable.update {it.copy(checkingIntegrity=true,integrityError=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {api(me).integrity(me)}
                if(gen!=generation)return@launch
                if(!result.ok)throw ApiException(0,result.message ?: "完整性校验失败，请重试")
                mutable.update {it.copy(checkingIntegrity=false,integrityDate=today().toString(),integrityError=null)}
                after()
            } catch(e: CancellationException) {throw e}
              catch(e: Exception) {
                if(gen!=generation)return@launch
                if(e is ApiException && e.status==401)failure(e,false)
                else mutable.update {it.copy(checkingIntegrity=false,integrityError=message(e))}
            }
        }
    }
    private fun failure(e: Throwable, editor: Boolean, taskEditor: Boolean = false) {
        if(e is ApiException && e.status==401) {
            generation++;refreshJob?.cancel();organizationJob?.cancel();dispatchJob?.cancel();store.clearSession()
            // Edits were already saved under the original server/user pair.
            mutable.value=AppState(server=state.value.server,username=state.value.username,
                authError="登录已过期，请重新登录。未保存内容将由同一账号恢复。")
        } else mutable.update {it.copy(saving=false,taskEditorError=if(taskEditor)message(e) else it.taskEditorError,editorError=if(editor)message(e) else it.editorError,
            notice=if(editor || taskEditor)it.notice else message(e))}
    }
    private fun message(e: Throwable): String = e.message ?: "操作失败，请重试"
}
