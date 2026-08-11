package com.battleheim.quantum2048.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.battleheim.quantum2048.R
import com.battleheim.quantum2048.designsystem.Cyan
import com.battleheim.quantum2048.designsystem.Electric
import com.battleheim.quantum2048.designsystem.GlassPanel
import com.battleheim.quantum2048.designsystem.NeonPink
import com.battleheim.quantum2048.designsystem.RadiantGold
import com.battleheim.quantum2048.designsystem.TextMuted
import com.battleheim.quantum2048.designsystem.TextSecondary
import com.battleheim.quantum2048.engine.Difficulty

@Composable
fun Difficulty.localizedLabel(): String = when (this) {
    Difficulty.EASY -> stringResource(R.string.easy)
    Difficulty.MEDIUM -> stringResource(R.string.medium)
    Difficulty.HARD -> stringResource(R.string.hard)
    Difficulty.QUANTUM -> stringResource(R.string.quantum)
    Difficulty.ZEN -> stringResource(R.string.zen)
    Difficulty.HARDCORE -> stringResource(R.string.hardcore)
    Difficulty.PUZZLE -> stringResource(R.string.puzzle)
    Difficulty.DAILY -> stringResource(R.string.daily)
}

@Composable
fun QuantumActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Cyan,
    filled: Boolean = false,
    icon: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, spring(stiffness = 520f), label = "quantum_button_scale")
    val shape = RoundedCornerShape(24.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) accent else MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
            contentColor = if (filled) Color(0xFF061016) else accent,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
            disabledContentColor = TextMuted,
        ),
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = if (enabled) 0.20f else 0.06f),
                        Color.Transparent,
                        NeonPink.copy(alpha = if (enabled && !filled) 0.12f else 0.02f),
                    ),
                ),
                shape,
            )
            .border(1.2.dp, accent.copy(alpha = if (enabled) 0.78f else 0.24f), shape),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Text(it, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
            }
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 0.35.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun QuantumChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Electric,
) {
    QuantumActionButton(
        text = text,
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        accent = accent,
        filled = selected,
        icon = if (selected) "✓" else null,
    )
}

@Composable
fun QuantumDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Cyan,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        ),
                    ),
                    RoundedCornerShape(22.dp),
                )
                .border(1.4.dp, accent.copy(alpha = 0.76f), RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title.uppercase(), color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Box(
                Modifier
                    .fillMaxWidth(0.56f)
                    .height(2.dp)
                    .background(Brush.horizontalGradient(listOf(accent, Color.Transparent)), RoundedCornerShape(2.dp)),
            )
            content()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                dismissText?.let {
                    QuantumActionButton(text = it, onClick = onDismiss, modifier = Modifier.weight(1f), accent = TextSecondary)
                }
                if (confirmText != null && onConfirm != null) {
                    QuantumActionButton(text = confirmText, onClick = onConfirm, modifier = Modifier.weight(1f), accent = accent, filled = true)
                }
            }
        }
    }
}

fun actionAccent(index: Int): Color = when (index % 4) {
    0 -> Cyan
    1 -> RadiantGold
    2 -> Electric
    else -> NeonPink
}
