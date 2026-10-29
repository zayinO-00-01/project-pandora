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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable fun TaskScreen(state: AppState, model: PandoraViewModel) {
    val selected=state.selectedTaskId
    val task=state.tasks.firstOrNull {it.id==selected} ?: state.pendingTaskApprovals.firstOrNull {it.id==selected} ?: state.reviews.firstOrNull {it.id==selected}
    var reviewNote by rememberSaveable(selected) {mutableStateOf("")}
    val buffer=state.taskEditor
    var discard by remember {mutableStateOf(false)}
    var rejectTarget by remember {mutableStateOf<DispatchTask?>(null)}
    var rejectReason by remember {mutableStateOf("")}
    var deleteTarget by remember {mutableStateOf<DispatchTask?>(null)}
    var changeTarget by remember {mutableStateOf<DispatchTask?>(null)}
    var changeTitle by remember {mutableStateOf("")}
    var changeDetail by remember {mutableStateOf("")}
    var changePriority by remember {mutableStateOf("normal")}
    val mine=state.session?.userId
    val isFounder=state.session?.role=="FOUNDER"
    val reviewIds=state.reviews.map {it.id}.toSet()
    if(rejectTarget!=null) {
        val target=rejectTarget!!
        AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={if(!state.saving){rejectTarget=null;rejectReason=""}},title={Text("驳回审核")},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("驳回「${target.title}」",style=MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value=rejectReason,onValueChange={rejectReason=it.take(500)},enabled=!state.saving,label={Text("驳回理由（必填）")},minLines=3,modifier=Modifier.fillMaxWidth())
            }},
            confirmButton={ControlTextButton(onClick={model.rejectTask(target,rejectReason);rejectTarget=null;rejectReason=""},enabled=!state.saving && rejectReason.isNotBlank()) {Text("确认驳回")}},
            dismissButton={ControlTextButton(onClick={rejectTarget=null;rejectReason=""},enabled=!state.saving) {Text("取消")}})
    }
    deleteTarget?.let {target->
        AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={if(!state.saving)deleteTarget=null},title={Text("删除任务")},
            text={Text(if(isFounder)"作为创始人，将直接删除「${target.title}」并留下审计记录。" else "将申请删除「${target.title}」。已开始或已完成的任务需上级审核，未开始的任务直接删除。")},
            confirmButton={ControlTextButton(onClick={model.deleteTask(target);deleteTarget=null},enabled=!state.saving) {Text("确认删除")}},
            dismissButton={ControlTextButton(onClick={deleteTarget=null},enabled=!state.saving) {Text("取消")}})
    }
    changeTarget?.let {target->
        AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={if(!state.saving)changeTarget=null},title={Text("修改任务")},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value=changeTitle,onValueChange={changeTitle=it.take(128)},enabled=!state.saving,label={Text("事项名称")},singleLine=true,modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("low" to "低","normal" to "普通","high" to "高").forEach {(value,label)->FilterChip(selected=changePriority==value,onClick={changePriority=value},enabled=!state.saving,label={Text(label,color=taskPriorityColor(value))})}}
                OutlinedTextField(value=changeDetail,onValueChange={changeDetail=it.take(5000)},enabled=!state.saving,label={Text("详细说明")},minLines=3,modifier=Modifier.fillMaxWidth())
                Text(if(isFounder)"创始人修改将直接生效。" else "修改后需上级审核，审核期间旧内容继续生效。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }},
            confirmButton={ControlTextButton(onClick={model.changeTask(target,changeTitle,changeDetail,changePriority,target.dueAt);changeTarget=null},enabled=!state.saving && changeTitle.isNotBlank()) {Text("提交修改")}},
            dismissButton={ControlTextButton(onClick={changeTarget=null},enabled=!state.saving) {Text("取消")}})
    }
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
            if(selected==null)ControlTextButton(onClick={model.screen("functions")},enabled=!state.saving,contentPadding=PaddingValues(0.dp)) {Text("← 返回职能")}
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
            if(AppRoles.canCreateTask(state.session?.role ?: ""))item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ControlOutlinedButton(onClick=model::openDispatch,enabled=!state.saving,modifier=Modifier.weight(1f).heightIn(min=48.dp)) {Text(if(state.dispatchEditor==null)"新建任务" else "继续填写")}
                }
            }
            if(state.reviews.isNotEmpty())item {OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("待我审核 · ${state.reviews.size}",style=MaterialTheme.typography.titleMedium)
                state.reviews.forEach {row-> Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(row.title,style=MaterialTheme.typography.bodyLarge,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text("${taskReviewLabel(row.reviewStatus) ?: "待审"} · 责任人 ${row.assigneeName}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        ControlButton(onClick={model.approveTask(row)},enabled=!state.saving) {Text("通过")}
                        ControlTextButton(onClick={rejectTarget=row;rejectReason=""},enabled=!state.saving) {Text("驳回")}
                    }
                    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                }}
            }}}
            if(state.tasks.isEmpty() && !state.taskLoading && state.taskError==null)item {
                EmptyCard("暂无任务","可以给自己安排任务，也可接收领导派发的任务。")
            }
            items(orderedTasks(state.tasks),key={it.id}) {row->OutlinedCard(onClick={model.openTask(row.id)},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(row.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {TaskPriorityText(row.priority);Text(taskFeedbackLabel(row),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);taskReviewLabel(row.reviewStatus)?.let {Text(it,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.tertiary)}}
                    Text("责任人：${row.assigneeName} · 截止 ${dueLabel(row.dueAt)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("查看详情 →",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
            item {OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(task.title,style=MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {TaskPriorityText(task.priority);Text(taskFeedbackLabel(task),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);taskReviewLabel(task.reviewStatus)?.let {Text(it,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.tertiary)}}
                Text(task.detail.ifBlank {"暂无任务说明"},style=MaterialTheme.typography.bodyLarge)
                HorizontalDivider()
                Text("派发人：${task.creatorName}\n责任人：${task.assigneeName}\n截止时间：${dueLabel(task.dueAt)}",style=MaterialTheme.typography.bodyMedium)
            }}}
            if(!taskIsApproved(task))item {Hint(if(task.approvalStatus=="pending")"等待直属上级审核，批准后才能开始反馈进度。" else "此任务未获批准，请根据审核意见重新安排。")}
            if(!task.approvalNote.isNullOrBlank())item {Hint("审核意见：${task.approvalNote}")}
            if(state.pendingTaskApprovals.any {it.id==task.id})item {
                OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("审核自建任务",style=MaterialTheme.typography.titleMedium)
                    Text("确认工作目标与安排是否合适，批准后责任人即可反馈进度。",style=MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value=reviewNote,onValueChange={reviewNote=it.take(500)},enabled=!state.saving,label={Text("审核意见（选填）")},modifier=Modifier.fillMaxWidth())
                    state.taskReviewError?.let {Hint(it,true)}
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        ControlOutlinedButton(onClick={model.reviewTask(task.id,"rejected",reviewNote)},enabled=!state.saving,modifier=Modifier.weight(1f)) {Text("拒绝")}
                        ControlButton(onClick={model.reviewTask(task.id,"approved",reviewNote)},enabled=!state.saving,modifier=Modifier.weight(1f)) {Text(if(state.saving)"提交中…" else "批准")}
                    }
                }}
            }
            val reviewLabel=taskReviewLabel(task.reviewStatus)
            if(reviewLabel!=null)item {OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("审核状态：$reviewLabel",style=MaterialTheme.typography.titleMedium)
                task.reviewReason?.takeIf {it.isNotBlank()}?.let {Text("理由：$it",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                if(task.id in reviewIds) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ControlButton(onClick={model.approveTask(task)},enabled=!state.saving) {Text("通过")}
                    ControlTextButton(onClick={rejectTarget=task;rejectReason=""},enabled=!state.saving) {Text("驳回")}
                } else Text("等待相关人审核。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }}}
            if(task.createdBy==mine || isFounder)item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(!isPendingReview(task.reviewStatus) && task.status!="done")ControlTextButton(onClick={changeTarget=task;changeTitle=task.title;changeDetail=task.detail;changePriority=task.priority},enabled=!state.saving) {Text("修改任务")}
                ControlTextButton(onClick={deleteTarget=task},enabled=!state.saving) {Text("删除任务")}
            }}
            if(taskIsApproved(task) && !task.deleted) {
            item {TaskProgressGraph(task)}
            item {OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("最新进度说明",style=MaterialTheme.typography.titleMedium)
                Text(task.progressNote.ifBlank {"尚未反馈进度"},style=MaterialTheme.typography.bodyLarge)
            }}}
            if(isPendingReview(task.reviewStatus))item {Hint("任务审核中，处理完成后可继续反馈。") }
            else if(task.status=="done")item {Hint("此任务已完成验收。") }
            else if(task.assigneeId!=state.session?.userId)item {Hint("仅当前责任人可反馈进度，你可以查看任务详情。")}
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
            } else if(buffer==null)item {ControlButton(onClick={model.editTask(task)},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {Text("反馈工作进度")}}
            }
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
    var discard by remember {mutableStateOf(false)}
    val selected=state.dispatchMembers.firstOrNull {it.id==draft.assigneeId}
    var memberQuery by remember {mutableStateOf("")}
    val filteredMembers=filterMembers(state.dispatchMembers,memberQuery)
    val self=draft.assigneeId==state.session?.userId
    val needsApproval=self && state.session?.role!="FOUNDER"
    if(discard)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={discard=false},title={Text("放弃未提交的任务？")},text={Text("本机保存的任务输入将被清除。")},
        confirmButton={ControlTextButton(onClick={discard=false;model.discardDispatch()}) {Text("放弃")}},dismissButton={ControlTextButton(onClick={discard=false}) {Text("继续填写")}})
    LazyColumn(Modifier.fillMaxSize().imePadding().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)) {
        item {ControlTextButton(onClick=model::closeDispatch,enabled=enabled) {Text("返回任务工作台 · 保留输入")}}
        item {ScreenHeading("新建任务",if(needsApproval)"为自己安排工作，提交后由直属上级审核。" else if(self)"为自己安排工作，创建后直接生效。" else "为下级安排工作，创建后直接生效。")}
        item {OutlinedTextField(value=draft.title,onValueChange={model.changeDispatch(title=it.take(128))},enabled=enabled,label={Text("事项名称（必填）")},singleLine=true,supportingText={Text("${draft.title.length} / 128")},modifier=Modifier.fillMaxWidth())}
        item {Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("责任人（必选）",style=MaterialTheme.typography.labelLarge)
            if(state.dispatchLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedTextField(value=memberQuery,onValueChange={memberQuery=it},enabled=enabled,label={Text("搜索姓名或账号")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Box {
                ControlOutlinedButton(onClick={assigneesExpanded=true},enabled=enabled && !state.dispatchLoading && state.dispatchMembers.isNotEmpty(),modifier=Modifier.fillMaxWidth()) {
                    Text(selected?.let {if(it.id==state.session?.userId)"我自己 · ${it.displayName ?: it.username}" else "${it.displayName ?: it.username} · ${AppRoles.label(it.role)}"} ?: if(draft.assigneeId!=null)"原责任人不在当前范围，请重新选择" else "选择本人或下级责任人")
                }
                DropdownMenu(expanded=assigneesExpanded,onDismissRequest={assigneesExpanded=false},modifier=Modifier.heightIn(max=320.dp)) {
                    filteredMembers.forEach {member->DropdownMenuItem(text={Text(if(member.id==state.session?.userId)"我自己 · ${member.displayName ?: member.username}" else "${member.displayName ?: member.username} · ${AppRoles.label(member.role)}")},onClick={model.changeDispatch(assigneeId=member.id);assigneesExpanded=false})}
                }
            }
            if(!state.dispatchLoading && state.dispatchMembers.isEmpty())Text("人员列表尚未加载，请刷新或检查网络。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            ControlTextButton(onClick=model::loadDispatchMembers,enabled=enabled && !state.dispatchLoading) {Text("刷新人员列表")}
        }}
        item {Column {Text("优先级",style=MaterialTheme.typography.labelLarge);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("low" to "低","normal" to "普通","high" to "高").forEach {(value,label)->FilterChip(selected=draft.priority==value,onClick={model.changeDispatch(priority=value)},enabled=enabled,label={Text(label,color=taskPriorityColor(value))})}
        }}}
        item {DeadlinePicker(draft.dueDate,enabled) {model.changeDispatch(dueDate=it)}}
        item {OutlinedTextField(value=draft.detail,onValueChange={model.changeDispatch(detail=it.take(5000))},enabled=enabled,label={Text("详细说明")},minLines=5,supportingText={Text("${draft.detail.length} / 5000")},modifier=Modifier.fillMaxWidth())}
        state.dispatchError?.let {item {Hint(it,true)}}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ControlTextButton(onClick={discard=true},enabled=enabled) {Text("放弃填写")}
            ControlButton(onClick=model::createTask,enabled=enabled && !state.dispatchLoading && selected!=null,modifier=Modifier.weight(1f)) {Text(if(state.saving)"提交中…" else if(needsApproval)"提交审核" else if(self)"创建任务" else "确认派发")}
        }}
        item {Text("暂时离开会保留输入，再次登录原账号可继续填写。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}
