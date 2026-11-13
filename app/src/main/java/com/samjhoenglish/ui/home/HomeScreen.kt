package com.samjhoenglish.ui.home

import com.samjhoenglish.data.db.SchoolTest
import com.samjhoenglish.ui.test.ActiveTestCards

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samjhoenglish.content.LevelInfo
import com.samjhoenglish.content.Mission
import com.samjhoenglish.content.PowerWord
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.learning.Learning
import com.samjhoenglish.speech.rememberSay
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.components.SpeakButton
import com.samjhoenglish.ui.components.TinkuSays
import com.samjhoenglish.ui.theme.Leaf
import com.samjhoenglish.ui.theme.LevelColors
import com.samjhoenglish.ui.theme.Indigo
import com.samjhoenglish.ui.theme.Teal
import java.util.Calendar

/** What Tinku's speech bubble currently says. An enum so it survives screen rotation. */
private enum class TinkuMessage(val text: L) {
    INTRO(UiStrings.tinkuIntro),
    LOCKED(UiStrings.lockedSay),
    SOON(UiStrings.levelComingSoon),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    child: Child,
    journey: JourneyUiState,
    voiceMissing: Boolean,
    onSwitchChild: () -> Unit,
    onOpenParents: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLevel: (Int) -> Unit,
    onStartToday: () -> Unit,
    onMissionDone: (String) -> Unit,
    onOpenReading: () -> Unit = {},
    onOpenExpress: () -> Unit = {},
    onOpenWordPower: () -> Unit = {},
    onOpenBook: () -> Unit = {},
    activeTests: List<SchoolTest> = emptyList(),
    onOpenTest: (Long) -> Unit = {},
) {
    val say = rememberSay()
    var message by rememberSaveable { mutableStateOf(TinkuMessage.INTRO) }
    var introSpoken by rememberSaveable { mutableStateOf(false) }

    fun tinkuSays(next: TinkuMessage) {
        message = next
        say(next.text)
    }

    LaunchedEffect(Unit) {
        if (!introSpoken) {
            introSpoken = true
            say(TinkuMessage.INTRO.text)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiButton(child.avatar, child.name, onClick = onSwitchChild)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                // Always in English: a tiny daily English moment.
                Text("${greeting()}, ${child.name}!", style = MaterialTheme.typography.titleLarge)
                Text(
                    UiStrings.tagline.text(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EmojiButton("🔒", UiStrings.forParents.text(), onClick = onOpenParents)
            Spacer(Modifier.size(8.dp))
            EmojiButton("⚙️", UiStrings.settings.text(), onClick = onOpenSettings)
        }

        Spacer(Modifier.height(12.dp))
        // Wraps onto a second line on narrow phones instead of squeezing the last chip.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("🔥", "${journey.streak} ${UiStrings.streakDays.text()}")
            StatChip("⭐", "${journey.totalStars}")
            Learning.nextPiece(journey.totalStars)?.let { piece ->
                StatChip("🎁", "${rewardName(piece).text()} ${journey.totalStars}/${piece.stars}")
            }
        }

        if (voiceMissing) {
            Spacer(Modifier.height(12.dp))
            VoiceMissingNotice()
        }

        Spacer(Modifier.height(4.dp))
        TinkuSays(
            message = message.text.text(),
            onListen = { say(message.text) },
            tinkuSize = 130.dp,
        )

        Spacer(Modifier.height(20.dp))
        // A test from the teacher comes first, so the child can't miss it.
        if (activeTests.isNotEmpty()) {
            ActiveTestCards(activeTests, onOpenTest)
            Spacer(Modifier.height(14.dp))
        }
        val next = journey.nextLesson
        BigButton(
            text = UiStrings.todayTask.text(),
            subtitle = when {
                next != null && journey.dueCount > 0 -> UiStrings.revisionPlusLesson.text()
                next != null -> "${next.emoji} ${next.title.text()}"
                else -> UiStrings.allDoneRevise.text()
            },
            emoji = "🚀",
            enabled = journey.loaded,
            onClick = onStartToday,
        )

        journey.myBook?.let { book ->
            Spacer(Modifier.height(14.dp))
            BigButton(
                text = "📕 ${UiStrings.myBook.text()}",
                subtitle = UiStrings.myBookCard.text().format(book.grade),
                emoji = "",
                color = Indigo,
                onClick = onOpenBook,
            )
        }

        if (journey.readingLevels.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            val level = journey.readingLevelNow
            BigButton(
                text = "📖 ${UiStrings.reading.text()}",
                subtitle = when {
                    journey.nextPassage != null && level != null ->
                        "${UiStrings.readingLevel.text()} ${level.number} · ${journey.nextPassage.emoji} ${journey.nextPassage.title.text()}"
                    else -> UiStrings.readingCardText.text()
                },
                emoji = "",
                color = Teal,
                onClick = onOpenReading,
            )
            Spacer(Modifier.height(14.dp))
            BigButton(
                text = "📚 ${UiStrings.wordPower.text()}",
                subtitle = UiStrings.wordPowerCard.text(),
                emoji = "",
                color = MaterialTheme.colorScheme.secondary,
                onClick = onOpenWordPower,
            )
            journey.wordOfDay?.let { word ->
                Spacer(Modifier.height(14.dp))
                WordOfDayCard(word, onClick = onOpenWordPower)
            }
            Spacer(Modifier.height(14.dp))
            BigButton(
                text = "🚂 ${UiStrings.express.text()}",
                subtitle = UiStrings.expressCard.text(),
                emoji = "",
                color = Leaf,
                onClick = onOpenExpress,
            )
        }

        journey.mission?.let { mission ->
            Spacer(Modifier.height(16.dp))
            MissionCard(mission, journey.missionPending, onDone = { onMissionDone(mission.id) })
        }

        Spacer(Modifier.height(28.dp))
        Text(UiStrings.yourJourney.text(), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            journey.levels.forEach { level ->
                val open = journey.isOpen(level)
                LevelCard(
                    level = level,
                    open = open,
                    passed = journey.passedCount(level),
                    onClick = {
                        when {
                            open -> onOpenLevel(level.number)
                            level.lessons.isEmpty() -> tinkuSays(TinkuMessage.SOON)
                            else -> tinkuSays(TinkuMessage.LOCKED)
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun rewardName(piece: Learning.TinkuPiece): L = when (piece) {
    Learning.TinkuPiece.CAP -> UiStrings.rewardCap
    Learning.TinkuPiece.BOW_TIE -> UiStrings.rewardBowTie
    Learning.TinkuPiece.GLASSES -> UiStrings.rewardGlasses
    Learning.TinkuPiece.CROWN -> UiStrings.rewardCrown
}

/** One word a day from the child's own band, with a nudge to use it in real life. */
@Composable
private fun WordOfDayCard(word: PowerWord, onClick: () -> Unit) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val hindi = if (language == AppLanguage.HINGLISH) word.hinglish else word.hindi
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(word.emoji, style = MaterialTheme.typography.displaySmall)
            Column(Modifier.weight(1f)) {
                Text("💬 ${UiStrings.wordOfTheDay.text()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(word.english, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                if (language != AppLanguage.ENGLISH) Text("= $hindi", style = MaterialTheme.typography.titleMedium)
                Text(UiStrings.useItToday.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SpeakButton(
                onClick = { speaker.speakAll(listOf(word.english to Speaker.ENGLISH, word.example to Speaker.ENGLISH)) },
                size = 48.dp,
            )
        }
    }
}

/** A small real-life English task. The child says it's done; a parent ticks it in their section. */
@Composable
private fun MissionCard(mission: Mission, pending: Boolean, onDone: () -> Unit) {
    val say = rememberSay()
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎯 ${UiStrings.todaysMission.text()}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                SpeakButton(onClick = { say(mission.text) }, size = 48.dp)
            }
            Spacer(Modifier.height(6.dp))
            Text("${mission.emoji} ${mission.text.text()}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            if (pending) {
                Text(UiStrings.missionWaiting.text(), style = MaterialTheme.typography.titleMedium, color = Leaf)
            } else {
                BigButton(text = UiStrings.iDidIt.text(), emoji = "✅", color = Leaf, onClick = onDone)
            }
        }
    }
}

@Composable
private fun StatChip(emoji: String, text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun LevelCard(level: LevelInfo, open: Boolean, passed: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val total = level.lessons.size
    val finished = total > 0 && passed == total
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (open) colors.surface else colors.surface.copy(alpha = 0.6f),
        border = BorderStroke(2.dp, colors.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .background(
                        LevelColors[level.number % LevelColors.size].copy(alpha = if (open) 1f else 0.45f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(level.emoji, style = MaterialTheme.typography.headlineMedium)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${UiStrings.level.text()} ${level.number}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    level.title.text(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (open) colors.onSurface else colors.onSurfaceVariant,
                )
                when {
                    total == 0 -> SmallNote(UiStrings.comingSoonShort.text())
                    !open -> SmallNote(UiStrings.locked.text())
                    else -> {
                        Spacer(Modifier.height(6.dp))
                        LevelProgress(passed, total)
                    }
                }
            }
            Text(
                when {
                    finished -> "✅"
                    open -> "▶️"
                    total == 0 -> "🔜"
                    else -> "🔒"
                },
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun SmallNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LevelProgress(passed: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outline),
        ) {
            if (passed > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(passed.toFloat() / total)
                        .clip(RoundedCornerShape(50))
                        .background(Leaf),
                )
            }
        }
        Text("$passed/$total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** For parents: the phone lacks a text-to-speech voice, so offer the system's voice download screen. */
@Composable
private fun VoiceMissingNotice() {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(UiStrings.voiceMissing.text(), style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure {
                    runCatching {
                        context.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }) {
                Text(UiStrings.voiceMissingButton.text(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
