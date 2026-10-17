package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.projectpandora.app.R
import com.projectpandora.app.AppState
import com.projectpandora.app.BuildConfig

@Composable fun LoginScreen(state: AppState, onLogin: (String,String,String)->Unit, onDevelopmentEntry: ()->Unit,showBackground: Boolean=true,revealed: Boolean=true,revealDuration: Int=0) {
    val wordmark=painterResource(R.drawable.pandora_ink_wordmark)
    var server by rememberSaveable(state.server) {mutableStateOf(state.server)}
    var username by rememberSaveable(state.username) {mutableStateOf(state.username)}
    // A password is intentionally not saveable or persisted.
    var password by remember {mutableStateOf("")}
    var visible by remember {mutableStateOf(false)}
    var connection by rememberSaveable {mutableStateOf(false)}
    Box(Modifier.fillMaxSize()) {
    if(showBackground)Image(painterResource(R.drawable.paper_background_balanced),contentDescription=null,contentScale=ContentScale.FillBounds,modifier=Modifier.matchParentSize())
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(MaterialTheme.colorScheme.background))
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
    val viewportHeight=maxHeight
    Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),contentAlignment=Alignment.TopCenter) {
    Column(Modifier.heightIn(min=viewportHeight).widthIn(max=480.dp).fillMaxWidth().padding(26.dp),verticalArrangement=Arrangement.Center) {
    AnimatedVisibility(visible=revealed,enter=fadeIn(tween(revealDuration))) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Image(wordmark,contentDescription="潘多拉",contentScale=ContentScale.Fit,modifier=Modifier.align(Alignment.CenterHorizontally).width(196.dp).height(78.dp).padding(bottom=12.dp))
        ControlOutlinedButton(onClick={connection=!connection},enabled=!state.busyLogin,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(if(connection)"收起连接设置" else "连接设置")}
        if(connection) {
            OutlinedTextField(value=server,onValueChange={server=it},label={Text("服务器地址")},placeholder={Text("https://公司服务地址")},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!state.busyLogin,colors=OutlinedTextFieldDefaults.colors(focusedContainerColor=ConsoleTokens.Paper.copy(alpha=0.94f),unfocusedContainerColor=ConsoleTokens.Paper.copy(alpha=0.94f)))
        }
        OutlinedTextField(value=username,onValueChange={username=it},label={Text("账号")},modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!state.busyLogin,colors=OutlinedTextFieldDefaults.colors(focusedContainerColor=ConsoleTokens.Paper.copy(alpha=0.94f),unfocusedContainerColor=ConsoleTokens.Paper.copy(alpha=0.94f)))
        OutlinedTextField(value=password,onValueChange={password=it},label={Text("密码")},visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!state.busyLogin,colors=OutlinedTextFieldDefaults.colors(focusedContainerColor=ConsoleTokens.Paper.copy(alpha=0.94f),unfocusedContainerColor=ConsoleTokens.Paper.copy(alpha=0.94f)),
            trailingIcon={ControlTextButton(onClick={visible=!visible},enabled=!state.busyLogin) {Text(if(visible)"隐藏" else "显示")}})
        state.authError?.let {Hint(it,true)}
        ControlButton(onClick={onLogin(server,username,password)},enabled=!state.busyLogin,modifier=Modifier.fillMaxWidth()) {
            if(state.busyLogin)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimary)
            else Text("进入工作空间")
        }
    }
    }
    }
    }
    }
    }
}
