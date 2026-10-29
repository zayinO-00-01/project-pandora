package com.projectpandora.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import com.projectpandora.app.BuildConfig
import com.projectpandora.app.data.*

@Composable fun HomeScreen(state: AppState,model: PandoraViewModel) {
    var selectedPanel by remember {mutableStateOf<String?>(null)}
    var selectedCompanyId by remember {mutableStateOf<Long?>(null)}
    var showCompanyExamples by remember {mutableStateOf(false)}
    val companyExamples=state.developmentPreview || (BuildConfig.DEBUG && showCompanyExamples)
    val company=if(companyExamples)CompanyPriorityExamples.items else state.panels?.companyImportant.orEmpty().sortedBy {it.sortOrder}
    val taskPositions=orderedTasks(state.tasks).mapIndexed {index,task->task.id to index}.toMap()
    val dispatch=state.panels?.companyDispatch.orEmpty().sortedWith(compareBy<PanelItem> {taskPositions[it.id] ?: Int.MAX_VALUE}.thenBy {it.sortOrder ?: Int.MAX_VALUE})
    val personal=state.localPriorities.personal ?: state.panels?.personalTop.orEmpty().sortedBy {it.sortOrder}
    val logs=state.logs.sortedByDescending {it.createdAt}
    AnimatedContent(targetState=selectedPanel,transitionSpec={overviewTransition(if((targetState ?: initialState)=="公司十大事")TransformOrigin(0f,0f) else TransformOrigin(0f,1f))},label="overview-tile-expand") {panel->
    if(panel!=null) {
        PriorityScreen(state,model,panel=="公司十大事",initialSelectedId=if(panel=="公司十大事")selectedCompanyId else null,companyExamples=companyExamples) {selectedPanel=null}
    } else {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fontScale=androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
        val tileHeight=maxOf(280.dp,(maxHeight-28.dp)/2)*fontScale
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=4.dp,bottom=12.dp)) {
            item {Row(Modifier.fillMaxWidth().height(tileHeight),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OverviewTile("公司十大事","组织重点",company.size,Modifier.weight(1f),onOpen={selectedCompanyId=null;selectedPanel="公司十大事"}) {
                    if(companyExamples)Text("示例预览",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                    if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
                    if(state.panelError!=null)Text("读取失败，请刷新",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                    if(company.isEmpty()) {
                        OverviewEmpty("尚未发布重点事项")
                        if(BuildConfig.DEBUG && !state.loading)ControlTextButton(onClick={showCompanyExamples=true},contentPadding=PaddingValues(0.dp)) {Text("查看示例",style=MaterialTheme.typography.labelSmall)}
                    }
                    company.take(2).forEachIndexed {index,entry->CompanyDepthCard(entry,index,compact=true) {selectedCompanyId=entry.id;selectedPanel="公司十大事"}}
                }
                OverviewTile("公司派发任务","工作安排",dispatch.size,Modifier.weight(1f),onOpen={model.screen("tasks")}) {
                    if(dispatch.isEmpty())OverviewEmpty(if(state.taskLoading)"正在读取任务…" else "暂无派发任务")
                    dispatch.take(2).forEach {entry->
                        val task=state.tasks.firstOrNull {it.id==entry.id}
                        Column(Modifier.fillMaxWidth().background(if(task?.label!=null)labelColor(task.label) else androidx.compose.ui.graphics.Color.Transparent,MaterialTheme.shapes.small).clickable(enabled=!state.saving) {model.openTask(entry.id)}.padding(horizontal=4.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(entry.title,color=if(task?.label!=null)labelTextColor(task.label) else MaterialTheme.colorScheme.onSurface,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                            if(task!=null) {TaskPriorityText(task.priority,onLabel=task.label!=null);Text(taskStatusLabel(task.status),style=MaterialTheme.typography.labelSmall,color=if(task.label!=null)labelTextColor(task.label) else MaterialTheme.colorScheme.onSurfaceVariant)}
                        }
                    }
                }
            }}
            item {Row(Modifier.fillMaxWidth().height(tileHeight),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OverviewTile("个人十大事","我的工作重点",personal.size,Modifier.weight(1f),onOpen={selectedPanel="个人十大事"}) {
                    if(personal.isEmpty())OverviewEmpty("整理个人重点，明确工作次序。")
                    personal.take(3).forEachIndexed {index,entry->Text("${index+1}. ${entry.title}",modifier=Modifier.fillMaxWidth().clickable {selectedPanel="个人十大事"}.padding(vertical=4.dp),style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis)}
                }
                OverviewTile("个人日志","独立工作记录",logs.size,Modifier.weight(1f),onOpen={model.screen("logs")}) {
                    if(logs.isEmpty())OverviewEmpty("记录工作成果、问题与下一步。")
                    logs.take(1).forEach {log->Column(Modifier.fillMaxWidth().clickable(enabled=!state.saving) {model.edit(log)}.padding(vertical=4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(logDateLabel(log.logDate,log.endDate),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(log.content,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                    }}
                }
            }}
        }
    }
    }
    }
}
@Composable private fun OverviewEmpty(text: String) {
    Text(text,modifier=Modifier.padding(top=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}
@Composable private fun OverviewTile(title: String,subtitle: String,count: Int,modifier: Modifier,onOpen: ()->Unit,content: @Composable ColumnScope.()->Unit) {
    Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=0.97f),shape=MaterialTheme.shapes.medium,modifier=modifier.fillMaxHeight().panelOutline()) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text("$subtitle · $count 项",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp),content=content)
            ControlTextButton(onClick=onOpen,modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(0.dp)) {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text("查看全部",style=MaterialTheme.typography.labelMedium);Text("↗")}}
        }
    }
}

@Composable fun TaskSummary(task: DispatchTask,enabled: Boolean,onClick: ()->Unit) {
    Surface(onClick=onClick,enabled=enabled,color=if(task.label!=null)labelColor(task.label) else androidx.compose.ui.graphics.Color.Transparent,contentColor=if(task.label!=null)labelTextColor(task.label) else MaterialTheme.colorScheme.onSurface,shape=MaterialTheme.shapes.small,modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(task.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {TaskPriorityText(task.priority,onLabel=task.label!=null);Text("${task.assigneeName} · ${taskStatusLabel(task.status)}",style=MaterialTheme.typography.bodySmall,color=if(task.label!=null)labelTextColor(task.label) else MaterialTheme.colorScheme.onSurfaceVariant)}
            }
        }
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
}
