package com.projectpandora.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val logs=remember(state.logs,state.session?.userId) {
        state.logs.filter {it.userId==state.session?.userId}.sortedByDescending {it.updatedAt}
    }
    WorkspaceCalendar(selected,onDate={model.selectDate(it.toString())}) {date->
        val entries=logs.filter {it.covers(date.toString())}
        val shown=if(entries.size>2)1 else 2
        entries.take(shown).forEach {log->
            val title=log.content.lineSequence().firstOrNull {it.isNotBlank()} ?: "工作日志"
            CalendarItemChip(title,contentTags(state,"log",log.id,log.label),
                Modifier.clickable(onClickLabel="查看当天日志") {model.selectDate(date.toString())})
        }
        if(entries.size>shown)CalendarOverflow(entries.size-shown) {model.selectDate(date.toString())}
    }
}
