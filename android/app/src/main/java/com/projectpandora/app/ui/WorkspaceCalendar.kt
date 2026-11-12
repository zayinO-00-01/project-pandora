package com.projectpandora.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectpandora.app.data.*
import java.time.LocalDate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.text.font.FontWeight

@Composable internal fun WorkspaceCalendar(
    selected: LocalDate,
    onDate: (LocalDate)->Unit,
    mode: String="month",
    onMode: ((String)->Unit)?=null,
    gridHeight: androidx.compose.ui.unit.Dp?=null,
    onResizeToggle: (() -> Unit)?=null,
    dayContent: @Composable (LocalDate)->Unit
) {
    var showDateJump by rememberSaveable {mutableStateOf(false)}
    val dates=if(mode=="week") {val first=selected.minusDays((selected.dayOfWeek.value%7).toLong());(0..6).map {first.plusDays(it.toLong())}} else periodDates(selected,mode)
    val weekdays=listOf("日","一","二","三","四","五","六")
    val unit=when(mode) {"week"->"周";"day"->"天";else->"月"}
    fun navigate(amount: Long) {
        onDate(when(mode) {"week"->selected.plusWeeks(amount);"day"->selected.plusDays(amount);else->selected.plusMonths(amount)})
    }
    val feedback=LocalControlFeedback.current
    val weekCount=(selected.withDayOfMonth(1).dayOfWeek.value%7+selected.lengthOfMonth()+6)/7
    val baseRowHeight=84f
    val normalHeight=baseRowHeight*weekCount
    val height=gridHeight?.value ?: if(mode=="week")baseRowHeight else normalHeight
    val calendarRowHeight=maxOf(baseRowHeight,height/weekCount).dp
    val collapse=((normalHeight-height)/(normalHeight-baseRowHeight)).coerceIn(0f,1f)
    val density=LocalDensity.current
    val monthDates=periodDates(selected,"month")
    val gridStart=monthDates.first().minusDays((monthDates.first().dayOfWeek.value%7).toLong())
    val padded=List(weekCount*7) {gridStart.plusDays(it.toLong())}
    val selectedRow=padded.indexOf(selected)/7
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        if(onMode!=null)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("month" to "月视图","week" to "周视图","day" to "日视图").forEach {(key,label)->
                FilterChip(selected=mode==key,onClick={feedback();onMode(key)},label={Text(label)},colors=FilterChipDefaults.filterChipColors(containerColor=MaterialTheme.colorScheme.surface.copy(alpha=0.92f),selectedContainerColor=MaterialTheme.colorScheme.secondaryContainer),modifier=Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            val heading=if(mode=="week")"${dates.first().year}年${dates.first().monthValue}月${dates.first().dayOfMonth}日 — ${dates.last().year}年${dates.last().monthValue}月${dates.last().dayOfMonth}日" else "${selected.year} 年 ${selected.monthValue} 月"
            Column(Modifier.weight(1f).heightIn(min=48.dp).clickable(role=Role.Button,onClickLabel="选择年月",onClick={feedback();showDateJump=true}).semantics(mergeDescendants=true) {contentDescription="$heading，选择年月"}.padding(vertical=6.dp)) {
                Text(heading,style=MaterialTheme.typography.titleMedium.copy(fontWeight=FontWeight.Medium,fontFeatureSettings="tnum"))
                Text("选择年月 ▾",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick={feedback();navigate(-1)},modifier=Modifier.background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small)) {Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft,"上一$unit",modifier=Modifier.size(20.dp))}
            ControlTextButton(onClick={onDate(today())},modifier=Modifier.widthIn(min=48.dp).background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.secondary)) {Text("今天",style=MaterialTheme.typography.labelMedium)}
            IconButton(onClick={feedback();navigate(1)},modifier=Modifier.background(MaterialTheme.colorScheme.background.copy(alpha=0.96f),MaterialTheme.shapes.small)) {Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"下一$unit",modifier=Modifier.size(20.dp))}
        }
        if(mode!="day")Column(Modifier.fillMaxWidth()) {
            Row {weekdays.forEach {Text(it,modifier=Modifier.weight(1f).padding(vertical=8.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelMedium.copy(fontWeight=FontWeight.Medium),color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            Box(Modifier.fillMaxWidth().height(height.dp).clipToBounds()) {
            // Measure every month row before clipping to the selected week.
            Column(Modifier.offset {IntOffset(0,(-selectedRow*baseRowHeight*collapse*density.density).roundToInt())}.wrapContentHeight(androidx.compose.ui.Alignment.Top,unbounded=true)) {
                padded.chunked(7).forEach {week->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach {date->
                            val festival=remember(date) {festivalName(date)}
                            ControlTextButton(onClick={onDate(date)},
                                modifier=Modifier.weight(1f).height(calendarRowHeight).semantics {
                                    this.selected=date==selected
                                    contentDescription=listOfNotNull(date.toString(),festival,holidayScheduleLabel(date)).distinct().joinToString("，")
                                },shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(0.dp),
                                colors=ButtonDefaults.textButtonColors(containerColor=if(date==selected)MaterialTheme.colorScheme.secondary.copy(alpha=.12f) else androidx.compose.ui.graphics.Color.Transparent)) {
                                Column(horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {CalendarDateNumber(date,date==selected);HolidayBadge(date)}
                                    if(festival!=null)Text(festival,maxLines=1,overflow=TextOverflow.Ellipsis,
                                        style=MaterialTheme.typography.labelSmall.copy(fontSize=9.sp,lineHeight=11.sp),color=MaterialTheme.colorScheme.secondary)
                                    Column(Modifier.fillMaxWidth().heightIn(min=28.dp).padding(horizontal=2.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {dayContent(date)}
                                    if(date==selected)Box(Modifier.width(16.dp).height(2.dp).background(MaterialTheme.colorScheme.secondary))
                                }
                            }
                        }
                    }
                }
            }
            }
            if(onResizeToggle!=null)Box(Modifier.fillMaxWidth().height(28.dp).semantics {
                contentDescription="调整日历大小"
            }.clickable(onClick=onResizeToggle),contentAlignment=androidx.compose.ui.Alignment.Center) {
                Box(Modifier.size(width=36.dp,height=4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.5f),MaterialTheme.shapes.small))
            }
        }
    }
    if(showDateJump)CalendarJumpDialog(selected,{showDateJump=false}) {onDate(it);showDateJump=false}
}

@Composable private fun CalendarDateNumber(date: LocalDate,selected: Boolean) {
    val colors=MaterialTheme.colorScheme
    Box(Modifier.size(30.dp).background(if(selected)colors.secondary else androidx.compose.ui.graphics.Color.Transparent,androidx.compose.foundation.shape.CircleShape),contentAlignment=androidx.compose.ui.Alignment.Center) {
        Text(date.dayOfMonth.toString(),color=if(selected)colors.onSecondary else if(date==today())colors.secondary else colors.onSurface,
            style=MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings="tnum",fontSize=18.sp,lineHeight=24.sp,fontWeight=if(selected || date==today())FontWeight.SemiBold else FontWeight.Normal))
    }
}
