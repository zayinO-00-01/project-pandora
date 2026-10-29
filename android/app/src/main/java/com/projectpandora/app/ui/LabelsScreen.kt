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

@Composable fun LabelsScreen(state: AppState,model: PandoraViewModel) {
    var editing by remember {mutableStateOf<WorkLabel?>(null)}
    var name by remember {mutableStateOf("")}
    var color by remember {mutableStateOf("#B9DAD0")}
    LaunchedEffect(state.notice) {if(state.notice=="标签已保存"){editing=null;name=""}}
    val palette=listOf("#B9DAD0","#F3CCA8","#BCD3EF","#D8C5E8","#EAB8B8","#F2D984","#285A59","#435473")
    val founder=state.session?.role=="FOUNDER"
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=28.dp)) {
        item {ControlTextButton(onClick={model.screen("profile")}) {Text("← 返回我的")}}
        item {ScreenHeading("品牌标签","统一维护品牌名称与颜色，任务卡片和任务日历同步使用这些颜色。")}
        if(state.developmentPreview)item {Hint("开发预览：以下为示例品牌标签；真实标签由创始人登录后维护。")}
        state.labelError?.let {item {Hint(it,true,onRetry=model::loadWorkspaceExtras)}}
        state.workspaceError?.let {item {Hint(it,true)}}
        if(!founder)item {Text("标签由创始人维护，所有成员可在任务与日志中选择。",style=MaterialTheme.typography.bodySmall)}
        if(state.labels.isEmpty() && state.labelError==null)item {EmptyCard("暂无品牌标签",if(founder)"在下方填写品牌名称并选择颜色后保存。" else "请创始人登录后添加品牌名称和颜色。")}
        if(founder) {
            item {OutlinedTextField(value=name,onValueChange={name=it.take(64)},label={Text(if(editing==null)"新标签名称" else "编辑标签名称")},modifier=Modifier.fillMaxWidth(),singleLine=true)}
            item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {palette.forEach {value->
                Surface(onClick={color=value},color=labelColor(WorkLabel(0,"",value)),shape=MaterialTheme.shapes.small,modifier=Modifier.weight(1f).height(44.dp)) {Box(contentAlignment=androidx.compose.ui.Alignment.Center) {if(color==value)Text("✓",color=labelTextColor(WorkLabel(0,"",value)))}}
            }}}
            item {OutlinedTextField(value=color,onValueChange={color=it.take(7)},label={Text("颜色（#RRGGBB）")},singleLine=true,modifier=Modifier.fillMaxWidth())}
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ControlButton(onClick={model.saveLabel(editing?.id,name.trim(),color)},enabled=!state.saving && name.isNotBlank() && Regex("#[0-9a-fA-F]{6}").matches(color)) {Text("保存标签")}
                if(editing!=null)ControlTextButton(onClick={editing=null;name=""}) {Text("取消编辑")}
            }}
        }
        items(state.labels,key={it.id}) {label->OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            LabelBadge(label)
            if(founder)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ControlTextButton(onClick={editing=label;name=label.name;color=label.color},enabled=!state.saving) {Text("编辑")}
                ControlTextButton(onClick={model.archiveLabel(label.id)},enabled=!state.saving) {Text("停用")}
            }
        }}}
    }
}
