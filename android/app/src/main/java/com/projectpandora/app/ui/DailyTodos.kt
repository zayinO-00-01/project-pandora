package com.projectpandora.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.projectpandora.app.AppState
import com.projectpandora.app.data.*
import kotlinx.coroutines.*
import java.time.LocalDate
import java.util.UUID

private data class TodoConflict(val original: DailyTodo,val proposed: DailyTodo,val latest: DailyTodo) {
    fun reapply(): DailyTodo=latest.copy(
        title=if(proposed.title!=original.title)proposed.title else latest.title,
        date=if(proposed.date!=original.date)proposed.date else latest.date,
        completed=if(proposed.completed!=original.completed)proposed.completed else latest.completed,
        labelIds=proposed.labelIds?.takeIf {it!=original.labels.orEmpty().map {label->label.id}}
    )
}

@Composable fun DailyTodos(state: AppState) {
    val me=state.session ?: return
    // Dispose requests, dialogs and drafts together when the authenticated account changes.
    key(me.server,me.userId,me.token,state.developmentPreview) {
        DailyTodosContent(me,state.developmentPreview,state.labels)
    }
}

@Composable private fun DailyTodosContent(me: Session,preview: Boolean,labels: List<WorkLabel>) {
    val context=LocalContext.current
    val prefs=context.getSharedPreferences("daily-todos-preview",0)
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
    var chosen by remember {mutableStateOf(emptyList<Long>())}
    var creating by remember {mutableStateOf(false)}
    var conflict by remember {mutableStateOf<TodoConflict?>(null)}
    var mode by rememberSaveable {mutableStateOf("month")}
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
    fun change(item: DailyTodo,new: Boolean=false,delete: Boolean=false,original: DailyTodo?=null) {
        if(busy)return
        busy=true;error=null
        scope.launch {
            try {
                val saved=if(preview)item else withContext(Dispatchers.IO) {
                    if(delete)api!!.deleteTodo(me,item.id) else api!!.saveTodo(me,item,new,original)
                }
                currentCoroutineContext().ensureActive()
                entries=entries.filter {it.id!=item.id && it.id!=saved.id}+if(delete)emptyList() else listOf(saved)
                if(preview)prefs.edit().putString(storageKey,Gson().toJson(entries)).apply()
                if(original!=null && item.date!=original.date)selectedText=saved.date
                editing=null;deleting=null
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {
                error=e.message ?: "保存失败，请重试"
                if(original!=null && e is ApiException && e.status==409) {
                    try {
                        load()
                        entries.firstOrNull {it.id==original.id}?.let {
                            conflict=TodoConflict(original,item,it);error=null
                        } ?: run {error="这条事项已被删除，本次修改未保存。"}
                    } catch(cancel:CancellationException) {throw cancel}
                    catch(_:Exception) {error="事项已在其他设备修改，暂时无法读取最新内容。请联网重试。"}
                }
            }
            finally {busy=false}
        }
    }
    fun move(item: DailyTodo,target: LocalDate) {
        if(item.date!=target.toString())change(item.copy(date=target.toString(),labelIds=null),original=item)
    }
    fun chooseDate(item: DailyTodo) {
        val date=LocalDate.parse(item.date)
        DatePickerDialog(context,{_,year,month,day->move(item,LocalDate.of(year,month+1,day))},date.year,date.monthValue-1,date.dayOfMonth).show()
    }
    LaunchedEffect(Unit) {
        try {load()}
        catch(e:CancellationException) {throw e}
        catch(e:Exception) {error=e.message ?: "读取失败，请重试"}
        finally {busy=false}
    }
    Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=.97f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
        val drag=remember {CalendarItemDrag()}
        CalendarDragLayer(drag) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("每日事项",style=MaterialTheme.typography.titleLarge)
                val todosByDate=remember(entries) {entries.groupBy {it.date}}
                WorkspaceCalendar(selected,onDate={selectedText=it.toString()},mode=mode,onMode={mode=it},drag=drag) {date->
                    val items=todosByDate[date.toString()].orEmpty()
                    val shown=if(items.size>2)1 else 2
                    items.take(shown).forEach {item->
                        CalendarItemChip((if(item.completed)"✓ " else "")+item.title,item.labels.orEmpty(),
                            Modifier.calendarDrag(drag,item.title,date,if(busy)null else {target->move(item,target)})
                                .clickable {selectedText=date.toString()})
                    }
                    if(items.size>shown)CalendarOverflow(items.size-shown) {selectedText=date.toString()}
                }
                HorizontalDivider()
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${selected.monthValue}月${selected.dayOfMonth}日",style=MaterialTheme.typography.titleMedium)
                        holidayLabel(selected)?.let {Text(it,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)}
                    }
                    ControlButton(onClick={creating=true;title="";chosen=emptyList();error=null;editing=DailyTodo(UUID.randomUUID().toString(),"",selectedText)},enabled=!busy && loaded) {Text("添加事项")}
                }
                if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                if(editing==null && deleting==null)error?.let {Hint(it,true,onRetry=::refresh)}
                val daily=entries.filter {it.date==selectedText}.sortedBy {it.completed}
                if(daily.isEmpty() && loaded && !busy)Text("暂无个人事项",color=MaterialTheme.colorScheme.onSurfaceVariant)
                daily.forEach {item->
                    Row(
                        Modifier.fillMaxWidth().calendarDrag(drag,item.title,selected,if(busy)null else {target->move(item,target)},fromAgenda=true),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Checkbox(item.completed,{change(item.copy(completed=it,labelIds=null),original=item)},enabled=!busy)
                        ControlTextButton(onClick={creating=false;title=item.title;chosen=item.labels.orEmpty().map {it.id};error=null;editing=item},enabled=!busy,modifier=Modifier.weight(1f)) {Column {Text(item.title);CalendarLabelNames(item.labels.orEmpty())}}
                        ControlTextButton(onClick={chooseDate(item)},enabled=!busy) {Text("改日期")}
                        ControlTextButton(onClick={error=null;deleting=item},enabled=!busy) {Text("删除")}
                    }
                }
            }
        }
    }
    if(conflict==null)editing?.let {item->AlertDialog(
        onDismissRequest={if(!busy)editing=null},
        title={Text(if(creating)"添加个人事项" else "编辑个人事项")},
        text={Column {OutlinedTextField(title,{title=it.take(200)},label={Text("事项内容")},enabled=!busy);MultiLabelPicker((labels+item.labels.orEmpty()).distinctBy {it.id},chosen,!busy) {chosen=it};error?.let {Text(it,color=MaterialTheme.colorScheme.error)}}},
        confirmButton={ControlTextButton(onClick={change(item.copy(title=title.trim(),labelIds=chosen,labels=chosen.mapNotNull((labels+item.labels.orEmpty()).associateBy {it.id}::get)),new=creating,original=item.takeUnless {creating})},enabled=!busy && title.isNotBlank()) {Text("保存")}},
        dismissButton={ControlTextButton(onClick={editing=null;error=null},enabled=!busy) {Text("取消")}}
    )}
    deleting?.let {item->AlertDialog(
        onDismissRequest={if(!busy)deleting=null},title={Text("删除这条个人事项？")},
        text={Column {Text(item.title);error?.let {Text(it,color=MaterialTheme.colorScheme.error)}}},
        confirmButton={ControlTextButton(onClick={change(item,delete=true)},enabled=!busy) {Text("删除")}},
        dismissButton={ControlTextButton(onClick={deleting=null;error=null},enabled=!busy) {Text("取消")}}
    )}
    conflict?.let {pending->
        val latest=pending.latest
        val proposed=pending.reapply()
        val names=(labels+latest.labels.orEmpty()+pending.original.labels.orEmpty()).associateBy {it.id}
        fun labelText(ids: List<Long>)=ids.joinToString("、") {names[it]?.name ?: "已停用标签"}.ifEmpty {"无标签"}
        AlertDialog(onDismissRequest={conflict=null},title={Text("每日事项发生冲突")},
            text={Column(Modifier.heightIn(max=384.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(latest.title!=proposed.title)Text("最新标题：${latest.title}\n本次修改：${proposed.title}")
                if(latest.date!=proposed.date)Text("最新日期：${latest.date}\n本次修改：${proposed.date}")
                if(latest.completed!=proposed.completed)Text("最新状态：${if(latest.completed)"已完成" else "未完成"}\n本次修改：${if(proposed.completed)"已完成" else "未完成"}")
                proposed.labelIds?.takeIf {it!=latest.labels.orEmpty().map {label->label.id}}?.let {
                    Text("最新标签：${labelText(latest.labels.orEmpty().map {label->label.id})}\n本次修改：${labelText(it)}")
                }
            }},
            confirmButton={ControlTextButton(onClick={conflict=null;change(proposed,original=latest)}) {Text("提交我的修改")}},
            dismissButton={ControlTextButton(onClick={conflict=null;editing=null;error=null}) {Text("采用最新版")}}
        )
    }
}
