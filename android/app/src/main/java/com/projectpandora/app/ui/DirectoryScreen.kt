package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun DirectoryScreen(state: AppState,model: PandoraViewModel) {
    val me=state.session ?: return
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("directory-stars",0)}
    val key="${me.server}:${me.userId}"
    var stars by remember(key) {mutableStateOf(prefs.getStringSet(key,emptySet()).orEmpty().toSet())}
    var query by rememberSaveable(me.userId) {mutableStateOf("")}
    var section by rememberSaveable(me.userId) {mutableStateOf("home")}
    var selectedId by rememberSaveable(me.userId) {mutableStateOf<Long?>(null)}
    val selected=state.directory.firstOrNull {it.id==selectedId}
    fun back() {if(selectedId!=null)selectedId=null else if(section!="home")section="home" else model.screen("day")}
    BackHandler(onBack=::back)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            BackdropTextButton(onClick=::back) {Text(if(selectedId!=null)"← 返回联系人" else if(section!="home")"← 返回通讯录" else "← 返回视图")}
            BackdropTextButton(onClick=model::loadDirectory,enabled=!state.directoryLoading) {Text("刷新通讯录")}
        }}
        if(state.directoryLoading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        state.directoryError?.let {item {Hint("通讯录读取失败：$it",true,onRetry=model::loadDirectory)}}
        if(selected!=null) {
            item {Surface(shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth().panelOutline(MaterialTheme.shapes.large)) {Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                ContactAvatar(selected,72)
                Text(selected.displayName ?: selected.username,style=MaterialTheme.typography.headlineSmall)
                Text(AppRoles.label(selected.role),color=MaterialTheme.colorScheme.secondary)
                val starred=selected.id.toString() in stars
                ControlOutlinedButton(onClick={
                    stars=if(starred)stars-selected.id.toString() else stars+selected.id.toString()
                    prefs.edit().putStringSet(key,stars).apply()
                }) {Text(if(starred)"★ 已星标 · 取消星标" else "☆ 添加为星标联系人")}
                HorizontalDivider()
                ContactInfo("账号",selected.username)
                ContactInfo("职位",AppRoles.label(selected.role))
                ContactInfo("直属上级",selected.managerName)
                ContactInfo("工作联系电话",selected.phone)
            }}}
        } else {
            item {OutlinedTextField(query,{query=it},singleLine=true,label={Text("搜索姓名、账号或联系电话")},modifier=Modifier.fillMaxWidth())}
            if(section=="home" && query.isBlank()) {
                item {ScreenHeading("公司通讯录","${state.directory.size} 位组织内联系人")}
                item {DirectoryEntry("组织内联系人", "按直属上级分组 · ${state.directory.size} 位") {section="organization"}}
                item {DirectoryEntry("★ 星标联系人","${state.directory.count {it.id.toString() in stars}} 位常用联系人 · 仅保存在本机") {section="starred"}}
            } else {
                val people=state.directory.filter {person->
                    (section!="starred" || person.id.toString() in stars) &&
                    (query.isBlank() || listOfNotNull(person.displayName,person.username,person.phone).any {it.contains(query.trim(),true)})
                }
                item {Text(if(query.isNotBlank())"搜索结果 · ${people.size} 位" else if(section=="starred")"星标联系人 · ${people.size} 位" else "组织内联系人 · ${people.size} 位",style=MaterialTheme.typography.titleMedium)}
                if(people.isEmpty() && !state.directoryLoading)item {EmptyCard("暂无匹配的联系人","可以搜索姓名，或在联系人详情中添加星标。")}
                people.groupBy {it.managerName?.let {name->"$name 的团队"} ?: "组织负责人"}.forEach {(group,members)->
                    item {Text(group,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary,modifier=Modifier.padding(top=8.dp))}
                    items(members,key={it.id}) {person->Surface(onClick={selectedId=person.id},color=MaterialTheme.colorScheme.surface.copy(alpha=.9f),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            ContactAvatar(person,42)
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                Text(person.displayName ?: person.username,style=MaterialTheme.typography.titleSmall)
                                Text(AppRoles.label(person.role),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(if(person.id.toString() in stars)"★  ›" else "›",color=MaterialTheme.colorScheme.secondary)
                        }
                    }}
                }
            }
        }
    }
}
@Composable private fun DirectoryEntry(title: String,subtitle: String,onClick: ()->Unit) {
    Surface(onClick=onClick,color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
        Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Text("›",style=MaterialTheme.typography.titleLarge)
        }
    }
}
@Composable private fun ContactInfo(label: String,value: String?) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(label,modifier=Modifier.width(100.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value?.takeIf {it.isNotBlank()} ?: "未填写",style=MaterialTheme.typography.bodyMedium)
    }
}
@Composable internal fun ContactAvatar(contact: DirectoryContact,size: Int) {
    val photo=remember(contact.avatarData){decodeAvatarImage(contact.avatarData)}
    Surface(shape=CircleShape,color=MaterialTheme.colorScheme.secondaryContainer,modifier=Modifier.size(size.dp)) {
        Box(contentAlignment=Alignment.Center) {
            if(photo!=null)Image(photo,null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize().clip(CircleShape))
            else Text(contact.avatar?.takeIf {it.isNotBlank()} ?: (contact.displayName ?: contact.username).take(1),style=MaterialTheme.typography.titleLarge)
        }
    }
}
