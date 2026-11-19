package com.projectpandora.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.time.LocalDate

internal class CalendarItemDrag {
    val cells=mutableMapOf<LocalDate,Rect>()
    var origin=Offset.Zero
    var item by mutableStateOf<Item?>(null)
    var position by mutableStateOf(Offset.Zero)
    var hover by mutableStateOf<LocalDate?>(null)
    data class Item(val title:String,val touch:Offset)
    fun clear(){item=null;hover=null}
}

@Composable internal fun CalendarDragLayer(drag: CalendarItemDrag,content: @Composable ()->Unit) {
    Box(Modifier.fillMaxWidth().onGloballyPositioned {drag.origin=it.positionInRoot()}) {
        content()
        drag.item?.let {item->
            Text(item.title,color=MaterialTheme.colorScheme.onSecondaryContainer,style=MaterialTheme.typography.labelMedium,
                maxLines=1,modifier=Modifier.zIndex(1f).widthIn(max=150.dp).graphicsLayer {
                    val at=drag.position-drag.origin
                    translationX=at.x
                    translationY=at.y
                    shadowElevation=6.dp.toPx()
                    shape=androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                }.background(MaterialTheme.colorScheme.secondaryContainer,MaterialTheme.shapes.small).padding(horizontal=8.dp,vertical=4.dp))
        }
    }
}

@Composable internal fun Modifier.calendarDrag(drag: CalendarItemDrag?,title: String,date: LocalDate,onMove: ((LocalDate)->Unit)?): Modifier {
    if(drag==null || onMove==null)return this
    var position by remember {mutableStateOf(Offset.Zero)}
    val move by rememberUpdatedState(onMove)
    return onGloballyPositioned {position=it.positionInRoot()}.pointerInput(drag,title,date) {
        detectDragGesturesAfterLongPress(
            onDragStart={touch->
                drag.item=CalendarItemDrag.Item(title,touch)
                drag.position=position
                drag.hover=date
            },
            onDrag={change,delta->
                change.consume()
                val item=drag.item
                if(item!=null){
                    drag.position+=delta
                    val point=drag.position+item.touch
                    drag.hover=drag.cells.entries.firstOrNull {it.value.contains(point)}?.key
                }
            },
            onDragEnd={val target=drag.hover;drag.clear();if(target!=null && target!=date)move(target)},
            onDragCancel=drag::clear
        )
    }
}
