package com.battleheim.quantum2048

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.battleheim.quantum2048.designsystem.QuantumTheme
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.KotlinRandomProvider
import com.battleheim.quantum2048.ui.QuantumAppShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuantumTheme {
                val app = application as QuantumApp
                QuantumAppShell(
                    gameRepository = app.repository,
                    collectionRepository = app.collectionRepository,
                    settingsRepository = app.settingsRepository,
                    engine = GameEngine(KotlinRandomProvider()),
                )
            }
        }
    }
}
