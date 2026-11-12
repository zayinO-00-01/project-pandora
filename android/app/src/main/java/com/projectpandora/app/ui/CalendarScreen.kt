package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*
import java.time.LocalDate

@Composable fun CalendarScreen(state: AppState,model: PandoraViewModel) {
    var showHelp by remember {mutableStateOf(false)}
    val selected=LocalDate.parse(state.selectedDate)
    val mode=state.calendarMode
    val feedback=LocalControlFeedback.current
    val listState=rememberLazyListState()
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
        val scope=rememberCoroutineScope()
        val startDrag by rememberUpdatedState<(Float)->Boolean> { direction->
            val canResize=mode!="day" && (if(direction>0)currentSize!="expanded" else currentSize!="week")
            if(canResize) {
                heightTarget=height
                distance=0f
                dragging=true
                if(listState.firstVisibleItemIndex!=0 || listState.firstVisibleItemScrollOffset!=0)
                    scope.launch {listState.scrollToItem(0)}
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
        LazyColumn(Modifier.fillMaxSize().pointerInput(Unit) {
            awaitEachGesture {
                val down=awaitFirstDown(requireUnconsumed=false,pass=PointerEventPass.Initial)
                val velocity=VelocityTracker()
                velocity.addPosition(down.uptimeMillis,down.position)
                var resizing=false
                var released=false
                try {
                    while(true) {
                        val event=awaitPointerEvent(PointerEventPass.Initial)
                        val change=event.changes.firstOrNull {it.id==down.id} ?: break
                        if(change.isConsumed)break
                        velocity.addPosition(change.uptimeMillis,change.position)
                        if(!change.pressed) {released=true;break}
                        if(!resizing) {
                            val travel=change.position-down.position
                            if(abs(travel.x)>viewConfiguration.touchSlop && abs(travel.x)>abs(travel.y))break
                            if(abs(travel.y)<=viewConfiguration.touchSlop)continue
                            if(!startDrag(travel.y))break
                            resizing=true
                            resize(travel.y-sign(travel.y)*viewConfiguration.touchSlop)
                        } else {
                            resize(change.position.y-change.previousPosition.y)
                        }
                        change.consume()
                    }
                } finally {
                    if(resizing)finishDrag(if(released)velocity.calculateVelocity().y else null)
                }
            }
        }.padding(horizontal=16.dp),state=listState,verticalArrangement=Arrangement.spacedBy(4.dp),contentPadding=PaddingValues(bottom=32.dp)) {
            item {
                WorkspaceCalendar(selected,onDate={model.selectDate(it.toString())},mode=mode,
                    onMode={expanded=false;model.calendarMode(it)},gridHeight=height.dp,
                    onResizeToggle={applySize(if(currentSize=="normal")"week" else "normal")}) {date->
                    val dayTasks=tasksByDay[date].orEmpty()
                                    dayTasks.take(2).forEach {task->
                                        val tags=contentTags(state,"task",task.id,task.label)
                                        val primary=tags.firstOrNull()
                                        Row(Modifier.fillMaxWidth().padding(vertical=1.dp).background(if(primary!=null)labelColor(primary) else MaterialTheme.colorScheme.surfaceVariant,MaterialTheme.shapes.extraSmall),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                                            Text(task.title,maxLines=if(dayTasks.size==1)2 else 1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f).padding(horizontal=3.dp,vertical=1.dp),style=MaterialTheme.typography.labelSmall.copy(fontSize=9.5.sp,lineHeight=13.5.sp),color=if(primary!=null)labelTextColor(primary) else MaterialTheme.colorScheme.onSurfaceVariant)
                                            tags.drop(1).take(11).forEach {tag->Box(Modifier.padding(end=1.dp).size(width=2.dp,height=10.dp).background(labelColor(tag)))}
                                        }
                                    }
                }
            }
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
                    AgendaEntry(task,state,!state.saving) {feedback();model.openTask(task.id)}
                }
            }
        }
        item {TaskManagementSection(state.copy(tasks=state.tasks.filter {taskDate(it)==selected}),model)}
    }
    }
    if(showHelp)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={showHelp=false},title={Text("显示说明")},text={Text("日历仅显示任务，日志在日志页独立管理。月视图查看整月，周视图查看选中日期所在周，下方仅列出选中当天的工作安排，日视图专注当天详情。点击日期标题可直接输入年份并选择月份；跳转保留所选日期，超出当月天数时使用月末。箭头按当前视图切换月、周或天，“今天”返回当前日期。日期摘要最多两排，详细内容在下方显示。仅展示当前已同步内容，没有截止日期的任务可在任务列表查看。")},confirmButton={ControlTextButton(onClick={showHelp=false}) {Text("关闭")}})
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
