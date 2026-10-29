package com.projectpandora.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.projectpandora.app.data.taskPriorityLabel

@Composable fun taskPriorityColor(priority: String): Color {
    val dark=LocalAppearance.current.mode==Appearance.DARK
    return when(priority) {
        "high"->if(dark)ConsoleTokens.ErrorLight else ConsoleTokens.Error
        "low"->if(dark)ConsoleTokens.NightMuted else ConsoleTokens.Muted
        else->if(dark)ConsoleTokens.TealLight else ConsoleTokens.Teal
    }
}

@Composable fun TaskPriorityText(priority: String,modifier: Modifier=Modifier,onLabel: Boolean=false) {
    if(onLabel)Surface(color=MaterialTheme.colorScheme.surface,shape=MaterialTheme.shapes.extraSmall) {Text(taskPriorityLabel(priority),modifier=modifier.padding(horizontal=4.dp,vertical=2.dp),color=taskPriorityColor(priority),style=MaterialTheme.typography.labelMedium)}
    else Text(taskPriorityLabel(priority),modifier=modifier,color=taskPriorityColor(priority),style=MaterialTheme.typography.labelMedium)
}
