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
import com.projectpandora.app.ui.PandoraApp
import com.projectpandora.app.ui.ControlTheme

class MainActivity: ComponentActivity() {
    private val model: PandoraViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
