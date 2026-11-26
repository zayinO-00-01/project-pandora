package com.projectpandora.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.projectpandora.app.data.AppNotification
import com.projectpandora.app.data.NewWorkNotifications
import com.projectpandora.app.data.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.projectpandora.app.ui.PandoraApp
import com.projectpandora.app.ui.ControlTheme

class MainActivity: ComponentActivity() {
    private val model: PandoraViewModel by viewModels()
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val arrival = NewWorkNotifications()
    private var account: Pair<String, Long>? = null
    private var pendingInbox: Pair<String, Long>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) readInboxIntent(intent)
        watchWorkNotifications()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readInboxIntent(intent)
        maybeOpenInbox(model.state.value)
    }

    private fun readInboxIntent(intent: Intent) {
        val server = intent.getStringExtra(INBOX_SERVER) ?: return
        val user = intent.getLongExtra(INBOX_USER, 0L)
        if (user > 0L) pendingInbox = server to user
    }

    private fun maybeOpenInbox(state: AppState) {
        val session = state.session ?: return
        val target = pendingInbox ?: return
        if (target != (session.server to session.userId)) {
            pendingInbox = null
        } else if (!state.saving && state.editor == null && !state.dispatchOpen &&
            (state.taskEditor == null || state.taskEditor.taskId != state.selectedTaskId)) {
            pendingInbox = null
            model.screen("notifications")
        }
    }

    private fun watchWorkNotifications() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "工作通知", NotificationManager.IMPORTANCE_HIGH))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    while (true) {
                        model.loadNotifications(silent = true)
                        delay(3000L)
                    }
                }
                model.state.collect { state ->
                    val session = state.session?.takeUnless { state.developmentPreview }
                    val key = session?.let { it.server to it.userId }
                    if (key != account) {
                        manager.cancelAll()
                        account = key
                        arrival.reset()
                    }
                    maybeOpenInbox(state)
                    if (session == null) return@collect
                    requestPermissionOnce()
                    if (!state.notificationsReady) return@collect
                    val messages = arrival.accept(state.notifications)
                    if (manager.areNotificationsEnabled()) messages.forEach { showWorkNotification(manager, session, it) }
                }
            }
        }
    }

    private fun requestPermissionOnce() {
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        val preferences = getSharedPreferences("work-notification-permission", MODE_PRIVATE)
        if (!preferences.getBoolean("asked", false)) {
            preferences.edit().putBoolean("asked", true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showWorkNotification(manager: NotificationManager, session: Session, item: AppNotification) {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(INBOX_SERVER, session.server).putExtra(INBOX_USER, session.userId)
        val tap = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.console_bell)
            .setContentTitle(item.title).setContentText(item.body).setStyle(Notification.BigTextStyle().bigText(item.body))
            .setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(tap).build()
        manager.notify("work-${item.id}", 0, notification)
    }

    private companion object {
        const val CHANNEL = "work-alerts"
        const val INBOX_SERVER = "pandora.inbox.server"
        const val INBOX_USER = "pandora.inbox.user"
    }
}
