package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.projectpandora.app.data.CALENDAR_FIRST_YEAR
import com.projectpandora.app.data.CALENDAR_LAST_YEAR
import com.projectpandora.app.data.jumpCalendarDate
import java.time.LocalDate

@Composable internal fun CalendarJumpDialog(selected: LocalDate,onDismiss: ()->Unit,onConfirm: (LocalDate)->Unit) {
    var yearInput by rememberSaveable(selected.toString(),stateSaver=TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(selected.year.toString(),TextRange(0,selected.year.toString().length)))
    }
    var month by rememberSaveable(selected.toString()) {mutableIntStateOf(selected.monthValue)}
    val year=yearInput.text.toIntOrNull()
    val target=year?.let {jumpCalendarDate(selected,it,month)}
    val keyboard=LocalSoftwareKeyboardController.current
    val feedback=LocalControlFeedback.current
    AlertDialog(
        modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),
        onDismissRequest=onDismiss,
        title={Text("选择年月")},
        text={Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value=yearInput,
                onValueChange={value->if(value.text.length<=4 && value.text.all {it in '0'..'9'})yearInput=value},
                modifier=Modifier.fillMaxWidth(),
                label={Text("年份")},
                supportingText={Text("请输入 $CALENDAR_FIRST_YEAR–$CALENDAR_LAST_YEAR 年")},
                isError=target==null,
                singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Done),
                keyboardActions=KeyboardActions(onDone={keyboard?.hide()})
            )
            Text("月份",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                (1..12).chunked(3).forEach {months->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        months.forEach {value->
                            FilterChip(
                                selected=month==value,
                                onClick={feedback();month=value;keyboard?.hide()},
                                label={Text("$value 月")},
                                modifier=Modifier.weight(1f).heightIn(min=48.dp).semantics {contentDescription="$value 月"}
                            )
                        }
                    }
                }
            }
            if(target!=null)Text(
                "跳转至 ${target.year} 年 ${target.monthValue} 月 ${target.dayOfMonth} 日"+
                    if(target.dayOfMonth!=selected.dayOfMonth)"（该月最后一天）" else "",
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant
            )
        }},
        confirmButton={ControlTextButton(enabled=target!=null,onClick={target?.let {keyboard?.hide();onConfirm(it)}}) {Text("跳转")}},
        dismissButton={ControlTextButton(onClick=onDismiss) {Text("取消")}}
    )
}
