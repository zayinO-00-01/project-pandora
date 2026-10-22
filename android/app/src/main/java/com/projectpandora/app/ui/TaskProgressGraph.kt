package com.projectpandora.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.projectpandora.app.data.DispatchTask
import com.projectpandora.app.data.taskStatusLabel

/** A qualitative workflow, not a percentage chart or invented historical time series. */
@Composable fun TaskProgressGraph(task: DispatchTask) {
    val stage=when(task.status) {"todo"->0;"doing"->1;"done"->2;else->null}
    val reveal=remember(task.id) {Animatable(0f)}
    LaunchedEffect(task.id,stage) {reveal.animateTo((stage ?: 0)/2f,tween(900,easing=FastOutSlowInEasing))}
    val line=MaterialTheme.colorScheme.secondary
    val accent=MaterialTheme.colorScheme.primary
    val grid=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.18f)
    val future=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.45f)
    val surface=MaterialTheme.colorScheme.surface
    val status=if(stage==null)"状态待同步" else taskStatusLabel(task.status)
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("任务工作进度",style=MaterialTheme.typography.titleMedium)
                Text(status,style=MaterialTheme.typography.labelLarge,color=line)
            }
            Text("派发后持续推进，及时记录每次工作反馈。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Canvas(Modifier.fillMaxWidth().height(128.dp).semantics {
                contentDescription="任务阶段曲线，当前$status。曲线表示工作阶段，不表示完成百分比或历史耗时。"
            }) {
                val left=12.dp.toPx();val right=size.width-left
                val top=12.dp.toPx();val bottom=size.height-top
                val points=listOf(Offset(left,bottom),Offset((left+right)/2,size.height/2),Offset(right,top))
                val dash=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),5.dp.toPx()))
                points.forEach {point->drawLine(grid,Offset(left,point.y),Offset(right,point.y),1.dp.toPx(),pathEffect=dash)}
                val curve=Path().apply {
                    moveTo(points[0].x,points[0].y)
                    for(i in 1..2) {
                        val a=points[i-1];val b=points[i];val bend=(b.x-a.x)*0.55f
                        cubicTo(a.x+bend,a.y,b.x-bend,b.y,b.x,b.y)
                    }
                }
                drawPath(curve,future,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round,pathEffect=dash))
                if(stage!=null) {
                    val measure=PathMeasure().apply {setPath(curve,false)}
                    val distance=measure.length*reveal.value
                    val active=Path();measure.getSegment(0f,distance,active,true)
                    if(distance>0f) {
                        val end=measure.getPosition(distance)
                        val area=Path().apply {addPath(active);lineTo(end.x,size.height);lineTo(left,size.height);close()}
                        drawPath(area,Brush.verticalGradient(listOf(line.copy(alpha=0.18f),Color.Transparent)))
                        drawPath(active,line,style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
                    }
                    points.forEachIndexed {index,point->
                        drawCircle(if(index<=stage)line else future,4.dp.toPx(),point)
                        drawCircle(surface,2.dp.toPx(),point)
                    }
                    val current=measure.getPosition(distance)
                    drawCircle(accent.copy(alpha=0.16f),10.dp.toPx(),current)
                    drawCircle(accent,5.dp.toPx(),current)
                    drawCircle(surface,2.dp.toPx(),current)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("待开始","进行中","已完成").forEachIndexed {index,label->
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        Text(label,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.labelLarge,
                            color=if(stage==index)accent else MaterialTheme.colorScheme.onSurface)
                        Text(if(stage==null)"待同步" else if(index==stage)"当前阶段" else if(index<stage)"已达成阶段" else "后续阶段",
                            modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider()
            Text("派发时间  ${timestamp(task.createdAt)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("最近更新  ${timestamp(task.updatedAt)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("按当前状态展示工作阶段",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
