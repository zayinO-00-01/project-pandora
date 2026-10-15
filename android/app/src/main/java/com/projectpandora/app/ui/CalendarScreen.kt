package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*
import java.time.LocalDate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.text.font.FontWeight

@Composable fun CalendarScreen(state: AppState,model: PandoraViewModel) {
    var showHelp by remember {mutableStateOf(false)}
    val selected=LocalDate.parse(state.selectedDate)
    val mode=state.calendarMode
    val dates=periodDates(selected,mode)
    val agendaDates=if(mode=="week")dates else listOf(selected)
    val dateKeys=agendaDates.map {it.toString()}.toSet()
    fun adjacent(step: Long)=when(mode) {"month"->selected.plusMonths(step);"week"->selected.plusWeeks(step);else->selected.plusDays(step)}
    val unit=when(mode) {"month"->"月";"week"->"周";else->"天"}
    val heading=if(mode=="week")"${dates.first().monthValue}月${dates.first().dayOfMonth}日 — ${dates.last().monthValue}月${dates.last().dayOfMonth}日"
        else if(mode=="day")"${selected.year}年${selected.monthValue}月${selected.dayOfMonth}日" else "${selected.year} 年 ${selected.monthValue} 月"
    val feedback=LocalControlFeedback.current
    val logs=state.logs.filter {it.logDate in dateKeys}
    val tasks=state.tasks.filter {taskDate(it) in agendaDates}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(4.dp),contentPadding=PaddingValues(bottom=32.dp)) {
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("month" to "月","week" to "周","day" to "日").forEach {(value,label)->
                FilterChip(selected=mode==value,onClick={model.calendarMode(value)},enabled=!state.saving,
                    label={Text(label)},modifier=Modifier.weight(1f).heightIn(min=48.dp))
            }
        }}
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Text(heading,modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.Medium,fontFeatureSettings="tnum"))
            IconButton(onClick={feedback();model.selectDate(adjacent(-1).toString())},enabled=!state.saving,modifier=Modifier.background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small)) {Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft,"上一$unit",modifier=Modifier.size(20.dp))}
            TextButton(onClick={feedback();model.selectDate(today().toString())},modifier=Modifier.widthIn(min=48.dp).background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text("今天",style=MaterialTheme.typography.labelMedium)}
            IconButton(onClick={feedback();model.selectDate(adjacent(1).toString())},enabled=!state.saving,modifier=Modifier.background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small)) {Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"下一$unit",modifier=Modifier.size(20.dp))}
        }}
        if(mode!="day")item {Column {
            Row {listOf("一","二","三","四","五","六","日").forEach {Text(it,modifier=Modifier.weight(1f).padding(vertical=8.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Medium),color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            val padded=List(dates.first().dayOfWeek.value-1) {null as LocalDate?}+dates
            padded.chunked(7).forEach {week->Row {
                repeat(7) {index->
                    val date=week.getOrNull(index)
                    if(date==null)Spacer(Modifier.weight(1f))
                    else {
                        val count=state.logs.count {it.logDate==date.toString()}+state.tasks.count {taskDate(it)==date}
                        TextButton(onClick={feedback();model.selectDate(date.toString())},modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics {this.selected=date==selected;contentDescription="$date，$count 项记录"},shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(0.dp),
                            colors=ButtonDefaults.textButtonColors(containerColor=if(date==selected)MaterialTheme.colorScheme.secondary.copy(alpha=0.12f) else androidx.compose.ui.graphics.Color.Transparent,contentColor=if(date==selected)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)) {
                            Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                CalendarDateNumber(date,state)
                                Surface(color=if(date==selected)MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,modifier=Modifier.width(16.dp).height(2.dp)) {}
                            }
                        }
                    }
                }
            }}
        }}
        item {HorizontalDivider(Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.outlineVariant)}
        item {Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if(mode=="week")"本周安排" else "${selected.monthValue} 月 ${selected.dayOfMonth} 日",style=MaterialTheme.typography.titleMedium)
                Text("${logs.size} 条日志 · ${tasks.size} 项任务",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick={showHelp=true}) {Text("说明",style=MaterialTheme.typography.labelMedium)}
        }}
        if(state.loading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        state.logError?.let {item {Hint("日志读取失败：$it。请同步后重试。",true)}}
        state.taskError?.let {item {Hint("任务读取失败：$it。请同步后重试。",true)}}
        if(logs.isEmpty() && tasks.isEmpty() && !state.loading && state.logError==null && state.taskError==null)item {
            Column(Modifier.fillMaxWidth().padding(vertical=24.dp)) {
                Text(if(mode=="week")"这一周暂无记录" else "这一天暂无记录",style=MaterialTheme.typography.titleLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Medium))
                Spacer(Modifier.height(8.dp))
                Text("记录日志或设置任务截止时间后，会显示在对应日期。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        agendaDates.forEach {date->
            val dayLogs=logs.filter {it.logDate==date.toString()}
            val dayTasks=tasks.filter {taskDate(it)==date}
            if(mode=="week" && (dayLogs.isNotEmpty() || dayTasks.isNotEmpty()))item(key="date-$date") {
                Text("${date.monthValue}月${date.dayOfMonth}日 · ${listOf("周一","周二","周三","周四","周五","周六","周日")[date.dayOfWeek.value-1]}",
                    modifier=Modifier.padding(top=16.dp,bottom=4.dp),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.secondary)
            }
            val events=dayLogs.map {Triple(it.updatedAt,it,null as DispatchTask?)}+dayTasks.map {Triple(it.dueAt.orEmpty(),null as WorkLog?,it)}
            events.sortedBy {it.first}.forEach {event->
                val log=event.second
                val task=event.third
                item(key=if(log!=null)"log-${log.id}" else "task-${task!!.id}") {
                    AgendaEntry(event.first,log,task,!state.saving) {feedback();if(log!=null)model.edit(log) else model.openTask(task!!.id)}
                }
            }
        }
    }
    if(showHelp)AlertDialog(onDismissRequest={showHelp=false},title={Text("显示说明")},text={Text("月视图点选日期看当天记录；周视图列出周一至周日的安排；日视图聚焦一天。圆点表示已有日志或到期任务。没有截止日期的任务请在任务列表查看。")},confirmButton={TextButton(onClick={showHelp=false}) {Text("关闭")}})
}
@Composable private fun CalendarDateNumber(date: LocalDate,state: AppState) {
    val hasContent=state.logs.any {it.logDate==date.toString()} || state.tasks.any {taskDate(it)==date}
    Column(Modifier.width(32.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text(date.dayOfMonth.toString(),style=MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings="tnum",fontSize=18.sp,lineHeight=24.sp,fontWeight=if(date==today())FontWeight.SemiBold else FontWeight.Normal))
        Row(Modifier.height(4.dp),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            if(hasContent)Surface(color=MaterialTheme.colorScheme.onSurfaceVariant,shape=androidx.compose.foundation.shape.CircleShape,modifier=Modifier.size(3.dp)) {}
        }
    }
}

@Composable private fun AgendaEntry(time: String,log: WorkLog?,task: DispatchTask?,enabled: Boolean,onClick: ()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled=enabled,onClick=onClick).padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.width(44.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(timestamp(time).substringAfter(' '),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurface,modifier=Modifier.semantics {contentDescription="${if(log!=null)"更新时间" else "截止时间"} ${timestamp(time)}"})
            Text(if(log!=null)"日志" else "任务",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(if(log!=null)if(log.status=="draft")"草稿" else "已提交" else taskFeedbackLabel(task!!),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(log?.content?.let {recordPreview(it).first} ?: task!!.title,style=MaterialTheme.typography.bodyLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Medium,lineBreak=androidx.compose.ui.text.style.LineBreak.Heading),maxLines=2,overflow=TextOverflow.Ellipsis)
        }
    }
}
