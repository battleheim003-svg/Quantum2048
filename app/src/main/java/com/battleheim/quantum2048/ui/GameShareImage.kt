package com.battleheim.quantum2048.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.net.Uri
import android.view.View.MeasureSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.runtime.CompositionLocalProvider
import com.battleheim.quantum2048.designsystem.QuantumTheme
import com.battleheim.quantum2048.engine.FusionRules
import com.battleheim.quantum2048.engine.GameState
import com.battleheim.quantum2048.engine.Tile
import androidx.compose.ui.unit.Density
import com.battleheim.quantum2048.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

private const val SHARE_IMAGE_WIDTH = 1080
private const val SHARE_IMAGE_HEIGHT = 1350
private const val SHARE_IMAGE_NAME = "quantum_2048_share.png"

data class ShareImageColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
) {
    companion object {
        fun from(colorScheme: ColorScheme): ShareImageColors = ShareImageColors(
            background = colorScheme.background,
            surface = colorScheme.surface,
            surfaceVariant = colorScheme.surfaceVariant,
            primary = colorScheme.primary,
            secondary = colorScheme.secondary,
            onSurface = colorScheme.onSurface,
            onSurfaceVariant = colorScheme.onSurfaceVariant,
            outline = colorScheme.outline,
        )
    }
}

fun createShareImageUri(context: Context, game: GameState, colors: ShareImageColors): Uri {
    val bitmap = ShareBannerBuilder.renderToBitmap(context, game, colors)
    val directory = File(context.cacheDir, "share_images").apply { mkdirs() }
    val file = File(directory, SHARE_IMAGE_NAME)
    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

fun sharePromptFor(context: Context, game: GameState): String {
    val energyLevel = game.cells
        .mapNotNull { it?.let(FusionRules::gameValueOf) }
        .maxOrNull() ?: 0
    return context.getString(R.string.share_prompt, energyLevel)
}

object ShareBannerBuilder {
    fun renderToBitmap(context: Context, game: GameState, colors: ShareImageColors): Bitmap {
        val bitmap = Bitmap.createBitmap(SHARE_IMAGE_WIDTH, SHARE_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val view = ComposeView(context).apply {
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    QuantumTheme {
                        ShareBanner(game, colors)
                    }
                }
            }
        }
        val widthSpec = MeasureSpec.makeMeasureSpec(SHARE_IMAGE_WIDTH, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(SHARE_IMAGE_HEIGHT, MeasureSpec.EXACTLY)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, SHARE_IMAGE_WIDTH, SHARE_IMAGE_HEIGHT)
        view.draw(AndroidCanvas(bitmap))
        view.disposeComposition()
        return bitmap
    }
}

@Composable
private fun ShareBanner(game: GameState, colors: ShareImageColors) {
    val highestTile = game.cells.filterNotNull().maxByOrNull { FusionRules.gameValueOf(it) }
    val highestLabel = highestTile?.let { highestElementLabel(it) } ?: stringResource(R.string.share_no_element)
    val stars = starRating(game.score)
    Box(
        Modifier
            .size(width = SHARE_IMAGE_WIDTH.dp, height = SHARE_IMAGE_HEIGHT.dp)
            .background(colors.background),
    ) {
        ShareBannerBackdrop(colors)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 72.dp, vertical = 58.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            BannerHeader(game, colors)
            ScoreHero(score = game.score, highestLabel = highestLabel, stars = stars, colors = colors)
            BoardPreview(game, colors)
            Spacer(Modifier.weight(1f))
            PremiumWatermark(
                studio = stringResource(R.string.studio_name),
                tagline = stringResource(R.string.share_watermark_tagline),
                colors = colors,
            )
        }
    }
}

@Composable
private fun BannerHeader(game: GameState, colors: ShareImageColors) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("QUANTUM 2048", color = colors.primary, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.share_run_report), color = colors.onSurfaceVariant, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "${game.difficulty.name}\n${game.size}x${game.size}",
            color = colors.secondary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
            lineHeight = 28.sp,
        )
    }
}

@Composable
private fun ScoreHero(score: Long, highestLabel: String, stars: Int, colors: ShareImageColors) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(colors.onSurface.copy(alpha = 0.13f), colors.surface.copy(alpha = 0.92f), colors.surfaceVariant.copy(alpha = 0.72f))),
                RoundedCornerShape(8.dp),
            )
            .border(2.dp, colors.primary.copy(alpha = 0.58f), RoundedCornerShape(8.dp))
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.share_final_score), color = colors.onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(formatNumber(score), color = colors.onSurface, fontSize = 86.sp, fontWeight = FontWeight.Black, lineHeight = 88.sp)
        StarRatingRow(stars, colors)
        Text(stringResource(R.string.share_highest_element, highestLabel), color = colors.secondary, fontSize = 25.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StarRatingRow(stars: Int, colors: ShareImageColors) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            StarIcon(filled = index < stars, colors = colors)
        }
    }
}

@Composable
private fun StarIcon(filled: Boolean, colors: ShareImageColors) {
    Canvas(Modifier.size(68.dp)) {
        val path = starPath(Rect(Offset.Zero, size), points = 5)
        drawPath(
            path = path,
            color = if (filled) colors.secondary else colors.background.copy(alpha = 0f),
        )
        drawPath(
            path = path,
            color = if (filled) colors.onSurface.copy(alpha = 0.35f) else colors.onSurfaceVariant.copy(alpha = 0.72f),
            style = Stroke(width = if (filled) 3.4f else 4.2f, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun BoardPreview(game: GameState, colors: ShareImageColors) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(690.dp)
            .background(colors.surface.copy(alpha = 0.80f), RoundedCornerShape(8.dp))
            .border(2.dp, colors.primary.copy(alpha = 0.58f), RoundedCornerShape(8.dp))
            .padding(22.dp),
    ) {
        val padding = 26f
        val gap = if (game.size >= 6) 10f else 16f
        val boardSize = minOf(size.width, size.height) - padding * 2f
        val left = (size.width - boardSize) / 2f
        val top = (size.height - boardSize) / 2f
        drawRoundRect(
            color = colors.background.copy(alpha = 0.66f),
            topLeft = Offset(left, top),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(24f, 24f),
        )
        val cell = (boardSize - padding * 2f - gap * (game.size - 1)) / game.size
        game.cells.forEachIndexed { index, tile ->
            val row = index / game.size
            val column = index % game.size
            val x = left + padding + column * (cell + gap)
            val y = top + padding + row * (cell + gap)
            drawTile(tile, Offset(x, y), cell, colors)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTile(tile: Tile?, topLeft: Offset, cell: Float, colors: ShareImageColors) {
    val color = when {
        tile == null -> colors.surfaceVariant.copy(alpha = 0.40f)
        tile.element != null -> colors.primary
        tile.superpositionValues.isNotEmpty() -> colors.secondary
        else -> colors.surfaceVariant
    }
    drawRoundRect(color = color, topLeft = topLeft, size = Size(cell, cell), cornerRadius = CornerRadius(18f, 18f))
    drawRoundRect(
        color = colors.onSurface.copy(alpha = if (tile == null) 0.10f else 0.58f),
        topLeft = topLeft,
        size = Size(cell, cell),
        cornerRadius = CornerRadius(18f, 18f),
        style = Stroke(width = if (tile == null) 1.4f else 3f),
    )
    if (tile != null) {
        val label = if (tile.element != null) FusionRules.displaySymbol(tile) else FusionRules.gameValueOf(tile).toString()
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textSize = (cell * 0.28f).coerceIn(18f, 48f)
            setColor(colors.onSurface.toArgb())
        }
        val x = topLeft.x + cell / 2f
        val y = topLeft.y + cell / 2f - (paint.descent() + paint.ascent()) / 2f
        drawContext.canvas.nativeCanvas.drawText(label, x, y, paint)
    }
}

@Composable
private fun ShareBannerBackdrop(colors: ShareImageColors) {
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(colors.primary.copy(alpha = 0.18f), radius = 330f, center = Offset(size.width * 0.88f, size.height * 0.12f))
        drawCircle(colors.secondary.copy(alpha = 0.15f), radius = 360f, center = Offset(size.width * 0.12f, size.height * 0.72f))
        repeat(4) { index ->
            rotate(degrees = -26f + index * 24f, pivot = Offset(size.width * 0.68f, size.height * 0.22f)) {
                drawOval(
                    color = colors.primary.copy(alpha = 0.08f - index * 0.01f),
                    topLeft = Offset(size.width * 0.26f, size.height * (0.12f + index * 0.015f)),
                    size = Size(size.width * 0.74f, 98f + index * 22f),
                    style = Stroke(width = 2f),
                )
            }
        }
    }
}

@Composable
private fun PremiumWatermark(studio: String, tagline: String, colors: ShareImageColors) {
    Canvas(Modifier.fillMaxWidth().height(92.dp)) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textSize = 42f
            color = colors.onSurface.toArgb()
            setShadowLayer(18f, 0f, 0f, colors.primary.copy(alpha = 0.86f).toArgb())
        }
        drawContext.canvas.nativeCanvas.drawText(studio, size.width / 2f, size.height * 0.56f, paint)
        paint.clearShadowLayer()
        paint.textSize = 22f
        paint.color = colors.onSurfaceVariant.copy(alpha = 0.86f).toArgb()
        drawContext.canvas.nativeCanvas.drawText(tagline, size.width / 2f, size.height * 0.90f, paint)
    }
}

private fun starRating(score: Long): Int = when {
    score > 10_000 -> 3
    score >= 5_000 -> 2
    score >= 2_000 -> 1
    else -> 0
}

private fun highestElementLabel(tile: Tile): String =
    tile.element?.let { "${it.symbol} (${it.title})" } ?: FusionRules.gameValueOf(tile).toString()

private fun starPath(rect: Rect, points: Int): Path {
    val center = rect.center
    val outer = minOf(rect.width, rect.height) * 0.48f
    val inner = outer * 0.46f
    val path = Path()
    repeat(points * 2) { index ->
        val radius = if (index % 2 == 0) outer else inner
        val angle = -Math.PI / 2.0 + index * Math.PI / points
        val point = Offset(
            x = center.x + cos(angle).toFloat() * radius,
            y = center.y + sin(angle).toFloat() * radius,
        )
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}
