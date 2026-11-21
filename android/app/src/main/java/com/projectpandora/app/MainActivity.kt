package com.projectpandora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.projectpandora.app.ui.PandoraApp
import com.projectpandora.app.ui.ControlTheme

class MainActivity: ComponentActivity() {
    private val model: PandoraViewModel by viewModels()
    private val notificationPermission=registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        watchNotifications()
        if(intent.getBooleanExtra("inbox",false))model.screen("inbox")
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while(true) { model.loadNotifications(); delay(3000) }
            }
        }
        volumeControlStream=android.media.AudioManager.STREAM_MUSIC
        enableEdgeToEdge()
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            ControlTheme(forceLight=state.session==null,onAppearanceChange={dark->
                val bars=if(dark)SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle=bars,navigationBarStyle=bars)
            }) {
                PandoraApp(state,model)
            }
        }
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if(intent.getBooleanExtra("inbox",false))model.screen("inbox")
    }
    private fun watchNotifications() {
        val manager=getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(android.app.NotificationChannel("work","工作通知",android.app.NotificationManager.IMPORTANCE_DEFAULT))
        lifecycleScope.launch {
            var account: String?=null
            var seen=setOf<Long>()
            var initialized=false
            var asked=false
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect {state->
                    val key=state.session?.let {it.server+":"+it.userId}
                    if(key!=account){manager.cancelAll();account=key;seen=emptySet();initialized=false}
                    if(key==null || state.developmentPreview)return@collect
                    if(!asked && android.os.Build.VERSION.SDK_INT>=33) {
                        asked=true
                        if(checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if(state.notificationsLoading)return@collect
                    val current=state.notifications.map {it.id}.toSet()
                    if(initialized && manager.areNotificationsEnabled())state.notifications.filter {!it.read && it.id !in seen}.forEach {message->
                        val tap=android.app.PendingIntent.getActivity(this@MainActivity,0,
                            android.content.Intent(this@MainActivity,MainActivity::class.java).putExtra("inbox",true).addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP),
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                        manager.notify(message.id.toInt(),android.app.Notification.Builder(this@MainActivity,"work")
                            .setSmallIcon(com.projectpandora.app.R.drawable.console_bell).setContentTitle(message.title).setContentText(message.body)
                            .setVisibility(android.app.Notification.VISIBILITY_PRIVATE).setContentIntent(tap).setAutoCancel(true).build())
                    }
                    seen=seen+current;initialized=true
                }
            }
        }
    }

}
