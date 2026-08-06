package com.battleheim.quantum2048

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battleheim.quantum2048.designsystem.QuantumTheme
import com.battleheim.quantum2048.engine.GameEngine
import com.battleheim.quantum2048.engine.KotlinRandomProvider
import com.battleheim.quantum2048.ui.GameScreen
import com.battleheim.quantum2048.ui.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { QuantumTheme { val app = application as QuantumApp; val vm: GameViewModel = viewModel { GameViewModel(app.repository, GameEngine(KotlinRandomProvider())) }; GameScreen(vm) } } }
}
