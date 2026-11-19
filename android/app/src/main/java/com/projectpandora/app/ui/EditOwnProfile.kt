package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.OwnProfileInput
import com.projectpandora.app.data.Profile

@Composable internal fun EditOwnProfile(profile: Profile,state: AppState,model: PandoraViewModel,onClose: ()->Unit) {
    var name by rememberSaveable {mutableStateOf(profile.displayName.orEmpty())}
    var phone by rememberSaveable {mutableStateOf(profile.phone.orEmpty())}
    var contact by rememberSaveable {mutableStateOf(profile.emergencyContact.orEmpty())}
    var emergencyPhone by rememberSaveable {mutableStateOf(profile.emergencyPhone.orEmpty())}
    var address by rememberSaveable {mutableStateOf(profile.address.orEmpty())}
    AlertDialog(onDismissRequest={if(!state.saving)onClose()},title={Text("编辑本人档案")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name,{name=it.take(64)},label={Text("姓名")},singleLine=true,enabled=!state.saving)
            OutlinedTextField(phone,{phone=it.take(20)},label={Text("联系电话")},singleLine=true,enabled=!state.saving)
            OutlinedTextField(contact,{contact=it.take(64)},label={Text("紧急联系人")},singleLine=true,enabled=!state.saving)
            OutlinedTextField(emergencyPhone,{emergencyPhone=it.take(20)},label={Text("紧急联系电话")},singleLine=true,enabled=!state.saving)
            OutlinedTextField(address,{address=it.take(255)},label={Text("联系地址")},enabled=!state.saving)
            state.profileError?.let {Hint(it,true)}
        }
    },confirmButton={ControlTextButton(onClick={model.updateOwnProfile(OwnProfileInput(name.trim(),phone.trim(),contact.trim(),emergencyPhone.trim(),address.trim()),onClose)},enabled=!state.saving && name.isNotBlank()) {Text("保存")}},dismissButton={ControlTextButton(onClick=onClose,enabled=!state.saving) {Text("取消")}})
}
