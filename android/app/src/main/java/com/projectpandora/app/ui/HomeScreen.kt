package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel

@Composable fun HomeScreen(state: AppState, model: PandoraViewModel) {
    val panels=state.panels
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(bottom=28.dp)) {
        item {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("你好，${state.session?.displayName ?: state.username}",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Medium)
            Text("每一篇日志，都是工作向前的一步。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        if(state.loading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        state.panelError?.let {item {Hint("首页同步失败：$it\n请刷新；下面可能保留上次成功读取的内容。",true)}}
        if(panels!=null)item {Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                PanelCard("01", "公司十大事", panels.companyImportant.map{it.title}, "暂无公司事项", Modifier.weight(1f))
                PanelCard("02", "公司派发", panels.companyDispatch.map{it.title}, "暂无派发任务", Modifier.weight(1f))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                PanelCard("03", "个人十大事", panels.personalTop.map{it.title}, "暂无个人事项", Modifier.weight(1f))
                PanelCard("04", "今天的日志", panels.todayLogs.map{it.content}, "今天还没有日志", Modifier.weight(1f)) {model.screen("day");model.selectDate(com.projectpandora.app.data.today().toString())}
            }
        }}
        item {FilledTonalButton(onClick={model.edit()},enabled=!state.saving,modifier=Modifier.fillMaxWidth().height(48.dp)) {Text("✎  写下今天的工作")}}
        if(panels?.todayLogs?.isNotEmpty()==true)item {Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("今日记录",style=MaterialTheme.typography.titleMedium)
            panels.todayLogs.forEach {log->LogCard(log,state.saving,{model.edit(log)},{model.submit(log)})}
        }}
    }
}
@Composable private fun PanelCard(number: String,title: String,lines: List<String>,empty: String,modifier: Modifier,onClick: (() -> Unit)?=null) {
    OutlinedCard(modifier.heightIn(min=172.dp)) {Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(number,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
        Text(title,style=MaterialTheme.typography.titleSmall)
        if(lines.isEmpty())Text(empty,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.outline)
        lines.take(3).forEach {Text(it,maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)}
        if(onClick!=null)TextButton(onClick=onClick,contentPadding=PaddingValues(0.dp)) {Text("查看今日记录 →",style=MaterialTheme.typography.labelSmall)}
    }}
}
