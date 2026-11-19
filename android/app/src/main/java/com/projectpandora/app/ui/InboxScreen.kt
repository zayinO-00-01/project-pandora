package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun InboxScreen(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    val canReview=AppRoles.canReview(me.role)
    var tab by rememberSaveable(me.userId,me.role) {mutableStateOf(if(canReview)"reviews" else "messages")}
    val reviewing=canReview && tab=="reviews"
    val colors=MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),
        contentPadding=PaddingValues(top=4.dp,bottom=28.dp)) {
        item {BackdropTextButton(onClick={model.screen("day")}) {Text("← 返回公司")}}
        if(canReview)item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("reviews" to "任务审核","messages" to "通知 · "+state.unreadCount).forEachIndexed {index,(key,label)->
                    SegmentedButton(selected=tab==key,onClick={tab=key},shape=SegmentedButtonDefaults.itemShape(index,2)) {Text(label)}
                }
            }
        }
        state.workspaceError?.let {item {Hint(it,true,onRetry=model::loadWorkspaceExtras)}}
        if(reviewing) {
            item {
                ControlOutlinedButton(onClick={model.screen("organization")},modifier=Modifier.fillMaxWidth()) {
                    Text("工作信息授权申请",Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,null)
                }
            }
            state.taskReviewError?.let {item {Hint(it,true,onRetry=model::refresh)}}
            val initial=state.pendingTaskApprovals
            val changes=state.reviews
            val records=state.taskReviewRecords
            if(initial.isEmpty() && changes.isEmpty() && records.isEmpty() && state.taskReviewError==null)item {
                InboxCard {Text("暂无任务审核记录",style=MaterialTheme.typography.bodyMedium,color=colors.onSurfaceVariant)}
            }
            if(initial.isNotEmpty()) {
                item {InboxHeading("创建申请",initial.size)}
                items(initial,key={"initial-"+it.id}) {task->ReviewTaskCard(task,!state.saving) {model.openTask(task.id)}}
            }
            if(changes.isNotEmpty()) {
                item {InboxHeading("任务变更",changes.size)}
                items(changes,key={"lifecycle-"+it.id}) {task->ReviewTaskCard(task,!state.saving) {model.openTask(task.id)}}
            }
            if(records.isNotEmpty())item {InboxHeading("进度、改期与关键词",records.size)}
            items(records.sortedBy {it.status!="pending"},key={"review-"+it.id}) {review->InboxCard {
                Text(review.taskTitle,style=MaterialTheme.typography.titleMedium)
                Text(reviewOperation(review.operation)+" · "+reviewStatus(review.status),style=MaterialTheme.typography.labelMedium,color=colors.secondary)
                Text("申请人 "+review.applicantName+" · 审核人 "+review.reviewerName,style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                Text(timestamp(review.createdAt),style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
                val details=remember(review.payload) {reviewDetails(review.payload)}
                if(details.isNotBlank())Text(details,style=MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color=colors.outlineVariant)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ControlOutlinedButton(onClick={model.openTask(review.taskId)},enabled=!state.saving) {Text("任务详情")}
                    if(review.applicantId==me.userId && review.status=="pending")ControlOutlinedButton(onClick={model.decideTask(review.id,"canceled")},enabled=!state.saving) {Text("撤回")}
                    if(review.reviewerId==me.userId && review.status=="pending") {
                        ControlOutlinedButton(onClick={model.decideTask(review.id,"rejected")},enabled=!state.saving) {Text("拒绝")}
                        ControlButton(onClick={model.decideTask(review.id,"approved")},enabled=!state.saving) {Text("批准")}
                    }
                }
            }}
        } else {
            item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("消息列表",style=MaterialTheme.typography.titleMedium)
                        Text("任务分配、变更和审批",style=MaterialTheme.typography.bodySmall,color=colors.onSurfaceVariant)
                    }
                    ControlOutlinedButton(onClick=model::markAllNotificationsRead,enabled=!state.saving && !state.notificationReadBusy && state.unreadCount>0) {Text("全部已读")}
                }
            }
            if(state.notificationsLoading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            if(state.notifications.isEmpty() && !state.notificationsLoading)item {
                InboxCard {Text("暂无通知",style=MaterialTheme.typography.bodyMedium,color=colors.onSurfaceVariant)}
            }
            items(state.notifications,key={"notice-"+it.id}) {message->InboxCard {
                if(!message.read)Text("● 未读",style=MaterialTheme.typography.labelMedium,color=colors.secondary)
                Text(message.title,style=MaterialTheme.typography.titleMedium)
                Text(message.body,style=MaterialTheme.typography.bodyMedium,color=colors.onSurfaceVariant)
                Text(timestamp(message.createdAt),style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
                HorizontalDivider(color=colors.outlineVariant)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(!message.read)ControlOutlinedButton(onClick={model.readNotification(message)},enabled=!state.saving && !state.notificationReadBusy) {Text("标为已读")}
                    ControlOutlinedButton(onClick={model.openNotification(message)},enabled=!state.saving) {Text("查看详情")}
                }
            }}
        }
    }
}

@Composable private fun InboxHeading(title: String,count: Int) {
    Row(Modifier.fillMaxWidth().padding(top=6.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
        Text(title,style=MaterialTheme.typography.titleMedium)
        Text(count.toString()+" 项",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun InboxCard(content: @Composable ColumnScope.()->Unit) {
    OutlinedCard(Modifier.fillMaxWidth(),colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surface),
        border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
    }
}

@Composable private fun ReviewTaskCard(task: DispatchTask,enabled: Boolean,onOpen: ()->Unit) {
    InboxCard {
        Text(task.title,style=MaterialTheme.typography.titleMedium)
        Text(task.assigneeName+" · "+(taskReviewLabel(task.reviewStatus) ?: taskFeedbackLabel(task)),
            style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        ControlOutlinedButton(onClick=onOpen,enabled=enabled) {Text("任务详情")}
    }
}

internal fun reviewDetails(payload: String): String=runCatching {
    val data=com.google.gson.JsonParser.parseString(payload).asJsonObject
    listOf("title" to "事项","detail" to "说明","priority" to "优先级","assigneeName" to "责任人","startAt" to "开始时间","dueAt" to "截止时间","status" to "进度状态","progressNote" to "进度说明","progress" to "百分比","labelId" to "品牌标签编号","labelName" to "品牌标签","labelColor" to "背景颜色","keywordBefore" to "原关键词","keywordText" to "新关键词").mapNotNull {(key,title)->
        data[key]?.takeUnless {it.isJsonNull}?.asString?.takeIf {it.isNotBlank()}?.let {value->"$title：${when(key){"priority"->taskPriorityLabel(value);"status"->taskStatusLabel(value);"dueAt","startAt"->timestamp(value);else->value}}"}
    }.joinToString("\n")
}.getOrDefault("无法读取变更说明，请刷新后重试。")
