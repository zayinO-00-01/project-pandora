package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*
import java.time.LocalDate

@Composable internal fun HolidayBadge(date: LocalDate) {
    val label=holidayScheduleLabel(date) ?: return
    val work=label=="调休上班"
    Text(if(work)"班" else "休",modifier=Modifier.semantics {contentDescription=label},
        style=MaterialTheme.typography.labelSmall,color=if(work)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
}

@Composable internal fun LogCalendar(state: AppState,model: PandoraViewModel) {
    val selected=LocalDate.parse(state.selectedDate)
    var jump by remember {mutableStateOf(false)}
    val first=selected.withDayOfMonth(1)
    val offset=first.dayOfWeek.value%7
    val logsByDay=state.logs
    val tasksByDay=state.tasks.filter {taskIsApproved(it) && !it.deleted && it.assigneeId==state.session?.userId}.groupBy {taskDate(it)}
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            ControlTextButton(onClick={jump=true},modifier=Modifier.weight(1f)) {Text("${selected.year} 年 ${selected.monthValue} 月 ▾")}
            ControlTextButton(onClick={model.selectDate(selected.minusMonths(1).toString())}) {Text("‹")}
            ControlTextButton(onClick={model.selectDate(today().toString())}) {Text("今天")}
            ControlTextButton(onClick={model.selectDate(selected.plusMonths(1).toString())}) {Text("›")}
        }
        Row {listOf("日","一","二","三","四","五","六").forEach {Text(it,Modifier.weight(1f),textAlign=TextAlign.Center)}}
        val cells=(offset+selected.lengthOfMonth()+6)/7*7
        (0 until cells).chunked(7).forEach {week->
            Row {
                week.forEach {index->
                    val day=index-offset+1
                    if(day !in 1..selected.lengthOfMonth())Spacer(Modifier.weight(1f))
                    else {
                        val date=first.withDayOfMonth(day)
                        val logCount=logsByDay.count {it.covers(date.toString())}
                        val taskCount=tasksByDay[date].orEmpty().size
                        ControlTextButton(onClick={model.selectDate(date.toString())},modifier=Modifier.weight(1f).height(64.dp),
                            contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.textButtonColors(containerColor=if(date==selected)MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                Row {Text(day.toString());HolidayBadge(date)}
                                if(logCount>0)Text("日志",style=MaterialTheme.typography.labelSmall,maxLines=1)
                                if(taskCount>0)Text("${taskCount}任务",style=MaterialTheme.typography.labelSmall,maxLines=1)
                            }
                        }
                    }
                }
            }
        }
    }
    if(jump)CalendarJumpDialog(selected,{jump=false}) {model.selectDate(it.toString());jump=false}
}
