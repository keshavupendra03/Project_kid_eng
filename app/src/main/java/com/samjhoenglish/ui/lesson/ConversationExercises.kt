package com.samjhoenglish.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samjhoenglish.content.END_CONVERSATION
import com.samjhoenglish.content.Meaning
import com.samjhoenglish.content.Roleplay
import com.samjhoenglish.content.RoleplayReply
import com.samjhoenglish.content.TeachTinku
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.LocalSoundFx
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.Mistake
import com.samjhoenglish.speech.ReplyMatch
import com.samjhoenglish.speech.ReplyMatcher
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.ui.components.SpeakButton
import com.samjhoenglish.ui.components.Tinku
import com.samjhoenglish.ui.components.TinkuMood
import com.samjhoenglish.ui.components.TinkuSays
import com.samjhoenglish.ui.theme.Leaf

/** The current child's name, for lines like "Nice to meet you, {child}!". */
val LocalChildName = staticCompositionLocalOf { "" }

private const val MAX_REPLY_TRIES = 3

@Composable
private fun Meaning.text(): String = if (LocalAppLanguage.current == AppLanguage.HINGLISH) hinglish else hi

// ---------- Teach Tinku ----------

@Composable
internal fun TeachTinkuStep(step: TeachTinku, feedback: Feedback, onAnswer: OnAnswer, onSkip: () -> Unit) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val play = { speaker.say(UiStrings.teachTinku, language, thenEnglish = step.wrong) }
    LaunchedEffect(step) { play() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TinkuSays(
            message = (if (feedback.isDone) UiStrings.teachTinkuThanks else UiStrings.teachTinku).text(),
            onListen = play,
            mood = if (feedback.isDone) TinkuMood.CHEER else TinkuMood.CONFUSED,
            tinkuSize = 90.dp,
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, if (feedback.isDone) Leaf else MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    step.wrong,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    textDecoration = if (feedback.isDone) TextDecoration.LineThrough else null,
                    color = if (feedback.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
                if (feedback.isDone) {
                    Text(step.answer, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = Leaf)
                }
                Text(
                    "= ${step.meaning.text()}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SpeakPanel(step.answer, step.accept, feedback, onAnswer, onSkip)
    }
}

// ---------- Role-play ----------

/**
 * A spoken conversation that follows what the child says. It is scripted, so it is always
 * safe, at the child's level, and free. A child who gets stuck is helped along, never blocked.
 */
@Composable
internal fun RoleplayStep(step: Roleplay, feedback: Feedback, onAnswer: OnAnswer, onSkip: () -> Unit) {
    val speaker = LocalSpeaker.current
    val sounds = LocalSoundFx.current
    val language = LocalAppLanguage.current
    val childName = LocalChildName.current
    val turns = remember(step) { step.turns.associateBy { it.id } }
    val order = remember(step) { step.turns.map { it.id } }

    var turnId by remember(step) { mutableStateOf(step.turns.first().id) }
    var repliesSpoken by remember(step) { mutableIntStateOf(0) }
    var tries by remember(step) { mutableIntStateOf(0) }
    var tip by remember(step) { mutableStateOf<L?>(null) }
    var lastSaid by remember(step) { mutableStateOf<String?>(null) }
    var modelToSay by remember(step) { mutableStateOf<String?>(null) }

    fun fill(text: String) = text.replace("{child}", childName)

    val turn = turns.getValue(turnId)
    val line = fill(turn.tinku)

    fun playLine(modelFirst: String? = null) {
        val parts = listOfNotNull(
            modelFirst?.let { it to Speaker.ENGLISH },
            line to Speaker.ENGLISH,
            if (language == AppLanguage.ENGLISH) null else turn.meaning.hi to Speaker.HINDI,
        )
        speaker.speakAll(parts)
    }
    LaunchedEffect(turnId) {
        playLine(modelFirst = modelToSay)
        modelToSay = null
    }

    fun advance(reply: RoleplayReply, spoken: Boolean) {
        val done = repliesSpoken + if (spoken) 1 else 0
        repliesSpoken = done
        tries = 0
        tip = null
        lastSaid = fill(reply.say)
        // When the child didn't say it, Tinku says a good reply before moving on.
        if (!spoken) modelToSay = fill(reply.say)
        val next = reply.next ?: order.getOrNull(order.indexOf(turnId) + 1) ?: END_CONVERSATION
        when {
            next != END_CONVERSATION -> turnId = next
            // Saying at least one line aloud counts; a child who skipped everything isn't marked wrong.
            done > 0 -> onAnswer(true, null)
            else -> onSkip()
        }
    }

    fun onHeard(guesses: List<String>) {
        when (val match = ReplyMatcher.match(turn.replies.map { it.patterns }, guesses)) {
            is ReplyMatch.Matched -> {
                sounds.playCorrect()
                advance(turn.replies[match.replyIndex], spoken = true)
            }
            is ReplyMatch.NoMatch -> {
                sounds.playTryAgain()
                tries++
                if (tries >= MAX_REPLY_TRIES) {
                    // Don't trap the child: the talk goes on.
                    advance(turn.replies.first(), spoken = false)
                } else {
                    tip = match.mistake.tipOr(UiStrings.roleplayTryLine)
                }
            }
        }
    }

    if (feedback.isDone) {
        TinkuSays(message = UiStrings.roleplayDone.text(), onListen = { speaker.say(UiStrings.roleplayDone, language) }, mood = TinkuMood.CHEER)
        return
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        lastSaid?.let {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Leaf.copy(alpha = 0.15f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(start = 48.dp),
            ) {
                Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(step.scene, fontSize = 26.sp)
                Tinku(size = 64.dp)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f),
            ) {
                Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(line, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        if (language != AppLanguage.ENGLISH) {
                            Text(turn.meaning.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    SpeakButton(onClick = { playLine() }, size = 44.dp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(UiStrings.roleplayYourTurn.text(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            turn.replies.forEach { reply ->
                val said = fill(reply.say)
                Surface(
                    onClick = { speaker.speak(said, Speaker.ENGLISH) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text("🔊 $said", style = MaterialTheme.typography.titleMedium)
                        if (language != AppLanguage.ENGLISH) {
                            Text(reply.meaning.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        tip?.let {
            Spacer(Modifier.height(8.dp))
            Text(it.text(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        // The mic restarts fresh for each turn.
        androidx.compose.runtime.key(turnId) {
            MicPanel(
                enabled = true,
                showHeard = true,
                onHeard = ::onHeard,
                onSkip = { advance(turn.replies.first(), spoken = false) },
            )
        }
    }
}

private fun Mistake?.tipOr(fallback: L): L = this?.tip ?: fallback
