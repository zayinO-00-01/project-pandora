package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun TaskManagementSection(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    val leader=AppRoles.canViewDown(me.role)
    val tasks=dutyTasks(state.tasks,me.userId,leader)
    Column(Modifier.fillMaxWidth().padding(top=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("任务管理",style=MaterialTheme.typography.titleLarge)
        ControlOutlinedButton(onClick={model.screen("inbox")},modifier=Modifier.fillMaxWidth()) {Text("通知与审核 · ${state.unreadCount} 条未读 · ${state.pendingTaskApprovals.size+state.reviews.size+state.taskReviewRecords.count {it.status=="pending" && it.reviewerId==me.userId}} 项待审")}
        Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=0.97f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                ScreenHeading(if(leader)"任务安排" else "待处理任务",if(me.role=="FOUNDER")"可为公司任一业务成员安排任务，创建与本人进度直接生效。" else "可为公司任一业务成员安排任务；创建和进度均经本人当前直属上级批准后生效。")
                ControlButton(onClick=model::openDispatch,enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {Text(if(state.dispatchEditor==null)"创建任务" else "继续创建任务")}
                if(state.taskLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
                state.taskError?.let {Hint("任务读取失败：$it",true,onRetry=model::refresh)}
                Text(if(leader)"我创建的任务 · ${tasks.size} 项" else "等待处理 · ${tasks.size} 项",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
                if(tasks.isEmpty() && !state.taskLoading && state.taskError==null)EmptyCard(if(leader)"尚未派发任务" else "暂无待处理任务",if(leader)"从上方按钮为公司业务成员安排工作。" else "收到新任务后会显示在这里。")
                tasks.take(4).forEach {task->TaskSummary(task,!state.saving) {model.openTask(task.id)}}
                ControlTextButton(onClick={model.screen("tasks")},enabled=!state.saving,contentPadding=PaddingValues(0.dp)) {Text(if(leader)"查看全部任务与进展 →" else "查看全部任务（含已完成） →")}
            }
        }
        ControlOutlinedButton(onClick={model.screen("labels")},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {Text(if(me.role=="FOUNDER")"维护品牌标签 · ${state.labels.size}" else "查看品牌标签 · ${state.labels.size}")}
        state.labelError?.let {Hint(it,true,onRetry=model::loadWorkspaceExtras)}
        Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=0.96f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(if(leader)AppRoles.scopeLabel(me.role) else "查看上级",style=MaterialTheme.typography.titleMedium)
                Text(if(leader)"输入姓名查找成员，查看已提交的工作信息。" else "申请查看直属上级的工作信息，获批后可只读查看。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                ControlOutlinedButton(onClick={model.screen("organization")},enabled=!state.saving) {Text(if(leader)"查看${AppRoles.scopeLabel(me.role)}" else "查看直属上级")}
            }
        }
    }
}
