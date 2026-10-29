package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*
import java.time.LocalDate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.text.font.FontWeight

@Composable fun CalendarScreen(state: AppState,model: PandoraViewModel) {
    var showHelp by remember {mutableStateOf(false)}
    var showDateJump by rememberSaveable {mutableStateOf(false)}
    val selected=LocalDate.parse(state.selectedDate)
    val mode=state.calendarMode
    val dates=periodDates(selected,mode)
    val weekdays=if(mode=="week")listOf("一","二","三","四","五","六","日") else listOf("日","一","二","三","四","五","六")
    val unit=when(mode) {"week"->"周";"day"->"天";else->"月"}
    fun navigate(amount: Long) {
        model.selectDate(when(mode) {"week"->selected.plusWeeks(amount);"day"->selected.plusDays(amount);else->selected.plusMonths(amount)}.toString())
    }
    val feedback=LocalControlFeedback.current
    val listState=rememberLazyListState()
    LaunchedEffect(mode) {listState.scrollToItem(0)}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),state=listState,verticalArrangement=Arrangement.spacedBy(4.dp),contentPadding=PaddingValues(bottom=32.dp)) {
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("month" to "月视图","week" to "周视图","day" to "日视图").forEach {(key,label)->
                FilterChip(selected=mode==key,onClick={feedback();model.calendarMode(key)},label={Text(label)},modifier=Modifier.weight(1f))
            }
        }}
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            val heading=if(mode=="week")"${dates.first().year}年${dates.first().monthValue}月${dates.first().dayOfMonth}日 — ${dates.last().year}年${dates.last().monthValue}月${dates.last().dayOfMonth}日" else "${selected.year} 年 ${selected.monthValue} 月"
            Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(role=Role.Button,onClickLabel="选择年月",onClick={feedback();showDateJump=true}).semantics(mergeDescendants=true) {contentDescription="$heading，选择年月"}.padding(vertical=6.dp)) {
                Text(heading,style=MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.Medium,fontFeatureSettings="tnum"))
                Text("选择年月 ▾",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick={feedback();navigate(-1)},modifier=Modifier.background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small)) {Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft,"上一$unit",modifier=Modifier.size(20.dp))}
            ControlTextButton(onClick={model.selectDate(today().toString())},modifier=Modifier.widthIn(min=48.dp).background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text("今天",style=MaterialTheme.typography.labelMedium)}
            IconButton(onClick={feedback();navigate(1)},modifier=Modifier.background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small)) {Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"下一$unit",modifier=Modifier.size(20.dp))}
        }}
        if(mode!="day")item {Column {
            Row {weekdays.forEach {Text(it,modifier=Modifier.weight(1f).padding(vertical=8.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Medium),color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            val padded=if(mode=="month")List(dates.first().dayOfWeek.value%7) {null as LocalDate?}+dates else dates
            padded.chunked(7).forEach {week->Row {
                repeat(7) {index->
                    val date=week.getOrNull(index)
                    if(date==null)Spacer(Modifier.weight(1f))
                    else {
                        val summaries=calendarSummaries(date,state.logs,state.tasks)
                        val count=summaries.size
                        ControlTextButton(onClick={model.selectDate(date.toString())},modifier=Modifier.weight(1f).heightIn(min=72.dp).semantics {this.selected=date==selected;contentDescription="$date，$count 项安排，${summaries.take(2).joinToString("，")}"},shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(0.dp),
                            colors=ButtonDefaults.textButtonColors(containerColor=if(date==selected)MaterialTheme.colorScheme.secondary.copy(alpha=0.12f) else androidx.compose.ui.graphics.Color.Transparent,contentColor=if(date==selected)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)) {
                            Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                CalendarDateNumber(date,state)
                                Column(Modifier.fillMaxWidth().heightIn(min=28.dp).padding(horizontal=2.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                                    Text(summaries.joinToString(" · "),maxLines=2,minLines=2,softWrap=true,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall.copy(fontSize=9.5.sp,lineHeight=13.5.sp),color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center)

                                }
                                Surface(color=if(date==selected)MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,modifier=Modifier.width(16.dp).height(2.dp)) {}
                            }
                        }
                    }
                }
            }}
        }}
        item {HorizontalDivider(Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.outlineVariant)}
        if(state.loading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        state.logError?.let {item {Hint("日志读取失败：$it。请同步后重试。",true)}}
        state.taskError?.let {item {Hint("任务读取失败：$it。请同步后重试。",true)}}
        val agendaDates=if(mode=="week")dates else listOf(selected)
        agendaDates.forEach {agendaDate->
            val logs=state.logs.filter {it.logDate==agendaDate.toString()}
            val tasks=orderedTasks(state.tasks.filter {taskIsApproved(it) && taskDate(it)==agendaDate})
            item {Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha=0.96f),MaterialTheme.shapes.medium).panelOutline().padding(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${agendaDate.monthValue} 月 ${agendaDate.dayOfMonth} 日 · 周${listOf("一","二","三","四","五","六","日")[agendaDate.dayOfWeek.value-1]}",style=MaterialTheme.typography.titleMedium)
                    Text("${logs.size} 条日志 · ${tasks.size} 项任务",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ControlTextButton(onClick={if(mode=="week") {model.selectDate(agendaDate.toString());model.calendarMode("day")} else showHelp=true}) {Text(if(mode=="week")"查看当日" else "说明",style=MaterialTheme.typography.labelMedium)}
            }}
            if(logs.isEmpty() && tasks.isEmpty() && !state.loading && state.logError==null && state.taskError==null)item {
                if(mode=="week")Text("暂无安排",modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                else Column(Modifier.fillMaxWidth().padding(vertical=24.dp)) {
                    Text("这一天暂无记录",style=MaterialTheme.typography.titleLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Medium))
                    Spacer(Modifier.height(8.dp))
                    Text("切换日期，查看日志和到期任务。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val events=logs.sortedBy {it.updatedAt}.map {Triple(it.updatedAt,it,null as DispatchTask?)}+tasks.map {Triple(it.dueAt.orEmpty(),null as WorkLog?,it)}
            events.forEach {event->
                val log=event.second
                val task=event.third
                item(key=if(log!=null)"${agendaDate}-log-${log.id}" else "${agendaDate}-task-${task!!.id}") {
                    AgendaEntry(event.first,log,task,!state.saving) {feedback();if(log!=null)model.edit(log) else model.openTask(task!!.id)}
                }
            }
        }

    }
    if(showDateJump)CalendarJumpDialog(selected,onDismiss={showDateJump=false},onConfirm={date->model.selectDate(date.toString());showDateJump=false})
    if(showHelp)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={showHelp=false},title={Text("显示说明")},text={Text("月视图查看整月，周视图查看周一至周日及整周工作安排，日视图专注当天详情。点击日期标题可直接输入年份并选择月份；跳转保留所选日期，超出当月天数时使用月末。箭头按当前视图切换月、周或天，“今天”返回当前日期。日期摘要最多两排，详细内容在下方显示。仅展示当前已同步内容，没有截止日期的任务可在任务列表查看。")},confirmButton={ControlTextButton(onClick={showHelp=false}) {Text("关闭")}})
}
@Composable private fun CalendarDateNumber(date: LocalDate,state: AppState) {
    val selected=date.toString()==state.selectedDate
    Box(Modifier.size(30.dp).background(if(selected)MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,androidx.compose.foundation.shape.CircleShape),contentAlignment=androidx.compose.ui.Alignment.Center) {
        Text(date.dayOfMonth.toString(),color=if(selected)MaterialTheme.colorScheme.onSecondary else if(date==today())MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,style=MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings="tnum",fontSize=18.sp,lineHeight=24.sp,fontWeight=if(date==today())FontWeight.SemiBold else FontWeight.Normal))
    }
}

@Composable private fun AgendaEntry(time: String,log: WorkLog?,task: DispatchTask?,enabled: Boolean,onClick: ()->Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha=0.96f),MaterialTheme.shapes.medium).panelOutline().clickable(enabled=enabled,onClick=onClick).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.width(44.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(timestamp(time).substringAfter(' '),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurface,modifier=Modifier.semantics {contentDescription="${if(log!=null)"更新时间" else "截止时间"} ${timestamp(time)}"})
            Text(if(log!=null)"日志" else "任务",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(if(log!=null)if(log.status=="draft")"草稿" else "已提交" else taskFeedbackLabel(task!!),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(log?.content ?: task!!.title,style=MaterialTheme.typography.bodyLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Medium,lineBreak=androidx.compose.ui.text.style.LineBreak.Heading))
            if(task!=null)TaskPriorityText(task.priority)
            if(task!=null && task.detail.isNotBlank())Text(task.detail,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
