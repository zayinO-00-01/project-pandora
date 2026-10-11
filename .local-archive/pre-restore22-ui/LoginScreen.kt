package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import com.projectpandora.app.R
import com.projectpandora.app.AppState
import com.projectpandora.app.BuildConfig

@Composable fun LoginScreen(state: AppState, onLogin: (String,String,String)->Unit, onDevelopmentEntry: ()->Unit) {
    var server by rememberSaveable(state.server) {mutableStateOf(state.server)}
    var username by rememberSaveable(state.username) {mutableStateOf(state.username)}
    // A password is intentionally not saveable or persisted.
    var password by remember {mutableStateOf("")}
    var visible by remember {mutableStateOf(false)}
    var connection by rememberSaveable {mutableStateOf(state.server.isBlank())}
    LandingJourney {
    Column(Modifier.widthIn(max=528.dp).fillMaxWidth().padding(horizontal=24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.widthIn(max=480.dp).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text("进入工作空间",style=MaterialTheme.typography.titleLarge)
            Text("使用公司分配的账号登录。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.widthIn(max=480.dp).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={connection=!connection},enabled=!state.busyLogin,modifier=Modifier.heightIn(min=48.dp),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurfaceVariant)) {Text(if(connection)"收起连接设置" else "连接设置")}
        if(connection) {
            OutlinedTextField(value=server,onValueChange={server=it},label={Text("服务器地址")},placeholder={Text("https://公司服务地址")},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!state.busyLogin)
            Text("本地调试：真机填写电脑 IPv4；官方模拟器可用 http://10.0.2.2:8080。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(value=username,onValueChange={username=it},label={Text("账号")},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!state.busyLogin)
        OutlinedTextField(value=password,onValueChange={password=it},label={Text("密码")},visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!state.busyLogin,
            trailingIcon={TextButton(onClick={visible=!visible},enabled=!state.busyLogin) {Text(if(visible)"隐藏" else "显示")}})
        state.authError?.let {Hint(it,true)}
        ControlButton(onClick={onLogin(server,username,password)},enabled=!state.busyLogin,modifier=Modifier.fillMaxWidth()) {
            if(state.busyLogin)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimary)
            else Text("进入工作空间")
        }
        HorizontalDivider()
        if(BuildConfig.DEBUG) {
            ControlOutlinedButton(onClick=onDevelopmentEntry,enabled=!state.busyLogin,modifier=Modifier.fillMaxWidth()) {Text("开发直接进入")}
            Text("无需登录即可预览页面与编辑布局；真实数据与提交操作需要登录。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("账号由管理员创建，无自助注册。管理员账号请使用 Web 管理端。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    }
}
