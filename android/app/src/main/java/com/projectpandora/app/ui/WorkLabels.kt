package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.projectpandora.app.data.*

fun labelColor(label: WorkLabel?): Color = runCatching {Color(android.graphics.Color.parseColor(label?.color ?: "#F5F0E5"))}.getOrDefault(Color(0xFFF5F0E5))
fun labelTextColor(label: WorkLabel?): Color {
    val c=labelColor(label)
    return if(0.2126*c.red+0.7152*c.green+0.0722*c.blue>0.55)Color(0xFF162C30) else Color.White
}
@Composable fun LabelBadge(label: WorkLabel?) {
    if(label!=null)Surface(color=labelColor(label),contentColor=labelTextColor(label),shape=androidx.compose.foundation.shape.CircleShape) {Text(label.name,modifier=Modifier.padding(horizontal=8.dp,vertical=4.dp),style=MaterialTheme.typography.labelSmall)}
}
@Composable fun LabelPicker(labels: List<WorkLabel>,selected: Long?,enabled: Boolean,onSelect: (Long)->Unit) {
    var open by remember {mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("品牌标签",style=MaterialTheme.typography.labelLarge)
        Box {
            ControlOutlinedButton(onClick={open=true},enabled=enabled,modifier=Modifier.fillMaxWidth()) {Text(labels.firstOrNull {it.id==selected}?.name ?: if(selected!=null && selected>0)"保留已有标签" else "不使用标签")}
            DropdownMenu(expanded=open,onDismissRequest={open=false}) {
                DropdownMenuItem(text={Text("不使用标签")},onClick={onSelect(0);open=false})
                labels.forEach {label->DropdownMenuItem(text={LabelBadge(label)},onClick={onSelect(label.id);open=false})}
            }
        }
        if(labels.isEmpty())Text("暂无可选标签，由创始人在“我的”中维护。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
