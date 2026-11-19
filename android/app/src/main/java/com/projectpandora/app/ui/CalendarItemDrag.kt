package com.projectpandora.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.time.LocalDate
import kotlin.math.roundToInt

internal class CalendarItemDrag {
    val cells=mutableMapOf<LocalDate,Rect>()
    val trayCells=mutableMapOf<LocalDate,Rect>()
    var origin=Offset.Zero
    var visibleTop by mutableFloatStateOf(0f)
    var item by mutableStateOf<Item?>(null)
    var position by mutableStateOf(Offset.Zero)
    var hover by mutableStateOf<LocalDate?>(null)
    var trayAnchor by mutableStateOf<LocalDate?>(null)
    data class Item(val title:String,val touch:Offset)
    fun dateAt(point: Offset): LocalDate? =
        trayCells.entries.firstOrNull {it.value.contains(point)}?.key ?: cells.entries.firstOrNull {it.value.contains(point)}?.key
    fun clear(){item=null;hover=null;trayAnchor=null;trayCells.clear()}
}

@Composable internal fun CalendarDragLayer(drag: CalendarItemDrag,content: @Composable ()->Unit) {
    Box(Modifier.fillMaxWidth().onGloballyPositioned {
        drag.origin=it.positionInRoot()
        drag.visibleTop=(it.boundsInRoot().top-drag.origin.y).coerceAtLeast(0f)
    }) {
        content()
        // List rows still have a date target when their calendar is scrolled away or in day view.
        drag.trayAnchor?.let {anchor->
            val first=anchor.minusDays((anchor.dayOfWeek.value%7).toLong())
            Surface(Modifier.zIndex(1f).offset {IntOffset(0,drag.visibleTop.roundToInt())}
                .fillMaxWidth().padding(horizontal=8.dp),shape=MaterialTheme.shapes.medium,
                color=MaterialTheme.colorScheme.surface,shadowElevation=6.dp) {
                Column(Modifier.padding(4.dp)) {
                    Text("拖到新日期",modifier=Modifier.padding(horizontal=6.dp),style=MaterialTheme.typography.labelMedium)
                    repeat(2){week->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7){day->
                                val date=first.plusDays((week*7+day).toLong())
                                Box(Modifier.weight(1f).height(36.dp)
                                    .onGloballyPositioned {drag.trayCells[date]=it.boundsInRoot()}
                                    .background(if(date==drag.hover)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface),
                                    contentAlignment=Alignment.Center) {
                                    Text("${date.monthValue}/${date.dayOfMonth}",style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        drag.item?.let {item->
            Text(item.title,color=MaterialTheme.colorScheme.onSecondaryContainer,style=MaterialTheme.typography.labelMedium,
                maxLines=1,modifier=Modifier.zIndex(2f).widthIn(max=150.dp).graphicsLayer {
                    val at=drag.position-drag.origin
                    translationX=at.x
                    translationY=at.y
                    shadowElevation=6.dp.toPx()
                    shape=androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                }.background(MaterialTheme.colorScheme.secondaryContainer,MaterialTheme.shapes.small).padding(horizontal=8.dp,vertical=4.dp))
        }
    }
}

@Composable internal fun Modifier.calendarDrag(drag: CalendarItemDrag?,title: String,date: LocalDate,onMove: ((LocalDate)->Unit)?,fromAgenda: Boolean=false): Modifier {
    if(drag==null || onMove==null)return this
    var position by remember(drag,date,title) {mutableStateOf(Offset.Zero)}
    val move by rememberUpdatedState(onMove)
    return onGloballyPositioned {position=it.positionInRoot()}.pointerInput(drag,title,date,fromAgenda) {
        detectDragGesturesAfterLongPress(
            onDragStart={touch->
                drag.item=CalendarItemDrag.Item(title,touch)
                drag.position=position
                drag.hover=date
                if(fromAgenda)drag.trayAnchor=date
            },
            onDrag={change,_->
                change.consume()
                val item=drag.item
                if(item!=null){
                    val point=position+change.position
                    drag.position=point-item.touch
                    drag.hover=drag.dateAt(point)
                }
            },
            onDragEnd={val target=drag.hover;drag.clear();if(target!=null && target!=date)move(target)},
            onDragCancel=drag::clear
        )
    }
}
