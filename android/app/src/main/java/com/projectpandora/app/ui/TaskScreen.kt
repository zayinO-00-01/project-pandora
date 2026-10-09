package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable fun TaskScreen(state: AppState, model: PandoraViewModel) {
    val selected=state.selectedTaskId
    val task=state.tasks.firstOrNull {it.id==selected}
    val buffer=state.taskEditor
    var discard by remember {mutableStateOf(false)}
    BackHandler(enabled=selected!=null) {if(!state.saving)model.closeTask()}
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("放弃未提交的反馈？")},
        text={Text("此次输入会被清除，已保存的任务进度不会改变。")},
        confirmButton={TextButton(onClick={discard=false;model.discardTaskEditor()}) {Text("放弃反馈")}},
        dismissButton={TextButton(onClick={discard=false}) {Text("继续保留")}})
    LazyColumn(Modifier.fillMaxSize().imePadding().padding(horizontal=20.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)) {
        item {Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(selected!=null)TextButton(onClick=model::closeTask,enabled=!state.saving,contentPadding=PaddingValues(0.dp)) {Text("← 返回任务列表")}
            Text(if(selected==null)"派发任务" else "任务 #$selected",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Medium)
            if(selected==null)Text("查看任务，及时记录进展与需要协调的问题。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(state.taskLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
            state.taskError?.let {Hint("任务读取失败：$it\n请刷新；已显示的任务可能是上次读取的内容。",true)}
        }}
        if(buffer!=null && buffer.taskId!=selected)item {Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Hint("任务 #${buffer.taskId} 有未提交的反馈，离开页面会保留输入。")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={model.openTask(buffer.taskId)},enabled=!state.saving) {Text("继续填写")}
                TextButton(onClick={discard=true},enabled=!state.saving) {Text("放弃反馈")}
            }
        }}
        if(selected==null) {
            if(state.tasks.isEmpty() && !state.taskLoading && state.taskError==null)item {
                EmptyCard("暂无派发任务","领导派发后，刷新这里即可查看。")
            }
            items(state.tasks,key={it.id}) {row->OutlinedCard(onClick={model.openTask(row.id)},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(row.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text("${priorityLabel(row.priority)} · ${statusLabel(row.status)} · ${row.progress}%",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(progress={row.progress.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth())
                    Text("责任人：${row.assigneeName} · 截止 ${dueLabel(row.dueAt)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("任务 #${row.id} · 查看详情 →",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.outline)
                }
            }}
        } else if(task==null) {
            if(!state.taskLoading && state.taskError==null)item {EmptyCard("暂时无法查看此任务","任务可能已不在你的可见范围内，请返回列表或刷新。")}
            if(buffer?.taskId==selected)item {Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Hint("未提交反馈仍在本机保留。刷新确认任务后即可继续填写。")
                Text("进度：${buffer.progress}%\n说明：${buffer.note}",style=MaterialTheme.typography.bodyMedium)
                TextButton(onClick={discard=true},enabled=!state.saving) {Text("放弃未提交反馈")}
            }}
        } else {
            item {OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(task.title,style=MaterialTheme.typography.titleLarge)
                Text("${priorityLabel(task.priority)} · ${statusLabel(task.status)} · ${task.progress}%",color=MaterialTheme.colorScheme.primary)
                LinearProgressIndicator(progress={task.progress.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth())
                Text(task.detail.ifBlank {"暂无任务说明"},style=MaterialTheme.typography.bodyLarge)
                HorizontalDivider()
                Text("派发人：${task.creatorName}\n责任人：${task.assigneeName}\n截止时间：${dueLabel(task.dueAt)}",style=MaterialTheme.typography.bodyMedium)
                Text("创建 ${dueLabel(task.createdAt)}\n更新 ${dueLabel(task.updatedAt)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.outline)
            }}}
            item {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("最新进度说明",style=MaterialTheme.typography.titleMedium)
                Text(task.progressNote.ifBlank {"尚未反馈进度"},style=MaterialTheme.typography.bodyLarge)
            }}
            if(task.assigneeId!=state.session?.userId)item {Hint("仅当前责任人可反馈进度，你可以查看任务详情。")}
            else if(buffer?.taskId==selected) {
                item {Text("反馈进度",style=MaterialTheme.typography.titleMedium)}
                item {OutlinedTextField(value=buffer.progress,onValueChange={model.changeTaskEditor(progress=it)},enabled=!state.saving,
                    label={Text("进度（0–100）")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                    supportingText={Text("0% 待开始 · 1–99% 进行中 · 100% 已完成")},modifier=Modifier.fillMaxWidth())}
                item {OutlinedTextField(value=buffer.note,onValueChange={if(it.length<=2000)model.changeTaskEditor(note=it)},enabled=!state.saving,
                    label={Text("进度说明（必填）")},placeholder={Text("完成了什么？有哪些问题？下一步是什么？")},minLines=5,
                    supportingText={Text("${buffer.note.length} / 2000")},modifier=Modifier.fillMaxWidth())}
                state.taskEditorError?.let {item {Hint(it,true)}}
                item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick={discard=true},enabled=!state.saving) {Text("放弃反馈")}
                    Button(onClick=model::saveTaskProgress,enabled=!state.saving,modifier=Modifier.weight(1f)) {Text(if(state.saving)"提交中…" else "提交进度")}
                }}
                item {Text("返回或切换页面会保留输入；提交成功后同步到领导端。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            } else if(buffer==null)item {Button(onClick={model.editTask(task)},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {Text("反馈 / 更正进度")}}
        }
    }
}
private fun priorityLabel(value: String)=when(value) {"low"->"低优先级";"high"->"高优先级";else->"普通优先级"}
private fun statusLabel(value: String)=when(value) {"doing"->"进行中";"done"->"已完成";else->"待开始"}
private fun dueLabel(value: String?): String = if(value==null)"未设置" else runCatching {
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")).format(Instant.parse(value))
}.getOrDefault(value)