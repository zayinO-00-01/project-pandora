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
    val originalPhone=rememberSaveable {profile.phone.orEmpty().trim()}
    var phone by rememberSaveable {mutableStateOf(originalPhone)}
    val changingPhone=phone.trim()!=originalPhone
    val invalidPhone=changingPhone && !validAccountPhone(phone.trim())
    val originalContact=rememberSaveable {profile.emergencyContact.orEmpty().trim()}
    val initialContact=profileContactName(originalContact).orEmpty()
    var contact by rememberSaveable {mutableStateOf(initialContact)}
    val formerEmergencyPhone=rememberSaveable {profile.emergencyPhone.orEmpty().trim()}
    var emergencyPhone by rememberSaveable {mutableStateOf(formerEmergencyPhone)}
    val currentEmergencyPhone=emergencyPhone.trim()
    val invalidEmergencyPhone=currentEmergencyPhone.isNotEmpty() && currentEmergencyPhone!=formerEmergencyPhone &&
        !validAccountPhone(currentEmergencyPhone)
    var address by rememberSaveable {mutableStateOf(profileAddress(profile.address).orEmpty())}
    AlertDialog(onDismissRequest={if(!state.saving)onClose()},title={Text("编辑本人档案")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(profile.displayName?.takeIf {it.isNotBlank()} ?: "未填写",{},label={Text("姓名")},
                supportingText={Text("由管理员维护")},singleLine=true,readOnly=true)
            OutlinedTextField(phone,{phone=accountPhoneInput(it)},label={Text("账号手机号")},
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),isError=invalidPhone,
                supportingText=if(invalidPhone) {{Text("请输入有效的 11 位手机号")}} else null,
                singleLine=true,enabled=!state.saving)
            OutlinedTextField(contact,{contact=emergencyContactInput(it)},label={Text("紧急联系人")},singleLine=true,enabled=!state.saving)
            OutlinedTextField(emergencyPhone,{emergencyPhone=accountPhoneInput(it)},label={Text("紧急联系电话")},singleLine=true,enabled=!state.saving,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),isError=invalidEmergencyPhone,
                supportingText={Text(if(invalidEmergencyPhone)"请输入有效的 11 位手机号" else "选填：11 位手机号")})
            OutlinedTextField(address,{address=it.take(255)},label={Text("联系地址")},enabled=!state.saving)
            state.profileError?.let {Hint(it,true)}
        }
    },confirmButton={ControlTextButton(onClick={
        // Saving an unrelated change should not silently clear a legacy demo value.
        val savedContact=if(contact==initialContact)originalContact else contact.trim()
        model.updateOwnProfile(OwnProfileInput(savedContact,emergencyPhone.trim(),address.trim(),phone.trim().takeIf {changingPhone}),onClose)
    },enabled=!state.saving && !invalidPhone && !invalidEmergencyPhone) {Text("保存")}},dismissButton={ControlTextButton(onClick=onClose,enabled=!state.saving) {Text("取消")}})
}
