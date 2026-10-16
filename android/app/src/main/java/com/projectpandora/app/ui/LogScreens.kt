package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*
import java.time.LocalDate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@Composable fun LogCard(log: WorkLog, busy: Boolean, onEdit: ()->Unit,showDate: Boolean=true) {
    Surface(onClick=onEdit,enabled=!busy,color=androidx.compose.ui.graphics.Color.Transparent,modifier=Modifier.fillMaxWidth()) {Column(Modifier.padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            if(showDate)Text(log.logDate,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(timestamp(log.updatedAt).substringAfter(' '),style=MaterialTheme.typography.labelSmall,modifier=Modifier.semantics {contentDescription="更新时间 ${timestamp(log.updatedAt)}"},color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(log.status=="draft") {Spacer(Modifier.width(8.dp));Text("草稿 · 仅自己可见",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"查看日志",modifier=Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val (summary,detail)=recordPreview(log.content)
        Text(summary,style=MaterialTheme.typography.bodyLarge.copy(fontSize=15.sp,lineHeight=24.sp,fontWeight=FontWeight.Medium,lineBreak=androidx.compose.ui.text.style.LineBreak.Heading),maxLines=2,overflow=TextOverflow.Ellipsis)
        val nextStep=detail.indexOf("下一步")
        val preview=if(nextStep>=0)detail.substring(0,nextStep).trim() else detail
        if(preview.isNotEmpty())Text(preview,style=MaterialTheme.typography.bodySmall.copy(lineHeight=18.sp),maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(nextStep>=0)Text(detail.substring(nextStep).trim(),style=MaterialTheme.typography.bodySmall.copy(lineHeight=18.sp),maxLines=1,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)

    }}
}
@Composable fun LogsScreen(state: AppState, model: PandoraViewModel, dayOnly: Boolean=false) {
    var allDates by rememberSaveable {mutableStateOf(true)}
    val logs=if(!dayOnly && allDates)state.logs else state.logs.filter {it.logDate==state.selectedDate}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp),contentPadding=PaddingValues(top=4.dp,bottom=32.dp)) {
        item {Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                if(!dayOnly && allDates)Text("全部记录 · ${logs.size} 条",modifier=Modifier.padding(vertical=14.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                else DateButton(state.selectedDate,!state.saving) {allDates=false;model.selectDate(it)}
                if(!dayOnly)TextButton(onClick={allDates=!allDates},enabled=!state.saving,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text(if(allDates)"按日期筛选" else "全部记录",style=MaterialTheme.typography.labelMedium)}
            }
            if(dayOnly)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ControlOutlinedButton(onClick={model.selectDate(LocalDate.parse(state.selectedDate).minusDays(1).toString())}) {Text("← 前一天")}
                TextButton(onClick={model.selectDate(today().toString())}) {Text("回到今天")}
            }
            state.logError?.let {Hint("读取失败：$it\n下面可能是上次成功读取的记录，请刷新。",true)}
            if(state.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        }}
        if(logs.isEmpty() && !state.loading && state.logError==null)item {
            if(dayOnly)EmptyCard("还没有工作记录","换个日期看看，或去日志页记录工作。")
            else Column(Modifier.fillMaxWidth().padding(top=48.dp,bottom=32.dp),verticalArrangement=Arrangement.Top) {
                Text(if(allDates)"从第一条记录开始" else "这一天，还没有记录。",style=MaterialTheme.typography.titleLarge.copy(fontWeight=FontWeight.Medium))
                Spacer(Modifier.height(12.dp))
                Text("留下完成的事、遇到的问题与下一步。\n草稿仅自己可见。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val ordered=logs.sortedWith(compareByDescending<WorkLog> {it.logDate}.thenByDescending {it.updatedAt})
        ordered.groupBy {it.logDate.take(7)}.forEach {(month,monthLogs)->
            item(key="month-$month") {Text(month.replace("-"," / "),modifier=Modifier.padding(top=16.dp,bottom=4.dp),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            items(monthLogs.groupBy {it.logDate}.entries.toList(),key={it.key}) {(logDate,entries)->
                Row(Modifier.fillMaxWidth().padding(top=8.dp,bottom=16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.width(44.dp).padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        val date=runCatching {LocalDate.parse(logDate)}.getOrNull()
                        Text(date?.dayOfMonth?.toString() ?: "—",style=MaterialTheme.typography.headlineMedium.copy(fontSize=30.sp,lineHeight=34.sp),fontFamily=androidx.compose.ui.text.font.FontFamily.Serif,fontWeight=FontWeight.Normal)
                        Text(date?.let {listOf("周一","周二","周三","周四","周五","周六","周日")[it.dayOfWeek.value-1]} ?: logDate,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {entries.forEach {log->LogCard(log,state.saving,{model.edit(log)},showDate=false)}}
                }
            }
        }

    }
}
@Composable fun EditorScreen(state: AppState, model: PandoraViewModel) {
    val editor=state.editor ?: return
    val readOnly=logEditorReadOnly(editor,state.logs)
    var discard by remember {mutableStateOf(false)}
    BackHandler {if(!state.saving) {if(readOnly)model.discardEditor() else discard=true}}
    if(discard)AlertDialog(containerColor=MaterialTheme.colorScheme.surface,onDismissRequest={discard=false},title={Text("放弃这次修改？")},text={Text("未保存内容会被清除，服务器里的日志不会改变。")},confirmButton={TextButton(onClick={discard=false;model.discardEditor()}) {Text("放弃修改")}},dismissButton={TextButton(onClick={discard=false}) {Text("继续编辑")}})
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal=20.dp)) {
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(bottom=20.dp)) {
            item {ScreenHeading(if(readOnly)"查看工作日志" else if(editor.logId==null)"新建工作日志" else "编辑工作日志",if(readOnly)"此记录已超出修改期限，内容仅供查看。" else "可更新七天前至今天的日志。")}
            item {Text(if(readOnly)"历史日志完整保留。" else if(editor.status=="submitted")"保存后，领导刷新即可看到最新内容。" else "草稿仅自己可见，提交后领导才能查看。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            item {DateButton(editor.date,!state.saving && !readOnly,minDate=if(readOnly)null else today().minusDays(7)) {model.changeEditor(date=it)}}
            item {OutlinedTextField(value=editor.content,onValueChange={if(it.length<=5000)model.changeEditor(content=it)},readOnly=readOnly,label={Text("工作记录")},placeholder={Text("今天完成了什么？遇到哪些问题？下一步准备做什么？")},enabled=!state.saving,modifier=Modifier.fillMaxWidth().heightIn(min=260.dp),minLines=9,supportingText={Text("${editor.content.length} / 5000")})}
            state.editorError?.let {item {Hint(it,true)}}
        }
        Row(Modifier.fillMaxWidth().padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(readOnly)TextButton(onClick={model.discardEditor()},enabled=!state.saving) {Text("返回")}
            else {
            TextButton(onClick={discard=true},enabled=!state.saving) {Text("取消")}
            if(editor.status!="submitted")ControlOutlinedButton(onClick={model.save("draft")},enabled=!state.saving) {Text("存草稿")}
            ControlButton(onClick={model.save("submitted")},enabled=!state.saving,modifier=Modifier.weight(1f)) {Text(if(state.saving)"保存中…" else if(editor.status=="submitted")"保存修改" else "提交日志")}
            }
        }
    }
}
