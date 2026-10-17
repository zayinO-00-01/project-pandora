package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun OrganizationScreen(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    var section by rememberSaveable {mutableStateOf(if(AppRoles.canViewDown(me.role))"members" else "manager")}
    var memberQuery by rememberSaveable(me.userId) {mutableStateOf("")}
    var reason by rememberSaveable {mutableStateOf("")}
    val enabled=!state.saving && !state.organizationLoading
    val member=state.member
    BackHandler(enabled=member!=null && !state.saving) {model.loadOrganization()}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=32.dp)) {
        item {ControlTextButton(onClick={if(member!=null)model.loadOrganization() else model.screen("functions")},enabled=!state.saving,contentPadding=PaddingValues(0.dp)) {Text(if(member!=null)"返回组织" else "返回职能")}}
        if(state.organizationLoading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        state.organizationError?.let {item {Hint(it,true,onRetry={if(member!=null)model.openMember(member) else model.loadOrganization()},retryEnabled=enabled)}}
        if(member!=null) {
            item {ScreenHeading(member.displayName ?: member.username,"${AppRoles.label(member.role)} · 只读查看，草稿不公开")}
            if(!state.organizationLoading && state.organizationError==null) {
                item {Text("已提交日志",style=MaterialTheme.typography.titleMedium)}
                if(state.memberLogs.isEmpty())item {Text("暂无已提交日志",color=MaterialTheme.colorScheme.onSurfaceVariant)}
                state.memberLogs.forEach {log->item(key="log-${log.id}") {Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(log.logDate,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(log.content,style=MaterialTheme.typography.bodyMedium);HorizontalDivider()}}}
                item {Text("任务进展",style=MaterialTheme.typography.titleMedium)}
                if(state.memberTasks.isEmpty())item {Text("暂无任务",color=MaterialTheme.colorScheme.onSurfaceVariant)}
                orderedTasks(state.memberTasks).forEach {task->item(key="task-${task.id}") {Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {Text(task.title,style=MaterialTheme.typography.bodyLarge);TaskPriorityText(task.priority);Text("${taskFeedbackLabel(task)} · ${task.progressNote.ifBlank {task.detail}}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);HorizontalDivider()}}}
            }
        } else {
            item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                if(AppRoles.canViewDown(me.role))ControlTextButton(onClick={section="members"}) {Text("下级工作")}
                if(me.role!="FOUNDER")ControlTextButton(onClick={section="manager"}) {Text("直属上级")}
                ControlTextButton(onClick={section="requests"}) {Text("查看申请")}
            }}
            if(section=="members") {
                item {ScreenHeading(AppRoles.scopeLabel(me.role),if(me.role=="FOUNDER")"查看全公司业务成员的已提交日志与任务，草稿不公开。" else "沿组织关系向下查看，其他部门与他人草稿不开放。")}
                val allMembers=state.organization?.members.orEmpty()
                val members=filterMembers(allMembers,memberQuery)
                item {OutlinedTextField(value=memberQuery,onValueChange={memberQuery=it},label={Text("搜索成员姓名")},placeholder={Text("输入姓名或账号")},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={if(memberQuery.isNotEmpty())ControlTextButton(onClick={memberQuery=""}) {Text("清空")}})}
                item {Text("找到 ${members.size} 位 / 共 ${allMembers.size} 位",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                if(members.isEmpty() && allMembers.isNotEmpty())item {EmptyCard("没有找到匹配的成员","请核对姓名，或清空搜索条件。")}
                if(allMembers.isEmpty() && !state.organizationLoading && state.organizationError==null)item {Text("暂无可查看的下级，请由管理员维护组织关系。",color=MaterialTheme.colorScheme.onSurfaceVariant)}
                members.groupBy {it.role}.forEach {(role,users)->
                    item {Text(AppRoles.label(role),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                    users.forEach {user->item(key="member-${user.id}") {ControlTextButton(onClick={model.openMember(user)},enabled=enabled,modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(vertical=12.dp)) {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(user.displayName ?: user.username);Text("查看工作 ›")}}}}
                }
            }
            if(section=="manager") {
                val manager=state.organization?.manager
                val request=state.viewRequests.firstOrNull {it.requester.id==me.userId && it.target.id==manager?.id}
                item {ScreenHeading("查看直属上级","提交理由，由上级批准后查看已提交日志与任务。批准可由上级撤销，组织关系变化后自动失去访问。")}
                if(manager==null && !state.organizationLoading && state.organizationError==null)item {Text("尚未配置直属上级",color=MaterialTheme.colorScheme.onSurfaceVariant)}
                if(manager!=null) {
                    item {Text("${manager.displayName ?: manager.username} · ${AppRoles.label(manager.role)}",style=MaterialTheme.typography.titleMedium)}
                    if(request?.status=="approved")item {ControlButton(onClick={model.openMember(manager)},enabled=enabled,modifier=Modifier.fillMaxWidth()) {Text("查看上级工作")}}
                    else if(request?.status=="pending")item {Text("已申请 · 等待上级审批",color=MaterialTheme.colorScheme.secondary);Text(request.reason,style=MaterialTheme.typography.bodyMedium)}
                    else item {
                        if(request!=null)Text(if(request.status=="denied")"申请未获批准，可补充理由后再次申请。" else "授权已撤销，可重新申请。",style=MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(value=reason,onValueChange={reason=it.take(500)},label={Text("申请理由")},supportingText={Text("${reason.length}/500")},enabled=enabled,modifier=Modifier.fillMaxWidth(),minLines=3)
                        ControlButton(onClick={model.requestView(manager.id,reason.trim())},enabled=enabled && reason.isNotBlank(),modifier=Modifier.fillMaxWidth()) {Text(if(state.saving)"提交中…" else "申请查看")}
                    }
                }
            }
            if(section=="requests") {
                item {ScreenHeading("查看申请","只处理申请查看你工作信息的直属下级；授权仅允许查看。")}
                if(state.viewRequests.isEmpty() && !state.organizationLoading && state.organizationError==null)item {Text("暂无查看申请",color=MaterialTheme.colorScheme.onSurfaceVariant)}
                state.viewRequests.forEach {request->item(key="request-${request.id}") {
                    val incoming=request.target.id==me.userId
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(if(incoming)"${request.requester.displayName ?: request.requester.username} 申请查看你的工作" else "我申请查看 ${request.target.displayName ?: request.target.username}",style=MaterialTheme.typography.titleMedium)
                        Text(when(request.status) {"pending"->"待审批";"approved"->"已批准";"denied"->"已拒绝";else->"已撤销"},color=MaterialTheme.colorScheme.secondary,style=MaterialTheme.typography.labelMedium)
                        Text(request.reason,style=MaterialTheme.typography.bodyMedium)
                        if(incoming && request.status=="pending")Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {ControlButton(onClick={model.decideView(request.id,"approved")},enabled=enabled) {Text("批准")};ControlOutlinedButton(onClick={model.decideView(request.id,"denied")},enabled=enabled) {Text("拒绝")}}
                        if(incoming && request.status=="approved")ControlOutlinedButton(onClick={model.decideView(request.id,"revoked")},enabled=enabled) {Text("撤销授权")}
                        HorizontalDivider()
                    }
                }}
            }
        }
    }
}
