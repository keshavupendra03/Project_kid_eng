package com.samjhoenglish.ui.lesson

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.samjhoenglish.content.ReadAlong
import com.samjhoenglish.content.StoryQuestion
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.learning.Learning
import com.samjhoenglish.speech.LocalReadingListener
import com.samjhoenglish.speech.LocalSoundFx
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.ReadAlongTracker
import com.samjhoenglish.speech.ReadingStatus
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.speech.SpeechChecker
import com.samjhoenglish.speech.WordMark
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.SpeakButton
import com.samjhoenglish.ui.components.Tinku
import com.samjhoenglish.ui.components.TinkuMood
import com.samjhoenglish.ui.theme.Leaf
import com.samjhoenglish.ui.theme.Saffron
import com.samjhoenglish.ui.theme.Sunny
import kotlinx.coroutines.delay

/** Saves how a passage was read: (step, words total, words read, slips, seconds). */
val LocalReadingReporter = staticCompositionLocalOf<(ReadAlong, Int, Int, Int, Int) -> Unit> { { _, _, _, _, _ -> } }

private enum class ReadPhase { LISTEN, READ }

// ---------- Read along ----------

/**
 * Tinku reads the passage first (each sentence lights up), then the child reads it aloud and
 * each word turns green as it is heard, yellow for an Indian-English slip. Tapping a word says
 * it; a word the child is stuck on is whispered after a few seconds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReadAlongStep(step: ReadAlong, feedback: Feedback, onAnswer: OnAnswer, onSkip: () -> Unit) {
    val context = LocalContext.current
    val speaker = LocalSpeaker.current
    val listener = LocalReadingListener.current
    val sounds = LocalSoundFx.current
    val language = LocalAppLanguage.current
    val report = LocalReadingReporter.current

    val words = remember(step) { step.readingWords }
    val sentences = remember(step) { step.sentences.filter { it.isNotBlank() } }
    val paragraph = remember(step) { sentences.joinToString(" ") }
    val keyWords = remember(step) { step.words.associateBy { SpeechChecker.normalize(it.english) } }
    // For each line of step.sentences: where its first word sits in [words], and which spoken sentence it is.
    val wordStart = remember(step) {
        step.sentences.runningFold(0) { n, s -> n + s.split(" ").count { it.isNotBlank() } }.dropLast(1)
    }
    val sentenceNumber = remember(step) {
        step.sentences.runningFold(-1) { n, s -> if (s.isBlank()) n else n + 1 }.drop(1)
    }

    var phase by remember(step) { mutableStateOf(ReadPhase.LISTEN) }
    var tracker by remember(step) { mutableStateOf(ReadAlongTracker(words)) }
    var marks by remember(step) { mutableStateOf(tracker.state) }
    var speakingSentence by remember(step) { mutableIntStateOf(-1) }
    var listening by remember(step) { mutableStateOf(false) }
    var status by remember(step) { mutableStateOf<ReadingStatus?>(null) }
    var stuckWord by remember(step) { mutableIntStateOf(-1) }
    var shownMeaning by remember(step) { mutableStateOf<String?>(null) }
    // Line-by-line Hindi under a poem: on by default, the child can hide it to read on their own.
    var showLineMeanings by rememberSaveable(step.id) { mutableStateOf(true) }
    var startedAt by remember(step) { mutableLongStateOf(0L) }
    var lastProgressAt by remember(step) { mutableLongStateOf(0L) }
    var finishedOnce by remember(step) { mutableStateOf(false) }
    var hasMic by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }

    // Tinku speaks only while the mic is muted, so he can't be heard as the child.
    fun speakEnglish(text: String, slow: Boolean = false, onDone: () -> Unit = {}) {
        listener.muted = true
        speaker.speakAll(listOf(text to Speaker.ENGLISH), slow = slow) {
            listener.muted = false
            onDone()
        }
    }

    /** Reads the sentences one by one, lighting each up. */
    fun readAloud(from: Int = 0, onDone: () -> Unit = {}) {
        if (from >= sentences.size) {
            speakingSentence = -1
            return onDone()
        }
        speakingSentence = from
        speakEnglish(sentences[from]) { if (speakingSentence == from) readAloud(from + 1, onDone) }
    }

    fun stopListening() {
        listener.stop()
        listening = false
        stuckWord = -1
    }

    fun finish() {
        if (!listening && finishedOnce) return
        stopListening()
        finishedOnce = true
        val read = marks.marks.count { it != WordMark.WAITING }
        val seconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt().coerceAtLeast(1)
        report(step, words.size, read, marks.slips.size, seconds)
        onAnswer(Learning.readingPassed(read, words.size), marks.slips.values.firstOrNull())
    }

    fun startListening() {
        speaker.stop()
        speakingSentence = -1
        if (finishedOnce) {
            tracker = ReadAlongTracker(words)
            marks = tracker.state
            finishedOnce = false
        }
        phase = ReadPhase.READ
        listening = true
        startedAt = System.currentTimeMillis()
        lastProgressAt = startedAt
        listener.start(
            paragraph, recordTo = null,
            onText = { guesses, final ->
                val before = marks.next
                marks = tracker.hearAny(guesses, final)
                if (marks.next != before) {
                    lastProgressAt = System.currentTimeMillis()
                    stuckWord = -1
                }
                if (tracker.done) {
                    sounds.playCorrect()
                    finish()
                }
            },
            onStatus = { status = it },
        )
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMic = granted
        if (granted) startListening()
    }

    // Tinku reads first, as soon as the step opens.
    LaunchedEffect(step) {
        speaker.say(UiStrings.tinkuReadsFirst, language) { readAloud { phase = ReadPhase.READ } }
    }
    // When the answer is shown, Tinku reads it again for the child to follow.
    LaunchedEffect(feedback) {
        if (feedback == Feedback.REVEALED) {
            stopListening()
            delay(2_500)
            readAloud()
        }
    }
    // A child stuck on a word for a few seconds hears it whispered, without penalty.
    LaunchedEffect(listening) {
        while (listening) {
            delay(500)
            val idle = System.currentTimeMillis() - lastProgressAt
            if (idle > STUCK_AFTER_MS && speakingSentence < 0 && marks.next < words.size) {
                stuckWord = marks.next
                lastProgressAt = System.currentTimeMillis()
                speakEnglish(words[marks.next], slow = true)
            }
        }
    }
    DisposableEffect(step) {
        onDispose {
            listener.stop()
            listener.muted = false
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // What Tinku wants right now.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tinku(mood = if (feedback == Feedback.CORRECT) TinkuMood.CHEER else TinkuMood.HAPPY, size = 56.dp)
            Text(
                when {
                    feedback.isDone -> step.title.text()
                    phase == ReadPhase.LISTEN -> UiStrings.tinkuReadsFirst.text()
                    status is ReadingStatus.Loading -> UiStrings.readingLoading.text()
                    status is ReadingStatus.Failed -> UiStrings.readingUnavailable.text()
                    listening -> UiStrings.readingListening.text()
                    else -> UiStrings.yourTurnToRead.text()
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))

        if (step.lineMeanings.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            ) {
                Text(
                    "🇮🇳 ${UiStrings.showLineMeanings.text()}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = showLineMeanings, onCheckedChange = { showLineMeanings = it })
            }
        }

        // The passage: one row of word chips per sentence.
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, if (feedback == Feedback.CORRECT) Leaf else MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                step.sentences.forEachIndexed { s, sentence ->
                    if (sentence.isBlank()) {
                        Spacer(Modifier.height(6.dp))
                        return@forEachIndexed
                    }
                    val lit = speakingSentence == sentenceNumber[s]
                    val start = wordStart[s]
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Positions come from the sentence's own start, not a running counter: a row can redraw on its own.
                        sentence.split(" ").filter { it.isNotBlank() }.forEachIndexed { j, word ->
                            val i = start + j
                            WordChip(
                                word = word,
                                mark = marks.marks.getOrElse(i) { WordMark.WAITING },
                                current = listening && i == marks.next,
                                stuck = i == stuckWord,
                                lit = lit,
                                onClick = {
                                    val plain = SpeechChecker.normalize(word)
                                    shownMeaning = keyWords[plain]?.let { k ->
                                        "${k.english} = ${if (language == AppLanguage.HINGLISH) k.hinglish ?: k.hindi else k.hindi}"
                                    }
                                    speakEnglish(plain)
                                },
                            )
                        }
                    }
                    // What this line means, in the child's language, just under it.
                    if (showLineMeanings) step.lineMeanings.getOrNull(s)?.let { m ->
                        val meaning = if (language == AppLanguage.HINGLISH) m.hinglish else m.hi
                        if (meaning.isNotBlank()) {
                            Text(
                                meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(
            shownMeaning ?: UiStrings.tapWordHint.text(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(10.dp))

        when {
            feedback.isDone -> ReadingSummary(words, marks)
            phase == ReadPhase.LISTEN -> {
                TextButton(onClick = {
                    speaker.stop()
                    speakingSentence = -1
                    phase = ReadPhase.READ
                }) { Text("⏭️ ${UiStrings.letMeRead.text()}", style = MaterialTheme.typography.titleMedium) }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SpeakButton(onClick = { stopListening(); readAloud() }, size = 56.dp)
                    ReadMicButton(
                        listening = listening,
                        onClick = {
                            when {
                                listening -> finish()
                                hasMic -> startListening()
                                else -> permission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    )
                    SlowButton(onClick = { stopListening(); speakEnglish(paragraph, slow = true) }, size = 56.dp)
                }
                if (listening || status is ReadingStatus.Failed) {
                    Spacer(Modifier.height(8.dp))
                    BigButton(text = UiStrings.doneReading.text(), emoji = "✔️", color = Leaf, onClick = { if (listening) finish() else { startedAt = System.currentTimeMillis(); listening = true; finish() } })
                }
                TextButton(onClick = { stopListening(); onSkip() }) {
                    Text(UiStrings.skipSpeaking.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private const val STUCK_AFTER_MS = 5_000L

@Composable
private fun WordChip(word: String, mark: WordMark, current: Boolean, stuck: Boolean, lit: Boolean, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "stuck").animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pulse",
    )
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = when {
            mark == WordMark.READ -> Leaf.copy(alpha = 0.35f)
            mark == WordMark.SLIP -> Sunny.copy(alpha = 0.6f)
            lit -> colors.primaryContainer
            else -> colors.surface
        },
        modifier = Modifier.scale(if (stuck) pulse else 1f),
    ) {
        Text(
            word,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = if (current || stuck) FontWeight.Bold else null,
            textDecoration = if (current) TextDecoration.Underline else null,
            color = if (lit && mark == WordMark.WAITING) colors.primary else colors.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Big round mic: tap to read, tap again when finished. */
@Composable
private fun ReadMicButton(listening: Boolean, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "pulse",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        if (listening) Box(Modifier.size(104.dp).scale(pulse * 1.12f).background(Saffron.copy(alpha = 0.18f), CircleShape))
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (listening) Saffron else MaterialTheme.colorScheme.secondary,
            shadowElevation = 6.dp,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Text(if (listening) "✔️" else "🎤", fontSize = 42.sp) }
        }
    }
}

@Composable
private fun ReadingSummary(words: List<String>, state: ReadAlongTracker.State) {
    val read = state.marks.count { it != WordMark.WAITING }
    Text(UiStrings.wordsReadScore.text().format(words.size, read), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    if (state.slips.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(UiStrings.practiseThese.text(), style = MaterialTheme.typography.titleMedium)
        state.slips.entries.sortedBy { it.key }.distinctBy { it.value.skill }.take(3).forEach { (i, mistake) ->
            Text("“${words[i]}”: ${mistake.tip.text()}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

// ---------- Story question ----------

@Composable
internal fun StoryQuestionStep(step: StoryQuestion, feedback: Feedback, onAnswer: OnAnswer) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val options = remember(step) { step.options.shuffled() }
    var wrong by remember(step) { mutableStateOf(setOf<String>()) }

    LaunchedEffect(step) { speaker.say(step.question, language) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tinku(mood = TinkuMood.CONFUSED, size = 64.dp)
            Text(step.question.text(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            SpeakButton(onClick = { speaker.say(step.question, language) }, size = 44.dp)
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val isAnswer = option == step.answer
                OptionTile(
                    state = optionState(isAnswer, option in wrong, feedback),
                    onClick = {
                        if (feedback.isDone || option in wrong) return@OptionTile
                        speaker.speak(option, Speaker.ENGLISH)
                        if (isAnswer) onAnswer(true, null) else {
                            wrong = wrong + option
                            onAnswer(false, null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                ) {
                    Text(option, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        }
    }
}

/** A short line Tinku says in the app language (kept here for the reading screens). */
@Composable
internal fun L.spoken(): String = text()
