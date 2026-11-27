package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun CompanyWorkLinks(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    val leader=AppRoles.canViewDown(me.role)
    Column(Modifier.fillMaxWidth().padding(top=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        ControlOutlinedButton(onClick={model.screen("inbox")},modifier=Modifier.fillMaxWidth()) {Text(if(AppRoles.canReview(me.role))"通知与审核 · ${state.unreadCount} 条未读 · ${state.pendingTaskApprovals.size+state.reviews.size+state.taskReviewRecords.count {it.status=="pending" && it.reviewerId==me.userId}} 项待审" else "消息通知 · ${state.unreadCount} 条未读")}
        ControlOutlinedButton(onClick={model.screen("labels")},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {Text(if(me.role=="FOUNDER")"维护品牌标签 · ${state.labels.size}" else "查看品牌标签 · ${state.labels.size}")}
        state.labelError?.let {Hint(it,true,onRetry=model::loadWorkspaceExtras)}
        Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=0.96f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(if(leader)AppRoles.scopeLabel(me.role) else "查看上级",style=MaterialTheme.typography.titleMedium)
                ControlOutlinedButton(onClick={model.screen("organization")},enabled=!state.saving) {Text(if(leader)"查看${AppRoles.scopeLabel(me.role)}" else "查看直属上级")}
            }
        }
    }
}
