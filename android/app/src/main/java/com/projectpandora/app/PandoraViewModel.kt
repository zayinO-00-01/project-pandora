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
    val busyLogin: Boolean = false, val loading: Boolean = false, val saving: Boolean = false,
    val logs: List<WorkLog> = emptyList(), val panels: PanelMap? = null,
    val logError: String? = null, val panelError: String? = null, val authError: String? = null,
    val editor: EditBuffer? = null, val editorError: String? = null,
    val screen: String = "home", val selectedDate: String = today().toString(),
    val notice: String? = null
)
class PandoraViewModel(app: Application): AndroidViewModel(app) {
    private val store = SessionStore(app)
    private val mutable = MutableStateFlow(AppState(server=store.server(), username=store.username()))
    val state = mutable.asStateFlow()
    private var refreshJob: Job? = null
    private var generation = 0
    init { store.session()?.let { restore(it) } }
    private fun api(session: Session) = ApiClient(session.server)
    private fun restore(old: Session) {
        mutable.update { it.copy(busyLogin=true) }
        viewModelScope.launch {
            try {
                val members=withContext(Dispatchers.IO) { api(old).users(old) }
                val me=members.firstOrNull { it.id==old.userId } ?: throw ApiException(401,"账号已失效，请重新登录")
                activate(old.copy(role=me.role,displayName=me.displayName))
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
        generation++;store.save(session)
        val pending=store.buffer(session)
        mutable.value=AppState(session=session,server=session.server,username=session.username,
            editor=pending,screen=if(pending==null)"home" else "logs",
            notice=if(pending==null)null else "已恢复上次未保存的内容，请检查后保存。")
        refresh()
    }
    fun logout() {
        if(state.value.saving)return
        generation++;refreshJob?.cancel();store.clearSession()
        mutable.value=AppState(server=state.value.server,username=state.value.username)
    }
    fun screen(name: String) {
        if(state.value.saving || state.value.editor!=null)return
        mutable.update {it.copy(screen=name,notice=null)}
        if(name=="home")refresh()
    }
    fun selectDate(date: String) {mutable.update {it.copy(selectedDate=date)}}
    fun edit(log: WorkLog? = null) {
        if(state.value.saving)return
        val me=state.value.session ?: return
        val buffer=EditBuffer(me.server,me.userId,log?.id,log?.logDate ?: today().toString(),log?.content ?: "",log?.status ?: "draft")
        store.saveBuffer(buffer)
        mutable.update {it.copy(editor=buffer,editorError=null,notice=null,screen="logs")}
    }
    fun changeEditor(date: String? = null, content: String? = null) {
        if(state.value.saving)return
        val current=state.value.editor ?: return
        val buffer=current.copy(date=date ?: current.date,content=content ?: current.content)
        store.saveBuffer(buffer)
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
        val me=current.session ?: return
        val buffer=current.editor ?: return
        val validation=LogValidation.error(buffer.date,buffer.content,status)
        if(validation!=null) {mutable.update {it.copy(editorError=validation)};return}
        if(buffer.status=="submitted" && status=="draft")return
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
        val me=state.value.session ?: return
        refreshJob?.cancel()
        val gen=generation
        mutable.update {it.copy(loading=true,logError=null,panelError=null)}
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
            mutable.update {it.copy(loading=false)}
        }
    }
    private fun failure(e: Throwable, editor: Boolean) {
        if(e is ApiException && e.status==401) {
            generation++;refreshJob?.cancel();store.clearSession()
            // Edits were already saved under the original server/user pair.
            mutable.value=AppState(server=state.value.server,username=state.value.username,
                authError="登录已过期，请重新登录。未保存内容将由同一账号恢复。")
        } else mutable.update {it.copy(saving=false,editorError=if(editor)message(e) else it.editorError,
            notice=if(editor)it.notice else message(e))}
    }
    private fun message(e: Throwable): String = e.message ?: "操作失败，请重试"
}
