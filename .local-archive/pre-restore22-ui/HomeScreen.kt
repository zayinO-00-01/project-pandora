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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Warning
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun HomeScreen(state: AppState, model: PandoraViewModel,onSyncDetails: ()->Unit) {
    var selectedPanel by remember {mutableStateOf<String?>(null)}
    var selectedItem by remember {mutableStateOf<PanelItem?>(null)}
    var emptyCategoryMenu by remember {mutableStateOf(false)}
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
            if(panelItems.isEmpty())item {EmptyCard("暂无事项","当前服务尚未提供事项维护，已有工作可在日志与任务中查看。")}
            panelItems.sortedBy {it.sortOrder}.take(10).forEach {entry->item(key=entry.id) {
                OutlinedCard(onClick={selectedItem=entry},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(entry.title,style=MaterialTheme.typography.titleMedium)
                        Text("查看详情",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    }
                }
            }}
            item {Hint(if(selectedPanel=="公司十大事")"公司原文只读；个人备注写入尚未开放。" else "个人事项编辑与排序尚未开放，不影响日志记录。")}
        }
        selectedItem?.let {entry->AlertDialog(containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={selectedItem=null},title={Text(entry.title)},
            text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(entry.detail?.ifBlank {"暂无详细说明"} ?: "当前服务仅提供事项标题。")
                Text("个人备注：${entry.myRemark?.ifBlank {"尚无备注"} ?: "尚无备注"}")
                Text("备注编辑尚未开放",style=MaterialTheme.typography.bodySmall)
            }},confirmButton={TextButton(onClick={selectedItem=null}) {Text("关闭")}})}
        return
    }
    val hasReadFailure=state.logError!=null || state.taskError!=null || state.panelError!=null
    val retained=state.logs.isNotEmpty() || state.tasks.isNotEmpty() || panels!=null
    val pending=state.tasks.filter {it.status!="done"}.sortedWith(compareBy<DispatchTask> {it.dueAt==null}.thenBy {it.dueAt}).take(2)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(top=4.dp,bottom=32.dp)) {
        if(state.loading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        item {Row(Modifier.fillMaxWidth().height(48.dp * maxOf(1f,LocalDensity.current.fontScale)),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text("${today().monthValue}月${today().dayOfMonth}日",style=MaterialTheme.typography.labelMedium)
                Text(listOf("周一","周二","周三","周四","周五","周六","周日")[today().dayOfWeek.value-1],style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.then(if(hasReadFailure)Modifier.clickable(onClick=onSyncDetails) else Modifier).heightIn(min=48.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=androidx.compose.ui.Alignment.End) {
                Text(if(hasReadFailure)"同步失败" else if(state.developmentPreview)"未连接" else if(state.loading)"同步中" else "已同步",style=MaterialTheme.typography.labelSmall,color=if(hasReadFailure)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                if(hasReadFailure)Text(if(retained)"上次内容 ›" else "暂未读取 ›",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(hasReadFailure)TextButton(onClick={feedback();model.refresh()},enabled=!state.loading && !state.saving,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text("重试",style=MaterialTheme.typography.labelMedium)}
        }}
        item {Row(Modifier.fillMaxWidth().heightIn(min=48.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Text("接下来处理",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={model.screen("tasks")},enabled=!state.saving,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text("全部任务",style=MaterialTheme.typography.labelMedium);Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(16.dp))}
        }}
        if(pending.isEmpty() && !state.loading && !state.taskLoading && state.taskError==null)item {
            Text("目前没有待处理任务。",modifier=Modifier.padding(top=8.dp,bottom=16.dp),style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        pending.firstOrNull()?.let {task->item(key="focus-${task.id}") {
            Surface(onClick={feedback();model.openTask(task.id)},enabled=!state.saving,color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(if(task.status=="doing")"进行中" else "待开始",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(task.dueAt?.let {"${timestamp(it)} 截止"} ?: "未设截止时间",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(task.title,style=MaterialTheme.typography.titleLarge.copy(fontSize=20.sp,lineHeight=28.sp,fontWeight=FontWeight.Medium),maxLines=2,overflow=TextOverflow.Ellipsis)
                    val note=task.progressNote.ifBlank {task.detail}
                    if(note.isNotBlank())Text(note,style=MaterialTheme.typography.bodySmall.copy(lineHeight=18.sp,lineBreak=androidx.compose.ui.text.style.LineBreak.Heading),color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("已完成 ${task.progress}%",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            LinearProgressIndicator(progress={task.progress.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth().height(3.dp),color=MaterialTheme.colorScheme.secondary,gapSize=0.dp,drawStopIndicator={})
                        }
                        TextButton(onClick={feedback();model.openTask(task.id)},enabled=!state.saving,modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(horizontal=8.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text("更新进展",style=MaterialTheme.typography.labelMedium)}
                    }
                }
            }
        }}
        pending.drop(1).forEach {task->item(key="next-${task.id}") {
            Row(Modifier.fillMaxWidth().clickable(enabled=!state.saving) {feedback();model.openTask(task.id)}.padding(vertical=12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(task.dueAt?.let {"${timestamp(it)} 截止"} ?: "未设截止时间",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(task.title,style=MaterialTheme.typography.bodyLarge.copy(fontWeight=FontWeight.Normal),maxLines=2,overflow=TextOverflow.Ellipsis)
                }
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }}
        item {Spacer(Modifier.height(8.dp));HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)}
        item {
            val excerpt=panels?.todayLogs?.firstOrNull()?.content?.let {recordPreview(it).first}
            if(excerpt==null && !state.loading && state.panelError==null && !hasReadFailure) {
                Column(Modifier.fillMaxWidth().padding(top=16.dp,bottom=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("今天的日志",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("记下今天的进展",style=MaterialTheme.typography.bodyLarge.copy(fontWeight=FontWeight.Medium))
                    Text("完成的事、遇到的问题，还有下一步。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    WriteLogButton(enabled=!state.saving && !state.checkingIntegrity,onClick={model.edit()})
                }
            } else Row(Modifier.fillMaxWidth().heightIn(min=64.dp).clickable {model.screen("logs");model.selectDate(today().toString())}.padding(vertical=8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("今天的日志",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(excerpt ?: if(state.loading)"正在读取…" else "今日记录暂不可用",maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyLarge.copy(fontSize=15.sp,lineHeight=24.sp,fontWeight=FontWeight.Normal),color=if(excerpt!=null)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if(panels?.let {it.companyImportant.isEmpty() && it.companyDispatch.isEmpty() && it.personalTop.isEmpty()}==true)item {Box {
            TextButton(onClick={emptyCategoryMenu=true},colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary),contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=48.dp)) {Text("查看工作分类",style=MaterialTheme.typography.bodyMedium);Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(16.dp))}
            DropdownMenu(expanded=emptyCategoryMenu,onDismissRequest={emptyCategoryMenu=false}) {
                listOf("公司十大事","公司派发","个人十大事").forEach {label->DropdownMenuItem(text={Text(label)},onClick={emptyCategoryMenu=false;if(label=="公司派发")model.screen("tasks") else selectedPanel=label})}
            }
        }} else item {Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(2f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("公司",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    WorkspaceIndex("十大事",panels?.companyImportant?.size,!state.saving,"公司",Modifier.weight(1f)) {feedback();selectedPanel="公司十大事"}
                    WorkspaceIndex("派发",panels?.companyDispatch?.size,!state.saving,"公司",Modifier.weight(1f)) {feedback();model.screen("tasks")}
                }
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("个人",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                WorkspaceIndex("十大事",panels?.personalTop?.size,!state.saving,"个人",Modifier.fillMaxWidth()) {feedback();selectedPanel="个人十大事"}
            }
        }}
    }
}

@Composable private fun WorkspaceIndex(title: String,count: Int?,enabled: Boolean,source: String,modifier: Modifier,onClick: ()->Unit) {
    Column(modifier.heightIn(min=48.dp).semantics {contentDescription="$source$title";stateDescription=count?.let {"$it 项"} ?: "尚未读取"}.clickable(enabled=enabled,onClick=onClick),verticalArrangement=Arrangement.Center) {
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,style=MaterialTheme.typography.labelMedium,maxLines=1)
            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(14.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(count?.let {"$it 项"} ?: "—",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun TaskSummary(task: DispatchTask,enabled: Boolean,onClick: ()->Unit) {
    Surface(onClick=onClick,enabled=enabled,color=MaterialTheme.colorScheme.background,modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(task.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text("${task.assigneeName} · ${if(task.status=="done")"已完成" else if(task.status=="doing")"进行中" else "待开始"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress={task.progress.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth().height(2.dp),color=MaterialTheme.colorScheme.secondary,gapSize=0.dp,drawStopIndicator={})
            }
            Text("${task.progress}%",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
        }
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
}
