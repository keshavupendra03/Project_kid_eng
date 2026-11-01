package com.samjhoenglish.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tinku with a speech bubble underneath. The 🔊 button replays the message. */
@Composable
fun TinkuSays(
    message: String,
    onListen: () -> Unit,
    modifier: Modifier = Modifier,
    mood: TinkuMood = TinkuMood.HAPPY,
    tinkuSize: Dp = 150.dp,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Tinku(mood = mood, size = tinkuSize)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnimatedContent(
                    targetState = message,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f),
                    label = "bubble",
                ) { text ->
                    Text(text, style = MaterialTheme.typography.titleMedium)
                }
                SpeakButton(onClick = onListen, size = 52.dp)
            }
        }
    }
}

@Composable
fun SpeakButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("🔊", fontSize = (size.value * 0.4f).sp)
        }
    }
}
