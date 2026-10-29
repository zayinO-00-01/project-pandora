package com.projectpandora.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun InboxScreen(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    var tab by remember {mutableStateOf("reviews")}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=28.dp)) {
        item {ControlTextButton(onClick={model.screen("day")}) {Text("← 返回视图")}}
        item {Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FilterChip(selected=tab=="reviews",onClick={tab="reviews"},label={Text("任务审核")})
            FilterChip(selected=tab=="messages",onClick={tab="messages"},label={Text("通知 · ${state.notifications.count {it.readAt==null}}")})
        }}
        state.workspaceError?.let {item {Hint(it,true,onRetry=model::loadWorkspaceExtras)}}
        if(tab=="reviews") {
            item {Text("待处理审核与我提交的变更",style=MaterialTheme.typography.titleMedium)}
            if(state.reviews.isEmpty())item {Text("暂无任务审核记录")}
            items(state.reviews.sortedBy {it.status!="pending"},key={"review-${it.id}"}) {review->OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(review.taskTitle,style=MaterialTheme.typography.titleMedium)
                Text("${reviewOperation(review.operation)} · ${reviewStatus(review.status)}",color=MaterialTheme.colorScheme.secondary)
                Text("申请人 ${review.applicantName} · 审核人 ${review.reviewerName}",style=MaterialTheme.typography.bodySmall)
                Text(timestamp(review.createdAt),style=MaterialTheme.typography.labelSmall)
                val details=remember(review.payload) {reviewDetails(review.payload)}
                if(details.isNotBlank())Text(details,style=MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ControlTextButton(onClick={model.openTask(review.taskId)},enabled=!state.saving) {Text("任务详情")}
                    if(review.applicantId==me.userId && review.status=="pending")ControlTextButton(onClick={model.decideTask(review.id,"canceled")},enabled=!state.saving) {Text("撤回")}
                    if(review.reviewerId==me.userId && review.status=="pending") {
                        ControlButton(onClick={model.decideTask(review.id,"approved")},enabled=!state.saving) {Text("批准")}
                        ControlOutlinedButton(onClick={model.decideTask(review.id,"rejected")},enabled=!state.saving) {Text("拒绝")}
                    }
                }
            }}}
            item {ControlOutlinedButton(onClick={model.screen("organization")}) {Text("查看工作信息的授权申请 →")}}
        } else {
            item {Text("任务分配、变更及审批通知",style=MaterialTheme.typography.titleMedium)}
            if(state.notifications.isEmpty())item {Text("暂无通知")}
            items(state.notifications,key={"notice-${it.id}"}) {message->OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${if(message.readAt==null)"● " else ""}${message.title}",style=MaterialTheme.typography.titleMedium)
                Text(message.message);Text(timestamp(message.createdAt),style=MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(message.readAt==null)ControlTextButton(onClick={model.readNotification(message)},enabled=!state.saving) {Text("标为已读")}
                    ControlTextButton(onClick={if(message.type=="view-request")model.screen("organization") else {tab="reviews";if(message.type=="task" && message.referenceId!=null)model.openTask(message.referenceId)}},enabled=!state.saving) {Text("查看详情")}
                }
            }}}
        }
    }
}
private fun reviewDetails(payload: String): String=runCatching {
    val data=com.google.gson.JsonParser.parseString(payload).asJsonObject
    listOf("title" to "事项","detail" to "说明","priority" to "优先级","assigneeName" to "责任人","dueAt" to "截止时间","status" to "进度状态","progressNote" to "进度说明","labelName" to "品牌标签","labelColor" to "背景颜色","keywordBefore" to "原关键词","keywordText" to "新关键词").mapNotNull {(key,title)->
        data[key]?.takeUnless {it.isJsonNull}?.asString?.takeIf {it.isNotBlank()}?.let {value->"$title：${when(key){"priority"->taskPriorityLabel(value);"status"->taskStatusLabel(value);"dueAt"->timestamp(value);else->value}}"}
    }.joinToString("\n")
}.getOrDefault("无法读取变更说明，请刷新后重试。")
