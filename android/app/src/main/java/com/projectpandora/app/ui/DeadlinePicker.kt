package com.projectpandora.app.ui

import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.projectpandora.app.data.today
import java.time.*
import java.time.format.DateTimeFormatter

/** The calendar chooses the date; independent wheels choose Shanghai-local hours and minutes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DeadlinePicker(value: String,enabled: Boolean,label: String="截止时间",required: Boolean=true,minDate: LocalDate?=null,onChange: (String)->Unit) {
    var open by remember {mutableStateOf(false)}
    val format=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
    val selected=runCatching {LocalDateTime.parse(value,format)}.getOrNull()
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text("$label（${if(required)"必选" else "选填"}）",style=MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ControlOutlinedButton(onClick={open=true},enabled=enabled,modifier=Modifier.weight(1f).heightIn(min=48.dp)) {
                Text(selected?.format(format) ?: "选择日期和时间")
            }
            if(!required && value.isNotBlank())ControlTextButton(onClick={onChange("")},enabled=enabled) {Text("清除")}
        }
    }
    if(open) {
        val initial=selected ?: LocalDateTime.of(today(),if(required)LocalTime.of(18,0) else LocalTime.of(9,0))
        val date=rememberDatePickerState(initialSelectedDateMillis=maxOf(initial.toLocalDate(),minDate ?: initial.toLocalDate()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),selectableDates=object:SelectableDates {override fun isSelectableDate(utcTimeMillis: Long)=minDate==null || !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(minDate)})
        var hour by remember {mutableIntStateOf(initial.hour)}
        var minute by remember {mutableIntStateOf(initial.minute)}
        ModalBottomSheet(onDismissRequest={open=false},containerColor=MaterialTheme.colorScheme.surface,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom=16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    ControlTextButton(onClick={open=false}) {Text("取消")}
                    Text(label,style=MaterialTheme.typography.titleMedium)
                    ControlTextButton(onClick={
                        date.selectedDateMillis?.let {millis->
                            val chosen=Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onChange(chosen.atTime(hour,minute).format(format));open=false
                        }
                    },enabled=date.selectedDateMillis!=null) {Text("保存")}
                }
                DatePicker(state=date,title=null,headline=null,showModeToggle=false,colors=DatePickerDefaults.colors(containerColor=MaterialTheme.colorScheme.surface),modifier=Modifier.fillMaxWidth())
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(horizontal=32.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                    TimeWheel(hour,23,"小时",Modifier.weight(1f)) {hour=it}
                    Text(":",style=MaterialTheme.typography.titleLarge)
                    TimeWheel(minute,59,"分钟",Modifier.weight(1f)) {minute=it}
                }
            }
        }
    }
}

@Composable private fun TimeWheel(value: Int,max: Int,label: String,modifier: Modifier,onChange: (Int)->Unit) {
    val color=MaterialTheme.colorScheme.onSurface.toArgb()
    val callback by rememberUpdatedState(onChange)
    AndroidView(modifier=modifier.height(136.dp),factory={context->NumberPicker(context).apply {
        minValue=0;maxValue=max;wrapSelectorWheel=true
        descendantFocusability=ViewGroup.FOCUS_BLOCK_DESCENDANTS
        setFormatter {"%02d".format(it)}
        contentDescription=label
        setOnValueChangedListener {_,_,number->callback(number)}
    }},update={picker->
        if(picker.value!=value)picker.value=value
        if(android.os.Build.VERSION.SDK_INT>=29)picker.textColor=color
    })
}
