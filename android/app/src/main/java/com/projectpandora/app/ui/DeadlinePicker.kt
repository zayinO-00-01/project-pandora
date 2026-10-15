package com.projectpandora.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.projectpandora.app.data.today
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Draft values remain Shanghai local time; TaskDispatchValidation converts to an instant. */
@Composable fun DeadlinePicker(value: String, enabled: Boolean, onChange: (String)->Unit) {
    val context=LocalContext.current
    val format=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
    val selected=runCatching {LocalDateTime.parse(value,format)}.getOrNull()
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("截止时间（选填）",style=MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ControlOutlinedButton(onClick={
                val initial=selected ?: LocalDateTime.of(today(),LocalTime.of(18,0))
                DatePickerDialog(context,{_,year,month,day->
                    TimePickerDialog(context,{_,hour,minute->
                        onChange(LocalDateTime.of(year,month+1,day,hour,minute).format(format))
                    },initial.hour,initial.minute,true).show()
                },initial.year,initial.monthValue-1,initial.dayOfMonth).show()
            },enabled=enabled,modifier=Modifier.weight(1f).heightIn(min=48.dp)) {
                Text(selected?.format(DateTimeFormatter.ofPattern("M月d日 HH:mm")) ?: if(value.isNotBlank())"重新选择截止时间" else "选择日期和时间")
            }
            if(value.isNotBlank())TextButton(onClick={onChange("")},enabled=enabled,modifier=Modifier.heightIn(min=48.dp)) {Text("清除")}
        }
        Text("按北京时间安排；不选择则不限制截止时间。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
