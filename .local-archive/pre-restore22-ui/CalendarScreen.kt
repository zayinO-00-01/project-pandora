package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable fun CalendarScreen(state: AppState,model: PandoraViewModel) {
    var mode by rememberSaveable {mutableStateOf("month")}
    var modeMenu by remember {mutableStateOf(false)}
    var showHelp by remember {mutableStateOf(false)}
    val selected=LocalDate.parse(state.selectedDate)
    val dates=periodDates(selected,"month")
    val weekDates=periodDates(selected,"week")
    val agendaDates=if(mode=="week")weekDates else listOf(selected)
    val feedback=LocalControlFeedback.current
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(4.dp),contentPadding=PaddingValues(top=0.dp,bottom=32.dp)) {
        stickyHeader {Surface(color=MaterialTheme.colorScheme.background) {Row(Modifier.fillMaxWidth().height(56.dp * maxOf(1f,LocalDensity.current.fontScale)),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                Column(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable {modeMenu=true},verticalArrangement=Arrangement.Center) {
                    Text("${selected.year}${if(state.developmentPreview)" · 本地预览" else ""}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(selected.monthValue.toString(),style=MaterialTheme.typography.titleLarge.copy(fontFamily=androidx.compose.ui.text.font.FontFamily.Serif,fontSize=24.sp,lineHeight=28.sp))
                        Text("月",style=MaterialTheme.typography.labelMedium)
                        Text("${when(mode){"day"->"日";"week"->"周";else->"月"}} ▾",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
                    }
                }
                DropdownMenu(expanded=modeMenu,onDismissRequest={modeMenu=false}) {
                    listOf("day" to "日视图","week" to "周视图","month" to "月视图").forEach {(key,label)->DropdownMenuItem(text={Text(label)},modifier=Modifier.semantics {this.selected=mode==key},onClick={feedback();mode=key;modeMenu=false})}
                    HorizontalDivider()
                    DropdownMenuItem(text={Text("同步")},enabled=!state.loading && !state.saving,onClick={modeMenu=false;feedback();model.refresh()})
                    DropdownMenuItem(text={Text("显示说明")},onClick={modeMenu=false;showHelp=true})
                }
            }
            IconButton(onClick={model.selectDate(shiftPeriod(selected,"month",-1).toString())}) {Text("‹",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics {contentDescription="上一${periodLabel("month")}"})}
            TextButton(onClick={model.selectDate(today().toString())},modifier=Modifier.width(48.dp),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurfaceVariant)) {Text("今天",style=MaterialTheme.typography.labelMedium)}
            IconButton(onClick={model.selectDate(shiftPeriod(selected,"month",1).toString())}) {Text("›",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics {contentDescription="下一${periodLabel("month")}"})}
        }}}
        if(state.loading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        state.logError?.let {item {Hint("日志读取失败：$it。请同步后重试。",true)}}
        state.taskError?.let {item {Hint("任务读取失败：$it。请同步后重试。",true)}}
        item {Surface(color=MaterialTheme.colorScheme.background,modifier=Modifier.fillMaxWidth()) {Column {
            Row {listOf("一","二","三","四","五","六","日").forEach {Text(it,modifier=Modifier.weight(1f).padding(vertical=2.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            val padded=List(dates.first().dayOfWeek.value-1) {null as LocalDate?}+dates
            padded.chunked(7).forEach {week->Row {
                repeat(7) {index->
                    val date=week.getOrNull(index)
                    if(date==null)Spacer(Modifier.weight(1f))
                    else {
                        val count=state.logs.count {it.logDate==date.toString()}+state.tasks.count {taskDate(it)==date}
                        TextButton(onClick={feedback();model.selectDate(date.toString())},modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics {this.selected=if(mode=="week")date in weekDates else date==selected;contentDescription="$date，$count 项记录"},shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(0.dp),
                            colors=ButtonDefaults.textButtonColors(containerColor=if(mode=="day" && date==selected)MaterialTheme.colorScheme.secondaryContainer else if(mode=="week" && date in weekDates)MaterialTheme.colorScheme.secondary.copy(alpha=0.10f) else androidx.compose.ui.graphics.Color.Transparent,contentColor=if(date==selected)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)) {
                            Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                CalendarDateNumber(date,state,compact=true)
                                Surface(color=if(date==selected)MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,modifier=Modifier.width(16.dp).height(2.dp)) {}
                            }
                        }
                    }
                }
            }}
        }}}
        item {HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)}
        val logs=state.logs.filter {runCatching {LocalDate.parse(it.logDate) in agendaDates}.getOrDefault(false)}
        val tasks=state.tasks.filter {taskDate(it) in agendaDates}
        item {Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(if(mode=="week")"${weekDates.first().monthValue} 月 ${weekDates.first().dayOfMonth} — ${weekDates.last().monthValue} 月 ${weekDates.last().dayOfMonth} 日" else "${selected.monthValue} 月 ${selected.dayOfMonth} 日",style=MaterialTheme.typography.labelLarge)
                if(logs.isNotEmpty() || tasks.isNotEmpty())Text(listOfNotNull(logs.size.takeIf {it>0}?.let {"$it 条日志"},tasks.size.takeIf {it>0}?.let {"$it 项任务"}).joinToString(" · "),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            WriteLogButton(enabled=!state.saving && !state.checkingIntegrity,onClick={model.edit()})
        }}
        if(logs.isEmpty() && tasks.isEmpty() && !state.loading && state.logError==null && state.taskError==null)item {
            Column(Modifier.fillMaxWidth().padding(vertical=24.dp),verticalArrangement=Arrangement.Top) {
                Text(when(mode){"day"->"这一天暂无记录";"week"->"本周暂无记录";else->"这一天暂无记录"},style=MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("选择其他日期查看工作；写日志可记录今天的进展。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        agendaDates.forEach {date->
            val dayLogs=logs.filter {it.logDate==date.toString()}
            val dayTasks=tasks.filter {taskDate(it)==date}
            if(mode=="week" && (dayLogs.isNotEmpty() || dayTasks.isNotEmpty()))item(key="date-$date") {
                Row(Modifier.fillMaxWidth().padding(top=12.dp,bottom=4.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Text("${date.monthValue} 月 ${date.dayOfMonth} 日",modifier=Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                    Text(listOfNotNull(dayLogs.size.takeIf {it>0}?.let {"$it 条日志"},dayTasks.size.takeIf {it>0}?.let {"$it 项任务"}).joinToString(" · "),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
    if(showHelp)AlertDialog(onDismissRequest={showHelp=false},title={Text("显示说明")},text={Text("仅展示当前已同步的日志与任务。没有截止日期的任务可在任务列表查看。月历始终显示完整月份；日视图标出所选日期，周视图标出所选日期所在周，并显示对应记录。")},confirmButton={TextButton(onClick={showHelp=false}) {Text("关闭")}})
}
private fun periodLabel(mode: String)=when(mode) {"week"->"周";"month"->"月";else->"天"}
@Composable private fun CalendarDateNumber(date: LocalDate,state: AppState,compact: Boolean=false) {
    val hasContent=state.logs.any {it.logDate==date.toString()} || state.tasks.any {taskDate(it)==date}
    val dateColor=if(date.toString()==state.selectedDate)MaterialTheme.colorScheme.secondary else if(hasContent || date==today())MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.width(32.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text(date.dayOfMonth.toString(),style=MaterialTheme.typography.bodyLarge.copy(fontFamily=androidx.compose.ui.text.font.FontFamily.Serif,fontSize=if(compact)16.sp else 18.sp,lineHeight=if(compact)18.sp else 20.sp,fontWeight=if(date==today())androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal),color=dateColor)
        Row(Modifier.height(4.dp),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            if(hasContent)Surface(color=MaterialTheme.colorScheme.onSurfaceVariant,shape=androidx.compose.foundation.shape.CircleShape,modifier=Modifier.size(3.dp)) {}
        }
    }
}
private fun shiftPeriod(date: LocalDate,mode: String,step: Long)=when(mode) {"week"->date.plusWeeks(step);"month"->date.plusMonths(step);else->date.plusDays(step)}

@Composable private fun AgendaEntry(time: String,log: WorkLog?,task: DispatchTask?,enabled: Boolean,onClick: ()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled=enabled,onClick=onClick).padding(vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.width(44.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(timestamp(time).substringAfter(' '),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurface,modifier=Modifier.semantics {contentDescription="${if(log!=null)"更新时间" else "截止时间"} ${timestamp(time)}"})
            Text(if(log!=null)"日志" else "任务",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            if(task!=null)Text("${if(task.status=="done")"已完成" else if(task.status=="doing")"进行中" else "待开始"} · ${task.progress}%",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(log?.content?.let {recordPreview(it).first} ?: task!!.title,style=MaterialTheme.typography.bodyLarge.copy(fontSize=15.sp,lineHeight=24.sp,fontWeight=if(task!=null)androidx.compose.ui.text.font.FontWeight.Medium else androidx.compose.ui.text.font.FontWeight.Normal,lineBreak=androidx.compose.ui.text.style.LineBreak.Heading),maxLines=2,overflow=TextOverflow.Ellipsis)
            if(log?.status=="draft")Text("草稿 · 仅自己可见",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(task!=null && task.progress>0)LinearProgressIndicator(progress={task.progress.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth().height(2.dp),color=MaterialTheme.colorScheme.secondary,gapSize=0.dp,drawStopIndicator={})
        }
    }
}
