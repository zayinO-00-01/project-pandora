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
    if(selected==null) {
        RelatedTaskList(state,model)
        return
    }
    LazyColumn(Modifier.fillMaxSize().imePadding().padding(horizontal=20.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)) {
        item {Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            BackdropTextButton(onClick=model::closeTask,enabled=!state.saving) {Text("← 返回任务列表")}
            ScreenHeading("任务详情","查看责任、时间节点与最新进度。")
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
        if(task==null) {
            if(!state.taskLoading && state.taskError==null)item {EmptyCard("暂时无法查看此任务","任务可能已不在你的可见范围内，请返回列表或刷新。")}
            if(buffer?.taskId==selected)item {Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Hint("未提交反馈仍在本机保留。刷新确认任务后即可继续填写。")
                Text("状态：${taskStatusLabel(buffer.status ?: "todo")}\n说明：${buffer.note}",style=MaterialTheme.typography.bodyMedium)
                ControlTextButton(onClick={discard=true},enabled=!state.saving) {Text("放弃未提交反馈")}
            }}
        } else {
            val pendingRecords=state.taskReviewRecords.filter {it.taskId==task.id && it.status=="pending"}
            val pendingReview=taskHasPendingReview(task,state.taskReviewRecords)
            item {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("任务状态：${if(taskIsApproved(task))"已生效" else reviewStatus(task.approvalStatus)}",color=MaterialTheme.colorScheme.secondary)
                state.taskReviewRecords.filter {it.taskId==task.id}.take(3).forEach {review->Text("${reviewOperation(review.operation)} · ${reviewStatus(review.status)} · 审核人 ${review.reviewerName}",style=MaterialTheme.typography.bodySmall)}
                state.workspaceError?.let {Hint(it,true)}
            }}
            item {OutlinedCard(Modifier.fillMaxWidth(),colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surface,contentColor=MaterialTheme.colorScheme.onSurface)) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(task.title,style=MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {TaskPriorityText(task.priority,onLabel=false);Text(taskFeedbackLabel(task),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                Text(task.detail.ifBlank {"暂无任务说明"},style=MaterialTheme.typography.bodyLarge)
                HorizontalDivider()
                Text("创建人：${task.creatorName}\n责任人：${task.assigneeName}\n开始时间：${task.startAt?.let {dueLabel(it)} ?: "未指定"}\n截止时间：${dueLabel(task.dueAt)}",style=MaterialTheme.typography.bodyMedium)
                LabelLines(contentTags(state,"task",task.id,task.label))
            }}}
            if(task.createdBy==mine || task.assigneeId==mine || isFounder)item {
                var chosen by remember(task.id,state.contentLabels) {mutableStateOf(contentTags(state,"task",task.id,task.label).map {it.id})}
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    MultiLabelPicker(state.labels,chosen,!state.saving) {chosen=it}
                    ControlTextButton(onClick={model.saveContentLabels("task",task.id,chosen)},enabled=!state.saving && chosen!=contentTags(state,"task",task.id,task.label).map {it.id}) {Text("保存任务标签")}
                    state.workspaceError?.let {Hint(it,true)}
                }
            }
            item {TaskKeywords(task,state,model)}
            if(!taskIsApproved(task))item {Hint(if(task.approvalStatus=="pending")"等待创建人当前直属上级审核，批准后才能开始反馈进度。" else "此任务未获批准，请根据审核意见重新安排。")}
            if(!task.approvalNote.isNullOrBlank())item {Hint("审核意见：${task.approvalNote}")}
            if(task.createdBy==mine && (task.approvalStatus=="pending" || task.reviewStatus=="pending_change"))item {
                ControlOutlinedButton(onClick={model.withdrawTask(task.id)},enabled=!state.saving) {Text("撤回申请")}
            }
            pendingRecords.forEach {record->item(key="pending-${record.id}") {
                OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("${reviewOperation(record.operation)} · 待审核",style=MaterialTheme.typography.titleMedium)
                    Text("申请人 ${record.applicantName} · 当前审核人 ${record.reviewerName}",style=MaterialTheme.typography.bodySmall)
                    Text(remember(record.payload) {reviewDetails(record.payload)},style=MaterialTheme.typography.bodyMedium)
                    Text("批准前保留当前已生效内容。",style=MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        if(record.applicantId==mine)ControlTextButton(onClick={model.decideTask(record.id,"canceled")},enabled=!state.saving) {Text("撤回申请")}
                        if(record.reviewerId==mine) {
                            ControlButton(onClick={model.decideTask(record.id,"approved")},enabled=!state.saving) {Text("批准")}
                            ControlOutlinedButton(onClick={model.decideTask(record.id,"rejected")},enabled=!state.saving) {Text("拒绝")}
                        }
                    }
                }}
            }}
            if(state.pendingTaskApprovals.any {it.id==task.id})item {
                OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("审核创建申请",style=MaterialTheme.typography.titleMedium)
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
                task.pendingChange?.takeIf {it.isNotBlank()}?.let {payload->
                    Text("拟变更内容",style=MaterialTheme.typography.labelLarge)
                    Text(remember(payload) {reviewDetails(payload)},style=MaterialTheme.typography.bodyMedium)
                }
                task.reviewReason?.takeIf {it.isNotBlank()}?.let {Text("理由：$it",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                if(task.id in reviewIds) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ControlButton(onClick={model.approveTask(task)},enabled=!state.saving) {Text("通过")}
                    ControlTextButton(onClick={rejectTarget=task;rejectReason=""},enabled=!state.saving) {Text("驳回")}
                } else if(isPendingReview(task.reviewStatus))Text("等待相关人审核。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }}}
            if(!task.deleted && (task.createdBy==mine || isFounder))item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if((task.createdBy==mine || isFounder) && task.approvalStatus!="pending" && !pendingReview)ControlTextButton(onClick={model.editTaskDetails(task)},enabled=!state.saving) {Text("修改任务")}
                ControlTextButton(onClick={deleteTarget=task},enabled=!state.saving && (!pendingReview || isFounder)) {Text("删除任务")}
            }}
            if(taskIsApproved(task) && !task.deleted) {
            item {OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("最新进度说明",style=MaterialTheme.typography.titleMedium)
                Text(task.progressNote.ifBlank {"尚未反馈进度"},style=MaterialTheme.typography.bodyLarge)
            }}}
            if(pendingReview)item {Hint("此任务已有待审申请，处理或撤回后可再次反馈进度。") }
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
                    ControlButton(onClick=model::saveTaskProgress,enabled=!state.saving && !state.taskLoading && state.taskReviewRecordsReady,modifier=Modifier.weight(1f)) {Text(if(state.saving)"提交中…" else "提交进度")}
                }}
                item {Text(if(isFounder)"返回或切换页面会保留输入；本人提交进度直接生效。" else "返回或切换页面会保留输入；进度提交当前直属上级批准后生效。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            } else if(buffer==null)item {ControlButton(onClick={model.editTask(task)},enabled=!state.saving && !state.taskLoading && state.taskReviewRecordsReady,modifier=Modifier.fillMaxWidth()) {Text("反馈工作进度")}}
            }
        }
    }
}
internal fun dueLabel(value: String?): String = if(value==null)"未设置" else runCatching {
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")).format(Instant.parse(value))
}.getOrDefault(value)

@Composable private fun DispatchTaskForm(state: AppState,model: PandoraViewModel) {
    val draft=state.dispatchEditor ?: return
    val enabled=!state.saving
    var assigneesExpanded by remember {mutableStateOf(false)}
    var discard by remember {mutableStateOf(false)}
    val selected=state.dispatchMembers.firstOrNull {it.id==draft.assigneeId}
    val selectedIds=TaskDispatchValidation.assigneeIds(draft)
    if(assigneesExpanded)AssigneeDirectoryDialog(state,selectedIds,model::toggleDispatchAssignee,model::loadDispatchMembers) {assigneesExpanded=false}
    val needsApproval=state.session?.role!="FOUNDER"
    if(discard)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={discard=false},title={Text("放弃未提交的任务？")},text={Text("本机保存的任务输入将被清除。")},
        confirmButton={ControlTextButton(onClick={discard=false;model.discardDispatch()}) {Text("放弃")}},dismissButton={ControlTextButton(onClick={discard=false}) {Text("继续填写")}})
    LazyColumn(Modifier.fillMaxSize().imePadding().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=28.dp)) {
        item {BackdropTextButton(onClick=model::closeDispatch,enabled=enabled) {Text("返回任务工作台 · 保留输入")}}
        item {ScreenHeading(if(draft.taskId==null)"新建任务" else "编辑任务","")}
        item {OutlinedTextField(value=draft.title,onValueChange={model.changeDispatch(title=it.take(128))},enabled=enabled,label={Text("事项名称（必填）")},singleLine=true,supportingText={Text("${draft.title.length} / 128")},modifier=Modifier.fillMaxWidth())}
        item {Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            state.workspaceError?.let {Hint("标签或审核连接异常：$it",true)}
            Text("责任人（必选，可多选）",style=MaterialTheme.typography.labelLarge)
            if(state.dispatchLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
            ControlOutlinedButton(onClick={assigneesExpanded=true},enabled=enabled && draft.taskId==null && !state.dispatchLoading,modifier=Modifier.fillMaxWidth()) {
                Text(if(selectedIds.isEmpty())"从通讯录选择责任人" else "已选 ${selectedIds.size} 人 · "+state.dispatchMembers.filter {it.id in selectedIds}.joinToString("、") {it.displayName ?: it.username},maxLines=2,overflow=TextOverflow.Ellipsis)
            }
            if(!state.dispatchLoading && state.dispatchMembers.isEmpty())Text("人员列表尚未加载，请刷新或检查网络。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            BackdropTextButton(onClick=model::loadDispatchMembers,enabled=enabled && !state.dispatchLoading) {Text("刷新通讯录")}
        }}
        item {Column {Text("优先级",style=MaterialTheme.typography.labelLarge);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("low" to "低","normal" to "普通","high" to "高").forEach {(value,label)->FilterChip(selected=draft.priority==value,onClick={model.changeDispatch(priority=value)},enabled=enabled,label={Text(label,color=taskPriorityColor(value))})}
        }}}
        state.labelError?.let {item {Hint(it,true,onRetry=model::loadWorkspaceExtras)}}
        item {MultiLabelPicker(state.labels,draft.labelIds ?: listOfNotNull(draft.labelId?.takeIf {it>0}),enabled,model::changeDispatchLabels)}
        item {DeadlinePicker(draft.startDate.orEmpty(),enabled,label="开始时间",required=false,minDate=if(draft.taskId==null)today().minusDays(2) else null) {model.changeDispatch(startDate=it)}}
        item {DeadlinePicker(draft.dueDate,enabled,minDate=if(draft.taskId==null)today().minusDays(2) else null) {model.changeDispatch(dueDate=it)}}
        item {OutlinedTextField(value=draft.detail,onValueChange={model.changeDispatch(detail=it.take(5000))},enabled=enabled,label={Text("详细说明")},minLines=5,supportingText={Text("${draft.detail.length} / 5000")},modifier=Modifier.fillMaxWidth())}
        state.dispatchError?.let {item {Hint(it,true)}}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ControlTextButton(onClick={discard=true},enabled=enabled) {Text("放弃填写")}
            ControlButton(onClick=model::createTask,enabled=enabled && !state.dispatchLoading && selected!=null,modifier=Modifier.weight(1f)) {Text(if(state.saving)"提交中…" else if(draft.taskId!=null)"提交修改" else if(needsApproval)"提交审核" else "创建任务")}
        }}
    }
}
