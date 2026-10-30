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
@Composable fun DeadlinePicker(value: String, enabled: Boolean, label: String = "截止时间", required: Boolean = true, onChange: (String)->Unit) {
    val context=LocalContext.current
    val format=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
    val selected=runCatching {LocalDateTime.parse(value,format)}.getOrNull()
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("$label（${if(required)"必选" else "选填"}）",style=MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ControlOutlinedButton(onClick={
                val initial=selected ?: LocalDateTime.of(today(),LocalTime.of(18,0))
                DatePickerDialog(context,{_,year,month,day->
                    TimePickerDialog(context,{_,hour,minute->
                        onChange(LocalDateTime.of(year,month+1,day,hour,minute).format(format))
                    },initial.hour,initial.minute,true).show()
                },initial.year,initial.monthValue-1,initial.dayOfMonth).show()
            },enabled=enabled,modifier=Modifier.weight(1f).heightIn(min=48.dp)) {
                Text(selected?.format(DateTimeFormatter.ofPattern("M月d日 HH:mm")) ?: if(value.isNotBlank())"重新选择$label" else "选择日期和时间")
            }
            if(!required && value.isNotBlank())ControlTextButton(onClick={onChange("")},enabled=enabled,modifier=Modifier.heightIn(min=48.dp)) {Text("清除")}
        }
        Text(if(required)"按北京时间安排，提交前必须选择。" else "按北京时间安排，可留空，不指定开始时间。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
