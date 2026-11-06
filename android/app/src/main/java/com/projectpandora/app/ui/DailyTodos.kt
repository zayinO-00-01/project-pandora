package com.projectpandora.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.data.*
import kotlinx.coroutines.*
import java.time.LocalDate
import java.util.UUID
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable fun DailyTodos(state: AppState) {
 val me=state.session ?: return
 val prefs=LocalContext.current.getSharedPreferences("daily-todos-preview",0)
 val key="${me.server}:${me.userId}"
 var selectedText by rememberSaveable(key) {mutableStateOf(today().toString())}
 val selected=LocalDate.parse(selectedText)
 var entries by remember(key) {mutableStateOf<List<DailyTodo>>(emptyList())}
 var busy by remember(key) {mutableStateOf(false)}
 var error by remember(key) {mutableStateOf<String?>(null)}
 var editing by remember {mutableStateOf<DailyTodo?>(null)}
 var title by remember {mutableStateOf("")}
 var creating by remember {mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 val api=remember(me.server,state.developmentPreview) {if(state.developmentPreview)null else ApiClient(me.server)}
 suspend fun load() {entries=if(state.developmentPreview)runCatching {Gson().fromJson<List<DailyTodo>>(prefs.getString(key,"[]"),object:TypeToken<List<DailyTodo>>(){}.type)}.getOrDefault(emptyList()) else withContext(Dispatchers.IO) {api!!.todos(me)}}
 fun change(item: DailyTodo,new: Boolean=false,delete: Boolean=false) {
  scope.launch {busy=true;error=null
   try {
    if(state.developmentPreview) {
     entries=entries.filter {it.id!=item.id}+if(delete)emptyList() else listOf(item)
     prefs.edit().putString(key,Gson().toJson(entries)).apply()
    } else {
     val saved=withContext(Dispatchers.IO) {if(delete)api!!.deleteTodo(me,item.id) else api!!.saveTodo(me,item,new)}
     entries=entries.filter {it.id!=item.id}+if(delete)emptyList() else listOf(saved)
    }
    editing=null
   } catch(e:Exception) {if(e is CancellationException)throw e;error=e.message ?: "保存失败"} finally {busy=false}
  }
 }
 LaunchedEffect(key) {busy=true;try {load()} catch(e:Exception) {if(e is CancellationException)throw e;error=e.message ?: "读取失败"} finally {busy=false}}
 Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=.97f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
 Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically) {
   Text("每日事项",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
   ControlTextButton(onClick={scope.launch {busy=true;try {load();error=null} catch(e:Exception) {if(e is CancellationException)throw e;error=e.message} finally {busy=false}}},enabled=!busy) {Text("刷新")}
  }
  Row(verticalAlignment=Alignment.CenterVertically) {
   Text("${selected.year} 年 ${selected.monthValue} 月",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
   ControlTextButton(onClick={selectedText=selected.minusMonths(1).toString()}) {Text("‹")}
   ControlTextButton(onClick={selectedText=today().toString()}) {Text("今天")}
   ControlTextButton(onClick={selectedText=selected.plusMonths(1).toString()}) {Text("›")}
  }
  Row {listOf("日","一","二","三","四","五","六").forEach {Text(it,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}
  val first=selected.withDayOfMonth(1)
  val offset=first.dayOfWeek.value%7
  val count=(offset+selected.lengthOfMonth()+6)/7*7
  (0 until count).chunked(7).forEach {week->Row {
   week.forEach {index->val day=index-offset+1
    if(day !in 1..selected.lengthOfMonth())Spacer(Modifier.weight(1f).height(64.dp)) else {
     val date=first.withDayOfMonth(day).toString()
     val todos=entries.filter {it.date==date}
     ControlTextButton(onClick={selectedText=date},modifier=Modifier.weight(1f).height(64.dp),contentPadding=PaddingValues(1.dp),colors=ButtonDefaults.textButtonColors(containerColor=if(date==selectedText)MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)) {
      Column(horizontalAlignment=Alignment.CenterHorizontally) {Text("$day");todos.take(2).forEach {Text((if(it.completed)"✓ " else "")+it.title,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall)}}
     }
    }
   }
  }}
  HorizontalDivider()
  Row(verticalAlignment=Alignment.CenterVertically) {Text("${selected.monthValue}月${selected.dayOfMonth}日",modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
   ControlButton(onClick={creating=true;title="";editing=DailyTodo(UUID.randomUUID().toString(),"",selectedText)},enabled=!busy) {Text("添加事项")}}
  if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
  error?.let {Hint(it,true)}
  val daily=entries.filter {it.date==selectedText}.sortedBy {it.completed}
  if(daily.isEmpty() && !busy && error==null)Text("暂无个人事项",color=MaterialTheme.colorScheme.onSurfaceVariant)
  daily.forEach {item->Row(verticalAlignment=Alignment.CenterVertically) {
   Checkbox(item.completed,{change(item.copy(completed=it))},enabled=!busy)
   ControlTextButton(onClick={creating=false;title=item.title;editing=item},enabled=!busy,modifier=Modifier.weight(1f)) {Text(item.title)}
   ControlTextButton(onClick={change(item,delete=true)},enabled=!busy) {Text("删除")}
  }}
 }
 }
 editing?.let {item->AlertDialog(onDismissRequest={if(!busy)editing=null},title={Text(if(creating)"添加个人事项" else "编辑个人事项")},text={Column {OutlinedTextField(title,{title=it.take(200)},label={Text("事项内容")},enabled=!busy);error?.let {Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={ControlTextButton(onClick={change(item.copy(title=title.trim()),new=creating)},enabled=!busy && title.isNotBlank()) {Text("保存")}},dismissButton={ControlTextButton(onClick={editing=null},enabled=!busy) {Text("取消")}})}
}
