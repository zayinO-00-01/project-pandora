package com.projectpandora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.projectpandora.app.ui.PandoraApp

class MainActivity: ComponentActivity() {
    private val model: PandoraViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme=lightColorScheme(
                primary=Color(0xFF183F35), onPrimary=Color(0xFFF5F3ED),
                secondary=Color(0xFFAF663F), secondaryContainer=Color(0xFFF1E3D2),
                background=Color(0xFFF5F3ED), surface=Color(0xFFFFFEF9),
                surfaceVariant=Color(0xFFE8EDDF), onSurfaceVariant=Color(0xFF536451),
                outline=Color(0xFF8B9783)
            )) {
                val state by model.state.collectAsStateWithLifecycle()
                PandoraApp(state,model)
            }
        }
    }
}
