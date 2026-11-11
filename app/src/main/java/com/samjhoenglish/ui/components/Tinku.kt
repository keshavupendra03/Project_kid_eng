package com.samjhoenglish.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.staticCompositionLocalOf
import com.samjhoenglish.learning.Learning
import com.samjhoenglish.ui.theme.Blush
import com.samjhoenglish.ui.theme.BowRed
import com.samjhoenglish.ui.theme.Glow
import com.samjhoenglish.ui.theme.LocalSeniorLook
import com.samjhoenglish.ui.theme.Navy
import com.samjhoenglish.ui.theme.SaffronLight
import com.samjhoenglish.ui.theme.Sky
import com.samjhoenglish.ui.theme.SkyDark
import com.samjhoenglish.ui.theme.Sunny
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class TinkuMood { HAPPY, CHEER, CONFUSED }

/** The outfit pieces the current child has earned. */
val LocalTinkuPieces = staticCompositionLocalOf { emptySet<Learning.TinkuPiece>() }

/** Tinku the robot tutor. Bounces gently, blinks, waves (faster when cheering), and wears what's been earned. */
@Composable
fun Tinku(
    modifier: Modifier = Modifier,
    mood: TinkuMood = TinkuMood.HAPPY,
    size: Dp = 160.dp,
) {
    val pieces = LocalTinkuPieces.current
    val shownSize = if (LocalSeniorLook.current) size * 0.8f else size
    val motion = rememberInfiniteTransition(label = "tinku")
    val bounce by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce",
    )
    val glow by motion.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow",
    )
    val wave by motion.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave",
    )
    val eyes = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200, 4500))
            eyes.animateTo(0.1f, tween(80))
            eyes.animateTo(1f, tween(120))
        }
    }

    Canvas(
        modifier
            .size(width = shownSize, height = shownSize * 1.15f)
            .semantics { contentDescription = "Tinku" },
    ) {
        val u = this.size.width / 100f
        // Shadow stays on the ground while Tinku bounces.
        drawOval(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(28 * u, 108 * u),
            size = Size(44 * u, 5 * u),
        )
        translate(top = -bounce * 3 * u) {
            drawTinku(u, mood, eyes.value, glow, wave)
            drawOutfit(u, pieces)
        }
    }
}

private fun DrawScope.drawOutfit(u: Float, pieces: Set<Learning.TinkuPiece>) {
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun polygon(vararg points: Pair<Float, Float>) = Path().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u) }
        close()
    }

    if (Learning.TinkuPiece.BOW_TIE in pieces) {
        drawPath(polygon(50f to 76f, 38f to 70f, 38f to 82f), BowRed)
        drawPath(polygon(50f to 76f, 62f to 70f, 62f to 82f), BowRed)
        drawCircle(BowRed, radius = 2.5f * u, center = p(50f, 76f))
    }
    if (Learning.TinkuPiece.GLASSES in pieces) {
        val frame = Stroke(width = 2f * u)
        drawCircle(Color.White, radius = 9.5f * u, center = p(38f, 46f), style = frame)
        drawCircle(Color.White, radius = 9.5f * u, center = p(62f, 46f), style = frame)
        drawLine(Color.White, p(47.5f, 46f), p(52.5f, 46f), strokeWidth = 2f * u)
    }
    // The crown replaces the cap.
    if (Learning.TinkuPiece.CROWN in pieces) {
        drawPath(polygon(32f to 23f, 30f to 8f, 41f to 15f, 50f to 4f, 59f to 15f, 70f to 8f, 68f to 23f), Sunny)
    } else if (Learning.TinkuPiece.CAP in pieces) {
        drawArc(SaffronLight, 180f, 180f, true, p(32f, 10f), Size(36f * u, 26f * u))
        drawRoundRect(SaffronLight, p(30f, 21f), Size(50f * u, 4f * u), CornerRadius(2f * u))
    }
}

private fun DrawScope.drawTinku(u: Float, mood: TinkuMood, eyeOpen: Float, glow: Float, wave: Float) {
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun s(w: Float, h: Float) = Size(w * u, h * u)
    fun r(v: Float) = CornerRadius(v * u)

    // Antenna with glowing ball
    drawLine(SkyDark, p(50f, 22f), p(50f, 11f), strokeWidth = 2.5f * u, cap = StrokeCap.Round)
    drawCircle(Sunny.copy(alpha = 0.25f * glow), radius = 8.5f * u, center = p(50f, 8f))
    drawCircle(Sunny, radius = 5f * u, center = p(50f, 8f))

    // Ears
    drawRoundRect(SaffronLight, p(8f, 38f), s(8f, 20f), r(3f))
    drawRoundRect(SaffronLight, p(84f, 38f), s(8f, 20f), r(3f))

    // Head and face screen
    drawRoundRect(Sky, p(14f, 22f), s(72f, 52f), r(18f))
    drawRoundRect(Navy, p(22f, 30f), s(56f, 36f), r(12f))

    // Eyes
    when (mood) {
        TinkuMood.HAPPY -> {
            val h = 12f * eyeOpen
            drawRoundRect(Glow, p(33.5f, 46f - h / 2), s(9f, h), r(4.5f))
            drawRoundRect(Glow, p(57.5f, 46f - h / 2), s(9f, h), r(4.5f))
        }
        TinkuMood.CHEER -> {
            // Happy "^ ^" eyes
            val stroke = Stroke(width = 3f * u, cap = StrokeCap.Round)
            drawArc(Glow, 180f, 180f, false, p(33f, 42f), s(10f, 9f), style = stroke)
            drawArc(Glow, 180f, 180f, false, p(57f, 42f), s(10f, 9f), style = stroke)
        }
        TinkuMood.CONFUSED -> {
            // One eye bigger than the other, like a raised eyebrow.
            drawCircle(Glow, radius = 3.5f * u, center = p(38f, 47f))
            drawCircle(Glow, radius = 5.5f * u, center = p(62f, 45f))
        }
    }

    // Cheeks
    drawCircle(Blush.copy(alpha = 0.7f), radius = 3f * u, center = p(29f, 57f))
    drawCircle(Blush.copy(alpha = 0.7f), radius = 3f * u, center = p(71f, 57f))

    // Mouth
    when (mood) {
        TinkuMood.HAPPY -> drawArc(
            Glow, 10f, 160f, false, p(42f, 49f), s(16f, 10f),
            style = Stroke(width = 3f * u, cap = StrokeCap.Round),
        )
        TinkuMood.CHEER -> drawArc(Glow, 0f, 180f, true, p(40f, 50f), s(20f, 12f))
        TinkuMood.CONFUSED -> drawLine(Glow, p(43f, 59f), p(57f, 56f), strokeWidth = 3f * u, cap = StrokeCap.Round)
    }

    // Neck and body with badge
    drawRect(SkyDark, p(44f, 73f), s(12f, 6f))
    drawRoundRect(Sky, p(27f, 78f), s(46f, 28f), r(12f))
    drawCircle(Sunny, radius = 7f * u, center = p(50f, 92f))
    drawCircle(SaffronLight, radius = 3f * u, center = p(50f, 92f))

    // Left arm resting
    drawLine(SkyDark, p(28f, 84f), p(16f, 98f), strokeWidth = 5f * u, cap = StrokeCap.Round)
    drawCircle(SaffronLight, radius = 4f * u, center = p(16f, 98f))

    // Right arm waving
    val swing = wave * if (mood == TinkuMood.CHEER) 28f else 12f
    rotate(degrees = swing, pivot = p(72f, 84f)) {
        drawLine(SkyDark, p(72f, 84f), p(87f, 70f), strokeWidth = 5f * u, cap = StrokeCap.Round)
        drawCircle(SaffronLight, radius = 4f * u, center = p(87f, 70f))
    }
}
