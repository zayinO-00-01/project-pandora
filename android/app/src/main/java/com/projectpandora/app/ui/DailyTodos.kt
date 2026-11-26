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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val todoPickerFormat=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
private val todoAgendaFormat=DateTimeFormatter.ofPattern("MM-dd HH:mm")

private data class TodoConflict(val original: DailyTodo,val proposed: DailyTodo,val latest: DailyTodo,val moveDay: LocalDate?) {
    fun reapply(): DailyTodo=latest.copy(
        title=if(proposed.title!=original.title)proposed.title else latest.title,
        date=if(proposed.date!=original.date)proposed.date else latest.date,
        startAt=if(proposed.startAt!=original.startAt)proposed.startAt else latest.startAt,
        endAt=if(proposed.endAt!=original.endAt)proposed.endAt else latest.endAt,
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
    var draftDate by remember {mutableStateOf("")}
    var startAt by remember {mutableStateOf<String?>(null)}
    var endAt by remember {mutableStateOf<String?>(null)}
    var chosen by remember {mutableStateOf(emptyList<Long>())}
    var creating by remember {mutableStateOf(false)}
    var conflict by remember {mutableStateOf<TodoConflict?>(null)}
    var mode by rememberSaveable {mutableStateOf("month")}
    val scope=rememberCoroutineScope()
    val api=remember {if(preview)null else ApiClient(me.server)}
    fun openEditor(item: DailyTodo,new: Boolean=false,draft: DailyTodo=item) {
        creating=new;title=draft.title;draftDate=draft.date;startAt=draft.startAt;endAt=draft.endAt
        chosen=draft.labelIds ?: draft.labels.orEmpty().map {it.id};error=null;editing=item
    }
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
    fun change(item: DailyTodo,new: Boolean=false,delete: Boolean=false,original: DailyTodo?=null,moveDay: LocalDate?=null) {
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
                if(moveDay!=null && moveDay in saved.scheduleDates())selectedText=moveDay.toString()
                else if(new || original!=null && item.date!=original.date)selectedText=saved.date
                editing=null;deleting=null
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {
                error=e.message ?: "保存失败，请重试"
                if(original!=null && e is ApiException && e.status==409) {
                    try {
                        load()
                        entries.firstOrNull {it.id==original.id}?.let {
                            conflict=TodoConflict(original,item,it,moveDay);error=null
                        } ?: run {error="这条事项已被删除，本次修改未保存。"}
                    } catch(cancel:CancellationException) {throw cancel}
                    catch(_:Exception) {error="事项已在其他设备修改，暂时无法读取最新内容。请联网重试。"}
                }
            }
            finally {busy=false}
        }
    }
    fun move(item: DailyTodo,source: LocalDate,target: LocalDate) {
        if(source!=target)change(item.moveTo(target,source),original=item,moveDay=target)
    }
    fun chooseDate(item: DailyTodo) {
        val date=LocalDate.parse(item.date)
        DatePickerDialog(context,{_,year,month,day->move(item,date,LocalDate.of(year,month+1,day))},date.year,date.monthValue-1,date.dayOfMonth).show()
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
                val schedules=remember(entries) {entries.map {it to it.scheduleDates()}}
                fun todosOn(day: LocalDate)=schedules.mapNotNull {(item,dates)->item.takeIf {day in dates}}
                WorkspaceCalendar(selected,onDate={selectedText=it.toString()},mode=mode,onMode={mode=it},drag=drag) {date->
                    val items=todosOn(date)
                    val shown=if(items.size>2)1 else 2
                    items.take(shown).forEach {item->
                        CalendarItemChip((if(item.completed)"✓ " else "")+item.title,item.labels.orEmpty(),
                            Modifier.calendarDrag(drag,item.title,date,if(busy)null else {target->move(item,date,target)})
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
                    ControlButton(onClick={openEditor(DailyTodo(UUID.randomUUID().toString(),"",selectedText),new=true)},enabled=!busy && loaded) {Text("添加事项")}
                }
                if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                if(editing==null && deleting==null)error?.let {Hint(it,true,onRetry=::refresh)}
                val daily=todosOn(selected).sortedBy {it.completed}
                if(daily.isEmpty() && loaded && !busy)Text("暂无个人事项",color=MaterialTheme.colorScheme.onSurfaceVariant)
                daily.forEach {item->
                    Row(
                        Modifier.fillMaxWidth().calendarDrag(drag,item.title,selected,if(busy)null else {target->move(item,selected,target)},fromAgenda=true),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        Checkbox(item.completed,{change(item.copy(completed=it,labelIds=null),original=item)},enabled=!busy)
                        ControlTextButton(onClick={openEditor(item)},enabled=!busy,modifier=Modifier.weight(1f)) {
                            Column {
                                Text(item.title)
                                item.startAt?.let {Text("开始 ${LocalDateTime.parse(it).format(todoAgendaFormat)}",style=MaterialTheme.typography.labelSmall)}
                                item.endAt?.let {Text("结束 ${LocalDateTime.parse(it).format(todoAgendaFormat)}",style=MaterialTheme.typography.labelSmall)}
                                CalendarLabelNames(item.labels.orEmpty())
                            }
                        }
                        ControlTextButton(onClick={chooseDate(item)},enabled=!busy) {Text("改日期")}
                        ControlTextButton(onClick={error=null;deleting=item},enabled=!busy) {Text("删除")}
                    }
                }
            }
        }
    }
    if(conflict==null)editing?.let {item->
        val start=startAt?.let(LocalDateTime::parse)
        val end=endAt?.let(LocalDateTime::parse)
        val date=start?.toLocalDate() ?: LocalDate.parse(draftDate)
        val availableLabels=(labels+item.labels.orEmpty()).distinctBy {it.id}
        val proposed=item.copy(title=title.trim(),date=date.toString(),startAt=startAt,endAt=endAt,
            labelIds=chosen,labels=chosen.mapNotNull(availableLabels.associateBy {it.id}::get))
        val valid=proposed.hasValidSchedule()
        AlertDialog(
            onDismissRequest={if(!busy)editing=null},
            title={Text(if(creating)"添加个人事项" else "编辑个人事项")},
            text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title,{title=it.take(200)},label={Text("事项内容")},enabled=!busy,modifier=Modifier.fillMaxWidth())
                DeadlinePicker(start?.format(todoPickerFormat).orEmpty(),!busy,"开始时间",required=false) {
                    startAt=it.takeIf(String::isNotBlank)?.replace(' ','T')
                }
                DeadlinePicker(end?.format(todoPickerFormat).orEmpty(),!busy,"结束时间",required=false,minTime=start ?: date.atStartOfDay()) {
                    endAt=it.takeIf(String::isNotBlank)?.replace(' ','T')
                }
                if(!valid)Text("结束时间不能早于开始时间或事项日期",color=MaterialTheme.colorScheme.error)
                MultiLabelPicker(availableLabels,chosen,!busy) {chosen=it}
                error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            }},
            confirmButton={ControlTextButton(onClick={change(proposed,new=creating,original=item.takeUnless {creating})},
                enabled=!busy && title.isNotBlank() && valid) {Text("保存")}},
            dismissButton={ControlTextButton(onClick={editing=null;error=null},enabled=!busy) {Text("取消")}}
        )
    }
    deleting?.let {item->AlertDialog(
        onDismissRequest={if(!busy)deleting=null},title={Text("删除这条个人事项？")},
        text={Column {Text(item.title);error?.let {Text(it,color=MaterialTheme.colorScheme.error)}}},
        confirmButton={ControlTextButton(onClick={change(item,delete=true)},enabled=!busy) {Text("删除")}},
        dismissButton={ControlTextButton(onClick={deleting=null;error=null},enabled=!busy) {Text("取消")}}
    )}
    conflict?.let {pending->
        val latest=pending.latest
        val proposed=pending.reapply()
        val valid=proposed.hasValidSchedule()
        val names=(labels+latest.labels.orEmpty()+pending.original.labels.orEmpty()).associateBy {it.id}
        fun labelText(ids: List<Long>)=ids.joinToString("、") {names[it]?.name ?: "已停用标签"}.ifEmpty {"无标签"}
        fun timeText(value: String?)=value?.replace('T',' ') ?: "未设置"
        AlertDialog(onDismissRequest={conflict=null},title={Text("每日事项发生冲突")},
            text={Column(Modifier.heightIn(max=384.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(latest.title!=proposed.title)Text("最新标题：${latest.title}\n本次修改：${proposed.title}")
                if(latest.date!=proposed.date)Text("最新日期：${latest.date}\n本次修改：${proposed.date}")
                if(latest.startAt!=proposed.startAt)Text("最新开始时间：${timeText(latest.startAt)}\n本次修改：${timeText(proposed.startAt)}")
                if(latest.endAt!=proposed.endAt)Text("最新结束时间：${timeText(latest.endAt)}\n本次修改：${timeText(proposed.endAt)}")
                if(latest.completed!=proposed.completed)Text("最新状态：${if(latest.completed)"已完成" else "未完成"}\n本次修改：${if(proposed.completed)"已完成" else "未完成"}")
                proposed.labelIds?.takeIf {it!=latest.labels.orEmpty().map {label->label.id}}?.let {
                    Text("最新标签：${labelText(latest.labels.orEmpty().map {label->label.id})}\n本次修改：${labelText(it)}")
                }
                if(!valid)Text("合并后的时间范围无效，请调整开始或结束时间",color=MaterialTheme.colorScheme.error)
            }},
            confirmButton={
                if(valid)ControlTextButton(onClick={conflict=null;change(proposed,original=latest,moveDay=pending.moveDay)}) {Text("提交我的修改")}
                else ControlTextButton(onClick={conflict=null;openEditor(latest,draft=proposed)}) {Text("返回编辑")}
            },
            dismissButton={ControlTextButton(onClick={conflict=null;editing=null;error=null}) {Text("采用最新版")}}
        )
    }
}
