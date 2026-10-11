package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.projectpandora.app.R
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PandoraApp(state: AppState, model: PandoraViewModel) {
    if(state.session==null) {Surface(Modifier.fillMaxSize()) {LoginScreen(state,model::login,model::enterDevelopment)};return}
    val feedback=LocalControlFeedback.current
    val homeHeader=state.editor==null && state.selectedTaskId==null && state.screen in listOf("home","ai")
    var syncDetails by remember {mutableStateOf(false)}
    val readFailures=listOfNotNull(state.panelError?.let {"总览：$it"},state.taskError?.let {"任务：$it"},state.logError?.let {"日志：$it"})
    val retained=state.tasks.isNotEmpty() || state.logs.isNotEmpty() || state.panels!=null
    if(syncDetails)AlertDialog(onDismissRequest={syncDetails=false},title={Text("同步失败")},text={Text(readFailures.joinToString("\n\n")+if(retained)"\n\n正在显示上次成功读取的内容。" else "\n\n暂时无法读取工作内容。")},confirmButton={TextButton(onClick={syncDetails=false;model.refresh()},enabled=!state.loading) {Text("重试")}},dismissButton={TextButton(onClick={syncDetails=false}) {Text("关闭")}})
    val canCreate=state.editor==null && state.selectedTaskId==null && state.screen in listOf("home","logs","day","ai")
    val headerHeight=(if(homeHeader && (readFailures.isNotEmpty() || state.developmentPreview))96.dp else if(homeHeader)72.dp else if(state.developmentPreview)64.dp else 48.dp) * maxOf(1f,LocalDensity.current.fontScale)
    BackHandler(enabled=state.editor==null && state.selectedTaskId==null && state.screen!="home") {if(!state.saving)model.screen("home")}
    val pageTitle=when {
        state.editor!=null -> "潘多拉"
        state.screen=="home" || state.screen=="ai" -> "工作总览"
        state.screen=="logs" -> "工作日志"
        state.screen=="day" -> "工作视图"
        state.screen=="profile" -> "我的"
        else -> "潘多拉"
    }
    Scaffold(topBar={Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.TopCenter) {TopAppBar(modifier=Modifier.widthIn(max=600.dp).fillMaxWidth(),expandedHeight=headerHeight,title={Column(Modifier.padding(start=8.dp).semantics {if(homeHeader && readFailures.isNotEmpty())contentDescription="同步失败"}.clickable(enabled=homeHeader && readFailures.isNotEmpty()) {syncDetails=true},verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Text(pageTitle,style=MaterialTheme.typography.headlineSmall,fontFamily=ConsoleDisplay,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
        if(homeHeader)Text("${today().monthValue}月${today().dayOfMonth}日 · ${listOf("周一","周二","周三","周四","周五","周六","周日")[today().dayOfWeek.value-1]}${if(readFailures.isNotEmpty())"" else if(state.developmentPreview)" · 未连接" else if(state.loading)" · 同步中" else " · 已同步"}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        if(homeHeader && readFailures.isNotEmpty())Text("同步失败 · ${if(retained)"上次内容" else "查看原因"} ›",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.error,maxLines=2)
        if(state.developmentPreview)Text("本地预览",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }},actions={
        if(canCreate) {WriteLogButton(enabled=!state.saving && !state.checkingIntegrity,onClick={model.edit()});Spacer(Modifier.width(4.dp))}
        if(state.screen!="profile")IconButton(onClick={feedback();model.refresh()},enabled=!state.loading && !state.saving) {
            if(state.loading)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            else Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.console_sync),"同步",modifier=Modifier.size(18.dp),tint=MaterialTheme.colorScheme.secondary)
                if(homeHeader && readFailures.isNotEmpty())Text("重试",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
            }
        }
    },colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))}},bottomBar={
        if(state.editor==null && state.selectedTaskId==null)Column {
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {NavigationBar(containerColor=MaterialTheme.colorScheme.background,tonalElevation=0.dp,modifier=Modifier.widthIn(max=600.dp).fillMaxWidth().height(72.dp),windowInsets=WindowInsets(0,0,0,0)) {
                val items=listOf(Triple("home","总览",R.drawable.console_home),Triple("logs","日志",R.drawable.console_log),Triple("day","视图",R.drawable.console_calendar),Triple("profile","我的",R.drawable.console_person))
                items.forEach {(key,label,icon)->NavigationBarItem(selected=state.screen==key || (key=="home" && state.screen in listOf("tasks","ai")),
                    onClick={feedback();model.screen(key)},enabled=!state.saving,
                    icon={Icon(painterResource(icon),contentDescription=null,modifier=Modifier.size(21.dp))},label={Text(label,maxLines=1,softWrap=false,style=MaterialTheme.typography.labelSmall)},
                    colors=NavigationBarItemDefaults.colors(indicatorColor=androidx.compose.ui.graphics.Color.Transparent,
                        selectedIconColor=MaterialTheme.colorScheme.secondary,selectedTextColor=MaterialTheme.colorScheme.onSurface))}
            }}
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }) {padding->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {Column(Modifier.fillMaxHeight().widthIn(max=600.dp).fillMaxWidth().padding(padding)) {
        state.notice?.takeUnless {state.developmentPreview && it.startsWith("开发预览：")}?.let {Box(Modifier.padding(horizontal=20.dp,vertical=8.dp)) {Hint(it)}}
        if(state.integrityError!=null)Box(Modifier.padding(horizontal=20.dp)) {
            Hint("日志校验失败：${state.integrityError}。输入仍保留，请重试。",true,onRetry={model.checkIntegrity()},retryEnabled=!state.checkingIntegrity)
        }
        if(state.editor!=null)EditorScreen(state,model)
        else when(state.screen) {
            "home" -> HomeScreen(state,model)
            "logs" -> LogsScreen(state,model)
            "tasks" -> TaskScreen(state,model)
            "day" -> CalendarScreen(state,model)
            "ai" -> HomeScreen(state,model)
            "profile" -> ProfileScreen(state,model)
        }
    }}}
}

@Composable private fun ProfileScreen(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    val settings=LocalFeedbackSettings.current
    val appearance=LocalAppearance.current
    var leaving by remember {mutableStateOf(false)}
    var details by remember {mutableStateOf<String?>(null)}
    val permissionText=when(me.role) {
        "FOUNDER"->"创始人：可按服务端授权查看公司业务用户。"
        "TEAM_LEAD","DEPT_HEAD"->"领导：可按服务端授权向下查看组织成员。"
        "LEADER"->"当前连接旧版演示服务，权限以该服务为准。"
        else->"员工：记录自己的工作，处理分配给自己的任务。"
    }
    details?.let {section->AlertDialog(containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={details=null},title={Text(if(section=="access")"账号与权限" else "当前服务")},text={Text(if(section=="access")"$permissionText\n\n资料与组织关系由管理员在 Web 维护；个人编辑和组织查看尚未开放。" else "${me.server}\n\nAI 工作地图尚未开放。")},confirmButton={TextButton(onClick={details=null}) {Text("关闭")}})}
    var details by remember {mutableStateOf<String?>(null)}
    val permissionText=when(me.role) {
        "FOUNDER"->"创始人：可按服务端授权查看公司业务用户。"
        "TEAM_LEAD","DEPT_HEAD"->"领导：可按服务端授权向下查看组织成员。"
        "LEADER"->"当前连接旧版演示服务，权限以该服务为准。"
        else->"员工：记录自己的工作，处理分配给自己的任务。"
    }
    details?.let {section->AlertDialog(containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={details=null},title={Text(if(section=="access")"账号与权限" else "当前服务")},text={Text(if(section=="access")"$permissionText\n\n资料与组织关系由管理员在 Web 维护；个人编辑和组织查看尚未开放。" else me.server)},confirmButton={TextButton(onClick={details=null}) {Text("关闭")}})}
    if(leaving)AlertDialog(containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={leaving=false},title={Text(if(state.developmentPreview)"退出开发预览？" else "退出当前账号？")},text={Text(if(state.developmentPreview)"预览没有登录业务账号，也未向服务器提交数据。" else "记录保存在服务器。未保存输入留在本机，同服务器同账号登录可恢复。")},confirmButton={TextButton(onClick={leaving=false;model.logout()}) {Text("退出登录")}},dismissButton={TextButton(onClick={leaving=false}) {Text("取消")}})
    Column(Modifier.fillMaxSize()) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(top=12.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Icon(painterResource(R.drawable.console_person),null,tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(28.dp))
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(me.displayName ?: me.username,style=MaterialTheme.typography.titleMedium)
                Text("${me.username} · ${AppRoles.label(me.role)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectableGroup(),verticalAlignment=Alignment.CenterVertically) {
            Text("外观",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f))
            listOf(Appearance.LIGHT to "浅色",Appearance.DARK to "深色").forEach {(mode,label)->
                Row(Modifier.widthIn(min=80.dp).heightIn(min=48.dp).selectable(selected=appearance.mode==mode,role=Role.RadioButton,onClick={appearance.select(mode)}),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    RadioButton(selected=appearance.mode==mode,onClick=null,modifier=Modifier.size(20.dp).scale(0.8f),colors=RadioButtonDefaults.colors(selectedColor=MaterialTheme.colorScheme.secondary,unselectedColor=MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(label,style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Text("操作偏好",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            FeedbackRow("按键触感","轻量震动，遵循系统触觉设置",settings.haptic,settings.setHaptic)
            FeedbackRow("按键声音","短促轻音，静音模式不播放",settings.sound,settings.setSound)
        }

        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        TextButton(onClick={details="access"},enabled=!state.saving,contentPadding=PaddingValues(0.dp),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface)) {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("账号与权限",style=MaterialTheme.typography.bodyMedium)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        TextButton(onClick={details="service"},enabled=!state.saving,contentPadding=PaddingValues(0.dp),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface)) {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("当前服务",style=MaterialTheme.typography.bodyMedium)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
    }
    Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text("潘多拉",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={leaving=true},enabled=!state.saving,modifier=Modifier.heightIn(min=48.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurfaceVariant)) {Text(if(state.developmentPreview)"退出预览" else "退出登录",style=MaterialTheme.typography.labelMedium)}
    }
    }
}

@Composable private fun FeedbackRow(title: String,detail: String,value: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).toggleable(value=value,role=Role.Switch,onValueChange=onChange),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {Text(title,style=MaterialTheme.typography.bodyMedium);Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Switch(checked=value,onCheckedChange=null,colors=SwitchDefaults.colors(checkedTrackColor=MaterialTheme.colorScheme.secondary,checkedThumbColor=MaterialTheme.colorScheme.background),modifier=Modifier.scale(0.75f))
    }
}

