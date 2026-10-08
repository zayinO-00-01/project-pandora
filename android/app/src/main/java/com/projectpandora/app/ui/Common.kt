package com.projectpandora.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectpandora.app.data.today
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable fun DateButton(date: String, enabled: Boolean=true, onDate: (String)->Unit) {
    val context=LocalContext.current
    OutlinedButton(enabled=enabled,onClick={
        val current=runCatching {LocalDate.parse(date)}.getOrDefault(today())
        DatePickerDialog(context,{_,y,m,d -> onDate(LocalDate.of(y,m+1,d).toString())},current.year,current.monthValue-1,current.dayOfMonth).apply {
            datePicker.maxDate=today().atTime(23,59).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        }.show()
    }) {Text("$date  ▾")}
}
@Composable fun Hint(text: String, error: Boolean=false) {
    Surface(color=if(error)MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape=MaterialTheme.shapes.medium, modifier=Modifier.fillMaxWidth()) {
        Text(text,modifier=Modifier.padding(14.dp),fontSize=13.sp,
            color=if(error)MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun EmptyCard(title: String, description: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(24.dp)) {
        Text(title,style=MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp));Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }}
}
fun timestamp(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")).format(Instant.parse(value))
}.getOrDefault(value)
