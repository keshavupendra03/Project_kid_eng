package com.samjhoenglish.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private val confettiColors = listOf(
    Color(0xFFFFC93C), Color(0xFF4FB3FF), Color(0xFFFF8A3D), Color(0xFF3E9B46), Color(0xFFFF8FA3),
)

private class Piece(val x: Float, val delay: Float, val speed: Float, val size: Float, val spin: Float, val color: Color)

/** A short burst of falling paper. Draws only, so taps pass through to the buttons underneath. */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 36) {
    val pieces = remember {
        List(count) {
            Piece(
                x = Random.nextFloat(),
                delay = Random.nextFloat() * 0.35f,
                speed = 0.8f + Random.nextFloat() * 0.5f,
                size = 8f + Random.nextFloat() * 10f,
                spin = 180f + Random.nextFloat() * 540f,
                color = confettiColors.random(),
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(2200, easing = LinearEasing)) }

    Canvas(modifier.fillMaxSize()) {
        if (progress.value >= 1f) return@Canvas
        pieces.forEach { piece ->
            val t = ((progress.value - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (t > 0f) {
                val position = Offset(piece.x * size.width, t * piece.speed * size.height - piece.size)
                rotate(degrees = t * piece.spin, pivot = position) {
                    drawRect(piece.color.copy(alpha = 1f - t * t), topLeft = position, size = Size(piece.size, piece.size * 0.6f))
                }
            }
        }
    }
}
