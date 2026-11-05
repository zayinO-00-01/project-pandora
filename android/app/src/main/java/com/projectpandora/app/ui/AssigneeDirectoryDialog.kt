package com.projectpandora.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.data.*
@Composable fun AssigneeDirectoryDialog(state: AppState,selected: List<Long>,toggle: (Long)->Unit,refresh: ()->Unit,onDismiss: ()->Unit) {
 var query by remember {mutableStateOf("")}
 val contacts=state.directory.filter {it.role in AppRoles.business && (query.isBlank() || listOfNotNull(it.displayName,it.username,it.phone).any {v->v.contains(query.trim(),true)})}.distinctBy {it.id}
 AlertDialog(containerColor=MaterialTheme.colorScheme.surface,onDismissRequest=onDismiss,title={Text("从通讯录选择责任人")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  OutlinedTextField(query,{query=it},label={Text("搜索姓名、账号或电话")},singleLine=true,modifier=Modifier.fillMaxWidth())
  Text("已选 ${selected.size} 人",style=MaterialTheme.typography.labelSmall)
  if(state.dispatchLoading)LinearProgressIndicator(Modifier.fillMaxWidth())
  state.dispatchError?.let {Hint(it,true)}
  LazyColumn(Modifier.heightIn(max=340.dp)) {
   if(contacts.isEmpty() && !state.dispatchLoading)item {Text("没有匹配的通讯录成员")}
   contacts.groupBy {it.managerId}.forEach {(_,people)->
    item {Text(people.first().managerName?.let {"$it 的团队"} ?: "组织负责人",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary,modifier=Modifier.padding(vertical=8.dp))}
    items(people,key={it.id}) {person->Surface(onClick={toggle(person.id)},enabled=!state.saving && (person.id in selected || selected.size<100),color=MaterialTheme.colorScheme.surface) {
     Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      Checkbox(checked=person.id in selected,onCheckedChange=null)
      ContactAvatar(person,32)
      Column(Modifier.weight(1f)) {Text(person.displayName ?: person.username);Text(AppRoles.label(person.role),style=MaterialTheme.typography.labelSmall)}
     }
    }}
   }
  }
 }},confirmButton={ControlTextButton(onClick=onDismiss){Text("完成选择")}},dismissButton={ControlTextButton(onClick=refresh,enabled=!state.dispatchLoading){Text("刷新通讯录")}})
}
