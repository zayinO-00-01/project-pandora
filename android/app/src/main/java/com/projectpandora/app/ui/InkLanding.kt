package com.projectpandora.app.ui

import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.projectpandora.app.R
import com.projectpandora.app.BuildConfig
import android.util.Log
import com.projectpandora.app.AppState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/** Independent fixed-palette entrance. No account data, no playback outside it. */
@Composable fun InkLogin(state: AppState,onLogin: (String,String,String)->Unit,onDevelopmentEntry: ()->Unit) {
    val context=LocalContext.current
    val reduced=remember {Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f}
    var backgroundReady by remember {mutableStateOf(reduced)}
    var showForm by remember {mutableStateOf(reduced)}
    LaunchedEffect(backgroundReady) {
        if(backgroundReady) {if(!reduced)delay(1000);showForm=true}
    }
    // An unavailable decoder must not keep login hidden.
    LaunchedEffect(Unit) {delay(5000);if(!backgroundReady) {backgroundReady=true;showForm=true}}
    Box(Modifier.fillMaxSize().background(ConsoleTokens.Paper).safeDrawingPadding()) {
        InkAutoplayFilm(Modifier.fillMaxSize(),onReady={backgroundReady=true},onFailure={showForm=true})
        LoginScreen(state,onLogin,onDevelopmentEntry,showBackground=false,revealed=showForm,revealDuration=if(reduced)0 else 1600)
    }
}

@Composable private fun InkAutoplayFilm(modifier: Modifier,onReady: ()->Unit,onFailure: ()->Unit) {
    val context=LocalContext.current
    val reduced=remember {Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f}
    val player=remember {MediaPlayer()}
    var ready by remember {mutableStateOf(false)}
    var initialized by remember {mutableStateOf(false)}
    var hasFrame by remember {mutableStateOf(false)}
    var failed by remember {mutableStateOf(false)}
    var finished by remember {mutableStateOf(false)}
    val lifecycleOwner=LocalLifecycleOwner.current
    var surfaceAvailable by remember {mutableStateOf(false)}
    DisposableEffect(player,lifecycleOwner) {
        player.setVolume(0f,0f)
        player.setOnPreparedListener {
            ready=true
            it.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            if(surfaceAvailable && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))it.start()
        }
        player.setOnInfoListener {_,what,_->
            if(what==MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {hasFrame=true;onReady()}
            false
        }
        player.setOnCompletionListener {finished=true;if(BuildConfig.DEBUG)Log.i("InkPlayback","completed ${it.metrics}")}
        player.setOnErrorListener {_,what,extra->if(BuildConfig.DEBUG)Log.e("InkPlayback","failed $what/$extra");ready=false;failed=true;hasFrame=false;onFailure();true}
        val observer=LifecycleEventObserver {_,event->
            if(ready && event==Lifecycle.Event.ON_STOP && player.isPlaying)player.pause()
            if(ready && surfaceAvailable && !finished && event==Lifecycle.Event.ON_START)player.start()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {lifecycleOwner.lifecycle.removeObserver(observer);ready=false;player.release()}
    }
    Box(modifier) {
        if(!reduced && !failed)AndroidView(modifier=Modifier.matchParentSize(),factory={ctx->SurfaceView(ctx).apply {
            // Decode directly to the compositor; no transparent texture copy per frame.
            holder.addCallback(object:SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    surfaceAvailable=true
                    player.setDisplay(holder)
                    if(Build.VERSION.SDK_INT>=30)holder.surface.setFrameRate(60f,Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
                    if(!initialized) {
                        initialized=true
                        player.setDataSource(ctx,Uri.parse("android.resource://${ctx.packageName}/${R.raw.ink_journey_60}"))
                        player.prepareAsync()
                    } else if(ready) {
                        if(finished)player.seekTo((player.duration-17).coerceAtLeast(0).toLong(),MediaPlayer.SEEK_CLOSEST)
                        else if(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))player.start()
                    }
                }
                override fun surfaceChanged(holder: SurfaceHolder,format: Int,width: Int,height: Int) {}
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    surfaceAvailable=false
                    if(ready) {if(player.isPlaying)player.pause();player.setDisplay(null)}
                }
            })
        }})
        // Cover the decoder's initial empty surface, then remove this full-screen layer.
        if(!hasFrame || reduced || failed)Image(painterResource(R.drawable.paper_background_balanced),contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.matchParentSize())
    }
}
