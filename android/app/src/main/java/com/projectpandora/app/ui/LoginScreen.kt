package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectpandora.app.AppState

@Composable fun LoginScreen(state: AppState, onLogin: (String,String,String)->Unit) {
    var server by rememberSaveable(state.server) {mutableStateOf(state.server)}
    var username by rememberSaveable(state.username) {mutableStateOf(state.username)}
    // A password is intentionally not saveable or persisted.
    var password by remember {mutableStateOf("")}
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(26.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(25.dp))
        Text("P / PANDORA",fontSize=13.sp,color=MaterialTheme.colorScheme.primary)
        Text("让每一天的工作，\n都有迹可循。",style=MaterialTheme.typography.headlineMedium)
        Text("工作日志 · 团队同步",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value=server,onValueChange={server=it},label={Text("服务器地址")},placeholder={Text("http://电脑IP:8080")},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!state.busyLogin)
        Text("真机填写同一 Wi-Fi 下的电脑 IPv4 地址；官方模拟器可用 10.0.2.2。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value=username,onValueChange={username=it},label={Text("账号")},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!state.busyLogin)
        OutlinedTextField(value=password,onValueChange={password=it},label={Text("密码")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!state.busyLogin)
        state.authError?.let {Hint(it,true)}
        Button(onClick={onLogin(server,username,password)},enabled=!state.busyLogin,modifier=Modifier.fillMaxWidth().height(50.dp)) {
            if(state.busyLogin)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimary)
            else Text("登录并同步记录  →")
        }
        HorizontalDivider()
        Text("本地演示",style=MaterialTheme.typography.labelLarge)
        Text("账号 staff / leader / admin\n演示密码 demo1234",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("记录进展，看见团队正在发生的事。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.outline)
    }
}
