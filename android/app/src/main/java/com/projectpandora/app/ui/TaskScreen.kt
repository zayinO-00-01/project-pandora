package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    var deleteConfirm by remember {mutableStateOf(false)}
    if(deleteConfirm && task!=null)AlertDialog(onDismissRequest={deleteConfirm=false},title={Text("确认删除任务？")},text={Text("创始人删除直接生效，此任务将从工作列表和日历中移除。")},confirmButton={ControlTextButton(onClick={deleteConfirm=false;model.deleteTask(task.id)}) {Text("确认删除")}},dismissButton={ControlTextButton(onClick={deleteConfirm=false}) {Text("取消")}})
    var discard by remember {mutableStateOf(false)}
    if(state.dispatchOpen) {
        BackHandler(enabled=!state.saving) {model.closeDispatch()}
        DispatchTaskForm(state,model)
        return
    }
    BackHandler(enabled=selected!=null) {if(!state.saving)model.closeTask()}
    if(discard)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={discard=false},title={Text("放弃未提交的反馈？")},
        text={Text("此次输入会被清除，已保存的任务进度不会改变。")},
        confirmButton={ControlTextButton(onClick={discard=false;model.discardTaskEditor()}) {Text("放弃反馈")}},
        dismissButton={ControlTextButton(onClick={discard=false}) {Text("继续保留")}})
    LazyColumn(Modifier.fillMaxSize().imePadding().padding(horizontal=20.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)) {
        item {Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(selected==null)ControlTextButton(onClick={model.screen("day")},enabled=!state.saving,contentPadding=PaddingValues(0.dp)) {Text("← 返回视图")}
            if(selected!=null)ControlTextButton(onClick=model::closeTask,enabled=!state.saving,contentPadding=PaddingValues(0.dp)) {Text("← 返回任务列表")}
            ScreenHeading(if(selected==null)"任务工作台" else "任务详情",if(selected==null)"接收派发，持续反馈工作进展。" else "查看责任、时间节点与最新进度。")
            if(selected==null)Text("查看任务，及时记录进展与需要协调的问题。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(state.taskLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
            state.taskError?.let {Hint("任务读取失败：$it\n请刷新；已显示的任务可能是上次读取的内容。",true)}
        }}
        if(buffer!=null && buffer.taskId!=selected)item {Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Hint("另一个任务有未提交的反馈，离开页面会保留输入。")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ControlTextButton(onClick={model.openTask(buffer.taskId)},enabled=!state.saving) {Text("继续填写")}
                ControlTextButton(onClick={discard=true},enabled=!state.saving) {Text("放弃反馈")}
            }
        }}
        if(selected==null) {
            if(AppRoles.canDispatch(state.session?.role ?: ""))item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ControlOutlinedButton(onClick=model::openDispatch,enabled=!state.saving,modifier=Modifier.weight(1f).heightIn(min=48.dp)) {Text(if(state.dispatchEditor==null)"创建任务" else "继续派发")}
                }
            }
            if(state.tasks.isEmpty() && !state.taskLoading && state.taskError==null)item {
                EmptyCard("暂无派发任务","领导派发后，刷新这里即可查看。")
            }
            items(orderedTasks(state.tasks),key={it.id}) {row->OutlinedCard(colors=CardDefaults.outlinedCardColors(containerColor=if(row.label!=null)labelColor(row.label) else MaterialTheme.colorScheme.surface,contentColor=if(row.label!=null)labelTextColor(row.label) else MaterialTheme.colorScheme.onSurface),onClick={model.openTask(row.id)},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    LabelBadge(row.label)
                    KeywordChips(row.keywords.orEmpty())
                    Text(row.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {TaskPriorityText(row.priority,onLabel=row.label!=null);Text(taskFeedbackLabel(row),style=MaterialTheme.typography.labelMedium,color=if(row.label!=null)labelTextColor(row.label) else MaterialTheme.colorScheme.onSurfaceVariant)}
                    Text("责任人：${row.assigneeName} · 截止 ${dueLabel(row.dueAt)}",style=MaterialTheme.typography.bodySmall,color=if(row.label!=null)labelTextColor(row.label) else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("查看详情 →",style=MaterialTheme.typography.labelSmall,color=if(row.label!=null)labelTextColor(row.label) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }}
        } else if(task==null) {
            if(!state.taskLoading && state.taskError==null)item {EmptyCard("暂时无法查看此任务","任务可能已不在你的可见范围内，请返回列表或刷新。")}
            if(buffer?.taskId==selected)item {Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Hint("未提交反馈仍在本机保留。刷新确认任务后即可继续填写。")
                Text("状态：${taskStatusLabel(buffer.status ?: "todo")}\n说明：${buffer.note}",style=MaterialTheme.typography.bodyMedium)
                ControlTextButton(onClick={discard=true},enabled=!state.saving) {Text("放弃未提交反馈")}
            }}
        } else {
            item {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("任务状态：${if(task.approvalStatus=="approved")"已生效" else reviewStatus(task.approvalStatus)}",color=MaterialTheme.colorScheme.secondary)
                state.reviews.filter {it.taskId==task.id}.take(3).forEach {review->Text("${reviewOperation(review.operation)} · ${reviewStatus(review.status)} · 审核人 ${review.reviewerName}",style=MaterialTheme.typography.bodySmall)}
                state.workspaceError?.let {Hint(it,true)}
                if(task.createdBy==state.session?.userId || state.session?.role=="FOUNDER")ControlOutlinedButton(onClick={model.editTaskDetails(task)},enabled=!state.saving && task.approvalStatus!="pending") {Text(if(state.session?.role=="FOUNDER")"编辑任务" else "编辑任务 · 提交审核")}
                if(state.session?.role=="FOUNDER")ControlTextButton(onClick={deleteConfirm=true},enabled=!state.saving) {Text("删除任务",color=MaterialTheme.colorScheme.error)}
            }}
            item {OutlinedCard(Modifier.fillMaxWidth(),colors=CardDefaults.outlinedCardColors(containerColor=if(task.label!=null)labelColor(task.label) else MaterialTheme.colorScheme.surface,contentColor=if(task.label!=null)labelTextColor(task.label) else MaterialTheme.colorScheme.onSurface)) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                LabelBadge(task.label)
                Text(task.title,style=MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {TaskPriorityText(task.priority,onLabel=task.label!=null);Text(taskFeedbackLabel(task),style=MaterialTheme.typography.labelMedium,color=if(task.label!=null)labelTextColor(task.label) else MaterialTheme.colorScheme.onSurfaceVariant)}
                Text(task.detail.ifBlank {"暂无任务说明"},style=MaterialTheme.typography.bodyLarge)
                HorizontalDivider()
                Text("派发人：${task.creatorName}\n责任人：${task.assigneeName}\n截止时间：${dueLabel(task.dueAt)}",style=MaterialTheme.typography.bodyMedium)
            }}}
            item {TaskKeywords(task,state,model)}
            item {OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("最新进度说明",style=MaterialTheme.typography.titleMedium)
                Text(task.progressNote.ifBlank {"尚未反馈进度"},style=MaterialTheme.typography.bodyLarge)
            }}}
            if(task.assigneeId!=state.session?.userId)item {Hint("仅当前责任人可反馈进度，你可以查看任务详情。")}
            else if(buffer?.taskId==selected) {
                item {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("工作状态",style=MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("todo","doing","done").forEach {status->FilterChip(
                            selected=buffer.status==status,onClick={model.changeTaskEditor(status=status)},enabled=!state.saving,
                            label={Text(taskStatusLabel(status))},modifier=Modifier.weight(1f).heightIn(min=48.dp))}
                    }
                }}
                item {OutlinedTextField(value=buffer.note,onValueChange={if(it.length<=2000)model.changeTaskEditor(note=it)},enabled=!state.saving,
                    label={Text("进度说明（必填）")},placeholder={Text("完成了什么？有哪些问题？下一步是什么？")},minLines=5,
                    supportingText={Text("${buffer.note.length} / 2000")},modifier=Modifier.fillMaxWidth())}
                state.taskEditorError?.let {item {Hint(it,true)}}
                item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ControlTextButton(onClick={discard=true},enabled=!state.saving) {Text("放弃反馈")}
                    ControlButton(onClick=model::saveTaskProgress,enabled=!state.saving,modifier=Modifier.weight(1f)) {Text(if(state.saving)"提交中…" else "提交进度")}
                }}
                item {Text("返回或切换页面会保留输入；提交成功后同步到领导端。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            } else if(buffer==null && task.approvalStatus=="approved")item {ControlButton(onClick={model.editTask(task)},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {Text("反馈工作进度")}}
        }
    }
}
private fun dueLabel(value: String?): String = if(value==null)"未设置" else runCatching {
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")).format(Instant.parse(value))
}.getOrDefault(value)

@Composable private fun DispatchTaskForm(state: AppState,model: PandoraViewModel) {
    val draft=state.dispatchEditor ?: return
    val enabled=!state.saving
    var assigneesExpanded by remember {mutableStateOf(false)}
    var memberQuery by remember {mutableStateOf("")}
    var discard by remember {mutableStateOf(false)}
    val selected=state.dispatchMembers.firstOrNull {it.id==draft.assigneeId}
    if(discard)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={discard=false},title={Text("放弃未派发的任务？")},text={Text("本机保存的派发输入将被清除。")},
        confirmButton={ControlTextButton(onClick={discard=false;model.discardDispatch()}) {Text("放弃")}},dismissButton={ControlTextButton(onClick={discard=false}) {Text("继续填写")}})
    LazyColumn(Modifier.fillMaxSize().imePadding().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)) {
        item {ControlTextButton(onClick=model::closeDispatch,enabled=enabled) {Text("返回任务工作台 · 保留输入")}}
        item {ScreenHeading(if(draft.taskId==null)"创建任务" else "编辑任务","提交后由直属上级审核；创始人的操作直接生效。")}
        item {OutlinedTextField(value=draft.title,onValueChange={model.changeDispatch(title=it.take(128))},enabled=enabled,label={Text("事项名称（必填）")},singleLine=true,supportingText={Text("${draft.title.length} / 128")},modifier=Modifier.fillMaxWidth())}
        item {Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            state.workspaceError?.let {Hint("标签或审核连接异常：$it",true)}
            Text("责任人（必选）",style=MaterialTheme.typography.labelLarge)
            if(state.dispatchLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedTextField(value=memberQuery,onValueChange={memberQuery=it},label={Text("按姓名查找责任人")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Box {
                ControlOutlinedButton(onClick={assigneesExpanded=true},enabled=enabled && !state.dispatchLoading && state.dispatchMembers.isNotEmpty(),modifier=Modifier.fillMaxWidth()) {
                    Text(selected?.let {"${it.displayName ?: it.username} · ${AppRoles.label(it.role)}"} ?: if(draft.assigneeId!=null)"原责任人不在当前范围，请重新选择" else "选择公司责任人")
                }
                DropdownMenu(expanded=assigneesExpanded,onDismissRequest={assigneesExpanded=false},modifier=Modifier.heightIn(max=320.dp)) {
                    filterMembers(state.dispatchMembers,memberQuery).forEach {member->DropdownMenuItem(text={Text("${member.displayName ?: member.username} · ${AppRoles.label(member.role)}")},onClick={model.changeDispatch(assigneeId=member.id);assigneesExpanded=false})}
                }
            }
            if(!state.dispatchLoading && state.dispatchMembers.isEmpty())Text("暂无有效公司成员，请刷新人员列表。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            ControlTextButton(onClick=model::loadDispatchMembers,enabled=enabled && !state.dispatchLoading) {Text("刷新人员列表")}
        }}
        item {Column {Text("优先级",style=MaterialTheme.typography.labelLarge);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("low" to "低","normal" to "普通","high" to "高").forEach {(value,label)->FilterChip(selected=draft.priority==value,onClick={model.changeDispatch(priority=value)},enabled=enabled,label={Text(label,color=taskPriorityColor(value))})}
        }}}
        state.labelError?.let {item {Hint(it,true,onRetry=model::loadWorkspaceExtras)}}
        item {LabelPicker(state.labels,draft.labelId,enabled) {model.changeDispatch(labelId=it)}}
        item {DeadlinePicker(draft.dueDate,enabled) {model.changeDispatch(dueDate=it)}}
        item {OutlinedTextField(value=draft.detail,onValueChange={model.changeDispatch(detail=it.take(5000))},enabled=enabled,label={Text("详细说明")},minLines=5,supportingText={Text("${draft.detail.length} / 5000")},modifier=Modifier.fillMaxWidth())}
        state.dispatchError?.let {item {Hint(it,true)}}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ControlTextButton(onClick={discard=true},enabled=enabled) {Text("放弃派发")}
            ControlButton(onClick=model::createTask,enabled=enabled && !state.dispatchLoading && selected!=null,modifier=Modifier.weight(1f)) {Text(if(state.saving)"派发中…" else "提交任务")}
        }}
        item {Text("暂时离开会保留输入，再次登录原账号可继续填写。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}
