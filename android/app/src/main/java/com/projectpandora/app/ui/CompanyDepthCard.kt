package com.projectpandora.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projectpandora.app.data.PanelItem

/** Native depth treatment: observe pointer position without consuming list scroll gestures. */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun CompanyDepthCard(entry: PanelItem,position: Int,modifier: Modifier=Modifier,compact: Boolean=false,labels: List<com.projectpandora.app.data.WorkLabel> = emptyList(),onLabels: (()->Unit)? = null,onClick: ()->Unit) {
    val interaction=remember {MutableInteractionSource()}
    val pressed by interaction.collectIsPressedAsState()
    var pointer by remember {mutableStateOf(Offset.Zero)}
    val motionEnabled=ValueAnimator.areAnimatorsEnabled()
    val tiltX by animateFloatAsState(if(motionEnabled)-pointer.y*4f else 0f,tween(180),label="company card tilt x")
    val tiltY by animateFloatAsState(if(motionEnabled)pointer.x*4f else 0f,tween(180),label="company card tilt y")
    val lift by animateFloatAsState(if(motionEnabled && pressed)1f else 0f,tween(180),label="company card depth")
    val colors=MaterialTheme.colorScheme
    val shape=MaterialTheme.shapes.medium
    val feedback=LocalControlFeedback.current
    val fontScale=androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    val cardHeight=(if(compact)104.dp else 244.dp)*fontScale
    Box(modifier.padding(end=3.dp,bottom=4.dp)) {
        Box(Modifier.matchParentSize().offset(x=3.dp,y=4.dp).background(colors.secondaryContainer,shape).border(1.dp,colors.outline,shape))
        Column(Modifier.fillMaxWidth().height(cardHeight)
            .graphicsLayer {
                rotationX=tiltX;rotationY=tiltY;cameraDistance=16*density
                translationY=-lift*2.dp.toPx();shadowElevation=(2+lift*4).dp.toPx()
                this.shape=shape;clip=true
            }
            .background(Brush.linearGradient(listOf(colors.surface,lerp(colors.surface,colors.secondary,0.07f))))
            .border(1.dp,colors.outline,shape)
            .pointerInput(entry.id) {
                awaitPointerEventScope {
                    while(true) {
                        val event=awaitPointerEvent(PointerEventPass.Initial)
                        val change=event.changes.firstOrNull()
                        pointer=if(event.type==PointerEventType.Exit || event.type==PointerEventType.Release || change==null)Offset.Zero
                        else Offset((change.position.x/size.width*2-1).coerceIn(-1f,1f),(change.position.y/size.height*2-1).coerceIn(-1f,1f))
                    }
                }
            }
            .clickable(interactionSource=interaction,indication=null,role=Role.Button,onClickLabel="查看事项详情") {feedback();onClick()}
            .padding(if(compact)8.dp else 12.dp),verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text((position+1).toString().padStart(2,'0'),style=if(compact)MaterialTheme.typography.labelSmall else MaterialTheme.typography.headlineSmall,color=colors.primary)
                Box(Modifier.size(width=20.dp,height=3.dp).background(colors.secondary))
            }
            if(!compact)Text("公司重点",style=MaterialTheme.typography.labelSmall,color=colors.secondary)
            Text(entry.title,style=MaterialTheme.typography.titleSmall,maxLines=if(compact)2 else 3,overflow=TextOverflow.Ellipsis)
            if(!compact)Text(entry.detail?.takeIf {it.isNotBlank()} ?: "查看完整工作安排",style=MaterialTheme.typography.bodySmall,
                color=colors.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                FlowRow(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {labels.forEach {label->Box(Modifier.size(width=18.dp,height=3.dp).background(labelColor(label),MaterialTheme.shapes.small))}}
                if(onLabels!=null)androidx.compose.material3.IconButton(onClick=onLabels,modifier=Modifier.size(28.dp)) {Text("≡",modifier=Modifier.semantics {contentDescription="设置事项标签"},color=colors.onSurfaceVariant)}
            }
            if(!compact)Text("查看事项 ↗",style=MaterialTheme.typography.labelSmall,color=colors.secondary)
        }
    }
}
