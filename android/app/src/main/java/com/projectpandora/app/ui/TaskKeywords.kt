package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun KeywordChips(keywords: List<TaskKeyword>,onClick: ((TaskKeyword)->Unit)?=null) {
    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        keywords.forEach {keyword->
            val content: @Composable ()->Unit = {Text("# ${keyword.text}",modifier=Modifier.padding(horizontal=12.dp,vertical=7.dp),style=MaterialTheme.typography.labelMedium)}
            val background=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.96f)
            if(onClick==null)Surface(color=background,contentColor=MaterialTheme.colorScheme.onSurfaceVariant,shape=CircleShape,content=content)
            else Surface(onClick={onClick(keyword)},color=background,contentColor=MaterialTheme.colorScheme.onSurfaceVariant,shape=CircleShape,content=content)
        }
    }
}

@Composable fun TaskKeywords(task: DispatchTask,state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    val participant=task.createdBy==me.userId || task.assigneeId==me.userId || me.role=="FOUNDER"
    val enabled=participant && !state.saving && !state.developmentPreview && taskIsApproved(task) && !task.deleted && state.taskReviewRecordsReady && !taskHasPendingReview(task,state.taskReviewRecords)
    var open by remember(task.id) {mutableStateOf(false)}
    var editing by remember(task.id) {mutableStateOf<TaskKeyword?>(null)}
    var text by remember(task.id) {mutableStateOf("")}
    var confirmDelete by remember {mutableStateOf(false)}
    LaunchedEffect(state.notice) {if(state.notice?.startsWith("关键词")==true){open=false;confirmDelete=false}}
    OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("任务关键词",style=MaterialTheme.typography.titleMedium)
        Text("用简短的词语说明任务，例如需求、联调、验收。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        KeywordChips(task.keywords.orEmpty(),if(enabled)({keyword->editing=keyword;text=keyword.text;open=true}) else null)
        if(task.keywords.isNullOrEmpty())Text("还没有关键词",style=MaterialTheme.typography.bodySmall)
        if(participant)ControlTextButton(onClick={editing=null;text="";open=true},enabled=enabled && task.keywords.orEmpty().size<12) {Text("＋ 添加关键词")}
        Text(if(state.developmentPreview)"本地示例；登录后可与任务执行人一起制定关键词。" else if(participant)"点击关键词可修改。修改自己的词直接生效，修改或删除他人添加的词需直属上级审核；创始人免审。" else "关键词由任务创建人和执行人共同制定。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }}
    if(open)AlertDialog(onDismissRequest={if(!state.saving)open=false},title={Text(if(editing==null)"添加关键词" else "修改关键词")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value=text,onValueChange={text=it.take(24)},singleLine=true,label={Text("关键词")},supportingText={Text("${text.length} / 24")},enabled=!state.saving)
        if(editing!=null && editing?.createdBy!=me.userId && me.role!="FOUNDER")Text("这是其他成员添加的词，变更需由你的直属上级批准。",style=MaterialTheme.typography.bodySmall)
        state.workspaceError?.let {Hint(it,true)}
        if(editing!=null)ControlTextButton(onClick={confirmDelete=true},enabled=!state.saving) {Text("删除此关键词",color=MaterialTheme.colorScheme.error)}
    }},confirmButton={ControlTextButton(onClick={model.saveTaskKeyword(task.id,editing?.id,text.trim())},enabled=!state.saving && text.isNotBlank()) {Text(if(editing!=null && editing?.createdBy!=me.userId && me.role!="FOUNDER")"提交审核" else "保存")}},dismissButton={ControlTextButton(onClick={open=false},enabled=!state.saving) {Text("取消")}})
    if(confirmDelete)AlertDialog(onDismissRequest={if(!state.saving)confirmDelete=false},title={Text("删除关键词？")},text={Text(if(editing?.createdBy==me.userId || me.role=="FOUNDER")"删除后会从当前任务移除。" else "申请将交给你的直属上级审核；批准前保留原词。")},confirmButton={ControlTextButton(onClick={editing?.let {model.deleteTaskKeyword(task.id,it.id)}},enabled=!state.saving) {Text("确认删除")}},dismissButton={ControlTextButton(onClick={confirmDelete=false},enabled=!state.saving) {Text("取消")}})
}
