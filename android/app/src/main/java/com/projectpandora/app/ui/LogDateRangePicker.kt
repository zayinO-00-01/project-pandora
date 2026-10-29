package com.projectpandora.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.projectpandora.app.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable fun LogDateRangeButton(start: String,end: String?,enabled: Boolean,onRange: (String,String)->Unit) {
    var open by remember {mutableStateOf(false)}
    ControlTextButton(onClick={open=true},enabled=enabled,contentPadding=PaddingValues(vertical=8.dp)) {
        Text(logDateLabel(start,end)+if(enabled)"  ▾" else "",style=MaterialTheme.typography.titleSmall)
    }
    if(!open)return
    val reference=today()
    val dates=(3L downTo 0L).map {reference.minusDays(it)}
    fun bounded(value: String?,floor: LocalDate): LocalDate {
        val date=runCatching {LocalDate.parse(value)}.getOrDefault(reference)
        return when {date<floor->floor;date>reference->reference;else->date}
    }
    var first by remember {mutableStateOf(bounded(start,dates.first()))}
    var last by remember {mutableStateOf(bounded(end ?: start,first))}
    AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={open=false},
        title={Text("选择连续日期")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("仅今天及往前 3 天，连续多日共用一份说明。",style=MaterialTheme.typography.bodySmall)
            RangeDateRow("开始",dates,first) {first=it;if(last<it)last=it}
            RangeDateRow("结束",dates,last,first) {last=it}
            Text(logDateLabel(first.toString(),last.toString()),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.secondary)
        }},confirmButton={ControlTextButton(onClick={onRange(first.toString(),last.toString());open=false}) {Text("确认日期")}},
        dismissButton={ControlTextButton(onClick={open=false}) {Text("取消")}})
}

@Composable private fun RangeDateRow(label: String,dates: List<LocalDate>,selected: LocalDate,min: LocalDate=dates.first(),onSelect: (LocalDate)->Unit) {
    val colors=MaterialTheme.colorScheme
    val feedback=LocalControlFeedback.current
    Text("${label}日期",style=MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        dates.forEach {date->
            OutlinedButton(onClick={feedback();onSelect(date)},enabled=date>=min,modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics {contentDescription="$label $date"},
                contentPadding=PaddingValues(horizontal=2.dp),shape=MaterialTheme.shapes.small,
                border=BorderStroke(1.dp,if(date==selected)colors.secondary else colors.outline),
                colors=ButtonDefaults.outlinedButtonColors(containerColor=if(date==selected)colors.secondaryContainer else Color.Transparent,contentColor=colors.onSurface)) {
                Text(date.format(DateTimeFormatter.ofPattern("MM/dd")),style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}
