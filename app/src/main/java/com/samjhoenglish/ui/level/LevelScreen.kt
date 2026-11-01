package com.samjhoenglish.ui.level

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.samjhoenglish.content.LevelInfo
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.rememberSay
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.home.JourneyUiState
import com.samjhoenglish.ui.theme.LevelColors

@Composable
fun LevelScreen(level: LevelInfo, journey: JourneyUiState, onBack: () -> Unit, onOpenLesson: (String) -> Unit) {
    val say = rememberSay()
    val levelColor = LevelColors[level.number % LevelColors.size]
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onBack)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier
                    .size(84.dp)
                    .background(levelColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(level.emoji, style = MaterialTheme.typography.displaySmall)
            }
            Column {
                Text(
                    "${UiStrings.level.text()} ${level.number}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(level.title.text(), style = MaterialTheme.typography.headlineSmall)
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            level.lessons.forEachIndexed { index, lesson ->
                val open = lesson.id in journey.unlocked
                val stars = journey.stars[lesson.id] ?: 0
                Surface(
                    onClick = { if (open) onOpenLesson(lesson.id) else say(UiStrings.lockedLessonSay) },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = if (open) 2.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (open) 1f else 0.55f),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(lesson.emoji, style = MaterialTheme.typography.headlineLarge)
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${UiStrings.lesson.text()} ${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(lesson.title.text(), style = MaterialTheme.typography.titleLarge)
                            if (stars > 0) Text("⭐".repeat(stars) + "☆".repeat(3 - stars), style = MaterialTheme.typography.titleMedium)
                        }
                        Box(
                            Modifier
                                .size(52.dp)
                                .background(
                                    if (open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                when {
                                    !open -> "🔒"
                                    lesson.id in journey.passed -> "✓"
                                    else -> "▶"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
