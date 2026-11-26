package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.data.DispatchTask

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun TaskKeywords(task: DispatchTask) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("历史关键词",style=MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                task.keywords.orEmpty().forEach {keyword->
                    Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=CircleShape) {
                        Text("# ${keyword.text}",modifier=Modifier.padding(horizontal=12.dp,vertical=7.dp),style=MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
