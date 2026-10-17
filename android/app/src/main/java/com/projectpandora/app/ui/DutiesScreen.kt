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
                ScreenHeading(if(leader)"任务派发" else "待处理任务",if(leader)"安排责任人、明确截止时间，持续跟进任务。" else "处理分配给我的工作，及时反馈进展。")
                if(leader)ControlButton(onClick=model::openDispatch,enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {Text(if(state.dispatchEditor==null)"派发任务" else "继续派发任务")}
                if(state.taskLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
                state.taskError?.let {Hint("任务读取失败：$it",true,onRetry=model::refresh)}
                Text(if(leader)"我派发的任务 · ${tasks.size} 项" else "等待处理 · ${tasks.size} 项",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
                if(tasks.isEmpty() && !state.taskLoading && state.taskError==null)EmptyCard(if(leader)"尚未派发任务" else "暂无待处理任务",if(leader)"从上方按钮创建任务，可选择授权下级。" else "收到新任务后会显示在这里。")
                tasks.take(4).forEach {task->TaskSummary(task,!state.saving) {model.openTask(task.id)}}
                ControlTextButton(onClick={model.screen("tasks")},enabled=!state.saving,contentPadding=PaddingValues(0.dp)) {Text(if(leader)"查看全部任务与进展 →" else "查看全部任务（含已完成） →")}
            }
        }}
        item {Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=0.96f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(if(leader)AppRoles.scopeLabel(me.role) else "查看上级",style=MaterialTheme.typography.titleMedium)
                Text(if(leader)"输入姓名查找成员，查看已提交的工作信息。" else "申请查看直属上级的工作信息，获批后可只读查看。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                ControlOutlinedButton(onClick={model.screen("organization")},enabled=!state.saving) {Text(if(leader)"查看${AppRoles.scopeLabel(me.role)}" else "查看直属上级")}
            }
        }}
    }
}
