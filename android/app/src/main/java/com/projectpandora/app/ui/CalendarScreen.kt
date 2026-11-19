package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlin.math.abs
import kotlin.math.sign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.canRescheduleTask
import com.projectpandora.app.data.*
import java.time.LocalDate
import java.time.Instant
import java.time.format.DateTimeFormatter

private val scheduleFormat=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
private data class CalendarScheduleRequest(val task: DispatchTask,val target: LocalDate?)

@Composable fun CalendarScreen(state: AppState,model: PandoraViewModel) {
    var showHelp by remember {mutableStateOf(false)}
    val selected=LocalDate.parse(state.selectedDate)
    val mode=state.calendarMode
    val feedback=LocalControlFeedback.current
    val listState=rememberLazyListState()
    val itemDrag=remember {CalendarItemDrag()}
    var moveRequest by remember(state.session?.userId) {mutableStateOf<CalendarScheduleRequest?>(null)}
    fun move(task: DispatchTask,target: LocalDate) {
        val start=task.startAt?.let(Instant::parse)
        val targetTime=Instant.parse(task.dueAt ?: return).atZone(TaskDispatchValidation.zone).toLocalTime()
        val due=target.atTime(targetTime).atZone(TaskDispatchValidation.zone).toInstant()
        if(start!=null && due>=start && due>=Instant.now())
            model.moveTaskDate(task,due.toString())
        else moveRequest=CalendarScheduleRequest(task,target)
    }
    var expanded by rememberSaveable {mutableStateOf(false)}
    LaunchedEffect(mode,expanded) {listState.scrollToItem(0)}
    val tasksByDay=remember(state.tasks) {
        orderedTasks(state.tasks.filter {taskIsApproved(it) && !it.deleted}).groupBy {taskDate(it)}
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density=LocalDensity.current.density
        val weekHeight=84f
        val weekCount=(selected.withDayOfMonth(1).dayOfWeek.value%7+selected.lengthOfMonth()+6)/7
        val monthHeight=weekHeight*weekCount
        val expandedHeight=maxOf(monthHeight+48f,maxHeight.value-164f)
        val currentSize=if(mode=="week")"week" else if(expanded)"expanded" else "normal"
        fun heightOf(size: String)=when(size) {"week"->weekHeight;"expanded"->expandedHeight;else->monthHeight}
        var heightTarget by remember {mutableFloatStateOf(heightOf(currentSize))}
        var dragging by remember {mutableStateOf(false)}
        var distance by remember {mutableFloatStateOf(0f)}
        val height by animateFloatAsState(heightTarget,if(dragging)snap() else spring(dampingRatio=1f,stiffness=420f),label="companyCalendarHeight")
        LaunchedEffect(currentSize,monthHeight,expandedHeight) {
            if(!dragging)heightTarget=heightOf(currentSize)
        }
        fun applySize(size: String) {
            heightTarget=heightOf(size)
            expanded=size=="expanded"
            model.calendarMode(if(size=="week")"week" else "month")
        }
        val startDrag by rememberUpdatedState<(Float)->Boolean> { direction->
            val layout=listState.layoutInfo
            val calendarVisible=layout.visibleItemsInfo.any {
                it.key=="company-calendar" && it.offset<layout.viewportEndOffset &&
                    it.offset+it.size>layout.viewportStartOffset
            }
            val canResize=itemDrag.item==null && calendarVisible && mode!="day" && (if(direction>0)currentSize!="expanded" else currentSize!="week")
            if(canResize) {
                heightTarget=height
                distance=0f
                dragging=true
            }
            canResize
        }
        val resize by rememberUpdatedState<(Float)->Unit> { delta->
            distance+=delta
            val minimum=if(currentSize=="expanded")monthHeight else weekHeight
            val maximum=if(currentSize=="week")monthHeight else expandedHeight
            heightTarget=(heightTarget+delta/density).coerceIn(minimum,maximum)
        }
        val finishDrag by rememberUpdatedState<(Float?)->Unit> { velocity->
            dragging=false
            val threshold=40*density
            val intent=when {
                velocity==null->0f
                abs(velocity)>650f->sign(velocity)*threshold
                else->distance
            }
            applySize(calendarSizeAfterDrag(currentSize,intent,threshold))
        }
        val calendarScroll=remember(listState) {
            CalendarScrollConnection(
                isAtTop={!listState.canScrollBackward},
                startResize={startDrag(it)},
                resize={resize(it)},
                finishResize={finishDrag(it)}
            )
        }
        CalendarDragLayer(itemDrag) {
        LazyColumn(Modifier.fillMaxSize().nestedScroll(calendarScroll).padding(horizontal=16.dp),state=listState,verticalArrangement=Arrangement.spacedBy(4.dp),contentPadding=PaddingValues(bottom=32.dp)) {
            item(key="company-calendar") {
                WorkspaceCalendar(selected,onDate={model.selectDate(it.toString())},mode=mode,
                    onMode={expanded=false;model.calendarMode(it)},gridHeight=height.dp,
                    onResizeToggle={applySize(if(currentSize=="normal")"week" else "normal")},drag=itemDrag) {date->
                    val tasks=tasksByDay[date].orEmpty()
                    tasks.take(if(tasks.size>2)1 else 2).forEach {task->
                        val canMove=canRescheduleTask(state,task)
                        CalendarItemChip(task.title,contentTags(state,"task",task.id,task.label),
                            Modifier.calendarDrag(itemDrag,task.title,date,if(canMove){{target->move(task,target)}} else null)
                                .clickable {model.selectDate(date.toString())})
                    }
                    if(tasks.size>2)CalendarOverflow(tasks.size-1) {model.selectDate(date.toString())}
                }
            }
        state.workspaceError?.let {item {Hint(it,true)}}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
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
                    holidayLabel(agendaDate)?.let {Text(it,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)}
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
                    val canMove=canRescheduleTask(state,task)
                    AgendaEntry(task,state,!state.saving,
                        Modifier.calendarDrag(itemDrag,task.title,agendaDate,if(canMove){{target->move(task,target)}} else null,fromAgenda=true),
                        onMove=if(canMove) {{moveRequest=CalendarScheduleRequest(task,null)}} else null
                    ) {feedback();model.openTask(task.id)}
                }
            }
        }
        item {TaskManagementSection(state.copy(tasks=state.tasks.filter {taskDate(it)==selected}),model)}
    }
    }
    }
    moveRequest?.let { request ->
        TaskScheduleDialog(request,{moveRequest=null}) { due,start ->
            moveRequest=null
            model.moveTaskDate(request.task,due,start)
        }
    }
    if(showHelp)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={showHelp=false},title={Text("日历怎么用")},text={Text("点日期看当天全部任务，点年月可直接跳转。长按任务标签或下方的任务卡片可拖到新日期，也可以点“改期”。创建人和责任人可申请改期，由本人直属上级审核；创始人改期直接生效。")},confirmButton={ControlTextButton(onClick={showHelp=false}) {Text("关闭")}})
}
@Composable private fun TaskScheduleDialog(request: CalendarScheduleRequest,onDismiss: ()->Unit,onConfirm: (String,String?)->Unit) {
    val task=request.task
    val oldDue=Instant.parse(task.dueAt).atZone(TaskDispatchValidation.zone).toLocalDateTime()
    val oldStart=task.startAt?.let {Instant.parse(it).atZone(TaskDispatchValidation.zone).toLocalDateTime()}
    var start by remember(request) {mutableStateOf("")}
    var due by remember(request) {mutableStateOf((request.target?.atTime(oldDue.toLocalTime()) ?: oldDue).format(scheduleFormat))}
    val selectedStart=TaskDispatchValidation.localTime(start)
    val selectedDue=TaskDispatchValidation.localTime(due)
    val earliest=listOfNotNull(currentDeadlineMinimum(),oldStart,selectedStart).max()
    val valid=(oldStart!=null || selectedStart!=null) && selectedDue!=null && selectedDue>=earliest
    AlertDialog(onDismissRequest=onDismiss,title={Text("任务改期")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(task.title)
            if(oldStart==null){
                Text("这项任务还没有开始时间，请先补填。")
                DeadlinePicker(start,true,label="开始时间",maxTime=selectedDue) {start=it}
            }
            DeadlinePicker(due,true,minTime=earliest,notBeforeNow=true) {due=it}
            if(selectedDue!=null && selectedDue<earliest)
                Text("截止时间不能早于开始时间或当前时间。",color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={ControlTextButton(enabled=valid,onClick={
        if(selectedDue!=null && selectedDue>=TaskDispatchValidation.earliestDeadlineMinute())
            onConfirm(TaskDispatchValidation.timestamp(due),start.takeIf {it.isNotBlank()}?.let(TaskDispatchValidation::timestamp))
    }){Text("确认改期")}},dismissButton={ControlTextButton(onClick=onDismiss){Text("取消")}})
}

@Composable private fun AgendaEntry(task: DispatchTask,state: AppState,enabled: Boolean,modifier: Modifier=Modifier,onMove: (() -> Unit)?=null,onClick: ()->Unit) {
    val background=MaterialTheme.colorScheme.surface.copy(alpha=0.96f)
    val foreground=MaterialTheme.colorScheme.onSurface
    Surface(color=background,contentColor=foreground,shape=MaterialTheme.shapes.medium,modifier=modifier.fillMaxWidth().panelOutline()) {
        Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(44.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(timestamp(task.dueAt.orEmpty()).substringAfter(' ').take(5),style=MaterialTheme.typography.labelMedium)
                Text("截止时间",style=MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.weight(1f).clickable(enabled=enabled,onClick=onClick),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(if(taskIsApproved(task))taskFeedbackLabel(task) else "创建${reviewStatus(task.approvalStatus)}",style=MaterialTheme.typography.labelSmall)
                Text(task.title,style=MaterialTheme.typography.bodyLarge)
                TaskPriorityText(task.priority,onLabel=false)
                if(task.detail.isNotBlank())Text(task.detail,style=MaterialTheme.typography.bodyMedium)
                if(task.progressNote.isNotBlank())Text("进度：${task.progressNote}",style=MaterialTheme.typography.bodySmall)
                CalendarLabelNames(contentTags(state,"task",task.id,task.label))
            }
            if(onMove!=null)ControlTextButton(onClick=onMove,enabled=enabled) {Text("改期")}
        }
    }
}
