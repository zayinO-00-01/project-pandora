package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PandoraApp(state: AppState, model: PandoraViewModel) {
    if(state.session==null) {Surface(Modifier.fillMaxSize()) {LoginScreen(state,model::login)};return}
    BackHandler(enabled=state.editor==null && state.screen!="home") {if(!state.saving)model.screen("home")}
    Scaffold(topBar={TopAppBar(title={Text("潘多拉",style=MaterialTheme.typography.titleLarge)},actions={
        TextButton(onClick=model::refresh,enabled=!state.loading && !state.saving) {Text(if(state.loading)"同步中…" else "刷新")}
    },colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))},bottomBar={
        if(state.editor==null)NavigationBar(containerColor=MaterialTheme.colorScheme.surface) {
            val items=listOf(Triple("home","首页",Icons.Default.Home),Triple("logs","日志",Icons.Default.Edit),Triple("tasks","任务",Icons.Default.List),Triple("day","日视图",Icons.Default.DateRange),Triple("profile","我的",Icons.Default.Person))
            items.forEach {(key,label,icon)->NavigationBarItem(selected=state.screen==key,onClick={model.screen(key)},enabled=!state.saving,icon={Icon(icon,contentDescription=label)},label={Text(label)})}
        }
    }) {padding->Column(Modifier.fillMaxSize().padding(padding)) {
        state.notice?.let {Box(Modifier.padding(horizontal=20.dp,vertical=8.dp)) {Hint(it)}}
        if(state.editor!=null)EditorScreen(state,model)
        else when(state.screen) {
            "home" -> HomeScreen(state,model)
            "logs" -> LogsScreen(state,model)
            "tasks" -> TaskScreen(state,model)
            "day" -> LogsScreen(state,model,true)
            "profile" -> ProfileScreen(state,model)
        }
    }}
}
@Composable private fun ProfileScreen(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    var leaving by remember {mutableStateOf(false)}
    if(leaving)AlertDialog(onDismissRequest={leaving=false},title={Text("退出当前账号？")},text={Text("记录仍保存在服务器。未提交反馈会留在本机，仅同服务器同账号重新登录可恢复。")},confirmButton={TextButton(onClick={leaving=false;model.logout()}) {Text("退出登录")}},dismissButton={TextButton(onClick={leaving=false}) {Text("取消")}})
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text("我的工作空间",style=MaterialTheme.typography.headlineSmall)
        OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(me.displayName ?: me.username,style=MaterialTheme.typography.titleLarge)
            Text("账号：${me.username}")
            Text("身份：${when(me.role){"ADMIN"->"管理员";"LEADER"->"部门领导";else->"员工"}}")
            Text("服务：${me.server}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        Text("已提供日志记录、任务查看与责任人进度反馈、首页同步和日视图。公司事项及其他日期视图将随后续增量开放。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick={leaving=true},modifier=Modifier.fillMaxWidth()) {Text("退出 / 更换服务器")}
        Text("PANDORA / 每天向前一点",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.outline)
    }
}
