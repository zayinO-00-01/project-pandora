package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
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
    var expanded by rememberSaveable {mutableStateOf(false)}
    var dragDistance by remember {mutableFloatStateOf(0f)}
    val density=LocalDensity.current
    val dragThreshold=with(density){40.dp.toPx()}
    val mode=state.calendarMode
    val dates=if(mode=="week") {val first=selected.minusDays((selected.dayOfWeek.value%7).toLong());(0..6).map {first.plusDays(it.toLong())}} else periodDates(selected,mode)
    val weekdays=listOf("日","一","二","三","四","五","六")
    val unit=when(mode) {"week"->"周";"day"->"天";else->"月"}
    fun navigate(amount: Long) {
        model.selectDate(when(mode) {"week"->selected.plusWeeks(amount);"day"->selected.plusDays(amount);else->selected.plusMonths(amount)}.toString())
    }
    val feedback=LocalControlFeedback.current
    val listState=rememberLazyListState()
    LaunchedEffect(mode) {listState.scrollToItem(0)}
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val weekCount=(selected.withDayOfMonth(1).dayOfWeek.value%7+selected.lengthOfMonth()+6)/7
    val normalHeight=72f*weekCount
    val expandedHeight=maxOf(normalHeight+48f,maxHeight.value-164f)
    fun sizeHeight(size: String)=when(size) {"week"->72f;"expanded"->expandedHeight;else->normalHeight}
    val currentSize=if(mode=="week")"week" else if(expanded)"expanded" else "normal"
    var dragging by remember {mutableStateOf(false)}
    var heightTarget by remember {mutableFloatStateOf(sizeHeight(currentSize))}
    val animatedHeight by animateFloatAsState(heightTarget,animationSpec=if(dragging)snap() else spring(dampingRatio=1f,stiffness=420f),label="calendarHeight")
    LaunchedEffect(currentSize,normalHeight,expandedHeight) {if(!dragging)heightTarget=sizeHeight(currentSize)}
    val gesture=rememberDraggableState {delta->
        dragDistance+=delta
        heightTarget=(heightTarget+delta/density.density).coerceIn(if(currentSize=="expanded")normalHeight else 72f,if(currentSize=="week")normalHeight else expandedHeight)
    }
    fun applySize(size: String) {
        heightTarget=sizeHeight(size)
        expanded=size=="expanded"
        model.calendarMode(if(size=="week")"week" else "month")
    }
    fun settle(velocity: Float) {
        dragging=false
        val intent=if(kotlin.math.abs(velocity)>650f)kotlin.math.sign(velocity)*dragThreshold else dragDistance
        applySize(calendarSizeAfterDrag(currentSize,intent,dragThreshold))
    }
    val calendarRowHeight=maxOf(72f,animatedHeight/weekCount).dp
    val collapse=((normalHeight-animatedHeight)/(normalHeight-72f)).coerceIn(0f,1f)
    val monthDates=periodDates(selected,"month")
    val gridStart=monthDates.first().minusDays((monthDates.first().dayOfWeek.value%7).toLong())
    val padded=List(weekCount*7) {gridStart.plusDays(it.toLong())}
    val selectedRow=padded.indexOf(selected)/7
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),state=listState,verticalArrangement=Arrangement.spacedBy(4.dp),contentPadding=PaddingValues(bottom=32.dp)) {
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("month" to "月视图","week" to "周视图","day" to "日视图").forEach {(key,label)->
                FilterChip(selected=mode==key,onClick={feedback();expanded=false;model.calendarMode(key)},label={Text(label)},colors=FilterChipDefaults.filterChipColors(containerColor=MaterialTheme.colorScheme.surface.copy(alpha=0.92f),selectedContainerColor=MaterialTheme.colorScheme.secondaryContainer),modifier=Modifier.weight(1f))
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
        if(mode!="day")item {Column(Modifier.fillMaxWidth().draggable(state=gesture,orientation=Orientation.Vertical,startDragImmediately=dragging,onDragStarted={heightTarget=animatedHeight;dragDistance=0f;dragging=true},onDragStopped={settle(it)})) {
            Row {weekdays.forEach {Text(it,modifier=Modifier.weight(1f).padding(vertical=8.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Medium),color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            Box(Modifier.fillMaxWidth().height(animatedHeight.dp).clipToBounds()) {
            // Measure every month row before clipping to the selected week.
            Column(Modifier.offset {IntOffset(0,(-selectedRow*72f*collapse*density.density).roundToInt())}.wrapContentHeight(androidx.compose.ui.Alignment.Top,unbounded=true)) {
            padded.chunked(7).forEach {week->Box(Modifier.fillMaxWidth()) {Row {
                repeat(7) {index->
                    val date=week.getOrNull(index)
                    if(date==null)Spacer(Modifier.weight(1f))
                    else {
                        val dayTasks=orderedTasks(state.tasks.filter {taskIsApproved(it) && !it.deleted && taskDate(it)==date})
                        val summaries=calendarSummaries(date,state.logs,state.tasks)
                        val count=summaries.size
                        val dayLabel: WorkLabel?=null
                        val dayForeground=if(dayLabel!=null)labelTextColor(dayLabel) else MaterialTheme.colorScheme.onSurface
                        ControlTextButton(onClick={model.selectDate(date.toString())},modifier=Modifier.weight(1f).height(calendarRowHeight).semantics {this.selected=date==selected;contentDescription="$date，$count 项安排，${dayLabel?.name?.let {"日期标签：$it，"}.orEmpty()}${summaries.take(2).joinToString("，")}"},shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(0.dp),
                            colors=ButtonDefaults.textButtonColors(containerColor=if(dayLabel!=null)labelColor(dayLabel) else if(date==selected)MaterialTheme.colorScheme.secondary.copy(alpha=0.12f) else androidx.compose.ui.graphics.Color.Transparent,contentColor=if(dayLabel!=null)dayForeground else if(date==selected)MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)) {
                            Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                CalendarDateNumber(date,state,dayLabel)
                                Column(Modifier.fillMaxWidth().heightIn(min=28.dp).padding(horizontal=2.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                                    if(summaries.isEmpty())Spacer(Modifier.height(28.dp))
                                    dayTasks.take(2).forEach {task->
                                        val tags=contentTags(state,"task",task.id,task.label)
                                        val primary=tags.firstOrNull()
                                        Row(Modifier.fillMaxWidth().padding(vertical=1.dp).background(if(primary!=null)labelColor(primary) else MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.extraSmall),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                                            Text(task.title,maxLines=if(dayTasks.size==1)2 else 1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f).padding(horizontal=3.dp,vertical=1.dp),style=MaterialTheme.typography.labelSmall.copy(fontSize=9.5.sp,lineHeight=13.5.sp),color=if(primary!=null)labelTextColor(primary) else MaterialTheme.colorScheme.onSurfaceVariant)
                                            tags.drop(1).take(11).forEach {tag->Box(Modifier.padding(end=1.dp).size(width=2.dp,height=10.dp).background(labelColor(tag)))}
                                        }
                                    }

                                }
                                Surface(color=if(date==selected && dayLabel!=null)dayForeground else if(date==selected)MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,modifier=Modifier.width(16.dp).height(2.dp)) {}
                            }
                        }
                    }
                }
            }

            }}
            }}
            Box(Modifier.fillMaxWidth().height(28.dp).semantics {contentDescription=when {mode=="week"->"下拉恢复正常月历";expanded->"上拉恢复正常月历";else->"下拉放大，上拉收起周历"}}.clickable {applySize(if(mode=="week" || expanded)"normal" else "week")},contentAlignment=androidx.compose.ui.Alignment.Center) {
                Box(Modifier.size(width=36.dp,height=4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.5f),MaterialTheme.shapes.small))
            }
        }}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ControlOutlinedButton(onClick={model.screen("tasks")},modifier=Modifier.weight(1f)) {Text("全部任务 · ${relatedTasks(state.tasks,state.session?.userId ?: 0).size}")}
            ControlButton(onClick=model::openDispatch,enabled=!state.saving,modifier=Modifier.weight(1f)) {Text("创建任务")}
        }}
        item {HorizontalDivider(Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.outlineVariant)}
        if(state.loading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
        state.taskError?.let {item {Hint("任务读取失败：$it。请同步后重试。",true)}}
        val agendaDates=listOf(selected)
        agendaDates.forEach {agendaDate->
            val tasks=orderedTasks(state.tasks.filter {taskIsApproved(it) && !it.deleted && taskDate(it)==agendaDate})
            item {Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha=0.96f),MaterialTheme.shapes.medium).panelOutline().padding(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${agendaDate.monthValue} 月 ${agendaDate.dayOfMonth} 日 · 周${listOf("一","二","三","四","五","六","日")[agendaDate.dayOfWeek.value-1]}",style=MaterialTheme.typography.titleMedium)
                    Text("${tasks.size} 项任务",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ControlTextButton(onClick={showHelp=true}) {Text("说明",style=MaterialTheme.typography.labelMedium)}
            }}
            if(tasks.isEmpty() && !state.taskLoading && state.taskError==null)item {
                if(mode=="week")Text("暂无安排",modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                else Column(Modifier.fillMaxWidth().padding(vertical=24.dp)) {
                    Text("这一天暂无任务",style=MaterialTheme.typography.titleLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Medium))
                    Spacer(Modifier.height(8.dp))
                }
            }
            tasks.forEach {task->
                item(key="${agendaDate}-task-${task.id}") {
                    AgendaEntry(task,state,!state.saving) {feedback();model.openTask(task.id)}
                }
            }
        }
        item {TaskManagementSection(state.copy(tasks=state.tasks.filter {taskDate(it)==selected}),model)}
    }
    }
    if(showDateJump)CalendarJumpDialog(selected,onDismiss={showDateJump=false},onConfirm={date->model.selectDate(date.toString());showDateJump=false})
    if(showHelp)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={showHelp=false},title={Text("显示说明")},text={Text("日历仅显示任务，日志在日志页独立管理。月视图查看整月，周视图查看选中日期所在周，下方仅列出选中当天的工作安排，日视图专注当天详情。点击日期标题可直接输入年份并选择月份；跳转保留所选日期，超出当月天数时使用月末。箭头按当前视图切换月、周或天，“今天”返回当前日期。日期摘要最多两排，详细内容在下方显示。仅展示当前已同步内容，没有截止日期的任务可在任务列表查看。")},confirmButton={ControlTextButton(onClick={showHelp=false}) {Text("关闭")}})
}
@Composable private fun CalendarDateNumber(date: LocalDate,state: AppState,label: WorkLabel?) {
    val selected=date.toString()==state.selectedDate
    Box(Modifier.size(30.dp).background(if(selected && label==null)MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,androidx.compose.foundation.shape.CircleShape),contentAlignment=androidx.compose.ui.Alignment.Center) {
        Text(date.dayOfMonth.toString(),color=if(label!=null)labelTextColor(label) else if(selected)MaterialTheme.colorScheme.onSecondary else if(date==today())MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,style=MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings="tnum",fontSize=18.sp,lineHeight=24.sp,fontWeight=if(selected || date==today())FontWeight.SemiBold else FontWeight.Normal))
    }
}

@Composable private fun AgendaEntry(task: DispatchTask,state: AppState,enabled: Boolean,onClick: ()->Unit) {
    val background=MaterialTheme.colorScheme.surface.copy(alpha=0.96f)
    val foreground=MaterialTheme.colorScheme.onSurface
    Surface(onClick=onClick,enabled=enabled,color=background,contentColor=foreground,shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth().panelOutline()) {
        Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(44.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(timestamp(task.dueAt.orEmpty()).substringAfter(' ').take(5),style=MaterialTheme.typography.labelMedium)
                Text("截止时间",style=MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(if(taskIsApproved(task))taskFeedbackLabel(task) else "创建${reviewStatus(task.approvalStatus)}",style=MaterialTheme.typography.labelSmall)
                Text(task.title,style=MaterialTheme.typography.bodyLarge)
                TaskPriorityText(task.priority,onLabel=false)
                if(task.detail.isNotBlank())Text(task.detail,style=MaterialTheme.typography.bodyMedium)
                if(task.progressNote.isNotBlank())Text("进度：${task.progressNote}",style=MaterialTheme.typography.bodySmall)
                LabelLines(contentTags(state,"task",task.id,task.label))
            }
        }
    }
}
