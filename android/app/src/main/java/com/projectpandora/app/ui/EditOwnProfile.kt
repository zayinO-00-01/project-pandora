package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.OwnProfileInput
import com.projectpandora.app.data.Profile

@Composable internal fun EditOwnProfile(profile: Profile,state: AppState,model: PandoraViewModel,onClose: ()->Unit) {
    var name by rememberSaveable {mutableStateOf(profile.displayName.orEmpty())}
    val originalPhone=rememberSaveable {profile.phone.orEmpty().trim()}
    var phone by rememberSaveable {mutableStateOf(originalPhone)}
    val changingPhone=phone.trim()!=originalPhone
    val invalidPhone=changingPhone && !Regex("1[3-9][0-9]{9}").matches(phone.trim())
    var contact by rememberSaveable {mutableStateOf(profile.emergencyContact.orEmpty())}
    var emergencyPhone by rememberSaveable {mutableStateOf(profile.emergencyPhone.orEmpty())}
    var address by rememberSaveable {mutableStateOf(profile.address.orEmpty())}
    AlertDialog(onDismissRequest={if(!state.saving)onClose()},title={Text("编辑本人档案")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name,{name=it.take(64)},label={Text("姓名")},singleLine=true,enabled=!state.saving)
            OutlinedTextField(phone,{phone=it.take(20)},label={Text("账号手机号")},
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),isError=invalidPhone,
                supportingText=if(invalidPhone) {{Text("请输入有效的 11 位手机号")}} else null,
                singleLine=true,enabled=!state.saving)
            OutlinedTextField(contact,{contact=it.take(64)},label={Text("紧急联系人")},singleLine=true,enabled=!state.saving)
            OutlinedTextField(emergencyPhone,{emergencyPhone=it.take(20)},label={Text("紧急联系电话")},singleLine=true,enabled=!state.saving)
            OutlinedTextField(address,{address=it.take(255)},label={Text("联系地址")},enabled=!state.saving)
            state.profileError?.let {Hint(it,true)}
        }
    },confirmButton={ControlTextButton(onClick={model.updateOwnProfile(OwnProfileInput(name.trim(),contact.trim(),emergencyPhone.trim(),address.trim(),phone.trim().takeIf {changingPhone}),onClose)},enabled=!state.saving && name.isNotBlank() && !invalidPhone) {Text("保存")}},dismissButton={ControlTextButton(onClick=onClose,enabled=!state.saving) {Text("取消")}})
}
