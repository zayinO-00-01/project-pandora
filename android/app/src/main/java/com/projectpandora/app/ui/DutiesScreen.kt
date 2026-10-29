package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun DutiesScreen(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    val leader=AppRoles.canDispatch(me.role)
    val tasks=dutyTasks(state.tasks,me.userId,leader)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(top=8.dp,bottom=28.dp)) {
        item {Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=0.97f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
            Column(Modifier.heightIn(min=320.dp).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                ScreenHeading(if(leader)"任务安排" else "我的任务",if(leader)"给自己安排工作，或向下级派发任务。" else "自建任务经直属上级审核后执行，也可接收派发任务。")
                if(AppRoles.canCreateTask(me.role))ControlButton(onClick=model::openDispatch,enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {Text(if(state.dispatchEditor==null)"新建任务" else "继续填写任务")}
                if(state.taskLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
                state.taskError?.let {Hint("任务读取失败：$it",true,onRetry=model::refresh)}
                Text(if(leader)"我创建的任务 · ${tasks.size} 项" else "未完成与待审 · ${tasks.size} 项",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
                if(tasks.isEmpty() && !state.taskLoading && state.taskError==null)EmptyCard("暂无任务","从上方按钮给自己安排一项工作。")
                tasks.take(4).forEach {task->TaskSummary(task,!state.saving) {model.openTask(task.id)}}
                ControlTextButton(onClick={model.screen("tasks")},enabled=!state.saving,contentPadding=PaddingValues(0.dp)) {Text(if(leader)"查看全部任务与进展 →" else "查看全部任务（含已完成） →")}
            }
        }}
        if(leader)item {OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("待我审核 · ${state.pendingTaskApprovals.size}",style=MaterialTheme.typography.titleMedium)
            state.taskReviewError?.let {Hint(it,true,onRetry=model::refresh)}
            if(state.pendingTaskApprovals.isEmpty() && state.taskReviewError==null)Text("暂无直属下级提交的自建任务。",style=MaterialTheme.typography.bodySmall)
            state.pendingTaskApprovals.forEach {task->TaskSummary(task,!state.saving) {model.openTask(task.id)}}
        }}}
        item {Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=0.96f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(if(leader)AppRoles.scopeLabel(me.role) else "查看上级",style=MaterialTheme.typography.titleMedium)
                Text(if(leader)"输入姓名查找成员，查看已提交的工作信息。" else "申请查看直属上级的工作信息，获批后可只读查看。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                ControlOutlinedButton(onClick={model.screen("organization")},enabled=!state.saving) {Text(if(leader)"查看${AppRoles.scopeLabel(me.role)}" else "查看直属上级")}
            }
        }}
    }
}
