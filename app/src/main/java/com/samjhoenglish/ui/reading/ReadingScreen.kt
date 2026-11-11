package com.samjhoenglish.ui.reading

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.samjhoenglish.content.LessonRef
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.rememberSay
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.components.TinkuSays
import com.samjhoenglish.ui.home.JourneyUiState
import com.samjhoenglish.ui.theme.LevelColors

/** The reading ladder: five levels of passages, opening one level at a time. */
@Composable
fun ReadingScreen(journey: JourneyUiState, onBack: () -> Unit, onOpenPassage: (String) -> Unit) {
    val say = rememberSay()
    LaunchedEffect(Unit) { say(UiStrings.readingPick) }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onBack)
            Text("📖 ${UiStrings.reading.text()}", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(12.dp))
        TinkuSays(message = UiStrings.readingPick.text(), onListen = { say(UiStrings.readingPick) }, tinkuSize = 80.dp)
        Spacer(Modifier.height(20.dp))

        journey.readingLevels.forEach { level ->
            val open = level.lessons.firstOrNull()?.id in journey.readingUnlocked
            val color = LevelColors[level.number % LevelColors.size]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.alpha(if (open) 1f else 0.55f)) {
                Box(Modifier.size(56.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                    Text(level.emoji, style = MaterialTheme.typography.headlineSmall)
                }
                Column(Modifier.weight(1f)) {
                    Text("${UiStrings.readingLevel.text()} ${level.number}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(level.title.text(), style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    if (open) "${journey.passedCount(level)}/${level.lessons.size}" else "🔒",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                level.lessons.forEachIndexed { index, passage ->
                    PassageCard(
                        passage = passage,
                        number = index + 1,
                        open = passage.id in journey.readingUnlocked,
                        passed = passage.id in journey.passed,
                        stars = journey.stars[passage.id] ?: 0,
                        isNext = passage.id == journey.nextPassage?.id,
                        onClick = { if (passage.id in journey.readingUnlocked) onOpenPassage(passage.id) else say(UiStrings.readingLockedSay) },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PassageCard(passage: LessonRef, number: Int, open: Boolean, passed: Boolean, stars: Int, isNext: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isNext) colors.primaryContainer else colors.surface,
        border = BorderStroke(2.dp, if (isNext) colors.primary else colors.outline),
        shadowElevation = if (open) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (open) 1f else 0.5f),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(passage.emoji, style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f)) {
                Text("${UiStrings.passage.text()} $number", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(passage.title.text(), style = MaterialTheme.typography.titleMedium)
                if (stars > 0) Text("⭐".repeat(stars) + "☆".repeat(3 - stars), style = MaterialTheme.typography.bodyLarge)
            }
            Box(
                Modifier.size(44.dp).background(if (open) colors.primary else colors.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (!open) "🔒" else if (passed) "✓" else "▶", style = MaterialTheme.typography.titleMedium, color = colors.onPrimary)
            }
        }
    }
}
