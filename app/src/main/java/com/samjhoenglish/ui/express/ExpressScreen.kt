package com.samjhoenglish.ui.express

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samjhoenglish.content.ExpressLevel
import com.samjhoenglish.content.ExpressPassage
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.rememberSay
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.components.TinkuSays
import com.samjhoenglish.ui.theme.LevelColors

/** The train map: ten trains of three stories, Smart speed or Challenge, and bonus re-reads. */
@Composable
fun ExpressScreen(state: ExpressUiState, onBack: () -> Unit, onPlay: (passageId: String, challenge: Boolean) -> Unit) {
    val say = rememberSay()
    var challenge by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { say(UiStrings.expressPick) }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onBack)
            Text("🚂 ${UiStrings.express.text()}", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(12.dp))
        TinkuSays(message = UiStrings.expressCard.text(), onListen = { say(UiStrings.expressCard) }, tinkuSize = 80.dp)
        state.bestWpm?.let {
            Spacer(Modifier.height(10.dp))
            Text("🏆 ${UiStrings.bestSpeed.text()}: $it ${UiStrings.wordsPerMinute.text()}", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeCard("🎯", UiStrings.smartSpeed, UiStrings.smartSpeedNote, selected = !challenge, Modifier.weight(1f)) { challenge = false }
            ModeCard("🏁", UiStrings.challenge, UiStrings.challengeNote, selected = challenge, Modifier.weight(1f)) { challenge = true }
        }

        Spacer(Modifier.height(20.dp))
        state.levels.forEach { level ->
            TrainCard(level, state, onPlay = { id -> onPlay(id, challenge) }, onLocked = { say(UiStrings.expressLocked) })
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text("🔁 ${UiStrings.bonusTitle.text()}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (state.bonus.isEmpty()) {
            Text(UiStrings.bonusNone.text(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.bonus.forEach { story ->
                Surface(
                    onClick = { onPlay(story.id, challenge) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(story.emoji, style = MaterialTheme.typography.headlineSmall)
                        Text(story.title.text(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("▶", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModeCard(emoji: String, title: L, note: L, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) colors.primary else colors.outline),
        modifier = modifier.heightIn(min = 96.dp),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$emoji ${title.text()}", style = MaterialTheme.typography.titleMedium)
            Text(note.text(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TrainCard(level: ExpressLevel, state: ExpressUiState, onPlay: (String) -> Unit, onLocked: () -> Unit) {
    val open = level.passages.firstOrNull()?.id in state.unlocked
    val color = LevelColors[level.number % LevelColors.size]
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(2.dp, colors.outline),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (open) 1f else 0.55f),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(52.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                    Text(level.emoji, style = MaterialTheme.typography.headlineSmall)
                }
                Column(Modifier.weight(1f)) {
                    Text("${UiStrings.train.text()} ${level.number}", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Text(level.title.text(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${speedEmoji(level.wpm)} ${level.wpm} ${UiStrings.wordsPerMinute.text()}" + if (level.lives) "  ❤️" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (!open) Text("🔒", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                level.passages.forEach { passage ->
                    PassageChip(passage, stars = state.stars[passage.id] ?: 0, open = open, Modifier.weight(1f)) {
                        if (open) onPlay(passage.id) else onLocked()
                    }
                }
            }
        }
    }
}

@Composable
private fun PassageChip(passage: ExpressPassage, stars: Int, open: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (stars > 0) colors.primaryContainer else colors.background,
        border = BorderStroke(1.dp, colors.outline),
        modifier = modifier.heightIn(min = 104.dp),
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (open) passage.emoji else "🔒", style = MaterialTheme.typography.headlineSmall)
            Text(passage.title.text(), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
            Text("⭐".repeat(stars) + "☆".repeat(3 - stars), style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun speedEmoji(wpm: Int) = when {
    wpm < 40 -> "🐢"
    wpm < 70 -> "🐇"
    else -> "🐆"
}
