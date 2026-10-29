package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
            Text(if(mode=="week")"${dates.first().monthValue}月${dates.first().dayOfMonth}日 — ${dates.last().monthValue}月${dates.last().dayOfMonth}日" else "${selected.year} 年 ${selected.monthValue} 月",modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.Medium,fontFeatureSettings="tnum"))
            IconButton(onClick={feedback();navigate(-1)},modifier=Modifier.background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small)) {Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft,"上一$unit",modifier=Modifier.size(20.dp))}
            ControlTextButton(onClick={model.selectDate(today().toString())},modifier=Modifier.widthIn(min=48.dp).background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text("今天",style=MaterialTheme.typography.labelMedium)}
            IconButton(onClick={feedback();navigate(1)},modifier=Modifier.background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small)) {Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"下一$unit",modifier=Modifier.size(20.dp))}
        }}
        if(mode!="day")item {Column {
            Row {weekdays.forEach {Text(it,modifier=Modifier.weight(1f).padding(vertical=8.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Medium),color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            val padded=if(mode=="month")List(dates.first().dayOfWeek.value%7) {null as LocalDate?}+dates else dates
            padded.chunked(7).forEach {week->Box(Modifier.fillMaxWidth()) {Row {
                repeat(7) {index->
                    val date=week.getOrNull(index)
                    if(date==null)Spacer(Modifier.weight(1f))
                    else {
                        val dayTasks=orderedTasks(state.tasks.filter {taskDate(it)==date})
                        val summaries=dayTasks.map {it.title}
                        val count=summaries.size
                        val dayLabel=calendarDayLabel(date,state.tasks)
                        val dayForeground=if(dayLabel!=null)labelTextColor(dayLabel) else MaterialTheme.colorScheme.onSurface
                        ControlTextButton(onClick={model.selectDate(date.toString())},modifier=Modifier.weight(1f).heightIn(min=72.dp).semantics {this.selected=date==selected;contentDescription="$date，$count 项安排，${dayLabel?.name?.let {"日期标签：$it，"}.orEmpty()}${summaries.take(2).joinToString("，")}"},shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(0.dp),
                            colors=ButtonDefaults.textButtonColors(containerColor=if(dayLabel!=null)labelColor(dayLabel) else if(date==selected)MaterialTheme.colorScheme.secondary.copy(alpha=0.12f) else androidx.compose.ui.graphics.Color.Transparent,contentColor=if(dayLabel!=null)dayForeground else if(date==selected)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)) {
                            Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                CalendarDateNumber(date,state,dayLabel)
                                Column(Modifier.fillMaxWidth().heightIn(min=28.dp).padding(horizontal=2.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                                    if(dayTasks.isEmpty())Spacer(Modifier.height(28.dp))
                                    dayTasks.take(2).forEach {task->Text(task.title,maxLines=if(dayTasks.size==1)2 else 1,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth().padding(horizontal=2.dp),style=MaterialTheme.typography.labelSmall.copy(fontSize=9.5.sp,lineHeight=13.5.sp),color=if(dayLabel!=null)dayForeground else MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}

                                }
                                Surface(color=if(date==selected && dayLabel!=null)dayForeground else if(date==selected)MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,modifier=Modifier.width(16.dp).height(2.dp)) {}
                            }
                        }
                    }
                }
            }

            }}
        }}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ControlOutlinedButton(onClick={model.screen("tasks")},modifier=Modifier.weight(1f)) {Text("全部任务 · ${state.tasks.size}")}
            ControlButton(onClick=model::openDispatch,enabled=!state.saving,modifier=Modifier.weight(1f)) {Text("创建任务")}
        }}
        item {HorizontalDivider(Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.outlineVariant)}
        if(state.loading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        state.taskError?.let {item {Hint("任务读取失败：$it。请同步后重试。",true)}}
        val agendaDates=if(mode=="week")dates else listOf(selected)
        agendaDates.forEach {agendaDate->
            val tasks=orderedTasks(state.tasks.filter {taskDate(it)==agendaDate})
            item {Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha=0.96f),MaterialTheme.shapes.medium).panelOutline().padding(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${agendaDate.monthValue} 月 ${agendaDate.dayOfMonth} 日 · 周${listOf("一","二","三","四","五","六","日")[agendaDate.dayOfWeek.value-1]}",style=MaterialTheme.typography.titleMedium)
                    Text("${tasks.size} 项任务",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ControlTextButton(onClick={if(mode=="week") {model.selectDate(agendaDate.toString());model.calendarMode("day")} else showHelp=true}) {Text(if(mode=="week")"查看当日" else "说明",style=MaterialTheme.typography.labelMedium)}
            }}
            if(tasks.isEmpty() && !state.taskLoading && state.taskError==null)item {
                if(mode=="week")Text("暂无安排",modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                else Column(Modifier.fillMaxWidth().padding(vertical=24.dp)) {
                    Text("这一天暂无到期任务",style=MaterialTheme.typography.titleLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Medium))
                    Spacer(Modifier.height(8.dp))
                    Text("切换日期查看任务，或打开全部任务管理未设截止时间的事项。",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            tasks.forEach {task->
                item(key="${agendaDate}-task-${task.id}") {
                    AgendaEntry(task,!state.saving) {feedback();model.openTask(task.id)}
                }
            }
        }
        item {TaskManagementSection(state,model)}
    }
    if(showHelp)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={showHelp=false},title={Text("显示说明")},text={Text("月视图查看整月，周视图查看周一至周日及整周工作安排，日视图专注当天详情。箭头按当前视图切换月、周或天，“今天”返回当前日期。此视图仅映射任务，日志在日志页面独立管理。日期摘要最多两排，详细内容在下方显示。每个日期格只使用一种标签底色；同日多标签时按任务列表顺序（未完成优先，再按优先级和截止时间）选取首个带标签任务的颜色。仅展示当前已同步内容，没有截止日期的任务可在任务列表查看。")},confirmButton={ControlTextButton(onClick={showHelp=false}) {Text("关闭")}})
}
@Composable private fun CalendarDateNumber(date: LocalDate,state: AppState,label: WorkLabel?) {
    val selected=date.toString()==state.selectedDate
    Box(Modifier.size(30.dp).background(if(selected && label==null)MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,androidx.compose.foundation.shape.CircleShape),contentAlignment=androidx.compose.ui.Alignment.Center) {
        Text(date.dayOfMonth.toString(),color=if(label!=null)labelTextColor(label) else if(selected)MaterialTheme.colorScheme.onSecondary else if(date==today())MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,style=MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings="tnum",fontSize=18.sp,lineHeight=24.sp,fontWeight=if(selected || date==today())FontWeight.SemiBold else FontWeight.Normal))
    }
}

@Composable private fun AgendaEntry(task: DispatchTask,enabled: Boolean,onClick: ()->Unit) {
    val background=if(task.label!=null)labelColor(task.label) else MaterialTheme.colorScheme.surface.copy(alpha=0.96f)
    val foreground=if(task.label!=null)labelTextColor(task.label) else MaterialTheme.colorScheme.onSurface
    Surface(onClick=onClick,enabled=enabled,color=background,contentColor=foreground,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
        Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(44.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(timestamp(task.dueAt.orEmpty()).substringAfter(' '),style=MaterialTheme.typography.labelMedium)
                Text("截止时间",style=MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                LabelBadge(task.label)
                Text(if(task.approvalStatus=="approved")taskFeedbackLabel(task) else "创建${reviewStatus(task.approvalStatus)}",style=MaterialTheme.typography.labelSmall)
                Text(task.title,style=MaterialTheme.typography.bodyLarge)
                TaskPriorityText(task.priority,onLabel=task.label!=null)
                if(task.detail.isNotBlank())Text(task.detail,style=MaterialTheme.typography.bodyMedium)
                if(task.progressNote.isNotBlank())Text("进度：${task.progressNote}",style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
