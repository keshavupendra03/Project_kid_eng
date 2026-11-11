package com.samjhoenglish.ui.words

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.samjhoenglish.content.WORD_MONTHLY
import com.samjhoenglish.content.WORD_REVIEW
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.rememberSay
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.components.TinkuSays
import com.samjhoenglish.ui.theme.LevelColors
import com.samjhoenglish.ui.theme.Leaf
import com.samjhoenglish.ui.theme.Teal

/** Word Power home: progress, review, placement, monthly test, and the class bands with their packs. */
@Composable
fun WordPowerScreen(state: WordPowerUiState, onBack: () -> Unit, onOpenLesson: (String) -> Unit) {
    val say = rememberSay()
    LaunchedEffect(Unit) { say(UiStrings.wordPowerHello) }
    // The child's own band starts open; others can be opened by tapping their header.
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onBack)
            Text("📚 ${UiStrings.wordPower.text()}", style = MaterialTheme.typography.headlineMedium)
        }
        TinkuSays(message = UiStrings.wordPowerHello.text(), onListen = { say(UiStrings.wordPowerHello) }, tinkuSize = 80.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("📖", "${state.wordsLearned}", UiStrings.wordsLearnedShort.text(), Modifier.weight(1f))
            Stat("💎", "${state.mastered}", UiStrings.wordsMastered.text(), Modifier.weight(1f))
        }

        if (state.due > 0) {
            BigButton(
                text = UiStrings.reviewWords.text(),
                subtitle = UiStrings.reviewWaiting.text().format(state.due),
                emoji = "🔁",
                color = Leaf,
                onClick = { onOpenLesson(WORD_REVIEW) },
            )
        }
        if (!state.placementDone && state.placementId.isNotEmpty()) {
            BigButton(text = UiStrings.findMyLevel.text(), subtitle = UiStrings.findMyLevelNote.text(), emoji = "🧭", color = Teal, onClick = { onOpenLesson(state.placementId) })
        }
        if (state.monthlyOpen) {
            BigButton(
                text = UiStrings.monthlyTest.text(),
                subtitle = UiStrings.monthlyTestNote.text() + (state.lastMonthly?.let { "  ·  ${UiStrings.lastMonthlyScore.text()}: " + "⭐".repeat(it) } ?: ""),
                emoji = "🏅",
                color = MaterialTheme.colorScheme.secondary,
                onClick = { onOpenLesson(WORD_MONTHLY) },
            )
        }

        state.bands.forEachIndexed { index, row ->
            val color = LevelColors[(index + 2) % LevelColors.size]
            val showPacks = row.open && (expanded == row.band.id || (expanded == null && row.home))
            Surface(
                onClick = {
                    if (!row.open) say(UiStrings.bandLockedSay) else expanded = if (showPacks) "" else row.band.id
                },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (row.home) 3.dp else 2.dp, if (row.home) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().alpha(if (row.open) 1f else 0.6f),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(52.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                            Text(row.band.emoji, style = MaterialTheme.typography.headlineSmall)
                        }
                        Column(Modifier.weight(1f)) {
                            if (row.home) Text("⭐ ${UiStrings.yourClass.text()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(row.band.title.text(), style = MaterialTheme.typography.titleLarge)
                            Text(
                                "✅ ${row.testsPassed}/${row.packs.size} ${UiStrings.test.text()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(if (!row.open) "🔒" else if (showPacks) "▲" else "▼", style = MaterialTheme.typography.titleLarge)
                    }
                    if (showPacks) {
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.packs.forEach { pack -> PackCard(pack, onLearn = { if (pack.open) onOpenLesson(pack.id) else say(UiStrings.packLockedSay) }, onTest = { if (pack.learned) onOpenLesson(pack.testId) else say(UiStrings.packLockedSay) }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(emoji: String, value: String, label: String, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$emoji $value", style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PackCard(pack: PackRow, onLearn: () -> Unit, onTest: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.background,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth().alpha(if (pack.open) 1f else 0.5f),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (pack.open) pack.emoji else "🔒", style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f)) {
                Text(pack.title.text(), style = MaterialTheme.typography.titleMedium)
                if (pack.testStars > 0) Text("⭐".repeat(pack.testStars) + "☆".repeat(3 - pack.testStars), style = MaterialTheme.typography.bodyMedium)
            }
            SmallAction(if (pack.learned) "✓ ${UiStrings.learn.text()}" else "📖 ${UiStrings.learn.text()}", highlight = pack.open && !pack.learned, onClick = onLearn)
            SmallAction("📝 ${UiStrings.test.text()}", highlight = pack.learned && pack.testStars < 2, onClick = onTest)
        }
    }
}

@Composable
private fun SmallAction(label: String, highlight: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (highlight) colors.primary else colors.surface,
        border = BorderStroke(1.dp, if (highlight) colors.primary else colors.outline),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlight) colors.onPrimary else colors.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
