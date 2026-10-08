package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*
import java.time.LocalDate

@Composable fun LogCard(log: WorkLog, busy: Boolean, onEdit: ()->Unit, onSubmit: ()->Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(log.logDate,style=MaterialTheme.typography.labelLarge)
            Text(if(log.status=="draft")"草稿 · 仅自己可见" else "已提交",style=MaterialTheme.typography.labelSmall,
                color=if(log.status=="draft")MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
        }
        Text(log.content,style=MaterialTheme.typography.bodyLarge,maxLines=5,overflow=TextOverflow.Ellipsis)
        Text("记录 #${log.id} · 更新 ${timestamp(log.updatedAt)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.outline)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            TextButton(onClick=onEdit,enabled=!busy) {Text("查看 / 编辑")}
            if(log.status=="draft")TextButton(onClick=onSubmit,enabled=!busy) {Text("提交日志")}
        }
    }}
}
@Composable fun LogsScreen(state: AppState, model: PandoraViewModel, dayOnly: Boolean=false) {
    var allDates by rememberSaveable {mutableStateOf(false)}
    val logs=if(!dayOnly && allDates)state.logs else state.logs.filter {it.logDate==state.selectedDate}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)) {
        item {Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(if(dayOnly)"这一天的工作" else "我的工作记录",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Medium)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                DateButton(state.selectedDate,!state.saving) {allDates=false;model.selectDate(it)}
                if(!dayOnly)TextButton(onClick={allDates=!allDates},enabled=!state.saving) {Text(if(allDates)"按日期" else "全部日期")}
            }
            if(dayOnly)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={model.selectDate(LocalDate.parse(state.selectedDate).minusDays(1).toString())}) {Text("← 前一天")}
                TextButton(onClick={model.selectDate(today().toString())}) {Text("回到今天")}
            }
            state.logError?.let {Hint("读取失败：$it\n下面可能是上次成功读取的记录，请刷新。",true)}
            if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(!dayOnly)Button(onClick={model.edit()},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {Text("＋ 写一篇日志")}
        }}
        if(logs.isEmpty() && !state.loading && state.logError==null)item {
            EmptyCard("还没有工作记录",if(dayOnly)"换个日期看看，或去日志页记录工作。" else "写下完成的事，也留下接下来要解决的问题。")
        }
        items(logs,key={it.id}) {log->LogCard(log,state.saving,{model.edit(log)},{model.submit(log)})}
    }
}
@Composable fun EditorScreen(state: AppState, model: PandoraViewModel) {
    val editor=state.editor ?: return
    var discard by remember {mutableStateOf(false)}
    BackHandler {if(!state.saving)discard=true}
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("放弃这次修改？")},text={Text("未保存内容会被清除，服务器里的日志不会改变。")},confirmButton={TextButton(onClick={discard=false;model.discardEditor()}) {Text("放弃修改")}},dismissButton={TextButton(onClick={discard=false}) {Text("继续编辑")}})
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal=20.dp)) {
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=20.dp)) {
            item {Text(if(editor.logId==null)"记录今天的工作" else "编辑记录 #${editor.logId}",style=MaterialTheme.typography.headlineSmall)}
            item {Text(if(editor.status=="submitted")"已提交日志仍可编辑；保存后，领导刷新即可看到最新内容。" else "草稿仅自己可见，提交后领导才能查看。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            item {DateButton(editor.date,!state.saving) {model.changeEditor(date=it)}}
            item {OutlinedTextField(value=editor.content,onValueChange={if(it.length<=5000)model.changeEditor(content=it)},label={Text("工作记录")},placeholder={Text("今天完成了什么？遇到哪些问题？下一步准备做什么？")},enabled=!state.saving,modifier=Modifier.fillMaxWidth().heightIn(min=260.dp),minLines=9,supportingText={Text("${editor.content.length} / 5000")})}
            state.editorError?.let {item {Hint(it,true)}}
        }
        Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={discard=true},enabled=!state.saving) {Text("取消")}
            if(editor.status!="submitted")OutlinedButton(onClick={model.save("draft")},enabled=!state.saving) {Text("存草稿")}
            Button(onClick={model.save("submitted")},enabled=!state.saving,modifier=Modifier.weight(1f)) {Text(if(state.saving)"保存中…" else if(editor.status=="submitted")"保存修改" else "提交日志")}
        }
    }
}
