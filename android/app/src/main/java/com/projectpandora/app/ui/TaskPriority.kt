package com.projectpandora.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

@Composable fun TaskPriorityText(priority: String,modifier: Modifier=Modifier) {
    Text(taskPriorityLabel(priority),modifier=modifier,color=taskPriorityColor(priority),style=MaterialTheme.typography.labelMedium)
}
