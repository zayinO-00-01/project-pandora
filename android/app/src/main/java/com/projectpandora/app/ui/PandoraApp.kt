package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.projectpandora.app.R
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PandoraApp(state: AppState, model: PandoraViewModel) {
    if(state.session==null) {
        Surface(Modifier.fillMaxSize()) {
            InkLogin(state,model::login,model::enterDevelopment)
        }
        return
    }
    val feedback=LocalControlFeedback.current
    val homeHeader=state.editor==null && state.selectedTaskId==null && state.screen in listOf("home","ai")
    var syncDetails by remember {mutableStateOf(false)}
    val readFailures=listOfNotNull(state.panelError?.let {"总览：$it"},state.taskError?.let {"任务：$it"},state.logError?.let {"日志：$it"})
    val retained=state.tasks.isNotEmpty() || state.logs.isNotEmpty() || state.panels!=null
    if(syncDetails)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={syncDetails=false},title={Text("同步失败")},text={Text(readFailures.joinToString("\n\n")+if(retained)"\n\n正在显示上次成功读取的内容。" else "\n\n暂时无法读取工作内容。")},confirmButton={ControlTextButton(onClick={syncDetails=false;model.refresh()},enabled=!state.loading) {Text("重试")}},dismissButton={ControlTextButton(onClick={syncDetails=false}) {Text("关闭")}})
    val canCreate=state.editor==null && state.selectedTaskId==null && state.screen in listOf("home","logs","day","ai")
    val headerHeight=(if(homeHeader && (readFailures.isNotEmpty() || state.developmentPreview))96.dp else if(homeHeader)72.dp else if(state.developmentPreview)64.dp else 48.dp) * maxOf(1f,LocalDensity.current.fontScale)
    BackHandler(enabled=state.editor==null && state.selectedTaskId==null && state.screen!="home" && !state.dispatchOpen) {if(!state.saving)model.screen(if(state.screen in listOf("tasks","organization"))"functions" else "home")}
    val pageTitle=when {
        state.editor!=null -> "潘多拉"
        state.screen=="home" || state.screen=="ai" -> AppRoles.homeTitle(state.session.role)
        state.dispatchOpen -> "新建任务"
        state.screen=="functions" -> "职能"
        state.screen=="tasks" -> if(state.selectedTaskId==null)"任务列表" else "任务详情"
        state.screen=="organization" -> AppRoles.scopeLabel(state.session.role)
        state.screen=="logs" -> "工作日志"
        state.screen=="day" -> "工作视图"
        state.screen=="profile" -> "我的"
        state.screen=="notifications" -> "消息中心"
        else -> "潘多拉"
    }
    Box(Modifier.fillMaxSize()) {
    WorkspaceBackground(Modifier.matchParentSize())
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(MaterialTheme.colorScheme.background))
    Scaffold(containerColor=Color.Transparent,contentColor=MaterialTheme.colorScheme.onBackground,topBar={Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)).padding(top=12.dp,bottom=6.dp),contentAlignment=Alignment.TopCenter) {TopAppBar(modifier=Modifier.widthIn(max=600.dp).fillMaxWidth(),expandedHeight=headerHeight,windowInsets=WindowInsets(0,0,0,0),navigationIcon={if(state.screen!="profile")Box(contentAlignment=Alignment.TopEnd){IconButton(onClick={feedback();model.loadNotifications();model.screen("notifications")},enabled=!state.saving){Icon(painterResource(R.drawable.console_bell),contentDescription="消息",modifier=Modifier.size(21.dp))};if(state.unreadCount>0)Badge(modifier=Modifier.align(Alignment.TopEnd).offset(x=6.dp,y=(-6).dp)){Text(if(state.unreadCount>99)"99+" else state.unreadCount.toString())}}},title={Column(Modifier.padding(start=8.dp).semantics {if(homeHeader && readFailures.isNotEmpty())contentDescription="同步失败"}.clickable(enabled=homeHeader && readFailures.isNotEmpty()) {syncDetails=true},verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Text(pageTitle,style=MaterialTheme.typography.headlineSmall,fontFamily=ConsoleDisplay,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)
        if(homeHeader)Text("${today().monthValue}月${today().dayOfMonth}日 · ${listOf("周一","周二","周三","周四","周五","周六","周日")[today().dayOfWeek.value-1]}${if(readFailures.isNotEmpty())"" else if(state.developmentPreview)" · 未连接" else if(state.loading)" · 同步中" else " · 已同步"}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2)
        if(homeHeader && readFailures.isNotEmpty())Text("同步失败 · ${if(retained)"上次内容" else "查看原因"} ›",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.error,maxLines=2)
        if(state.developmentPreview)Text("本地预览",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }},actions={
        if(canCreate) {
            if(state.screen=="day") {
                if(AppRoles.canCreateTask(state.session.role)) {InlineAction("新建任务",enabled=!state.saving,icon=R.drawable.console_add) {feedback();model.openDispatch()};Spacer(Modifier.width(4.dp))}
            } else {WriteLogButton(enabled=!state.saving,onClick={model.edit()});Spacer(Modifier.width(4.dp))}
        }
        if(state.screen!="profile")InlineAction(if(homeHeader && readFailures.isNotEmpty())"重试" else "刷新",enabled=!state.loading && !state.organizationLoading && !state.saving,icon=R.drawable.console_sync,secondary=true,loading=state.loading || state.organizationLoading) {feedback();if(state.screen=="organization")model.loadOrganization() else model.refresh()}
    },colors=TopAppBarDefaults.topAppBarColors(containerColor=Color.Transparent))}},bottomBar={
        if(state.editor==null && state.selectedTaskId==null && !state.dispatchOpen)Column {
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {NavigationBar(containerColor=MaterialTheme.colorScheme.background,tonalElevation=0.dp,modifier=Modifier.widthIn(max=600.dp).fillMaxWidth().height(72.dp),windowInsets=WindowInsets(0,0,0,0)) {
                val items=listOf(Triple("home","总览",R.drawable.console_home),Triple("functions","职能",R.drawable.console_duties),Triple("logs","日志",R.drawable.console_log),Triple("day","视图",R.drawable.console_calendar),Triple("profile","我的",R.drawable.console_person))
                items.forEach {(key,label,icon)->NavigationBarItem(selected=state.screen==key || (key=="functions" && state.screen in listOf("tasks","organization")) || (key=="home" && state.screen=="ai"),
                    onClick={feedback();model.screen(key)},enabled=!state.saving,
                    icon={Icon(painterResource(icon),contentDescription=null,modifier=Modifier.size(21.dp))},label={Text(label,maxLines=1,softWrap=false,style=MaterialTheme.typography.labelSmall)},
                    colors=NavigationBarItemDefaults.colors(indicatorColor=androidx.compose.ui.graphics.Color.Transparent,
                        selectedIconColor=MaterialTheme.colorScheme.secondary,selectedTextColor=MaterialTheme.colorScheme.onSurface))}
            }}
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }) {padding->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {Column(Modifier.fillMaxHeight().widthIn(max=600.dp).fillMaxWidth().padding(padding)) {
        state.notice?.takeUnless {state.developmentPreview && it.startsWith("开发预览：")}?.let {Box(Modifier.padding(horizontal=20.dp,vertical=8.dp)) {Hint(it)}}
        if(state.editor!=null)EditorScreen(state,model)
        else when(state.screen) {
            "home" -> HomeScreen(state,model)
            "functions" -> DutiesScreen(state,model)
            "logs" -> LogsScreen(state,model)
            "tasks" -> TaskScreen(state,model)
            "day" -> CalendarScreen(state,model)
            "ai" -> HomeScreen(state,model)
            "profile" -> ProfileScreen(state,model)
            "notifications" -> NotificationsScreen(state,model)
            "organization" -> OrganizationScreen(state,model)
        }
    }}}
    }
}

@Composable private fun NotificationsScreen(state: AppState, model: PandoraViewModel) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(top=4.dp,bottom=28.dp)) {
        item {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            ScreenHeading("消息中心","派发、验收、变更与删除的提醒都会出现在这里。")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text(if(state.unreadCount>0)"未读 ${state.unreadCount} 条" else "全部已读",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                ControlTextButton(onClick=model::markAllNotificationsRead,enabled=!state.saving && state.unreadCount>0) {Text("全部已读")}
            }
            if(state.notificationsLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
        }}
        if(state.notifications.isEmpty() && !state.notificationsLoading)item {EmptyCard("暂无消息","有新的派发或审核进展时会在这里提醒你。")}
        items(state.notifications,key={it.id}) {n->OutlinedCard(onClick={model.openNotification(n)},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text(n.title,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f),color=if(n.read)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary)
                    if(!n.read)Badge {Text("新")}
                }
                Text(n.body,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(notificationTime(n.createdAt),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }}
    }
}
private fun notificationTime(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")).format(Instant.parse(value))
}.getOrDefault(value)

@Composable private fun WorkspaceBackground(modifier: Modifier) {
    val dark=LocalAppearance.current.mode==Appearance.DARK
    // Map the same artwork's ivory, petrol and copper to their night counterparts.
    val nightFilter=remember {ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        0.75f,-0.85f,0f,0f,40.8f,
        -0.20f,0.10f,0f,0f,55.335f,
        -0.20f,0.13f,0f,0f,52.785f,
        0f,0f,0f,1f,0f
    )))}
    Image(painter=painterResource(R.drawable.paper_background_balanced),contentDescription=null,
        modifier=modifier,contentScale=ContentScale.FillBounds,colorFilter=if(dark)nightFilter else null)
}

@Composable private fun ProfileScreen(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    val settings=LocalFeedbackSettings.current
    val appearance=LocalAppearance.current
    var leaving by remember {mutableStateOf(false)}
    var details by remember {mutableStateOf<String?>(null)}
    var avatarPicker by remember {mutableStateOf(false)}
    var passwordDialog by remember {mutableStateOf(false)}
    var currentPw by remember {mutableStateOf("")}
    var newPw by remember {mutableStateOf("")}
    val avatars=listOf("🦊","🐼","🐯","🐻","🦉","🐳","🌿","⭐","🔥","🍀")
    val context=LocalContext.current
    val pickImage=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri-> if(uri!=null) {val data=imageUriToDataUrl(context,uri); if(data!=null){model.changeAvatar(data);avatarPicker=false}}}
    val permissionText=when(me.role) {
        "FOUNDER"->"创始人：可查看公司业务成员已提交的日志与任务；可向授权下级派发任务，给自己建任务直接生效。"
        "DEPT_HEAD"->"部门老总：自建任务须直属上级审核，可审核直属下级自建任务；可穿透查看部门下属；查看直属上级须申请并获批准。"
        "TEAM_LEAD"->"团队长：自建任务须直属上级审核，可审核直属下级自建任务；可查看团队下属；查看直属上级须申请并获批准。"
        "LEADER"->"当前连接旧版演示服务，权限以该服务为准。"
        else->"员工：可给自己建任务，经直属上级审核后执行；记录工作并处理分配任务；查看直属上级须申请并获批准。"
    }
    details?.let {section->AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={details=null},title={Text(if(section=="access")"账号与权限" else "当前服务")},text={Text(if(section=="access")"$permissionText\n\n资料与组织关系由管理员在 Web 维护；组织查看与申请从职能入口进入。" else "${me.server}\n\n工作记录保存在此服务。无法连接时，请检查网络或联系管理员。")},confirmButton={ControlTextButton(onClick={details=null}) {Text("关闭")}})}
    if(avatarPicker)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={avatarPicker=false},title={Text("选择头像")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        avatars.chunked(5).forEach {row-> Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            row.forEach {emoji-> Surface(shape=androidx.compose.foundation.shape.CircleShape,color=if(state.profile?.avatar==emoji)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.size(48.dp).clickable(enabled=!state.saving){model.changeAvatar(emoji);avatarPicker=false}) {Box(contentAlignment=Alignment.Center){Text(emoji,style=MaterialTheme.typography.titleLarge)}}
        }}
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        ControlTextButton(onClick={pickImage.launch("image/*")},enabled=!state.saving) {Text("从相册上传图片")}
        Text("可选系统头像，或从相册上传方形照片（会自动压缩）。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }},confirmButton={ControlTextButton(onClick={avatarPicker=false}) {Text("关闭")}})
    if(passwordDialog)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={if(!state.saving)passwordDialog=false},title={Text("修改密码")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value=currentPw,onValueChange={currentPw=it},enabled=!state.saving,label={Text("原密码")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=newPw,onValueChange={newPw=it.take(72)},enabled=!state.saving,label={Text("新密码（6-72 位）")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        Text("忘记密码请联系管理员在 Web 重置。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }},confirmButton={ControlTextButton(onClick={model.changePassword(currentPw,newPw){passwordDialog=false;currentPw="";newPw=""}},enabled=!state.saving && currentPw.isNotBlank() && newPw.length in 6..72) {Text(if(state.saving)"提交中…" else "确认修改")}},dismissButton={ControlTextButton(onClick={passwordDialog=false},enabled=!state.saving) {Text("取消")}})
    if(leaving)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={leaving=false},title={Text(if(state.developmentPreview)"退出开发预览？" else "退出当前账号？")},text={Text(if(state.developmentPreview)"预览没有登录业务账号，也未向服务器提交数据。" else "工作记录已保存在当前服务。未提交输入会保留在这台设备，再次登录原账号可继续。")},confirmButton={ControlTextButton(onClick={leaving=false;model.logout()}) {Text("退出登录")}},dismissButton={ControlTextButton(onClick={leaving=false}) {Text("取消")}})
    Column(Modifier.fillMaxSize()) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(top=12.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        val profile=state.profile
        val avatarImage=remember(state.profile?.avatarImage){decodeAvatarImage(state.profile?.avatarImage)}
        Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Surface(shape=androidx.compose.foundation.shape.CircleShape,color=MaterialTheme.colorScheme.surfaceVariant,modifier=Modifier.size(56.dp).clickable(enabled=!state.saving && !state.developmentPreview){avatarPicker=true}) {
                Box(contentAlignment=androidx.compose.ui.Alignment.Center,modifier=Modifier.fillMaxSize()) {
                    if(avatarImage!=null) Image(bitmap=avatarImage,contentDescription="头像",contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape))
                    else Text(profile?.avatar?.takeIf {it.isNotBlank()} ?: (me.displayName ?: me.username).take(1),style=MaterialTheme.typography.titleLarge)
                }
            }
            Column(verticalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.weight(1f)) {
                Text(profile?.displayName ?: me.displayName ?: me.username,style=MaterialTheme.typography.titleMedium)
                Text("手机号：${profile?.phone?.takeIf {it.isNotBlank()} ?: "未填写"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                val manager=profile?.managerName
                val managerText=when {profile?.managerId==null->"未设置";manager.isNullOrBlank()->"#${profile.managerId}";else->"$manager（#${profile.managerId}）"}
                Text("直属上级：$managerText",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        Text("个人资料",style=MaterialTheme.typography.titleSmall,modifier=Modifier.padding(top=12.dp,bottom=8.dp))
        if(state.profileLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
        state.profileError?.let {Hint(it,true,onRetry=model::refresh)}
        if(state.developmentPreview)Text("登录业务账号后可查看个人资料",style=MaterialTheme.typography.bodySmall)
        state.profile?.let {profile->
            listOf("姓名" to profile.displayName,"手机号" to profile.phone,"身份证号" to profile.idCard,
                "出生日期" to profile.birthDate,"性别" to profile.gender,"入职日期" to profile.hireDate,
                "紧急联系人" to profile.emergencyContact,"紧急联系电话" to profile.emergencyPhone,"联系地址" to profile.address,
                "直属上级编号" to profile.managerId?.toString()).forEach {(label,value)->
                Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.width(100.dp))
                    Text(value?.takeIf {it.isNotBlank()} ?: "未填写",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f))
                }
            }
            Text("资料由管理员维护，如需更正请联系管理员。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp))
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
            FeedbackRow("按键声音","跟随媒体音量；静音/振动模式不播放",settings.sound,settings.setSound)
            ControlTextButton(onClick={},enabled=settings.sound || settings.haptic) {Text("试听声音与触感")}
            Text("听不到声音时，请调高媒体音量并检查蓝牙输出；没有触感时，请开启系统触摸反馈。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        ControlTextButton(onClick={details="access"},enabled=!state.saving,contentPadding=PaddingValues(0.dp),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface)) {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("账号与权限",style=MaterialTheme.typography.bodyMedium)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        ControlTextButton(onClick={currentPw="";newPw="";passwordDialog=true},enabled=!state.saving && !state.developmentPreview,contentPadding=PaddingValues(0.dp),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface)) {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("修改密码",style=MaterialTheme.typography.bodyMedium)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        ControlTextButton(onClick={details="service"},enabled=!state.saving,contentPadding=PaddingValues(0.dp),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface)) {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("当前服务",style=MaterialTheme.typography.bodyMedium)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,null,modifier=Modifier.size(20.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
    }
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha=0.92f)).padding(horizontal=24.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text("潘多拉",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        ControlTextButton(onClick={leaving=true},enabled=!state.saving,modifier=Modifier.heightIn(min=48.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurfaceVariant)) {Text(if(state.developmentPreview)"退出预览" else "退出登录",style=MaterialTheme.typography.labelMedium)}
    }
    }
}

@Composable private fun FeedbackRow(title: String,detail: String,value: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).toggleable(value=value,role=Role.Switch,onValueChange=onChange),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {Text(title,style=MaterialTheme.typography.bodyMedium);Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Switch(checked=value,onCheckedChange=null,colors=SwitchDefaults.colors(checkedTrackColor=MaterialTheme.colorScheme.secondary,checkedThumbColor=MaterialTheme.colorScheme.background),modifier=Modifier.scale(0.75f))
    }
}
