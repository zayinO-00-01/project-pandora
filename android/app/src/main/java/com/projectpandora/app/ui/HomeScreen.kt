package com.projectpandora.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Warning
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun HomeScreen(state: AppState, model: PandoraViewModel) {
    var selectedPanel by remember {mutableStateOf<String?>(null)}
    var selectedItem by remember {mutableStateOf<PanelItem?>(null)}
    val feedback=LocalControlFeedback.current
    val panels=state.panels
    val panelItems=when(selectedPanel) {
        "公司十大事"->panels?.companyImportant.orEmpty()
        "个人十大事"->panels?.personalTop.orEmpty()
        else->emptyList()
    }
    if(selectedPanel!=null) {
        androidx.activity.compose.BackHandler {selectedPanel=null;selectedItem=null}
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=24.dp)) {
            item {TextButton(onClick={selectedPanel=null;selectedItem=null}) {Text("返回总览")}}
            item {ScreenHeading(selectedPanel!!,if(selectedPanel=="公司十大事")"公司发布的正式事项，只读查看。" else "从个人日志汇总，聚焦重要工作。")}
            if(panelItems.isEmpty())item {EmptyCard(if(selectedPanel=="公司十大事")"公司事项筹备中" else "个人重点筹备中",if(selectedPanel=="公司十大事")"发布方式确认后开放，目前可先查看日志与任务。" else "重点整理功能后续开放，目前可先在日志里记录重要工作。")}
            panelItems.sortedBy {it.sortOrder}.take(10).forEach {entry->item(key=entry.id) {
                OutlinedCard(onClick={selectedItem=entry},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(entry.title,style=MaterialTheme.typography.titleMedium)
                        Text("查看详情",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    }
                }
            }}
        }
        selectedItem?.let {entry->AlertDialog(containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={selectedItem=null},title={Text(entry.title)},
            text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(entry.detail?.ifBlank {"暂无详细说明"} ?: "暂无详细说明")
                Text("个人备注：${entry.myRemark?.ifBlank {"尚无备注"} ?: "尚无备注"}")
            }},confirmButton={TextButton(onClick={selectedItem=null}) {Text("关闭")}})}
        return
    }
    val pending=state.tasks.filter {it.status!="done"}.sortedWith(compareBy<DispatchTask> {it.dueAt==null}.thenBy {it.dueAt}).take(2)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(top=4.dp,bottom=32.dp)) {
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text(AppRoles.label(state.session?.role.orEmpty()),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={feedback();model.screen("organization")},enabled=!state.saving,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {
                Text(AppRoles.scopeLabel(state.session?.role.orEmpty()),style=MaterialTheme.typography.labelMedium)
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(16.dp))
            }
        }}
        if(state.loading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        if(pending.isNotEmpty() || state.loading || state.taskError!=null)item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("接下来处理",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)

            }
            TextButton(onClick={model.screen("tasks")},enabled=!state.saving,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text("全部任务",style=MaterialTheme.typography.labelMedium);Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(16.dp))}
        }}

        if(pending.isEmpty() && !state.loading && !state.taskLoading && state.taskError==null)item {Column(Modifier.fillMaxWidth().then(if(panels?.todayLogs.isNullOrEmpty())Modifier.padding(top=56.dp,bottom=16.dp) else Modifier.padding(vertical=16.dp)),verticalArrangement=if(panels?.todayLogs.isNullOrEmpty())Arrangement.Center else Arrangement.Top) {
            Text("没有待处理的任务",style=MaterialTheme.typography.titleLarge.copy(fontWeight=FontWeight.Medium))
            Spacer(Modifier.height(8.dp))
            Text("记录进展，或查看已有工作。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={model.screen("tasks")},enabled=!state.saving,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary),contentPadding=PaddingValues(0.dp)) {Text("查看全部任务",style=MaterialTheme.typography.labelMedium)}
        }}
        pending.firstOrNull()?.let {task->item(key="focus-${task.id}") {
            Surface(onClick={feedback();model.openTask(task.id)},enabled=!state.saving,color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(if(task.status=="doing")"进行中" else "待开始",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
                        Text(task.dueAt?.let {"${timestamp(it)} 截止"} ?: "未设截止时间",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(task.title,style=MaterialTheme.typography.titleLarge.copy(fontSize=20.sp,lineHeight=28.sp,fontWeight=FontWeight.Medium),maxLines=2,overflow=TextOverflow.Ellipsis)
                    val note=task.progressNote.ifBlank {task.detail}
                    if(note.isNotBlank())Text(note,style=MaterialTheme.typography.bodySmall.copy(lineHeight=18.sp,lineBreak=androidx.compose.ui.text.style.LineBreak.Heading),color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        TextButton(onClick={feedback();model.openTask(task.id)},enabled=!state.saving,modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(horizontal=8.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text("更新进展",style=MaterialTheme.typography.labelMedium)}
                    }
                }
            }
        }}
        pending.drop(1).forEach {task->item(key="next-${task.id}") {
            Row(Modifier.fillMaxWidth().clickable(enabled=!state.saving) {feedback();model.openTask(task.id)}.padding(vertical=12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(task.dueAt?.let {"${timestamp(it)} 截止"} ?: "未设截止时间",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(task.title,style=MaterialTheme.typography.bodyLarge,maxLines=2,overflow=TextOverflow.Ellipsis)
                }
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }}
        item {Spacer(Modifier.height(8.dp));HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)}
        if(pending.isNotEmpty() || !panels?.todayLogs.isNullOrEmpty() || state.loading || state.panelError!=null)item {
            val excerpt=panels?.todayLogs?.firstOrNull()?.content?.let {recordPreview(it).first}
            Row(Modifier.fillMaxWidth().heightIn(min=64.dp).clickable {model.screen("logs");model.selectDate(today().toString())}.padding(vertical=8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("今天的日志",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(excerpt ?: if(state.loading)"正在读取…" else if(state.panelError!=null)"今日记录暂不可用" else "今天还没有记录。",maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyLarge.copy(fontSize=15.sp,lineHeight=24.sp,fontWeight=FontWeight.Medium),color=if(excerpt!=null)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {Column {
            Text("工作分类",modifier=Modifier.padding(top=12.dp,bottom=4.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            WorkspacePanel("公司十大事",panels?.companyImportant?.size,!state.saving,"筹备中") {feedback();selectedPanel="公司十大事"}
            WorkspacePanel("公司派发",panels?.companyDispatch?.size,!state.saving,"") {feedback();model.screen("tasks")}
            WorkspacePanel("个人十大事",panels?.personalTop?.size,!state.saving,"筹备中") {feedback();selectedPanel="个人十大事"}
        }}

    }
}

@Composable private fun WorkspacePanel(title: String,count: Int?,enabled: Boolean,source: String,onClick: ()->Unit) {
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable(enabled=enabled,onClick=onClick),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurface,modifier=Modifier.weight(1f))
            Text(if(source.isNotBlank() && (count ?: 0)==0)source else count?.let {"$it 项"} ?: "—",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
}

@Composable fun TaskSummary(task: DispatchTask,enabled: Boolean,onClick: ()->Unit) {
    Surface(onClick=onClick,enabled=enabled,color=androidx.compose.ui.graphics.Color.Transparent,modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(task.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text("${task.assigneeName} · ${if(task.status=="done")"已完成" else if(task.status=="doing")"进行中" else "待开始"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
}
