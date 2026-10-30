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
    val labelError: String? = null, val labels: List<WorkLabel> = emptyList(),val taskReviewRecords: List<TaskReview> = emptyList(),val taskReviewRecordsReady: Boolean = false,val workspaceError: String? = null,
    val myProfile: MyProfile? = null,
    val session: Session? = null, val server: String = "", val username: String = "staff",
    val developmentPreview: Boolean = false,
    val localPriorities: LocalPriorities = LocalPriorities(),
    val personal: PersonalSync = PersonalSync(), val personalLoading: Boolean = false, val personalError: String? = null,
    val companyNoteDrafts: Map<Long,String> = emptyMap(), val companyNoteError: String? = null,
    val profile: Profile? = null, val profileLoading: Boolean = false, val profileError: String? = null,
    val busyLogin: Boolean = false, val loading: Boolean = false, val saving: Boolean = false,
    val logs: List<WorkLog> = emptyList(), val panels: PanelMap? = null,
    val tasks: List<DispatchTask> = emptyList(), val taskLoading: Boolean = false,
    val reviews: List<DispatchTask> = emptyList(), val reviewsLoading: Boolean = false,
    val notifications: List<AppNotification> = emptyList(), val unreadCount: Long = 0, val notificationsLoading: Boolean = false,
    val taskError: String? = null, val selectedTaskId: Long? = null,
    val pendingTaskApprovals: List<DispatchTask> = emptyList(), val taskReviewError: String? = null,
    val organization: Organization? = null,val viewRequests: List<ViewRequest> = emptyList(),val organizationLoading: Boolean = false,
    val organizationError: String? = null,val member: UserSummary? = null,val memberLogs: List<WorkLog> = emptyList(),val memberTasks: List<DispatchTask> = emptyList(),
    val dispatchEditor: TaskDispatchBuffer? = null, val dispatchOpen: Boolean = false,
    val dispatchMembers: List<UserSummary> = emptyList(), val dispatchLoading: Boolean = false, val dispatchError: String? = null,
    val taskEditor: TaskEditBuffer? = null, val taskEditorError: String? = null,
    val logError: String? = null, val panelError: String? = null, val authError: String? = null,
    val editor: EditBuffer? = null, val editorError: String? = null,
    val screen: String = "home", val selectedDate: String = today().toString(), val calendarMode: String = "month",
    val checkingIntegrity: Boolean = false, val integrityDate: String? = null, val integrityError: String? = null,
    val notice: String? = null
)
class PandoraViewModel(app: Application): AndroidViewModel(app) {
    private val store = SessionStore(app)
    private val priorityStore = PriorityStore(app)
    private val mutable = MutableStateFlow(AppState(server=store.server(), username=store.username()))
    val state = mutable.asStateFlow()
    private var refreshJob: Job? = null
    private var generation = 0
    private var organizationJob: Job? = null
    private var dispatchJob: Job? = null
    private var reviewsJob: Job? = null
    init { store.session()?.let { restore(it) } }
    private fun api(session: Session): ApiClient {
        check(!state.value.developmentPreview) {"开发预览不提交数据，请退出预览并登录真实账号。"}
        return ApiClient(session.server)
    }
    fun enterDevelopment() {
        if(!BuildConfig.DEBUG || state.value.busyLogin || state.value.session!=null)return
        generation++;refreshJob?.cancel();organizationJob?.cancel();dispatchJob?.cancel();reviewsJob?.cancel()
        // This local identity is never stored as a login or sent to any server.
        val preview=Session("本地开发预览","",-1,"STAFF","开发预览","developer")
        mutable.value=AppState(session=preview,server=state.value.server,username=state.value.username,
            developmentPreview=true,localPriorities=priorityStore.read(preview),labels=PreviewWorkspace.labels,tasks=PreviewWorkspace.tasks(),
            panels=PanelMap(companyImportant=CompanyPriorityExamples.items),integrityDate=today().toString(),
            logs=listOf(WorkLog(-201,-1,today().minusDays(2).toString(),"病假：连续三天未到岗。\n恢复后跟进工作安排。","submitted",java.time.Instant.now().toString(),java.time.Instant.now().toString(),today().toString())),
            notice="开发预览：可查看页面及编辑布局，真实数据和保存需登录。")
    }
    private fun persistWorkspace() {
        state.value.session?.let {priorityStore.saveWorkspace(it,WorkspaceDraft(state.value.personal,state.value.companyNoteDrafts))}
    }
    private fun personalPriorities()=if(state.value.developmentPreview)state.value.localPriorities.personal.orEmpty() else state.value.personal.items
    private fun changePersonal(items: List<PanelItem>) {
        if(state.value.saving || (!state.value.developmentPreview && state.value.personal.revision==null))return
        if(state.value.developmentPreview) {
            val local=state.value.localPriorities.copy(personal=items.mapIndexed {i,item->item.copy(sortOrder=i)})
            state.value.session?.let {priorityStore.save(it,local)}
            mutable.update {it.copy(localPriorities=local,notice="预览内容已保存在本机。")}
        } else {
            mutable.update {it.copy(personal=it.personal.edit(items),personalError=null,notice="个人事项待同步，可点击同步保存。")}
            persistWorkspace()
        }
    }
    fun beginPersonalPriorityEdit(expectedRevision: Long?): Boolean {
        val current=state.value
        if(current.saving || current.personalLoading)return false
        if(current.developmentPreview)return true
        if(current.personal.revision==null || current.personal.revision!=expectedRevision) {
            mutable.update {it.copy(personalError="清单刚刚更新，请重新打开事项后编辑。")};return false
        }
        mutable.update {it.copy(personal=it.personal.beginEdit(),personalError=null)}
        persistWorkspace()
        return true
    }
    fun savePersonalPriority(id: Long?,title: String,detail: String): Boolean {
        if(state.value.saving || state.value.personalLoading || (!state.value.developmentPreview && state.value.personal.revision==null))return false
        if(title.isBlank() || title.length>120 || detail.length>2000)return false
        val current=personalPriorities()
        if(id==null && current.size>=10)return false
        if(id!=null && current.none {it.id==id}) {
            mutable.update {it.copy(personalError="事项已变化，输入仍保留，请检查清单后重试。")};return false
        }
        val entry=PanelItem(id ?: ((current.minOfOrNull {it.id} ?: 0L).coerceAtMost(0L)-1),title.trim(),null,null,detail.trim())
        changePersonal(if(id==null)current+entry else current.map {if(it.id==id)entry else it})
        return true
    }
    fun deletePersonalPriority(id: Long) {changePersonal(personalPriorities().filterNot {it.id==id})}
    fun previewPersonalPriorityOrder(id: Long,targetId: Long) {
        if(state.value.saving || state.value.personalLoading)return
        val reordered=reorderedPriorities(personalPriorities(),id,targetId)
        if(state.value.developmentPreview)mutable.update {it.copy(localPriorities=it.localPriorities.copy(personal=reordered))}
        else mutable.update {it.copy(personal=it.personal.copy(draft=reordered))}
    }
    fun finishPersonalPriorityOrder(originalIds: List<Long>,originalPersonal: List<PanelItem>?,cancelled: Boolean) {
        val current=personalPriorities()
        if(cancelled || current.map {it.id}==originalIds) {
            if(state.value.developmentPreview)mutable.update {it.copy(localPriorities=it.localPriorities.copy(personal=originalPersonal))}
            else mutable.update {it.copy(personal=it.personal.copy(draft=originalPersonal))}
        } else changePersonal(current)
    }
    fun importLegacyPersonal() {
        val current=state.value
        if(current.saving || current.personalLoading || current.developmentPreview || current.personal.dirty || current.personal.revision==null)return
        val old=current.localPriorities.personal ?: return
        changePersonal(old.take(10).mapIndexed {index,item->item.copy(id=-(index+1L))})
    }
    fun changeCompanyNote(id: Long,content: String) {
        if(state.value.saving || content.length>2000)return
        mutable.update {it.copy(companyNoteDrafts=it.companyNoteDrafts+(id to content),companyNoteError=null)}
        persistWorkspace()
    }
    fun importLegacyCompanyNotes() {
        if(state.value.saving || state.value.developmentPreview)return
        val ids=state.value.panels?.companyImportant.orEmpty().map {it.id}.toSet()
        mutable.update {it.copy(companyNoteDrafts=it.localPriorities.remarks.filterKeys {id->id in ids}+it.companyNoteDrafts,notice="旧版本机备注已导入待保存区，请逐条确认并保存。")}
        persistWorkspace()
    }
    fun saveCompanyRemark(id: Long,note: String) {
        val me=state.value.session ?: return
        if(state.value.saving || note.length>2000 || state.value.panels?.companyImportant?.none {it.id==id}!=false)return
        changeCompanyNote(id,note)
        if(state.value.developmentPreview) {mutable.update {it.copy(notice="预览备注已保存在本机。")};return}
        val gen=generation
        refreshJob?.cancel()
        mutable.update {it.copy(saving=true,loading=false,taskLoading=false,personalLoading=false,profileLoading=false,companyNoteError=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {api(me).saveCompanyNote(me,id,note)}
                if(gen!=generation || state.value.session!=me)return@launch
                require(result.id==id) {"保存结果不一致，请刷新后确认"}
                mutable.update {it.copy(saving=false,companyNoteDrafts=it.companyNoteDrafts-id,
                    panels=it.panels?.copy(companyImportant=it.panels.companyImportant.map {entry->if(entry.id==id)result.panel() else entry}),notice="备注已同步保存。")}
                persistWorkspace()
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation && state.value.session==me) {
                if(e is ApiException && e.status==401)failure(e,false)
                else mutable.update {it.copy(saving=false,companyNoteError="备注未保存，输入已保留。"+message(e))}
            }}
        }
    }
    fun syncPersonalPriorities(overwrite: Boolean=false) {
        val current=state.value;val me=current.session ?: return
        if(current.saving || current.personalLoading || current.developmentPreview || !current.personal.dirty)return
        if(!overwrite && !current.personal.canSave())return
        if(overwrite && current.personal.conflictRevision==null)return
        val input=current.personal.input(overwrite)
        val gen=generation
        refreshJob?.cancel()
        mutable.update {it.copy(saving=true,loading=false,taskLoading=false,personalLoading=false,profileLoading=false,personalError=null)}
        viewModelScope.launch {
            try {
                val saved=withContext(Dispatchers.IO) {api(me).savePersonalPriorities(me,input)}
                if(gen!=generation || state.value.session!=me)return@launch
                mutable.update {it.copy(saving=false,personal=it.personal.saved(saved),notice="个人事项已同步保存。")}
                persistWorkspace()
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {
                if(gen!=generation || state.value.session!=me)return@launch
                if(e is ApiException && e.status==401) {failure(e,false);return@launch}
                if(e is ApiException && e.status==409) {
                    val latest=withContext(Dispatchers.IO) {runCatching {api(me).personalPriorities(me)}}
                    ensureActive()
                    if(gen!=generation || state.value.session!=me)return@launch
                    latest.onSuccess {snapshot->mutable.update {it.copy(personal=it.personal.receive(snapshot))}}
                    mutable.update {it.copy(saving=false,personalError=if(latest.isSuccess)"另一台设备已修改清单。你的修改已保留，请选择重新载入或确认覆盖。" else "另一台设备已修改清单。你的修改已保留，请刷新后处理。")}
                } else mutable.update {it.copy(saving=false,personalError="个人事项未同步，修改已保留。"+message(e))}
                persistWorkspace()
            }
        }
    }
    fun discardPersonalDraft() {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview)return
        val gen=generation
        refreshJob?.cancel()
        mutable.update {it.copy(saving=true,personalLoading=true,loading=false,taskLoading=false,profileLoading=false,personalError=null)}
        viewModelScope.launch {
            try {
                val snapshot=withContext(Dispatchers.IO) {api(me).personalPriorities(me)}
                if(gen!=generation || state.value.session!=me)return@launch
                mutable.update {it.copy(saving=false,personalLoading=false,personal=PersonalSync().receive(snapshot),notice="已载入服务器清单。")}
                persistWorkspace()
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation && state.value.session==me) {
                if(e is ApiException && e.status==401)failure(e,false)
                else mutable.update {it.copy(saving=false,personalLoading=false,personalError="读取失败，原修改仍保留。"+message(e))}
            }}
        }
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
              catch(e:Exception) {if(gen==generation)mutable.update {it.copy(organizationLoading=false,organization=null,viewRequests=emptyList(),organizationError=if(e is ApiException && e.status==404)"暂时无法读取组织信息，请检查连接设置或联系管理员。" else message(e))}}
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
                    val me=api(old).profile(old)
                    if(me.id!=old.userId)throw ApiException(401,"账号已失效，请重新登录")
                    old.copy(role=me.role,displayName=me.displayName)
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
        generation++;refreshJob?.cancel();organizationJob?.cancel();dispatchJob?.cancel();reviewsJob?.cancel();store.save(session)
        val pending=store.buffer(session)
        val taskPending=store.taskBuffer(session)
        val dispatchPending=if(AppRoles.canCreateTask(session.role))store.dispatchBuffer(session) else null
        val workspace=priorityStore.readWorkspace(session)
        mutable.value=AppState(session=session,server=session.server,username=session.username,localPriorities=priorityStore.read(session),
            personal=if(workspace.personal.dirty)workspace.personal else PersonalSync(),companyNoteDrafts=workspace.companyNotes,personalLoading=true,
            editor=pending,taskEditor=taskPending,selectedTaskId=taskPending?.taskId,dispatchEditor=dispatchPending,
            screen=if(pending!=null)"logs" else if(taskPending!=null || dispatchPending!=null)"tasks" else "home",
            notice=if(pending==null && taskPending==null && dispatchPending==null)null else "已恢复上次未保存的内容，请检查后保存。")
        refresh()
    }
    fun loadWorkspaceExtras() {
        val me=state.value.session ?: return
        if(state.value.developmentPreview || state.value.saving)return
        val gen=generation
        viewModelScope.launch {
            // Keep label data independent from optional review/notification endpoints.
            try {
                val labels=withContext(Dispatchers.IO) {api(me).labels(me)}
                if(gen==generation)mutable.update {it.copy(labels=labels,labelError=null)}
            } catch(e:CancellationException) {throw e}
              catch(e:Exception) {if(gen==generation)mutable.update {it.copy(labelError=if(e is ApiException && e.status==404)"当前服务未提供标签接口，请将本地后端升级到 0.6.0 或更新版本。" else "标签读取失败：${message(e)}")}}
            val failures=mutableListOf<String>()
            if(gen==generation)mutable.update {it.copy(taskReviewRecordsReady=false)}
            try {
                val reviews=withContext(Dispatchers.IO) {api(me).reviews(me)}
                if(gen==generation)mutable.update {it.copy(taskReviewRecords=reviews,taskReviewRecordsReady=true)}
            } catch(e:CancellationException) {throw e}
              catch(e:Exception) {failures.add("审核：${message(e)}")}
            try {
                val notifications=withContext(Dispatchers.IO) {api(me).notifications(me)}
                if(gen==generation)mutable.update {it.copy(notifications=notifications.items,unreadCount=notifications.unreadCount)}
            } catch(e:CancellationException) {throw e}
              catch(e:Exception) {failures.add("通知：${message(e)}")}
            if(gen==generation)mutable.update {it.copy(workspaceError=failures.takeIf {it.isNotEmpty()}?.joinToString("\n"))}
        }
    }

    private fun workspaceAction(action: (Session)->Unit,notice: String,onSuccess: ()->Unit = {}) {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview)return
        val gen=generation;mutable.update {it.copy(saving=true,workspaceError=null,notice=null)}
        viewModelScope.launch {
            try {withContext(Dispatchers.IO) {action(me)}
                if(gen==generation){mutable.update {it.copy(saving=false,notice=notice)};onSuccess();refresh()}
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation) {if(e is ApiException && e.status==401)failure(e,false) else mutable.update {it.copy(saving=false,workspaceError=message(e))}}}
        }
    }
    fun withdrawTask(id: Long)=workspaceAction({api(it).withdrawTask(it,id);Unit},"申请已撤回，可修改后重新提交")
    fun decideTask(id: Long,status: String)=workspaceAction({api(it).decideTask(it,id,status);Unit},"审核结果已保存")
    fun deleteTask(id: Long)=workspaceAction({api(it).deleteTask(it,id);Unit},"任务已删除") {closeTask()}
    fun saveTaskKeyword(taskId: Long,id: Long?,text: String) {
        var result=""
        workspaceAction({result=api(it).saveKeyword(it,taskId,id,text).message},"") {mutable.update {it.copy(notice=result)}}
    }
    fun deleteTaskKeyword(taskId: Long,id: Long) {
        var result=""
        workspaceAction({result=api(it).deleteKeyword(it,taskId,id).message},"") {mutable.update {it.copy(notice=result)}}
    }
    fun saveLabel(id: Long?,name: String,color: String)=workspaceAction({api(it).saveLabel(it,id,name,color);Unit},"标签已保存")
    fun archiveLabel(id: Long)=workspaceAction({api(it).archiveLabel(it,id);Unit},"标签已停用，历史记录保留原标签")
    fun readNotification(message: AppNotification) = workspaceAction({api(it).readNotification(it,message.id)},"通知已读")
    fun editTaskDetails(task: DispatchTask) {
        val me=state.value.session ?: return
        if(state.value.saving || (task.createdBy!=me.userId && me.role!="FOUNDER") || taskHasPendingReview(task,state.value.taskReviewRecords) || task.approvalStatus=="pending" || task.deleted)return
        val due=task.dueAt?.let {runCatching {java.time.Instant.parse(it).atZone(java.time.ZoneId.of("Asia/Shanghai")).format(java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm"))}.getOrNull()}.orEmpty()
        val start=task.startAt?.let {runCatching {java.time.Instant.parse(it).atZone(java.time.ZoneId.of("Asia/Shanghai")).format(java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm"))}.getOrNull()}
        val draft=TaskDispatchBuffer(me.server,me.userId,task.title,task.detail,task.priority,task.assigneeId,due,task.id,task.label?.id ?: 0,start)
        store.saveDispatchBuffer(draft)
        mutable.update {it.copy(dispatchEditor=draft,dispatchOpen=true,screen="tasks",dispatchError=null)}
        loadDispatchMembers()
    }
    fun loadProfile() {
        val me=state.value.session ?: return
        if(state.value.developmentPreview)return
        val gen=generation
        viewModelScope.launch {
            try {val profile=withContext(Dispatchers.IO) {api(me).accountProfile(me)}
                if(gen==generation)mutable.update {it.copy(myProfile=profile,profileError=null)}
            } catch(e:Exception) {if(gen==generation)mutable.update {it.copy(profileError=message(e))}}
        }
    }
    fun changeAvatar(uri: android.net.Uri?) {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview)return
        val gen=generation;mutable.update {it.copy(saving=true,profileError=null)}
        viewModelScope.launch {
            try {
                val profile=withContext(Dispatchers.IO) {
                    val data=uri?.let {encodeAvatar(getApplication<Application>(),it)} ?: ""
                    api(me).avatar(me,data)
                }
                if(gen==generation) {mutable.update {it.copy(myProfile=profile,saving=false,notice="头像已更新")};refresh()}
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation) {if(e is ApiException && e.status==401)failure(e,false) else mutable.update {it.copy(saving=false,profileError=message(e))}}}
        }
    }
    fun changePassword(current: String,new: String) {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview)return
        val gen=generation;mutable.update {it.copy(saving=true,profileError=null)}
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {api(me).password(me,current,new)}
                if(gen==generation) {
                    mutable.update {it.copy(saving=false)};logout()
                    mutable.update {it.copy(authError="密码已修改，请使用新密码登录")}
                }
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation) {if(e is ApiException && e.status==401)failure(e,false) else mutable.update {it.copy(saving=false,profileError=message(e))}}}
        }
    }
    fun logout() {
        if(state.value.saving)return
        generation++;refreshJob?.cancel();organizationJob?.cancel();dispatchJob?.cancel();reviewsJob?.cancel()
        if(!state.value.developmentPreview)store.clearSession()
        mutable.value=AppState(server=state.value.server,username=state.value.username)
    }
    fun screen(destination: String) {
        val name=when(destination) {"functions"->"day";"notifications"->"inbox";else->destination}
        if(state.value.saving || state.value.editor!=null)return
        mutable.update {it.copy(screen=name,selectedTaskId=null,dispatchOpen=false,notice=null)}
        if(name=="organization")loadOrganization()
        if(name=="profile")loadProfile()
        if(name=="labels")loadWorkspaceExtras()
        if(name in listOf("home","tasks","day","profile","inbox"))refresh()
    }
    fun changeAvatar(avatar: String) {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview)return
        val gen=generation
        mutable.update {it.copy(saving=true,profileError=null)}
        viewModelScope.launch {
            try {
                val profile=withContext(Dispatchers.IO) {api(me).updateAvatar(me,avatar)}
                if(gen==generation && state.value.session==me) {
                    mutable.update {it.copy(saving=false,profile=profile,notice="头像已更新")}
                    loadProfile()
                }
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation && state.value.session==me) {
                if(e is ApiException && e.status==401)failure(e,false)
                else mutable.update {it.copy(saving=false,profileError=message(e))}
            }}
        }
    }
    fun openDispatch() {
        val current=state.value
        val me=current.session ?: return
        if(current.saving || !AppRoles.canCreateTask(me.role))return
        if(current.developmentPreview) {mutable.update {it.copy(notice="请登录真实业务账号后派发任务。")};return}
        val draft=current.dispatchEditor ?: TaskDispatchBuffer(me.server,me.userId,assigneeId=me.userId)
        store.saveDispatchBuffer(draft)
        mutable.update {it.copy(dispatchEditor=draft,dispatchOpen=true,dispatchError=null,selectedTaskId=null,screen="tasks",notice=null)}
        loadDispatchMembers()
    }
    fun loadDispatchMembers() {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview || !AppRoles.canCreateTask(me.role))return
        dispatchJob?.cancel()
        val gen=generation
        mutable.update {it.copy(dispatchLoading=true,dispatchError=null,dispatchMembers=emptyList())}
        dispatchJob=viewModelScope.launch {
            try {
                val members=withContext(Dispatchers.IO) {TaskDispatchValidation.assignees(me,api(me).organization(me))}
                val currentTask=state.value.tasks.firstOrNull {it.id==state.value.dispatchEditor?.taskId}
                val available=if(currentTask!=null && members.none {it.id==currentTask.assigneeId})members+UserSummary(currentTask.assigneeId,"",currentTask.assigneeName,"STAFF") else members
                if(gen==generation)mutable.update {it.copy(dispatchMembers=available,dispatchLoading=false)}
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
    fun changeDispatch(title: String? = null,detail: String? = null,priority: String? = null,assigneeId: Long? = null,dueDate: String? = null,labelId: Long? = null,startDate: String? = null) {
        if(state.value.saving)return
        val draft=state.value.dispatchEditor ?: return
        val updated=draft.copy(title=title ?: draft.title,detail=detail ?: draft.detail,priority=priority ?: draft.priority,
            assigneeId=if(draft.taskId==null)assigneeId ?: draft.assigneeId else draft.assigneeId,dueDate=dueDate ?: draft.dueDate,labelId=labelId ?: draft.labelId,startDate=startDate ?: draft.startDate)
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
        if(current.saving || current.dispatchLoading || current.developmentPreview || !AppRoles.canCreateTask(me.role))return
        TaskDispatchValidation.error(draft,current.dispatchMembers)?.let {error->mutable.update {it.copy(dispatchError=error)};return}
        val gen=generation
        mutable.update {it.copy(saving=true,dispatchError=null,notice=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {
                    val members=TaskDispatchValidation.assignees(me,api(me).organization(me))
                    if(draft.taskId==null)TaskDispatchValidation.error(draft,members)?.let {throw IllegalArgumentException(it)}
                    if(draft.taskId==null)api(me).createTask(me,TaskDispatchValidation.input(draft)) else api(me).editTask(me,draft.taskId,TaskDispatchValidation.input(draft))
                }
                if(gen!=generation || state.value.session!=me)return@launch
                store.clearDispatchBuffer(me)
                mutable.update {it.copy(saving=false,dispatchEditor=null,dispatchOpen=false,dispatchError=null,
                    tasks=listOf(result)+it.tasks.filterNot {old->old.id==result.id},selectedTaskId=result.id,notice=if(result.reviewStatus=="pending_change")"变更已提交，等待上级审核。" else if(result.approvalStatus=="pending")"已提交，等待直属上级审核。" else "任务已生效，可开始处理。")}
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
        mutable.update {it.copy(screen="tasks",selectedTaskId=id,taskReviewError=null,notice=null)}
        refresh()
    }
    fun closeTask() {
        if(state.value.saving)return
        // The task buffer stays available when navigating or signing out.
        mutable.update {it.copy(selectedTaskId=null,taskEditorError=null)}
    }
    fun editTask(task: DispatchTask) {
        val me=state.value.session ?: return
        if(state.value.saving || task.assigneeId!=me.userId || !taskCanReportProgress(task,state.value.taskReviewRecords) || state.value.taskLoading || !state.value.taskReviewRecordsReady)return
        val pending=state.value.taskEditor
        if(pending!=null && pending.taskId!=task.id)return
        val buffer=pending ?: TaskEditBuffer(me.server,me.userId,task.id,"",task.progressNote,task.status)
        store.saveTaskBuffer(buffer)
        mutable.update {it.copy(taskEditor=buffer,taskEditorError=null,selectedTaskId=task.id,screen="tasks",notice=null)}
    }
    fun changeTaskEditor(note: String? = null, status: String? = null) {
        if(state.value.saving)return
        val current=state.value.taskEditor ?: return
        val buffer=current.copy(progress="",note=note ?: current.note,status=status ?: current.status ?: "todo")
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
        if(task==null || task.assigneeId!=me.userId || !taskCanReportProgress(task,current.taskReviewRecords) || current.taskLoading || !current.taskReviewRecordsReady) {
            mutable.update {it.copy(taskEditorError="请刷新任务并确认你是当前责任人后再提交。")};return
        }
        val validation=TaskValidation.error("",buffer.note,buffer.status)
        if(validation!=null) {mutable.update {it.copy(taskEditorError=validation)};return}
        val gen=generation
        refreshJob?.cancel()
        mutable.update {it.copy(saving=true,loading=false,taskLoading=false,taskEditorError=null,notice=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {
                    api(me).saveProgress(me,buffer.taskId,TaskProgressInput(null,buffer.note.trim(),buffer.status))
                }
                if(gen!=generation || state.value.session!=me)return@launch
                val records=withContext(Dispatchers.IO) {runCatching {api(me).reviews(me)}.getOrNull()}
                if(gen!=generation || state.value.session!=me)return@launch
                store.clearTaskBuffer(me)
                mutable.update {it.copy(saving=false,taskEditor=null,taskEditorError=null,
                    taskReviewRecords=records ?: it.taskReviewRecords,taskReviewRecordsReady=records!=null,
                    tasks=it.tasks.map {old->if(old.id==result.id)result else old},
                    notice=if(me.role!="FOUNDER")"进度已提交当前直属上级审核，批准后生效。" else "进度已生效。")}
                refresh()
            } catch(e: CancellationException) {throw e}
              catch(e: Exception) {if(gen==generation && state.value.session==me)failure(e,false,true)}
        }
    }
    fun reviewTask(id: Long,decision: String,note: String) {
        val me=state.value.session ?: return
        if(state.value.saving || !AppRoles.canViewDown(me.role))return
        val gen=generation
        refreshJob?.cancel()
        mutable.update {it.copy(saving=true,loading=false,taskLoading=false,taskReviewError=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {api(me).decideTaskApproval(me,id,decision,note)}
                if(gen!=generation || state.value.session!=me)return@launch
                mutable.update {it.copy(saving=false,tasks=it.tasks.map {old->if(old.id==id)result else old},
                    pendingTaskApprovals=it.pendingTaskApprovals.filterNot {old->old.id==id},notice=if(decision=="approved")"已批准，责任人可开始处理。" else "已拒绝，申请人可查看审核意见。")}
                refresh()
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation && state.value.session==me) {
                if(e is ApiException && e.status==401)failure(e,false)
                else mutable.update {it.copy(saving=false,taskReviewError=message(e))}
            }}
        }
    }
    fun loadReviews() {
        val me=state.value.session ?: return
        if(state.value.developmentPreview || me.role !in AppRoles.business)return
        reviewsJob?.cancel()
        val gen=generation
        mutable.update {it.copy(reviewsLoading=true)}
        reviewsJob=viewModelScope.launch {
            try {
                val list=withContext(Dispatchers.IO) {api(me).taskReviews(me)}
                if(gen==generation)mutable.update {it.copy(reviews=list,reviewsLoading=false)}
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation)mutable.update {it.copy(reviewsLoading=false)}}
        }
    }
    fun approveTask(task: DispatchTask, note: String="")=reviewTask(task,true,note)
    fun rejectTask(task: DispatchTask, note: String) {
        if(note.isBlank()) {mutable.update {it.copy(notice="请填写驳回理由")};return}
        reviewTask(task,false,note)
    }
    private fun reviewTask(task: DispatchTask, approve: Boolean, note: String) {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview)return
        val gen=generation
        mutable.update {it.copy(saving=true,notice=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {
                    if(approve)api(me).approveTask(me,task.id,note) else api(me).rejectTask(me,task.id,note)
                }
                if(gen!=generation || state.value.session!=me)return@launch
                mutable.update {s->s.copy(saving=false,tasks=s.tasks.map {if(it.id==result.id)result else it},
                    reviews=s.reviews.filterNot {it.id==result.id},notice=if(approve)"已通过审核。" else "已驳回。")}
                refresh()
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation && state.value.session==me) {
                if(e is ApiException && e.status==401)failure(e,false) else mutable.update {it.copy(saving=false,notice=message(e))}
            }}
        }
    }
    fun deleteTask(task: DispatchTask) {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview)return
        val gen=generation
        mutable.update {it.copy(saving=true,notice=null)}
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {api(me).deleteTask(me,task.id)}
                if(gen!=generation || state.value.session!=me)return@launch
                mutable.update {s->s.copy(saving=false,tasks=s.tasks.filterNot {it.id==task.id},
                    reviews=s.reviews.filterNot {it.id==task.id},
                    selectedTaskId=if(s.selectedTaskId==task.id)null else s.selectedTaskId,notice="已处理删除请求。")}
                refresh()
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation && state.value.session==me) {
                if(e is ApiException && e.status==401)failure(e,false) else mutable.update {it.copy(saving=false,notice=message(e))}
            }}
        }
    }
    fun changeTask(task: DispatchTask,title: String,detail: String,priority: String,dueAt: String?) {
        val me=state.value.session ?: return
        if(state.value.saving || state.value.developmentPreview)return
        val input=TaskCreateInput(title.trim(),detail.trim(),priority,task.assigneeId,dueAt,startAt=task.startAt)
        TaskDispatchValidation.inputError(input,me.userId)?.let {mutable.update {s->s.copy(notice=it)};return}
        val gen=generation
        mutable.update {it.copy(saving=true,notice=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {api(me).changeTask(me,task.id,input)}
                if(gen!=generation || state.value.session!=me)return@launch
                mutable.update {s->s.copy(saving=false,tasks=s.tasks.map {if(it.id==result.id)result else it},
                    notice=if(result.reviewStatus=="pending_change")"变更已提交上级审核。" else "任务已更新。")}
                refresh()
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation && state.value.session==me) {
                if(e is ApiException && e.status==401)failure(e,false) else mutable.update {it.copy(saving=false,notice=message(e))}
            }}
        }
    }
    fun loadNotifications() {
        val me=state.value.session ?: return
        if(state.value.developmentPreview || me.role !in AppRoles.business)return
        val gen=generation
        mutable.update {it.copy(notificationsLoading=true)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {api(me).notifications(me)}
                if(gen==generation)mutable.update {it.copy(notifications=result.items,unreadCount=result.unreadCount,notificationsLoading=false)}
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(gen==generation)mutable.update {it.copy(notificationsLoading=false)}}
        }
    }
    fun openNotification(n: AppNotification) {
        val me=state.value.session ?: return
        if(!n.read) {
            mutable.update {s->s.copy(notifications=s.notifications.map {if(it.id==n.id)it.copy(read=true) else it},unreadCount=(s.unreadCount-1).coerceAtLeast(0))}
            if(!state.value.developmentPreview) viewModelScope.launch {
                runCatching {withContext(Dispatchers.IO) {api(me).readNotification(me,n.id)}}
            }
        }
        if(n.taskId!=null)openTask(n.taskId) else screen("notifications")
    }
    fun markAllNotificationsRead() {
        val me=state.value.session ?: return
        mutable.update {s->s.copy(notifications=s.notifications.map {it.copy(read=true)},unreadCount=0)}
        if(state.value.developmentPreview)return
        viewModelScope.launch {runCatching {withContext(Dispatchers.IO) {api(me).readAllNotifications(me)}}}
    }
    fun selectDate(date: String) {mutable.update {it.copy(selectedDate=date)}}
    fun calendarMode(mode: String) {if(mode in setOf("month","week","day"))mutable.update {it.copy(calendarMode=mode)}}
    fun edit(log: WorkLog? = null) {
        if(state.value.saving)return
        val me=state.value.session ?: return
        val buffer=EditBuffer(me.server,me.userId,log?.id,log?.logDate ?: today().toString(),log?.content ?: "",log?.status ?: "draft",log?.endDate,log?.label?.id ?: 0)
        if(!state.value.developmentPreview)store.saveBuffer(buffer)
        mutable.update {it.copy(editor=buffer,editorError=null,notice=null,screen="logs")}
    }
    fun changeEditor(date: String? = null, content: String? = null, endDate: String? = null,labelId: Long? = null) {
        state.value.editor?.let {if(logEditorReadOnly(it,state.value.logs))return}
        if(state.value.saving)return
        val current=state.value.editor ?: return
        val buffer=current.copy(date=if(current.logId==null)date ?: current.date else current.date,content=content ?: current.content,endDate=if(current.logId==null && date!=null)endDate else current.endDate,labelId=labelId ?: current.labelId)
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
        current.editor?.let {if(logEditorReadOnly(it,current.logs)) {mutable.update {s->s.copy(editorError="该日志已超出创建后 24 小时，仅供查看")};return}}
        val me=current.session ?: return
        val buffer=current.editor ?: return
        val original=current.logs.firstOrNull {it.id==buffer.logId}
        if(original!=null && (buffer.date!=original.logDate || (buffer.endDate ?: buffer.date)!=original.lastDate)) {
            mutable.update {it.copy(editorError="已保存日志的日期范围不可修改，请重新打开日志。")};return
        }
        val validation=LogValidation.error(buffer.date,buffer.content,status,buffer.endDate,creating=buffer.logId==null)
        if(validation!=null) {mutable.update {it.copy(editorError=validation)};return}
        if((buffer.status=="submitted" || current.logs.any {it.id==buffer.logId && it.status=="submitted"}) && status=="draft") {mutable.update {it.copy(editorError="已提交日志不能改为草稿，请保存已提交修改。")};return}
        val gen=generation
        mutable.update {it.copy(saving=true,editorError=null,notice=null)}
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {api(me).save(me,buffer.logId,LogInput(buffer.date,buffer.content.trim(),status,buffer.endDate,buffer.labelId))}
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
        if(!canModifyLog(log)) {mutable.update {it.copy(notice="日志仅在创建后 24 小时内可修改或提交，当前记录仅供查看")};return}
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
        loadWorkspaceExtras()
        if(state.value.developmentPreview)return
        if(state.value.saving)return
        val me=state.value.session ?: return
        refreshJob?.cancel()
        val gen=generation
        mutable.update {it.copy(loading=true,taskLoading=true,personalLoading=true,profileLoading=true,personalError=null,profileError=null,logError=null,panelError=null,taskError=null)}
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
            panelResult.onSuccess {panels-> mutable.update {it.copy(panels=panels.copy(companyImportant=it.panels?.companyImportant.orEmpty()))} }
                .onFailure {e->mutable.update {it.copy(panelError=message(e))}}
            val notices=withContext(Dispatchers.IO) {runCatching {api(me).companyNotices(me)}}
            ensureActive()
            if(gen!=generation || state.value.session!=me)return@launch
            if((notices.exceptionOrNull() as? ApiException)?.status==401) {failure(notices.exceptionOrNull()!!,false);return@launch}
            notices.onSuccess {items->mutable.update {it.copy(panels=(it.panels ?: PanelMap()).copy(companyImportant=items.filter {notice->notice.enabled}.sortedBy {notice->notice.sortOrder}.map {notice->notice.panel()}),panelError=null)}}
                .onFailure {e->mutable.update {it.copy(panelError=message(e))}}
            val personalResult=withContext(Dispatchers.IO) {runCatching {api(me).personalPriorities(me)}}
            ensureActive()
            if(gen!=generation || state.value.session!=me)return@launch
            if((personalResult.exceptionOrNull() as? ApiException)?.status==401) {failure(personalResult.exceptionOrNull()!!,false);return@launch}
            personalResult.onSuccess {snapshot->mutable.update {it.copy(personal=it.personal.receive(snapshot),personalLoading=false)};persistWorkspace()}
                .onFailure {e->mutable.update {it.copy(personalLoading=false,personalError="暂时无法读取个人事项。"+message(e))}}
            val profileResult=withContext(Dispatchers.IO) {runCatching {api(me).profile(me)}}
            ensureActive()
            if(gen!=generation || state.value.session!=me)return@launch
            if((profileResult.exceptionOrNull() as? ApiException)?.status==401) {failure(profileResult.exceptionOrNull()!!,false);return@launch}
            profileResult.onSuccess {profile->mutable.update {it.copy(profile=profile,profileLoading=false)}}
                .onFailure {e->mutable.update {it.copy(profileLoading=false,profileError="暂时无法读取个人资料。"+message(e))}}
            val taskResult=withContext(Dispatchers.IO) {runCatching {api(me).tasks(me)}}
            ensureActive()
            if(gen!=generation)return@launch
            if((taskResult.exceptionOrNull() as? ApiException)?.status==401) {failure(taskResult.exceptionOrNull()!!,false);return@launch}
            taskResult.onSuccess {tasks->mutable.update {it.copy(tasks=tasks)}}
                .onFailure {e->mutable.update {it.copy(taskError=message(e))}}
            if(AppRoles.canDispatch(me.role)) {
                val reviews=withContext(Dispatchers.IO) {runCatching {api(me).pendingTaskApprovals(me)}}
                ensureActive()
                if(gen!=generation || state.value.session!=me)return@launch
                if((reviews.exceptionOrNull() as? ApiException)?.status==401) {failure(reviews.exceptionOrNull()!!,false);return@launch}
                reviews.onSuccess {items->mutable.update {it.copy(pendingTaskApprovals=items,taskReviewError=null)}}
                    .onFailure {e->mutable.update {it.copy(pendingTaskApprovals=emptyList(),taskReviewError="待审核任务读取失败："+message(e))}}
            }
            val reviewResult=withContext(Dispatchers.IO) {runCatching {api(me).taskReviews(me)}}
            ensureActive()
            if(gen!=generation)return@launch
            reviewResult.onSuccess {list->mutable.update {it.copy(reviews=list)}}
            val notifResult=withContext(Dispatchers.IO) {runCatching {api(me).notifications(me)}}
            ensureActive()
            if(gen!=generation)return@launch
            notifResult.onSuccess {list->mutable.update {it.copy(notifications=list.items,unreadCount=list.unreadCount)}}
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
            generation++;refreshJob?.cancel();organizationJob?.cancel();dispatchJob?.cancel();reviewsJob?.cancel();store.clearSession()
            // Edits were already saved under the original server/user pair.
            mutable.value=AppState(server=state.value.server,username=state.value.username,
                authError="登录已过期，请重新登录。未保存内容将由同一账号恢复。")
        } else mutable.update {it.copy(saving=false,taskEditorError=if(taskEditor)message(e) else it.taskEditorError,editorError=if(editor)message(e) else it.editorError,
            notice=if(editor || taskEditor)it.notice else message(e))}
    }
    private fun message(e: Throwable): String = e.message ?: "操作失败，请重试"
}
