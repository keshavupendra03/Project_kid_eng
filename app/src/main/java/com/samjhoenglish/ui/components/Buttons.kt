package com.samjhoenglish.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Big, squishy main-action button. Shrinks slightly when pressed so kids feel the tap. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "press")

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        color = if (enabled) color else MaterialTheme.colorScheme.outline,
        contentColor = Color.White,
        shadowElevation = if (enabled) 4.dp else 0.dp,
        interactionSource = interaction,
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .heightIn(min = 80.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        ) {
            if (emoji != null) Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Column(horizontalAlignment = if (emoji != null) Alignment.Start else Alignment.CenterHorizontally) {
                Text(text, style = MaterialTheme.typography.headlineSmall)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

/** Small round icon button using an emoji, sized for small fingers. */
@Composable
fun EmojiButton(emoji: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Text(
            emoji,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(14.dp)
                .semantics { contentDescription = label },
        )
    }
}
