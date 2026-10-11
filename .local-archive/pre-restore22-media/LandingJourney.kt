package com.projectpandora.app.ui

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.projectpandora.app.R
import kotlinx.coroutines.launch
import androidx.compose.animation.Crossfade
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke

private val JourneySerif=FontFamily(Font(R.font.landing_serif,FontWeight.Medium))

/** A fixed-color, reversible scroll film. Login remains reachable without watching it. */
@Composable fun LandingJourney(content: @Composable ()->Unit) {
    val scroll=rememberScrollState()
    val scope=rememberCoroutineScope()
    val chapterHeight=maxOf(600,LocalConfiguration.current.screenHeightDp).dp
    val density=LocalDensity.current
    val chapterPx=with(density){chapterHeight.toPx()}
    val progress=(scroll.value/(chapterPx*2)).coerceIn(0f,1f)
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Box(Modifier.fillMaxSize().background(ConsoleTokens.LandingPaper))
        ScrollFilm(progress,Modifier.fillMaxWidth().fillMaxHeight(0.85f).align(Alignment.BottomCenter))
        Box(Modifier.fillMaxWidth().height(360.dp).background(Brush.verticalGradient(listOf(ConsoleTokens.LandingPaper,Color.Transparent))))
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Spacer(Modifier.height(chapterHeight*3))
            Surface(color=ConsoleTokens.LandingPaper,modifier=Modifier.fillMaxWidth().heightIn(min=chapterHeight)) {
                Column(Modifier.fillMaxWidth().padding(top=72.dp,bottom=32.dp),horizontalAlignment=Alignment.CenterHorizontally) {content()}
            }
        }
        val story=listOf("让每一步工作，\n都有迹可循。" to "潘多拉 · 团队与个人的工作记录",
            "今天的进展。" to "写下完成的事，留住下一步。",
            "把线索，\n连成时间线。" to "日志与任务，在同一张月历相遇。")
        val copyAlpha=((chapterPx*2.85f-scroll.value)/(chapterPx*0.3f)).coerceIn(0f,1f)
        val stage=when {progress<0.42f->0;progress<0.78f->1;else->2}
        val compact=maxHeight<650.dp || density.fontScale>1.1f
        if(copyAlpha>0f)Crossfade(targetState=stage,label="journey-copy",modifier=Modifier.widthIn(max=700.dp).fillMaxWidth().align(Alignment.TopCenter).padding(horizontal=24.dp,vertical=if(compact)80.dp else if(stage==0)112.dp else 96.dp).graphicsLayer {alpha=copyAlpha}) {chapter->
            Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(if(compact)8.dp else 16.dp),horizontalAlignment=if(chapter==2)Alignment.CenterHorizontally else Alignment.Start) {
                Text("0${chapter+1} / 03",style=MaterialTheme.typography.labelSmall,color=ConsoleTokens.Copper)
                Text(story[chapter].first,style=MaterialTheme.typography.headlineMedium.copy(fontFamily=JourneySerif,fontSize=if(compact)24.sp else 30.sp,lineHeight=if(compact)32.sp else 40.sp,fontWeight=FontWeight.Medium),textAlign=if(chapter==2)TextAlign.Center else TextAlign.Start)
                Text(story[chapter].second,style=if(chapter==2 || compact)MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,color=ConsoleTokens.Muted,textAlign=if(chapter==2)TextAlign.Center else TextAlign.Start)
                if(chapter==0)Text("向下滚动，展开工作之旅 ↓",style=MaterialTheme.typography.labelSmall,color=ConsoleTokens.Muted)
            }
        }
        if(stage>0 && copyAlpha>0f)Column(Modifier.align(Alignment.TopCenter).padding(top=maxHeight*0.32f).widthIn(max=544.dp).fillMaxWidth().padding(horizontal=32.dp).graphicsLayer {alpha=copyAlpha}) {LandingProof(stage,compact)}
        if(stage==2 && copyAlpha>0f)Column(Modifier.align(Alignment.BottomCenter).widthIn(max=544.dp).fillMaxWidth().padding(horizontal=32.dp,vertical=24.dp).graphicsLayer {alpha=copyAlpha}) {
            HorizontalDivider(color=ConsoleTokens.Copper)
            TextButton(onClick={scope.launch {scroll.scrollTo((chapterPx*3).toInt())}},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).background(ConsoleTokens.LandingPaper.copy(alpha=0.94f)),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.textButtonColors(contentColor=ConsoleTokens.Ink)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text("进入工作空间",modifier=Modifier.weight(1f),fontFamily=JourneySerif,fontSize=22.sp);Text("↗",fontSize=22.sp,color=ConsoleTokens.Copper)}
            }
        }
        Surface(color=Color.Transparent,modifier=Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("潘多拉",modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontFamily=JourneySerif)
                TextButton(onClick={scope.launch {scroll.scrollTo((chapterPx*3).toInt())}}) {Text("直接登录 ↗")}
            }
        }
    }
}

@Composable private fun ScrollFilm(progress: Float,modifier: Modifier) {
    val context=LocalContext.current
    val reduced=remember {android.provider.Settings.Global.getFloat(context.contentResolver,android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f}
    var hasFrame by remember {mutableStateOf(false)}
    val player=remember {MediaPlayer()}
    var ready by remember {mutableStateOf(false)}
    var seeking by remember {mutableStateOf(false)}
    var target by remember {mutableIntStateOf(0)}
    var surface: Surface? by remember {mutableStateOf(null)}
    fun seekLatest() {if(ready && !seeking && kotlin.math.abs(player.currentPosition-target)>16) {seeking=true;player.seekTo(target.toLong(),MediaPlayer.SEEK_CLOSEST)}}
    DisposableEffect(player) {
        player.setVolume(0f,0f)
        player.setOnPreparedListener {ready=true;it.start();seekLatest()}
        player.setOnSeekCompleteListener {seeking=false;seekLatest()}
        player.setOnErrorListener {_,_,_->ready=false;hasFrame=false;true}
        onDispose {ready=false;player.release();surface?.release()}
    }
    Box(modifier) {
        val poster=if(progress<0.42f)R.drawable.landing_initial_v5 else if(progress<0.78f)R.drawable.landing_record_v5 else R.drawable.landing_time_v5
        Image(painterResource(poster),contentDescription="从山水路径进入工作笔记，再展开为工作月历",contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
        if(!reduced)AndroidView(modifier=Modifier.fillMaxSize(),factory={ctx->TextureView(ctx).apply {
            isOpaque=false;alpha=0f
            fun fit(width: Int,height: Int) {val factor=maxOf(width/720f,height/960f);val m=Matrix();m.setScale(720f*factor/width,960f*factor/height,width/2f,height/2f);setTransform(m)}
            surfaceTextureListener=object: TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(texture: SurfaceTexture,width: Int,height: Int) {
                    fit(width,height);surface=Surface(texture);player.setSurface(surface)
                    player.setDataSource(ctx,Uri.parse("android.resource://${ctx.packageName}/${R.raw.landing_journey_v5}"));player.prepareAsync()
                }
                override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture,width: Int,height: Int) {fit(width,height)}
                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture)=true
                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {hasFrame=true;if(ready && player.isPlaying)player.pause()}
            }
        }},update={view->
            view.alpha=if(hasFrame)1f else 0f
            target=(progress*9000).toInt();seekLatest()
        })
    }
}

/** Clearly labelled examples explain the product without claiming to be account data. */
@Composable private fun LandingProof(stage: Int,compact: Boolean) {
    val scale=LocalDensity.current.fontScale.coerceAtLeast(1f)
    val wide=LocalConfiguration.current.screenWidthDp>=600
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom) {
        Text(if(stage==1)"10 月 11 日" else "2026 / 10",modifier=Modifier.weight(1f),fontFamily=JourneySerif,fontSize=if(stage==1)32.sp else 26.sp,lineHeight=38.sp)
        Text(if(stage==1)"工作日志 · 示例" else "工作轨迹 · 示例",fontSize=11.sp,lineHeight=16.sp,color=ConsoleTokens.Muted)
    }
    HorizontalDivider(Modifier.padding(top=14.dp),color=ConsoleTokens.Line)
    if(stage==1) {
        Spacer(Modifier.height(if(compact)18.dp else 24.dp))
        Text("已完成",fontSize=11.sp,lineHeight=16.sp,color=ConsoleTokens.Muted)
        Text("完成登录与权限联调。",modifier=Modifier.padding(top=10.dp),fontSize=if(compact)17.sp else if(wide)24.sp else 20.sp,lineHeight=if(compact)26.sp else 32.sp,fontWeight=FontWeight.Medium)
        Text("已确认员工仅能查看\n本人工作记录。",modifier=Modifier.padding(top=10.dp),fontSize=if(wide)15.sp else 13.sp,lineHeight=23.sp,color=ConsoleTokens.Muted)
        Spacer(Modifier.height(if(compact)18.dp else 32.dp))
        Row {
            Box(Modifier.width(1.dp).height(80.dp*scale).background(ConsoleTokens.Copper))
            Column(Modifier.padding(start=18.dp)) {Text("下一步",fontSize=11.sp,lineHeight=16.sp,color=ConsoleTokens.Muted);Text("整理异常网络场景，\n补充验收记录。",modifier=Modifier.padding(top=8.dp),fontSize=if(wide)15.sp else 14.sp,lineHeight=26.sp)}
        }
    } else {
        Spacer(Modifier.height(8.dp))
        Row {for(day in listOf("一","二","三","四","五","六","日"))Box(Modifier.weight(1f).height(22.dp*scale),contentAlignment=Alignment.Center) {Text(day,fontSize=10.sp,lineHeight=14.sp,color=ConsoleTokens.Muted)}}
        repeat(5) {week->Row {repeat(7) {column->val day=week*7+column-2
            Surface(color=Color.Transparent,border=if(day==11)BorderStroke(1.dp,ConsoleTokens.Copper) else null,modifier=Modifier.weight(1f).height((if(compact)19.dp else if(wide)28.dp else 22.dp)*scale)) {
                Box(contentAlignment=Alignment.Center) {
                    Text(if(day in 1..31)day.toString() else "",fontSize=if(wide)15.sp else 13.sp,lineHeight=16.sp,color=if(day==11)ConsoleTokens.Copper else ConsoleTokens.Muted)
                    if(day==9 || day==10)Box(Modifier.size(3.dp).align(Alignment.BottomCenter).background(ConsoleTokens.Muted,androidx.compose.foundation.shape.CircleShape))
                }
            }
        }}}
        HorizontalDivider(Modifier.padding(top=16.dp),color=ConsoleTokens.Line)
        val entries=listOf(Triple("09","对齐交付范围","日志 · 核对权限与反馈验收顺序"),Triple("10","恢复输入和草稿","日志 · 完成离线恢复测试"),Triple("11","完成日志模块联调","任务 · 17:00 截止 · 进展 60%"))
        Box(Modifier.fillMaxWidth().padding(top=16.dp)) {
            Box(Modifier.padding(start=3.dp,top=8.dp).width(1.dp).height((if(compact)124.dp else if(wide)164.dp else 146.dp)*scale).background(ConsoleTokens.Copper))
            Column {for((date,title,detail) in entries)Row(Modifier.heightIn(min=(if(compact)48.dp else if(wide)64.dp else 58.dp)*scale),verticalAlignment=Alignment.Top) {
                Box(Modifier.padding(top=8.dp).size(7.dp).background(ConsoleTokens.Copper,androidx.compose.foundation.shape.CircleShape))
                Text(date,modifier=Modifier.padding(start=11.dp,end=18.dp),fontFamily=JourneySerif,fontSize=20.sp,lineHeight=26.sp)
                Column {Text(title,fontSize=if(wide)16.sp else 14.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium);Text(detail,modifier=Modifier.padding(top=5.dp),fontSize=11.sp,lineHeight=18.sp,color=ConsoleTokens.Muted)}
            }}
        }
    }
}
