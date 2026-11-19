package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.data.*

@Composable fun LogDayButton(date: String,end: String?,enabled: Boolean,logs: List<WorkLog>,onSelect: (String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("日志日期",style=MaterialTheme.typography.labelLarge)
        if(!enabled)Text(logDateLabel(date,end),style=MaterialTheme.typography.titleSmall)
        else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("今天","昨天","前天").forEachIndexed {offset,label->
                val value=today().minusDays(offset.toLong()).toString()
                val exists=logs.any {it.covers(value)}
                FilterChip(selected=date==value,onClick={onSelect(value)},enabled=!exists,label={Column {Text(label);Text(value.takeLast(5)+if(exists)" · 已写" else "",style=MaterialTheme.typography.labelSmall)}})
            }
        }
        if(enabled && logs.any {it.covers(date)})Text("所选日期已有日志，请返回列表打开原日志。",color=MaterialTheme.colorScheme.error)
    }
}
