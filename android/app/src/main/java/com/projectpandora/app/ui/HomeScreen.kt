package com.projectpandora.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
    val personal=if(state.developmentPreview)state.localPriorities.personal.orEmpty() else state.personal.items
    var tagging by remember {mutableStateOf<Pair<String,Long>?>(null)}
    var chosen by remember {mutableStateOf<List<Long>>(emptyList())}
    fun tags(kind: String,id: Long,fallback: WorkLabel?=null)=state.contentLabels.firstOrNull {it.kind==kind && it.contentId==id}?.labels ?: listOfNotNull(fallback)
    fun pick(kind: String,id: Long,fallback: WorkLabel?=null) {chosen=tags(kind,id,fallback).map {it.id};tagging=kind to id}
    tagging?.let {target->AlertDialog(onDismissRequest={tagging=null},title={Text("选择事项标签（可多选）")},text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())) {
        state.contentLabelError?.let {Hint(it,true)}
        if(state.labels.isEmpty())Text("暂无标签，请先由创始人维护品牌标签。")
        state.labels.forEach {label->Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {Checkbox(checked=label.id in chosen,onCheckedChange={checked->chosen=if(checked)chosen+label.id else chosen-label.id});Box(Modifier.padding(end=8.dp).size(width=5.dp,height=20.dp).background(labelColor(label),MaterialTheme.shapes.small));Text(label.name)}}
        val inactive=chosen.filter {id->state.labels.none {it.id==id}}
        if(inactive.isNotEmpty())ControlTextButton(onClick={chosen=chosen-inactive.toSet()},enabled=!state.saving) {Text("移除已停用的标签（${inactive.size}）")}
    }},confirmButton={ControlTextButton(onClick={model.saveContentLabels(target.first,target.second,chosen);tagging=null},enabled=!state.saving && state.contentLabelError==null) {Text("保存")}},dismissButton={ControlTextButton(onClick={tagging=null}) {Text("取消")}})}
    val logs=state.logs.sortedByDescending {it.createdAt}
    AnimatedContent(targetState=selectedPanel,transitionSpec={overviewTransition(if((targetState ?: initialState)=="公司十大事")TransformOrigin(0f,0f) else TransformOrigin(0f,1f))},label="overview-tile-expand") {panel->
    if(panel!=null) {
        PriorityScreen(state,model,panel=="公司十大事",initialSelectedId=if(panel=="公司十大事")selectedCompanyId else null,companyExamples=companyExamples) {selectedPanel=null}
    } else {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fontScale=androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
        val tileHeight=maxOf(340.dp,(maxHeight-28.dp)/2)*fontScale
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=4.dp,bottom=12.dp)) {
            state.workspaceError?.let {item {Hint(it,true)}}
            item {Row(Modifier.fillMaxWidth().height(tileHeight),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OverviewTile("公司十大事","组织重点",company.size,Modifier.weight(1f),onOpen={selectedCompanyId=null;selectedPanel="公司十大事"}) {
                    if(companyExamples)Text("示例预览",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                    if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
                    if(state.panelError!=null)Text("读取失败，请刷新",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                    if(company.isEmpty()) {
                        OverviewEmpty("尚未发布重点事项")
                        if(BuildConfig.DEBUG && !state.loading)ControlTextButton(onClick={showCompanyExamples=true},contentPadding=PaddingValues(0.dp)) {Text("查看示例",style=MaterialTheme.typography.labelSmall)}
                    }
                    company.take(2).forEachIndexed {index,entry->CompanyDepthCard(entry,index,compact=true,labels=tags("company",entry.id),onLabels=if(state.session?.role=="FOUNDER" && !companyExamples)({pick("company",entry.id)}) else null) {selectedCompanyId=entry.id;selectedPanel="公司十大事"}}
                }
                OverviewTile("公司派发任务","工作安排",dispatch.size,Modifier.weight(1f),onOpen={model.screen("tasks")}) {
                    if(dispatch.isEmpty())OverviewEmpty(if(state.taskLoading)"正在读取任务…" else "暂无派发任务")
                    dispatch.take(2).forEachIndexed {index,entry->
                        val task=state.tasks.firstOrNull {it.id==entry.id}
                        CompanyDepthCard(entry,index,compact=true,labels=tags("task",entry.id,task?.label),onLabels=if(task!=null && (task.createdBy==state.session?.userId || task.assigneeId==state.session?.userId || state.session?.role=="FOUNDER"))({pick("task",entry.id,task?.label)}) else null) {model.openTask(entry.id)}
                    }
                }
            }}
            item {Row(Modifier.fillMaxWidth().height(tileHeight),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OverviewTile("个人十大事","我的工作重点",personal.size,Modifier.weight(1f),onOpen={selectedPanel="个人十大事"}) {
                    if(state.personalLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.personalError?.let {Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}
                    if(state.personal.dirty)Text("有待同步的个人事项",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
                    if(personal.isEmpty())OverviewEmpty("整理个人重点，明确工作次序。")
                    personal.take(2).forEachIndexed {index,entry->CompanyDepthCard(entry,index,compact=true,labels=tags("personal",entry.id),onLabels={pick("personal",entry.id)}) {selectedPanel="个人十大事"}}
                }
                OverviewTile("个人日志","独立工作记录",logs.size,Modifier.weight(1f),onOpen={model.screen("logs")}) {
                    if(logs.isEmpty())OverviewEmpty("记录工作成果、问题与下一步。")
                    logs.take(2).forEachIndexed {index,log->CompanyDepthCard(PanelItem(log.id,log.content,null,null),index,compact=true,labels=tags("log",log.id,log.label),onLabels={pick("log",log.id,log.label)}) {model.edit(log)}}
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
    Surface(onClick=onClick,enabled=enabled,color=MaterialTheme.colorScheme.surface,contentColor=MaterialTheme.colorScheme.onSurface,shape=MaterialTheme.shapes.small,modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                taskReviewLabel(task.reviewStatus)?.let {Text(it,style=MaterialTheme.typography.labelSmall)}
                Text(task.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {TaskPriorityText(task.priority,onLabel=false);Text("${task.assigneeName} · ${taskFeedbackLabel(task)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            }
        }
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
}
