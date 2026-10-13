package com.projectpandora.app.ui

import android.app.DatePickerDialog
import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectpandora.app.data.today
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.painterResource
import com.projectpandora.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning

@Composable fun WriteLogButton(enabled: Boolean=true,onClick: ()->Unit) {
    val feedback=LocalControlFeedback.current
    Box(Modifier.heightIn(min=48.dp),contentAlignment=androidx.compose.ui.Alignment.Center) {
        OutlinedButton(onClick={feedback();onClick()},enabled=enabled,modifier=Modifier.heightIn(min=36.dp),shape=MaterialTheme.shapes.small,border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.primary),colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.primary),contentPadding=PaddingValues(horizontal=10.dp,vertical=4.dp)) {
            Icon(painterResource(R.drawable.console_add),null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(4.dp));Text("写日志",style=MaterialTheme.typography.labelMedium,maxLines=1,softWrap=false)
        }
    }
}

@Composable fun InlineAction(label: String, enabled: Boolean=true, icon: Int?=null, onClick: ()->Unit) {
    Box(Modifier.heightIn(min=48.dp),contentAlignment=androidx.compose.ui.Alignment.Center) {
        Button(onClick=onClick,enabled=enabled,shape=MaterialTheme.shapes.small,
            modifier=Modifier.heightIn(min=36.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)) {
            if(icon!=null) {Icon(painterResource(icon),null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(4.dp))}
            Text(label,style=MaterialTheme.typography.labelMedium,maxLines=1,softWrap=false)
        }
    }
}

@Composable fun DateButton(date: String, enabled: Boolean=true, onDate: (String)->Unit) {
    val context=LocalContext.current
    val dark=LocalAppearance.current.mode==Appearance.DARK
    TextButton(enabled=enabled,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface),contentPadding=PaddingValues(horizontal=0.dp,vertical=8.dp),onClick={
        val current=runCatching {LocalDate.parse(date)}.getOrDefault(today())
        val dialogContext=ContextThemeWrapper(context,if(dark)android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert)
        DatePickerDialog(dialogContext,{_,y,m,d -> onDate(LocalDate.of(y,m+1,d).toString())},current.year,current.monthValue-1,current.dayOfMonth).apply {
            datePicker.maxDate=today().atTime(23,59).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        }.show()
    }) {Text("$date  ▾",style=MaterialTheme.typography.titleMedium)}
}
@Composable fun Hint(text: String, error: Boolean=false, onRetry: (()->Unit)?=null, retryEnabled: Boolean=true) {
    if(error) {
        var expanded by remember {mutableStateOf(false)}
        val summary=text.lineSequence().first().substringBefore('：') + if(text.contains("上次"))" · 上次内容" else ""
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Warning,null,tint=MaterialTheme.colorScheme.error,modifier=Modifier.size(16.dp))
            Text(summary,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error,maxLines=2)
            TextButton(onClick={expanded=true},colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurfaceVariant),contentPadding=PaddingValues(horizontal=8.dp)) {Text("详情",style=MaterialTheme.typography.labelMedium)}
            if(onRetry!=null)TextButton(onClick=onRetry,enabled=retryEnabled,contentPadding=PaddingValues(horizontal=8.dp)) {Text("重试",style=MaterialTheme.typography.labelMedium)}
        }
        if(expanded)AlertDialog(onDismissRequest={expanded=false},title={Text(summary)},text={Text(text)},confirmButton={TextButton(onClick={expanded=false}) {Text("关闭")}})
        return
    }
    Surface(color=if(error)MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape=MaterialTheme.shapes.medium, modifier=Modifier.fillMaxWidth()) {
        Text(text,modifier=Modifier.padding(14.dp),fontSize=13.sp,
            color=if(error)MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun EmptyCard(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical=24.dp)) {
        Text(title,style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp));Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
fun timestamp(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai")).format(Instant.parse(value))
}.getOrDefault(value)

@Composable fun ConsoleBanner(label: String, title: String, detail: String) {
    Surface(color=ConsoleTokens.Navy,contentColor=ConsoleTokens.White,shape=MaterialTheme.shapes.large,modifier=Modifier.fillMaxWidth()) {
        Box {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(label,style=MaterialTheme.typography.labelSmall,fontFamily=FontFamily.Monospace,color=ConsoleTokens.TealLight)
                Text(title,style=MaterialTheme.typography.headlineSmall)
                Text(detail,style=MaterialTheme.typography.bodySmall,color=ConsoleTokens.Line)
            }
        }
    }
}

// Preview exact text from the record; the editor always retains the entire content.
fun recordPreview(content: String): Pair<String,String> {
    val text=content.trim()
    val split=text.indexOfFirst {it=='，' || it=='。' || it=='\n'}
    return if(split>=0)text.substring(0,split).trim() to text.substring(split+1).trim()
        else text to ""
}
