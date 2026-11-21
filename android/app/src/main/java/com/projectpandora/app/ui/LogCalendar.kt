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
    WorkspaceCalendar(selected,onDate={model.selectDate(it.toString())}) {date->
        val content=logCalendarContent(date,state.session?.userId,state.logs,state.contentLabels)
        val entries=content.logs.map {log->
            (log.content.lineSequence().firstOrNull {it.isNotBlank()} ?: "工作日志") to contentTags(state,"log",log.id,log.label)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal=2.dp)) {
            if(entries.size>2) {
                entries.first().let {(title,tags)->CalendarItemChip(title,tags)}
                CalendarItemChip("另 ${entries.size-1} 项记录",emptyList())
            } else entries.forEach {(title,tags)->CalendarItemChip(title,tags)}
        }
    }
}
