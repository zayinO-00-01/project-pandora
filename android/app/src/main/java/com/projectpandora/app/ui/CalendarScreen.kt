package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.Instant
import java.time.format.DateTimeFormatter

private val scheduleFormat=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
private data class CalendarScheduleRequest(val task: CalendarTaskPreview,val target: LocalDate?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CalendarScreen(state: AppState,model: PandoraViewModel) {
    val session=state.session ?: return
    var overflowDay by rememberSaveable(session.server,session.userId) {mutableStateOf<String?>(null)}
    var showHelp by remember {mutableStateOf(false)}
    val selected=LocalDate.parse(state.selectedDate)
    val mode=state.calendarMode
    val feedback=LocalControlFeedback.current
    val listState=rememberLazyListState()
    val itemDrag=remember {CalendarItemDrag()}
    var moveRequest by remember(session) {mutableStateOf<CalendarScheduleRequest?>(null)}
    fun move(task: CalendarTaskPreview,target: LocalDate) {
        val start=task.startAt?.let(Instant::parse)
        val targetTime=Instant.parse(task.dueAt).atZone(TaskDispatchValidation.zone).toLocalTime()
        val due=target.atTime(targetTime).atZone(TaskDispatchValidation.zone).toInstant()
        if(start!=null && due>=start && due>=Instant.now())
            model.moveCalendarTask(task.id,due.toString())
        else moveRequest=CalendarScheduleRequest(task,target)
    }
    var expanded by rememberSaveable {mutableStateOf(false)}
    LaunchedEffect(mode,expanded) {listState.scrollToItem(0)}
    val revision=state.calendarRevision
    val dates=companyCalendarDates(selected,mode)
    val from=dates.first()
    val to=dates.last()
    val previewTasks=if(state.developmentPreview)remember(state.tasks) {
        orderedTasks(state.tasks.filter {taskIsApproved(it) && !it.deleted && taskDate(it)!=null}).groupBy {taskDate(it)}
    } else emptyMap()
    fun preview(task: DispatchTask)=CalendarTaskPreview(task.id,task.title,requireNotNull(task.dueAt),task.startAt,
        canRescheduleTask(state,task),contentTags(state,"task",task.id,task.label))
    var summaries by remember(session,from,to,revision) {mutableStateOf<Map<String,CalendarDaySummary>?>(null)}
    var summaryError by remember(session,from,to,revision) {mutableStateOf<String?>(null)}
    LaunchedEffect(session,from,to,revision,mode=="day",state.developmentPreview) {
        if(mode=="day" || state.developmentPreview)return@LaunchedEffect
        try {summaries=model.calendarSummary(session,from,to).associateBy {it.date}}
        catch(e:CancellationException) {throw e}
        catch(e:Exception) {summaryError=e.message ?: "暂时无法读取日历摘要"}
    }
    var agenda by remember(session,selected,revision) {mutableStateOf<CalendarAgendaPage?>(null)}
    var agendaError by remember(session,selected,revision) {mutableStateOf<String?>(null)}
    var agendaLoading by remember(session,selected,revision) {mutableStateOf(!state.developmentPreview)}
    var page by remember(session,selected,revision) {mutableIntStateOf(0)}
    var retry by remember(session,selected,revision) {mutableIntStateOf(0)}
    LaunchedEffect(session,selected,revision,page,retry,state.developmentPreview) {
        if(state.developmentPreview)return@LaunchedEffect
        try {
            val result=model.calendarAgenda(session,selected,page)
            agenda=if(page==0)result else agenda?.append(result) ?: result
        } catch(e:CancellationException) {throw e}
          catch(e:Exception) {agendaError=e.message ?: "暂时无法读取当天任务"}
        finally {agendaLoading=false}
    }
    fun nextPage() {
        if(agendaLoading)return
        val next=agenda?.nextPage ?: return
        agendaError=null
        agendaLoading=true
        page=next
    }
    fun retryAgenda() {
        if(agendaLoading)return
        agendaError=null
        agendaLoading=true
        retry++
    }
    val demoAgenda=previewTasks[selected].orEmpty().map {task->
        CalendarAgendaItem(task,canRescheduleTask(state,task),contentTags(state,"task",task.id,task.label))
    }
    val agendaItems=if(state.developmentPreview)demoAgenda else agenda?.items.orEmpty()
    val agendaTotal=if(state.developmentPreview)demoAgenda.size.toLong() else agenda?.total
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
                    val summary=if(state.developmentPreview) {
                        val tasks=previewTasks[date].orEmpty()
                        CalendarDaySummary(date.toString(),tasks.size.toLong(),tasks.take(2).map(::preview))
                    } else summaries?.get(date.toString())
                    summary?.shown.orEmpty().forEach {task->
                        val canMove=task.canReschedule && !state.saving
                        CalendarItemChip(task.title,task.labels,
                            Modifier.calendarDrag(itemDrag,task.title,date,if(canMove){{target->move(task,target)}} else null)
                                .clickable(enabled=!state.saving) {feedback();model.selectDate(date.toString());model.openTask(task.id)})
                    }
                    val more=summary?.remaining ?: 0L
                    if(more>0)CalendarOverflow(more) {
                        feedback()
                        model.selectDate(date.toString())
                        overflowDay=date.toString()
                    }
                }
            }
        if(!state.developmentPreview && mode!="day" && summaries==null && summaryError==null)item {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        summaryError?.let {item {Hint("日历摘要：$it",true,onRetry=model::refreshCalendar)}}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ControlButton(onClick=model::openDispatch,enabled=!state.saving,modifier=Modifier.weight(1f)) {Text("创建任务")}
            if(!state.developmentPreview)ControlTextButton(onClick=model::refreshCalendar,enabled=!state.saving) {Text("刷新日历")}
        }}
        item {HorizontalDivider(Modifier.padding(top=12.dp),color=MaterialTheme.colorScheme.outlineVariant)}
        val agendaDate=selected
            item {Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha=0.96f),MaterialTheme.shapes.medium).panelOutline().padding(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${agendaDate.monthValue} 月 ${agendaDate.dayOfMonth} 日 · 周${listOf("一","二","三","四","五","六","日")[agendaDate.dayOfWeek.value-1]}",style=MaterialTheme.typography.titleMedium)
                    holidayLabel(agendaDate)?.let {Text(it,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)}
                    Text(agendaTotal?.let {"$it 项任务"} ?: if(agendaLoading)"正在读取任务…" else "任务暂不可用",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ControlTextButton(onClick={showHelp=true}) {Text("说明",style=MaterialTheme.typography.labelMedium)}
            }}
            if(agendaTotal==0L && !agendaLoading && agendaError==null)item {
                if(mode=="week")Text("暂无安排",modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                else Column(Modifier.fillMaxWidth().padding(vertical=24.dp)) {
                    Text("这一天暂无任务",style=MaterialTheme.typography.titleLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Medium))
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(agendaItems,key={"${agendaDate}-task-${it.task.id}"}) {entry->
                val task=entry.task
                val canMove=entry.canReschedule && !state.saving
                val schedule=CalendarTaskPreview(task.id,task.title,requireNotNull(task.dueAt),task.startAt,canMove,entry.labels)
                AgendaEntry(task,entry.labels,!state.saving,
                    Modifier.calendarDrag(itemDrag,task.title,agendaDate,if(canMove){{target->move(schedule,target)}} else null,fromAgenda=true),
                    onMove=if(canMove) {{moveRequest=CalendarScheduleRequest(schedule,null)}} else null,onLabel=model::openTaskLabel
                ) {feedback();model.openTask(task.id)}
            }
        item(key="calendar-agenda-status") {
            CalendarPageStatus(agendaLoading,agendaError,agenda?.nextPage!=null,::retryAgenda,::nextPage)
        }
        item {CompanyWorkLinks(state,model)}
    }
    }
    }
    overflowDay?.takeIf {it==selected.toString()}?.let { value ->
        val date=LocalDate.parse(value)
        ModalBottomSheet(onDismissRequest={overflowDay=null},
            sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
            containerColor=MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(start=16.dp,end=8.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Text("${date.monthValue} 月 ${date.dayOfMonth} 日" + (agendaTotal?.let {" · $it 项任务"} ?: ""),
                    modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                IconButton(onClick={overflowDay=null}) {Icon(Icons.Default.Close,"关闭当天任务")}
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max=440.dp),
                contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=16.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(agendaItems,key={it.task.id}) { entry ->
                    AgendaEntry(entry.task,entry.labels,!state.saving,onLabel={label->
                        overflowDay=null
                        model.openTaskLabel(label)
                    }) {
                        feedback()
                        overflowDay=null
                        model.openTask(entry.task.id)
                    }
                }
                item {CalendarPageStatus(agendaLoading,agendaError,agenda?.nextPage!=null,::retryAgenda,::nextPage)}
            }
        }
    }
    moveRequest?.let { request ->
        TaskScheduleDialog(request,{moveRequest=null}) { due,start ->
            moveRequest=null
            model.moveCalendarTask(request.task.id,due,start)
        }
    }
    if(showHelp)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={showHelp=false},title={Text("日历怎么用")},text={Text("点日期可在日历下方查看当天任务，点「+数字」可展开当天列表，点「加载更多」继续查看。点击任务卡上的标签可筛选任务。点年月可直接跳转。长按任务标签或下方的任务卡片可拖到新日期，也可以点“改期”。创建人和责任人可申请改期，由本人直属上级审核；创始人改期直接生效。")},confirmButton={ControlTextButton(onClick={showHelp=false}) {Text("关闭")}})
}
@Composable private fun CalendarPageStatus(loading: Boolean,error: String?,hasMore: Boolean,retry: ()->Unit,next: ()->Unit) {
    if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
    else if(error!=null)Hint("当天任务：$error",true,onRetry=retry)
    else if(hasMore)ControlTextButton(onClick=next,modifier=Modifier.fillMaxWidth()) {Text("加载更多")}
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

@Composable private fun AgendaEntry(task: DispatchTask,labels: List<WorkLabel>,enabled: Boolean,modifier: Modifier=Modifier,onMove: (() -> Unit)?=null,onLabel: (WorkLabel)->Unit,onClick: ()->Unit) {
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
                CompactLabelRow(labels,onClick=onLabel.takeIf {enabled})
            }
            if(onMove!=null)ControlTextButton(onClick=onMove,enabled=enabled) {Text("改期")}
        }
    }
}
