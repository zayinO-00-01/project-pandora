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

@Composable fun CalendarScreen(state: AppState,model: PandoraViewModel) {
    var mode by rememberSaveable {mutableStateOf("day")}
    var modeMenu by remember {mutableStateOf(false)}
    var showHelp by remember {mutableStateOf(false)}
    val selected=LocalDate.parse(state.selectedDate)
    val dates=periodDates(selected,mode)
    val feedback=LocalControlFeedback.current
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(4.dp),contentPadding=PaddingValues(top=0.dp,bottom=32.dp)) {
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Row(Modifier.weight(1f),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("${selected.year}.${selected.monthValue.toString().padStart(2,'0')}",style=MaterialTheme.typography.labelMedium)
                Box {
                    TextButton(onClick={modeMenu=true},contentPadding=PaddingValues(horizontal=4.dp),modifier=Modifier.widthIn(min=48.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text("${when(mode){"day"->"日";"week"->"周";else->"月"}} ▾",style=MaterialTheme.typography.labelMedium)}
                    DropdownMenu(expanded=modeMenu,onDismissRequest={modeMenu=false}) {
                        listOf("day" to "日视图","week" to "周视图","month" to "月视图").forEach {(key,label)->DropdownMenuItem(text={Text(label)},modifier=Modifier.semantics {this.selected=mode==key},onClick={feedback();mode=key;modeMenu=false})}
                        HorizontalDivider()
                        DropdownMenuItem(text={Text("显示说明")},onClick={modeMenu=false;showHelp=true})
                    }
                }
            }
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                IconButton(onClick={model.selectDate(shiftPeriod(selected,mode,-1).toString())}) {Text("‹",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics {contentDescription="上一${periodLabel(mode)}"})}
                TextButton(onClick={model.selectDate(today().toString())},modifier=Modifier.width(48.dp),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurfaceVariant)) {Text("今天",style=MaterialTheme.typography.labelMedium)}
                IconButton(onClick={model.selectDate(shiftPeriod(selected,mode,1).toString())}) {Text("›",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.semantics {contentDescription="下一${periodLabel(mode)}"})}
            }
        }}
        if(mode=="day")item {Row(Modifier.fillMaxWidth()) {
            periodDates(selected,"week").forEach {date->TextButton(onClick={feedback();model.selectDate(date.toString())},modifier=Modifier.weight(1f).heightIn(min=56.dp).semantics {this.selected=date==selected;contentDescription="$date，${state.logs.count {it.logDate==date.toString()}} 条日志，${state.tasks.count {taskDate(it)==date}} 项任务"},contentPadding=PaddingValues(0.dp),shape=MaterialTheme.shapes.small,
                colors=ButtonDefaults.textButtonColors(containerColor=androidx.compose.ui.graphics.Color.Transparent,contentColor=if(date==selected)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)) {
                Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(listOf("一","二","三","四","五","六","日")[date.dayOfWeek.value-1],style=MaterialTheme.typography.labelSmall)
                    CalendarDateNumber(date,state)
                    Surface(color=if(date==selected)MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,modifier=Modifier.width(16.dp).height(2.dp)) {}
                }
            }}
        }}
        if(state.loading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        state.logError?.let {item {Hint("日志读取失败：$it。请同步后重试。",true)}}
        state.taskError?.let {item {Hint("任务读取失败：$it。请同步后重试。",true)}}
        if(mode!="day")item {Surface(color=MaterialTheme.colorScheme.background,modifier=Modifier.fillMaxWidth()) {Column {
            Row {listOf("一","二","三","四","五","六","日").forEach {Text(it,modifier=Modifier.weight(1f).padding(vertical=4.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            val padded=List(if(mode=="month")dates.first().dayOfWeek.value-1 else 0) {null as LocalDate?}+dates
            padded.chunked(7).forEach {week->Row {
                repeat(7) {index->
                    val date=week.getOrNull(index)
                    if(date==null)Spacer(Modifier.weight(1f))
                    else {
                        val count=state.logs.count {it.logDate==date.toString()}+state.tasks.count {taskDate(it)==date}
                        TextButton(onClick={feedback();model.selectDate(date.toString());mode="day"},modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics {this.selected=date==selected;contentDescription="$date，$count 项记录"},shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(0.dp),
                            colors=ButtonDefaults.textButtonColors(containerColor=androidx.compose.ui.graphics.Color.Transparent,contentColor=if(date==selected)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)) {
                            Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                CalendarDateNumber(date,state)
                                Surface(color=if(date==selected)MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,modifier=Modifier.width(16.dp).height(2.dp)) {}
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }}
        }}}
        item {HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)}
        val logs=state.logs.filter {runCatching {LocalDate.parse(it.logDate) in dates}.getOrDefault(false)}
        val tasks=state.tasks.filter {taskDate(it) in dates}
        if(logs.isEmpty() && tasks.isEmpty() && !state.loading && state.logError==null && state.taskError==null)item {
            Column(Modifier.fillMaxWidth().padding(vertical=32.dp),verticalArrangement=Arrangement.Top) {
                Text(when(mode){"day"->"这一天暂无记录";"week"->"本周暂无记录";else->"本月暂无记录"},style=MaterialTheme.typography.titleLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Medium))
                Spacer(Modifier.height(8.dp))
                Text("日志与有截止日期的任务会显示在这里。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        dates.forEach {date->
            val dayLogs=logs.filter {it.logDate==date.toString()}
            val dayTasks=tasks.filter {taskDate(it)==date}
            if(dayLogs.isNotEmpty() || dayTasks.isNotEmpty())item(key="date-$date") {
                Row(Modifier.fillMaxWidth().padding(top=12.dp,bottom=4.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Text("${date.monthValue} 月 ${date.dayOfMonth} 日",modifier=Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                    Text("${dayLogs.size} 条日志 · ${dayTasks.size} 项任务",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
    if(showHelp)AlertDialog(onDismissRequest={showHelp=false},title={Text("显示说明")},text={Text("仅展示当前已同步的日志与任务。没有截止日期的任务可在任务列表查看。")},confirmButton={TextButton(onClick={showHelp=false}) {Text("关闭")}})
}
private fun periodLabel(mode: String)=when(mode) {"week"->"周";"month"->"月";else->"天"}
@Composable private fun CalendarDateNumber(date: LocalDate,state: AppState) {
    val hasContent=state.logs.any {it.logDate==date.toString()} || state.tasks.any {taskDate(it)==date}
    Column(Modifier.width(32.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text(date.dayOfMonth.toString(),style=MaterialTheme.typography.bodyLarge.copy(fontFamily=androidx.compose.ui.text.font.FontFamily.Serif,fontSize=18.sp,lineHeight=20.sp,fontWeight=if(date==today())androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal))
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
            Text(if(log!=null)if(log.status=="draft")"草稿" else "已提交" else if(task!!.status=="done")"已完成" else "${if(task.status=="doing")"进行中" else "待开始"} · ${task.progress}%",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(log?.content?.let {recordPreview(it).first} ?: task!!.title,style=MaterialTheme.typography.bodyLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Medium,lineBreak=androidx.compose.ui.text.style.LineBreak.Heading),maxLines=2,overflow=TextOverflow.Ellipsis)
            if(task!=null && task.progress>0)LinearProgressIndicator(progress={task.progress.coerceIn(0,100)/100f},modifier=Modifier.fillMaxWidth().height(2.dp),color=MaterialTheme.colorScheme.secondary,gapSize=0.dp,drawStopIndicator={})
        }
    }
}
