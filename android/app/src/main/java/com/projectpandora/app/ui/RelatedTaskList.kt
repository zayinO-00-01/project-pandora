package com.projectpandora.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun RelatedTaskList(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    var query by rememberSaveable(me.userId) {mutableStateOf("")}
    var filter by rememberSaveable(me.userId) {mutableStateOf("all")}
    val tasks=relatedTasks(state.tasks,me.userId).filter {
        (filter!="created" || it.createdBy==me.userId) && (filter!="assigned" || it.assigneeId==me.userId) &&
        (query.isBlank() || it.title.contains(query.trim(),true) || it.assigneeName.contains(query.trim(),true))
    }
    Column(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            BackdropTextButton(onClick={model.screen("day")}) {Text("← 返回视图")}
            ControlButton(onClick=model::openDispatch,enabled=!state.saving) {Text("创建任务")}
        }
        Text("与我相关的任务",style=MaterialTheme.typography.titleLarge)
        OutlinedTextField(query,{query=it},singleLine=true,label={Text("搜索任务或责任人")},modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("all" to "全部","assigned" to "我负责的","created" to "我创建的").forEach {(key,label)->FilterChip(selected=filter==key,onClick={filter=key},label={Text(label)})}
        }
        if(state.taskLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
        state.taskError?.let {Hint(it,true,onRetry=model::refresh)}
        Text("${tasks.size} 项",style=MaterialTheme.typography.labelSmall)
        if(tasks.isEmpty() && !state.taskLoading)EmptyCard("暂无匹配的任务","只有本人创建或负责的任务会显示在这里。")
        Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            LazyColumn(Modifier.width(1010.dp).fillMaxHeight(),contentPadding=PaddingValues(bottom=24.dp)) {
                item {Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical=12.dp)) {
                    TaskCell("序号",45);TaskCell("任务名称",200);TaskCell("截止时间",155);TaskCell("优先级",110);TaskCell("完成情况",110);TaskCell("进度备注",230);TaskCell("负责人",160)
                }}
                itemsIndexed(tasks,key={_,task->task.id}) {index,task->
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha=.9f)).clickable(enabled=!state.saving) {model.openTask(task.id)}.padding(vertical=14.dp)) {
                        TaskCell((index+1).toString(),45);Column(Modifier.width(200.dp)) {TaskCell(task.title,200);LabelLines(contentTags(state,"task",task.id,task.label),Modifier.padding(start=8.dp,top=6.dp))};TaskCell(dueLabel(task.dueAt),155)
                        Box(Modifier.width(110.dp).padding(horizontal=8.dp)) {TaskPriorityText(task.priority,onLabel=false)}
                        TaskCell(taskFeedbackLabel(task),110);TaskCell(task.progressNote.ifBlank {"暂无进度说明"},230);TaskCell(task.assigneeName,160)
                    }}
                    HorizontalDivider()
                }
            }
        }
    }
}
@Composable private fun TaskCell(text: String,width: Int) {Text(text,modifier=Modifier.width(width.dp).padding(horizontal=8.dp),style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)}
