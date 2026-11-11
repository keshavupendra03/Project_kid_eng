package com.samjhoenglish.ui.express

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.samjhoenglish.AppContainer
import com.samjhoenglish.content.ReadAlong
import com.samjhoenglish.content.isExpressRound
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.learning.Express
import com.samjhoenglish.speech.LocalReadingListener
import com.samjhoenglish.speech.LocalSoundFx
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.ReadAlongTracker
import com.samjhoenglish.speech.ReadingStatus
import com.samjhoenglish.speech.WordMark
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.components.Tinku
import com.samjhoenglish.ui.components.TinkuMood
import com.samjhoenglish.ui.theme.Leaf
import com.samjhoenglish.ui.theme.Saffron
import com.samjhoenglish.ui.theme.Sunny
import com.samjhoenglish.ui.theme.Teal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Everything one round needs, from a train passage or a Reading story re-read as a bonus. */
data class ExpressRound(
    val id: String,
    val emoji: String,
    val title: L,
    val text: String,
    val golden: List<String>,
    val wpm: Int,
    val lives: Boolean,
    /** The next story on the same train, if any. */
    val nextId: String?,
)

private suspend fun loadRound(container: AppContainer, childId: Long, id: String): ExpressRound? {
    if (id.isExpressRound) {
        val levels = container.content.express()
        val level = levels.firstOrNull { l -> l.passages.any { it.id == id } } ?: return null
        val index = level.passages.indexOfFirst { it.id == id }
        val p = level.passages[index]
        return ExpressRound(p.id, p.emoji, p.title, p.text, p.golden, level.wpm, level.lives, level.passages.getOrNull(index + 1)?.id)
    }
    // A Reading story, re-read a little faster than the child's best on it (repeated reading builds fluency).
    val lesson = container.content.lesson(id)
    val story = lesson.steps.filterIsInstance<ReadAlong>().firstOrNull() ?: return null
    val ref = container.content.readingLevels().flatMap { it.lessons }.firstOrNull { it.id == id }
    val best = container.progress.expressBestWpmFor(childId, id)
    val wpm = ((best ?: BONUS_START_WPM) * 1.1f).toInt().coerceIn(BONUS_START_WPM, 110)
    return ExpressRound(
        id, ref?.emoji ?: "📖", story.title, story.sentences.filter { it.isNotBlank() }.joinToString(" "),
        story.words.map { it.english }, wpm, lives = false, nextId = null,
    )
}

private const val BONUS_START_WPM = 30

@Composable
fun ExpressGameScreen(
    container: AppContainer,
    childId: Long,
    passageId: String,
    challenge: Boolean,
    onExit: () -> Unit,
    onNext: (String) -> Unit,
) {
    val round by produceState<ExpressRound?>(null, passageId) { value = loadRound(container, childId, passageId) }
    var attempt by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(onBack = onExit)
    val r = round
    if (r == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    key(passageId, attempt) {
        RoundPlay(container, childId, r, challenge, onExit, onPlayAgain = { attempt++ }, onNext = onNext)
    }
}

private enum class Phase { WAITING_MIC, COUNTDOWN, RUNNING, PAUSED, DONE }

private data class RoundResult(val score: Express.Score, val wpm: Int, val bestStreak: Int, val previousBest: Int?)

private val Gold = Color(0xFFB07800)

@Composable
private fun RoundPlay(
    container: AppContainer,
    childId: Long,
    round: ExpressRound,
    challenge: Boolean,
    onExit: () -> Unit,
    onPlayAgain: () -> Unit,
    onNext: (String) -> Unit,
) {
    val context = LocalContext.current
    val listener = LocalReadingListener.current
    val speaker = LocalSpeaker.current
    val sounds = LocalSoundFx.current
    val language = LocalAppLanguage.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val passage = remember { Express.passage(round.text, round.golden) }
    val tracker = remember { ReadAlongTracker(passage.words) }
    var marks by remember { mutableStateOf(tracker.state) }
    var escaped by remember { mutableStateOf(setOf<Int>()) }
    var phase by remember { mutableStateOf(Phase.WAITING_MIC) }
    var countdown by remember { mutableIntStateOf(3) }
    var status by remember { mutableStateOf<ReadingStatus?>(null) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewport by remember { mutableIntStateOf(0) }
    val scroll = remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(1f) }
    var moving by remember { mutableStateOf(false) }
    var firstReadAt by remember { mutableLongStateOf(0L) }
    var lastReadAt by remember { mutableLongStateOf(0L) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<RoundResult?>(null) }

    // Character offsets of each word in the text "w1 w2 w3 …".
    val starts = remember { passage.words.runningFold(0) { at, w -> at + w.length + 1 }.dropLast(1) }

    fun finish() {
        if (phase == Phase.DONE) return
        phase = Phase.DONE
        listener.stop()
        val score = Express.score(passage, marks.marks, round.lives)
        val wpm = Express.wordsPerMinute(score.wordsRead, lastReadAt - firstReadAt)
        scope.launch {
            val previous = container.progress.finishExpress(
                childId, round.id, score.stars, score.wordsRead, score.words, wpm, bestStreak, challenge,
            )
            result = RoundResult(score, wpm, bestStreak, previous)
            if (score.failed) speaker.say(UiStrings.roundFailed, language) else {
                sounds.playCelebrate()
                delay(700)
                speaker.say(UiStrings.roundDoneSay, language)
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) phase = Phase.COUNTDOWN else onExit()
    }
    LaunchedEffect(Unit) {
        listener.warmUp()
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) phase = Phase.COUNTDOWN else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    // 3, 2, 1 …
    LaunchedEffect(phase) {
        if (phase != Phase.COUNTDOWN) return@LaunchedEffect
        for (n in 3 downTo 1) {
            countdown = n
            sounds.playMicReady()
            delay(800)
        }
        phase = Phase.RUNNING
    }

    // The round: listen, scroll, let unread words escape, and end when the text is gone.
    LaunchedEffect(phase) {
        if (phase != Phase.RUNNING) return@LaunchedEffect
        val runStartedAt = System.currentTimeMillis()
        listener.start(
            round.text, recordTo = null,
            onText = { guesses, final ->
                val readBefore = marks.marks.count { it == WordMark.READ || it == WordMark.SLIP }
                marks = tracker.hearAny(guesses, final)
                val readNow = marks.marks.count { it == WordMark.READ || it == WordMark.SLIP }
                if (readNow > readBefore) {
                    val now = System.currentTimeMillis()
                    if (firstReadAt == 0L) firstReadAt = now
                    lastReadAt = now
                    moving = true
                }
                streak = Express.streak(marks.marks, marks.next)
                if (streak > bestStreak) bestStreak = streak
            },
            onStatus = { status = it },
        )
        try {
            var last = withFrameNanos { it }
            var escapeFrom = 0
            var doneAt = 0L
            while (true) {
                val now = withFrameNanos { it }
                val seconds = ((now - last) / 1e9f).coerceAtMost(0.1f)
                last = now
                val l = layout ?: continue
                if (viewport == 0 || l.lineCount == 0) continue
                val lineH = l.getLineBottom(0) - l.getLineTop(0)
                val readingLine = viewport * READING_LINE
                fun top(i: Int) = l.getLineTop(l.getLineForOffset(starts[i]))

                // The text waits for the child to start, but not for ever.
                if (!moving && System.currentTimeMillis() - runStartedAt > START_GRACE_MS) moving = true
                if (moving) {
                    val next = marks.next.coerceAtMost(passage.words.lastIndex)
                    if (!challenge) speed = Express.adaptSpeed(speed, (scroll.floatValue - top(next)) / lineH, seconds)
                    val pxPerWord = l.size.height.toFloat() / passage.words.size
                    scroll.floatValue += round.wpm / 60f * pxPerWord * speed * seconds
                }

                // A word whose line has scrolled out of the top has escaped.
                var newlyEscaped = emptySet<Int>()
                while (escapeFrom < passage.words.size && top(escapeFrom) + lineH / 2 + readingLine < scroll.floatValue) {
                    if (marks.marks[escapeFrom] == WordMark.WAITING) newlyEscaped = newlyEscaped + escapeFrom
                    escapeFrom++
                }
                if (newlyEscaped.isNotEmpty()) escaped = escaped + newlyEscaped
                if (marks.next < escapeFrom) {
                    tracker.skipTo(escapeFrom)
                    marks = tracker.state
                    streak = 0
                }

                if (tracker.done && doneAt == 0L) doneAt = System.currentTimeMillis()
                if ((doneAt > 0 && System.currentTimeMillis() - doneAt > 700) || escapeFrom >= passage.words.size) {
                    finish()
                    break
                }
            }
        } finally {
            listener.stop()
        }
    }
    DisposableEffect(Unit) { onDispose { listener.stop() } }

    val text = buildAnnotatedString {
        passage.words.forEachIndexed { i, word ->
            if (i > 0) append(" ")
            val mark = marks.marks.getOrElse(i) { WordMark.WAITING }
            var style = when {
                mark == WordMark.READ -> SpanStyle(background = Leaf.copy(alpha = 0.35f))
                mark == WordMark.SLIP -> SpanStyle(background = Sunny.copy(alpha = 0.7f))
                i in escaped -> SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                i in passage.golden -> SpanStyle(color = Gold, fontWeight = FontWeight.Bold)
                else -> SpanStyle()
            }
            if (phase == Phase.RUNNING && i == marks.next) style = style.merge(SpanStyle(textDecoration = TextDecoration.Underline))
            withStyle(style) { append(word) }
        }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        // Top bar: leave, pause, combo, golden words.
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmojiButton("✖️", UiStrings.back.text(), onClick = onExit)
            if (phase == Phase.RUNNING || phase == Phase.PAUSED) {
                EmojiButton(if (phase == Phase.PAUSED) "▶️" else "⏸️", UiStrings.paused.text(), onClick = {
                    phase = if (phase == Phase.PAUSED) Phase.RUNNING else Phase.PAUSED
                })
            }
            Spacer(Modifier.weight(1f))
            Chip("🔥 $streak", highlight = streak >= 5)
            if (passage.golden.isNotEmpty()) Chip("✨ ${passage.golden.count { marks.marks[it] == WordMark.READ }}/${passage.golden.size}")
        }
        Text(
            "${round.emoji} ${round.title.text()}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        // The track: text moving up past the reading line.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .onSizeChanged { viewport = it.height },
        ) {
            val lineH = layout?.let { it.getLineBottom(0) - it.getLineTop(0) } ?: 0f
            Text(
                text,
                fontSize = 30.sp,
                lineHeight = 48.sp,
                onTextLayout = { layout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .padding(horizontal = 18.dp)
                    .graphicsLayer { translationY = viewport * READING_LINE - lineH / 2 - scroll.floatValue },
            )
            // The reading line.
            Box(
                Modifier
                    .offset { IntOffset(0, (viewport * READING_LINE + lineH / 2).toInt()) }
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Teal.copy(alpha = 0.5f)),
            )
            Text("👉", fontSize = 22.sp, modifier = Modifier.offset { IntOffset(0, (viewport * READING_LINE - with(density) { 14.dp.toPx() }).toInt()) })
            // Soft edges, so words fade in and out.
            Box(Modifier.fillMaxWidth().height(36.dp).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, Color.Transparent))))
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(48.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.surface))),
            )

            when (phase) {
                Phase.COUNTDOWN -> Overlay {
                    Text(UiStrings.getReady.text(), style = MaterialTheme.typography.headlineSmall)
                    CountdownNumber(countdown)
                    Text(UiStrings.startsWhenYouRead.text(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
                Phase.PAUSED -> Overlay {
                    Text("⏸️ ${UiStrings.paused.text()}", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    BigButton(text = UiStrings.resume.text(), emoji = "▶️", onClick = { phase = Phase.RUNNING })
                }
                else -> Unit
            }
        }

        // What the listener is doing.
        if (phase == Phase.RUNNING) {
            Text(
                when {
                    status is ReadingStatus.Loading -> UiStrings.readingLoading.text()
                    status is ReadingStatus.Failed -> UiStrings.readingUnavailable.text()
                    !moving -> "🎤 ${UiStrings.startsWhenYouRead.text()}"
                    else -> "🎤 ${UiStrings.readingListening.text()}"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (status is ReadingStatus.Failed) Saffron else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
    }

    result?.let { r -> ResultSheet(round, r, onPlayAgain, onNext, onExit) }
}

private const val READING_LINE = 0.38f
private const val START_GRACE_MS = 4_000L

@Composable
private fun Chip(label: String, highlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (highlight) Saffron.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
    }
}

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) { content() }
    }
}

@Composable
private fun CountdownNumber(n: Int) {
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(n) {
        pop.snapTo(0.4f)
        pop.animateTo(1f, spring(dampingRatio = 0.45f))
    }
    Text("$n", fontSize = 96.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.scale(pop.value))
}

@Composable
private fun ResultSheet(round: ExpressRound, result: RoundResult, onPlayAgain: () -> Unit, onNext: (String) -> Unit, onExit: () -> Unit) {
    val score = result.score
    val newBest = result.wpm > 0 && !score.failed && (result.previousBest == null || result.wpm > result.previousBest)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Tinku(mood = if (score.failed) TinkuMood.HAPPY else TinkuMood.CHEER, size = 120.dp)
            if (score.failed) {
                Text(UiStrings.roundFailed.text(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) { i -> Text("⭐", fontSize = 48.sp, modifier = Modifier.alpha(if (i < score.stars) 1f else 0.2f)) }
                }
            }
            Text(UiStrings.linesCaught.text().format(score.sentences, score.sentencesCaught), style = MaterialTheme.typography.titleLarge)
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (score.goldenTotal > 0) ResultLine("✨ ${UiStrings.golden.text()}", "${score.golden}/${score.goldenTotal}")
                    ResultLine("🔥 ${UiStrings.bestStreak.text()}", "${result.bestStreak}")
                    if (result.wpm > 0) ResultLine("🚂 ${UiStrings.yourSpeed.text()}", "${result.wpm} ${UiStrings.wordsPerMinute.text()}")
                }
            }
            if (newBest) Text(UiStrings.newBest.text(), style = MaterialTheme.typography.headlineSmall, color = Leaf)
            Spacer(Modifier.height(4.dp))
            val next = round.nextId
            if (next != null && !score.failed) BigButton(text = UiStrings.nextStory.text(), emoji = "👉", color = Leaf, onClick = { onNext(next) })
            BigButton(text = UiStrings.playAgain.text(), emoji = "🔁", onClick = onPlayAgain)
            BigButton(text = UiStrings.allTrains.text(), emoji = "🚂", color = Teal, onClick = onExit)
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
