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
    val dispatch=orderedTasks(state.tasks.filter {it.assigneeId==state.session?.userId && taskIsApproved(it) && !it.deleted}).map {PanelItem(it.id,it.title,null,null)}
    var tagging by remember {mutableStateOf<Pair<String,Long>?>(null)}
    var chosen by remember {mutableStateOf<List<Long>>(emptyList())}
    fun tags(kind: String,id: Long,fallback: WorkLabel?=null)=state.contentLabels.firstOrNull {it.kind==kind && it.contentId==id}?.labels ?: listOfNotNull(fallback)
    fun pick(kind: String,id: Long,fallback: WorkLabel?=null) {chosen=tags(kind,id,fallback).map {it.id};tagging=kind to id}
    tagging?.let {target->AlertDialog(onDismissRequest={tagging=null},title={Text("选择事项标签（可多选）")},text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())) {
        state.contentLabelError?.let {Hint(it,true)}
        if(state.labels.isEmpty())Text("暂无标签，请先由创始人维护品牌标签。")
        state.labels.forEach {label->Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {Checkbox(checked=label.id in chosen,onCheckedChange={checked->chosen=if(checked)chosen+label.id else chosen-label.id});Box(Modifier.padding(end=8.dp).size(width=5.dp,height=20.dp).background(labelColor(label),MaterialTheme.shapes.small));Text(label.name)}}
    }},confirmButton={ControlTextButton(onClick={model.saveContentLabels(target.first,target.second,chosen);tagging=null},enabled=!state.saving && state.contentLabelError==null) {Text("保存")}},dismissButton={ControlTextButton(onClick={tagging=null}) {Text("取消")}})}
    AnimatedContent(targetState=selectedPanel,transitionSpec={overviewTransition(if((targetState ?: initialState)=="公司十大事")TransformOrigin(0f,0f) else TransformOrigin(0f,1f))},label="overview-tile-expand") {panel->
    if(panel=="任务看板") {
        androidx.activity.compose.BackHandler {selectedPanel=null}
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp)) {
            item {BackdropTextButton(onClick={selectedPanel=null}) {Text("返回首页")}}
            item {Text("任务看板",style=MaterialTheme.typography.titleLarge)}
            val assigned=orderedTasks(state.tasks.filter {it.assigneeId==state.session?.userId && taskIsApproved(it) && !it.deleted})
            if(assigned.isEmpty())item {Text("暂无派发任务")}
            assigned.forEach {task->item(key=task.id) {TaskSummary(task,!state.saving) {model.openTask(task.id)}}}
        }
    } else if(panel!=null) {
        PriorityScreen(state,model,panel=="公司十大事",initialSelectedId=if(panel=="公司十大事")selectedCompanyId else null,companyExamples=companyExamples) {selectedPanel=null}
    } else {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fontScale=androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
        val tileHeight=300.dp*fontScale
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=4.dp,bottom=12.dp)) {
            state.workspaceError?.let {item {Hint(it,true)}}
            item {Row(Modifier.fillMaxWidth().height(tileHeight),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OverviewTile("事件纪","组织重点",company.size,Modifier.weight(1f),onOpen={selectedCompanyId=null;selectedPanel="公司十大事"}) {
                    if(companyExamples)Text("示例预览",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                    if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
                    if(state.panelError!=null)Text("读取失败，请刷新",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                    if(company.isEmpty()) {
                        OverviewEmpty("尚未发布重点事项")
                        if(BuildConfig.DEBUG && !state.loading)ControlTextButton(onClick={showCompanyExamples=true},contentPadding=PaddingValues(0.dp)) {Text("查看示例",style=MaterialTheme.typography.labelSmall)}
                    }
                    company.take(2).forEachIndexed {index,entry->CompanyDepthCard(entry,index,compact=true,labels=tags("company",entry.id),onLabels=if(state.session?.role=="FOUNDER" && !companyExamples)({pick("company",entry.id)}) else null) {selectedCompanyId=entry.id;selectedPanel="公司十大事"}}
                }
                OverviewTile("任务看板","工作安排",dispatch.size,Modifier.weight(1f),onOpen={selectedPanel="任务看板"}) {
                    if(dispatch.isEmpty())OverviewEmpty(if(state.taskLoading)"正在读取任务…" else "暂无派发任务")
                    dispatch.take(2).forEachIndexed {index,entry->
                        val task=state.tasks.firstOrNull {it.id==entry.id}
                        CompanyDepthCard(entry,index,compact=true,labels=tags("task",entry.id,task?.label),onLabels=if(task!=null && (task.createdBy==state.session?.userId || task.assigneeId==state.session?.userId || state.session?.role=="FOUNDER"))({pick("task",entry.id,task?.label)}) else null) {model.openTask(entry.id)}
                    }
                }
            }}
            item {DailyTodos(state)}
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
