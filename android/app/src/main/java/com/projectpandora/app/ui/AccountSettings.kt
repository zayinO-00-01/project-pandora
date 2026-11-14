package com.projectpandora.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import android.util.Base64
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel

@Composable fun ProfileAvatar(data: String?) {
    val image=remember(data) {runCatching {data?.substringAfter(',')?.let {Base64.decode(it,Base64.DEFAULT)}?.let {BitmapFactory.decodeByteArray(it,0,it.size)}?.asImageBitmap()}.getOrNull()}
    if(image==null)Icon(Icons.Outlined.Person,"默认头像",modifier=Modifier.size(48.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
    else Image(image,"我的头像",modifier=Modifier.size(48.dp).clip(CircleShape))
}

@Composable fun AccountSettings(state: AppState,model: PandoraViewModel,onAvatar: ()->Unit) {
    var passwordOpen by remember {mutableStateOf(false)}
    val enabled=!state.saving && !state.developmentPreview
    ProfileSection("账号安全") {
        ProfileEntry("更换头像",enabled,onAvatar)
        if(state.myProfile?.avatarData!=null)ProfileEntry("移除头像",enabled) {model.changeAvatar(null)}
        ProfileEntry("修改密码",enabled) {passwordOpen=true}
    }
    ProfileSection("工作入口") {
        ProfileEntry("通知与审核 · "+state.unreadCount+" 条未读",!state.saving) {model.screen("inbox")}
        ProfileEntry(if(state.session?.role=="FOUNDER")"维护品牌标签" else "查看品牌标签",!state.saving) {model.screen("labels")}
    }
    if(passwordOpen)PasswordDialog(state,model) {passwordOpen=false}
}

@Composable private fun PasswordDialog(state: AppState,model: PandoraViewModel,onClose: ()->Unit) {
    // Passwords are deliberately never saved to disk or restored after process death.
    var current by remember {mutableStateOf("")}
    var new by remember {mutableStateOf("")}
    var confirm by remember {mutableStateOf("")}
    val valid=new.length>=8 && new.toByteArray(Charsets.UTF_8).size<=72 && current.isNotEmpty() && new==confirm
    AlertDialog(modifier=Modifier.imePadding().panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={if(!state.saving)onClose()},title={Text("修改密码")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("修改成功后，所有设备需要使用新密码重新登录。")
        OutlinedTextField(value=current,onValueChange={current=it},label={Text("当前密码")},singleLine=true,visualTransformation=PasswordVisualTransformation(),enabled=!state.saving)
        OutlinedTextField(value=new,onValueChange={new=it},label={Text("新密码（至少 8 位）")},singleLine=true,visualTransformation=PasswordVisualTransformation(),enabled=!state.saving)
        OutlinedTextField(value=confirm,onValueChange={confirm=it},label={Text("确认新密码")},singleLine=true,visualTransformation=PasswordVisualTransformation(),enabled=!state.saving,isError=confirm.isNotEmpty() && confirm!=new)
        state.profileError?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={ControlTextButton(onClick={model.changePassword(current,new)},enabled=valid && !state.saving) {Text(if(state.saving)"保存中…" else "确认修改")}},dismissButton={ControlTextButton(onClick=onClose,enabled=!state.saving) {Text("取消")}})
}
