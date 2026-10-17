package com.projectpandora.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun HomeScreen(state: AppState,model: PandoraViewModel) {
    var selectedPanel by remember {mutableStateOf<String?>(null)}
    if(selectedPanel!=null) {
        PriorityScreen(state,model,selectedPanel=="公司十大事") {selectedPanel=null}
        return
    }
    val company=state.panels?.companyImportant.orEmpty().sortedBy {it.sortOrder}
    val taskPositions=orderedTasks(state.tasks).mapIndexed {index,task->task.id to index}.toMap()
    val dispatch=state.panels?.companyDispatch.orEmpty().sortedWith(compareBy<PanelItem> {taskPositions[it.id] ?: Int.MAX_VALUE}.thenBy {it.sortOrder ?: Int.MAX_VALUE})
    val personal=state.localPriorities.personal ?: state.panels?.personalTop.orEmpty().sortedBy {it.sortOrder}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=8.dp,bottom=24.dp)) {
        item {OverviewSection("公司十大事","公司的重点与工作方向",company.size,248.dp,onOpen={selectedPanel="公司十大事"}) {
            if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            state.panelError?.let {Hint("公司事项读取失败：$it",true,onRetry=model::refresh)}
            if(company.isEmpty() && !state.loading && state.panelError==null)Text("公司尚未发布重点事项",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            company.take(3).forEachIndexed {index,entry->
                Column(Modifier.fillMaxWidth().clickable {selectedPanel="公司十大事"}.padding(vertical=6.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text("${(index+1).toString().padStart(2,'0')}  ${entry.title}",style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                    entry.detail?.takeIf {it.isNotBlank()}?.let {Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)}
                }
                if(index<company.take(3).lastIndex)HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            }
        }}
        item {OverviewSection("公司派发任务","接收组织的工作安排",dispatch.size,168.dp,onOpen={model.screen("tasks")}) {
            if(dispatch.isEmpty())Text(if(state.loading)"正在读取派发任务…" else if(state.panelError!=null)"派发任务暂不可用" else "暂无公司派发任务",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            dispatch.take(2).forEach {entry->
                val task=state.tasks.firstOrNull {it.id==entry.id}
                Column(Modifier.fillMaxWidth().clickable(enabled=!state.saving) {model.openTask(entry.id)}.padding(vertical=5.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(entry.title,style=MaterialTheme.typography.bodyLarge,maxLines=2,overflow=TextOverflow.Ellipsis)
                    if(task!=null)Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {TaskPriorityText(task.priority);Text("${task.assigneeName} · ${taskStatusLabel(task.status)}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                }
            }
        }}
        item {OverviewSection("个人十大事","我的工作重点",personal.size,112.dp,onOpen={selectedPanel="个人十大事"}) {
            if(personal.isEmpty())Text("还没有个人事项，点击查看全部后添加。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            personal.take(2).forEachIndexed {index,entry->Text("${index+1}. ${entry.title}",modifier=Modifier.fillMaxWidth().clickable {selectedPanel="个人十大事"}.padding(vertical=3.dp),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)}
        }}
    }
}

@Composable private fun OverviewSection(title: String,subtitle: String,count: Int,minHeight: Dp,onOpen: ()->Unit,content: @Composable ColumnScope.()->Unit) {
    Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=0.97f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
        Column(Modifier.heightIn(min=minHeight).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(title,style=MaterialTheme.typography.titleLarge)
                    Text("$subtitle · $count 项",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ControlTextButton(onClick=onOpen,contentPadding=PaddingValues(horizontal=4.dp)) {Text("查看全部",style=MaterialTheme.typography.labelSmall)}
            }
            content()
        }
    }
}

@Composable fun TaskSummary(task: DispatchTask,enabled: Boolean,onClick: ()->Unit) {
    Surface(onClick=onClick,enabled=enabled,color=androidx.compose.ui.graphics.Color.Transparent,modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(task.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {TaskPriorityText(task.priority);Text("${task.assigneeName} · ${taskStatusLabel(task.status)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
        }
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
}
