package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.projectpandora.app.AppState
import com.projectpandora.app.data.*
import kotlinx.coroutines.*
import java.time.LocalDate
import java.util.UUID

@Composable fun DailyTodos(state: AppState) {
    val me=state.session ?: return
    // Dispose requests, dialogs and drafts together when the authenticated account changes.
    key(me.server,me.userId,me.token,state.developmentPreview) {
        DailyTodosContent(me,state.developmentPreview)
    }
}

@Composable private fun DailyTodosContent(me: Session,preview: Boolean) {
    val prefs=LocalContext.current.getSharedPreferences("daily-todos-preview",0)
    val storageKey="${me.server}:${me.userId}"
    var selectedText by rememberSaveable {mutableStateOf(today().toString())}
    val selected=LocalDate.parse(selectedText)
    var entries by remember {mutableStateOf<List<DailyTodo>>(emptyList())}
    var busy by remember {mutableStateOf(true)}
    var loaded by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    var editing by remember {mutableStateOf<DailyTodo?>(null)}
    var deleting by remember {mutableStateOf<DailyTodo?>(null)}
    var title by remember {mutableStateOf("")}
    var creating by remember {mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val api=remember {if(preview)null else ApiClient(me.server)}
    suspend fun load() {
        val result=if(preview) runCatching {
            Gson().fromJson<List<DailyTodo>>(prefs.getString(storageKey,"[]"),object:TypeToken<List<DailyTodo>>(){}.type) ?: emptyList()
        }.getOrDefault(emptyList()) else withContext(Dispatchers.IO) {api!!.todos(me)}
        currentCoroutineContext().ensureActive()
        entries=result
        loaded=true
    }
    fun refresh() {
        if(busy)return
        busy=true;error=null
        scope.launch {
            try {load()}
            catch(e:CancellationException) {throw e}
            catch(e:Exception) {error=e.message ?: "读取失败，请重试"}
            finally {busy=false}
        }
    }
    fun change(item: DailyTodo,new: Boolean=false,delete: Boolean=false) {
        if(busy)return
        busy=true;error=null
        scope.launch {
            try {
                val saved=if(preview)item else withContext(Dispatchers.IO) {
                    if(delete)api!!.deleteTodo(me,item.id) else api!!.saveTodo(me,item,new)
                }
                currentCoroutineContext().ensureActive()
                entries=entries.filter {it.id!=item.id && it.id!=saved.id}+if(delete)emptyList() else listOf(saved)
                if(preview)prefs.edit().putString(storageKey,Gson().toJson(entries)).apply()
                editing=null;deleting=null
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {error=e.message ?: "保存失败，请重试"}
            finally {busy=false}
        }
    }
    LaunchedEffect(Unit) {
        try {load()}
        catch(e:CancellationException) {throw e}
        catch(e:Exception) {error=e.message ?: "读取失败，请重试"}
        finally {busy=false}
    }
    Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=.97f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("每日事项",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                ControlTextButton(onClick=::refresh,enabled=!busy) {Text("刷新")}
            }
            val todosByDate=remember(entries) {entries.groupBy {it.date}}
            WorkspaceCalendar(selected,onDate={selectedText=it.toString()}) {date->
                val items=todosByDate[date.toString()].orEmpty()
                items.take(if(items.size>2)1 else 2).forEach {item->
                    Text((if(item.completed)"✓ " else "")+item.title,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)
                }
                if(items.size>2)Text("共 ${items.size} 项",style=MaterialTheme.typography.labelSmall)
            }
            HorizontalDivider()
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${selected.monthValue}月${selected.dayOfMonth}日",style=MaterialTheme.typography.titleMedium)
                    holidayLabel(selected)?.let {Text(it,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)}
                }
                ControlButton(onClick={creating=true;title="";error=null;editing=DailyTodo(UUID.randomUUID().toString(),"",selectedText)},enabled=!busy && loaded) {Text("添加事项")}
            }
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(editing==null && deleting==null)error?.let {Hint(it,true,onRetry=::refresh)}
            val daily=entries.filter {it.date==selectedText}.sortedBy {it.completed}
            if(daily.isEmpty() && loaded && !busy)Text("暂无个人事项",color=MaterialTheme.colorScheme.onSurfaceVariant)
            daily.forEach {item->
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(item.completed,{change(item.copy(completed=it))},enabled=!busy)
                    ControlTextButton(onClick={creating=false;title=item.title;error=null;editing=item},enabled=!busy,modifier=Modifier.weight(1f)) {Text(item.title)}
                    ControlTextButton(onClick={error=null;deleting=item},enabled=!busy) {Text("删除")}
                }
            }
        }
    }
    editing?.let {item->AlertDialog(
        onDismissRequest={if(!busy)editing=null},
        title={Text(if(creating)"添加个人事项" else "编辑个人事项")},
        text={Column {OutlinedTextField(title,{title=it.take(200)},label={Text("事项内容")},enabled=!busy);error?.let {Text(it,color=MaterialTheme.colorScheme.error)}}},
        confirmButton={ControlTextButton(onClick={change(item.copy(title=title.trim()),new=creating)},enabled=!busy && title.isNotBlank()) {Text("保存")}},
        dismissButton={ControlTextButton(onClick={editing=null;error=null},enabled=!busy) {Text("取消")}}
    )}
    deleting?.let {item->AlertDialog(
        onDismissRequest={if(!busy)deleting=null},title={Text("删除这条个人事项？")},
        text={Column {Text(item.title);error?.let {Text(it,color=MaterialTheme.colorScheme.error)}}},
        confirmButton={ControlTextButton(onClick={change(item,delete=true)},enabled=!busy) {Text("删除")}},
        dismissButton={ControlTextButton(onClick={deleting=null;error=null},enabled=!busy) {Text("取消")}}
    )}
}
