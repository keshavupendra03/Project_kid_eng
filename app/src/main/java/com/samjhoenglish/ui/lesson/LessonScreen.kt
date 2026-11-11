package com.samjhoenglish.ui.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samjhoenglish.content.answerSpeech
import com.samjhoenglish.content.isQuestion
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.LocalSoundFx
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.speech.rememberSay
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.Confetti
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.components.Tinku
import com.samjhoenglish.ui.components.TinkuMood
import com.samjhoenglish.ui.components.TinkuSays
import com.samjhoenglish.ui.theme.Leaf
import com.samjhoenglish.ui.theme.Teal
import kotlinx.coroutines.delay

@Composable
fun LessonScreen(
    viewModel: LessonViewModel,
    onExit: () -> Unit,
    onHome: () -> Unit,
    onNextLesson: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = !state.finished && !state.empty) { confirmExit = true }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        when {
            state.loading -> Unit
            state.error -> TinkuNotice(UiStrings.loadError, onExit)
            state.empty -> TinkuNotice(UiStrings.nothingToRevise, onExit)
            state.finished -> LessonComplete(
                state,
                onPlayAgain = viewModel::restart,
                onHome = onHome,
                onNextLesson = onNextLesson,
            )
            else -> CompositionLocalProvider(LocalReadingReporter provides viewModel::recordReading) {
                LessonPlayer(
                    state = state,
                    onAnswer = viewModel::answer,
                    onSkip = viewModel::skip,
                    onNext = viewModel::next,
                    onClose = { confirmExit = true },
                )
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(UiStrings.leaveTitle.text(), style = MaterialTheme.typography.headlineSmall) },
            confirmButton = {
                TextButton(onClick = { confirmExit = false }) {
                    Text(UiStrings.leaveNo.text(), style = MaterialTheme.typography.titleMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmExit = false
                    onExit()
                }) {
                    Text(UiStrings.leaveYes.text(), style = MaterialTheme.typography.titleMedium)
                }
            },
        )
    }
}

@Composable
private fun LessonPlayer(
    state: LessonUiState,
    onAnswer: OnAnswer,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val step = state.current ?: return
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val sounds = LocalSoundFx.current

    // Pick one praise/encouragement per answer so what Tinku says matches what's on screen.
    // A recognised pronunciation slip gets its specific tip first; after that, the hint ladder takes over.
    val message: L? = remember(state.position, state.feedback, state.wrongTries) {
        when (state.feedback) {
            Feedback.CORRECT -> UiStrings.praise.random()
            Feedback.TRY_AGAIN -> state.mistake?.tip?.takeIf { state.wrongTries == 1 } ?: state.hint?.text ?: UiStrings.tryAgain.random()
            Feedback.REVEALED -> if (state.skipped) UiStrings.skipped else state.hint?.text ?: UiStrings.revealed
            Feedback.NONE -> null
        }
    }
    LaunchedEffect(state.position, state.feedback, state.wrongTries) {
        val slow = state.hint?.slowEnglish.orEmpty().map { it to Speaker.ENGLISH }
        val playSlowly: (() -> Unit)? = if (slow.isEmpty()) null else ({ speaker.speakAll(slow, slow = true) })
        when (state.feedback) {
            Feedback.CORRECT -> {
                sounds.playCorrect()
                delay(350)
                speaker.say(message!!, language, thenEnglish = step.answerSpeech)
            }
            Feedback.TRY_AGAIN -> {
                sounds.playTryAgain()
                delay(250)
                speaker.say(message!!, language, onDone = playSlowly)
            }
            // When words are played slowly, those already include the answer.
            Feedback.REVEALED -> speaker.say(message!!, language, thenEnglish = step.answerSpeech.takeIf { slow.isEmpty() }, onDone = playSlowly)
            Feedback.NONE -> Unit
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 12.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            EmojiButton("✖️", UiStrings.back.text(), onClick = onClose)
            ProgressBar(state.progress, Modifier.weight(1f))
        }
        if (state.inReview) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp),
            ) {
                Text(
                    UiStrings.reviewBadge.text(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }
        }

        // Each position is its own content, so every step (including second chances) starts fresh.
        AnimatedContent(
            targetState = state.position,
            transitionSpec = { (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 8 }) togetherWith fadeOut(tween(120)) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "step",
        ) { position ->
            val shown = state.steps.getOrNull(position) ?: return@AnimatedContent
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                // The outgoing step is shown as finished so it can't take taps while it fades.
                StepContent(shown, if (position == state.position) state.feedback else Feedback.REVEALED, onAnswer, onSkip)
            }
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
            if (message != null) {
                FeedbackPanel(state.feedback, message)
                Spacer(Modifier.height(10.dp))
            }
            val canGoOn = !step.isQuestion || state.feedback == Feedback.CORRECT || state.feedback == Feedback.REVEALED
            if (canGoOn) {
                BigButton(
                    text = UiStrings.next.text(),
                    emoji = "👉",
                    color = if (step.isQuestion) Leaf else MaterialTheme.colorScheme.primary,
                    onClick = onNext,
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress, label = "progress")
    Box(
        modifier
            .height(18.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.outline),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated.coerceIn(0.03f, 1f))
                .clip(RoundedCornerShape(50))
                .background(Leaf),
        )
    }
}

@Composable
private fun FeedbackPanel(feedback: Feedback, message: L) {
    val color = if (feedback == Feedback.CORRECT) Leaf.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 2 }) {
        Surface(shape = RoundedCornerShape(20.dp), color = color, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Tinku(mood = if (feedback == Feedback.CORRECT) TinkuMood.CHEER else TinkuMood.HAPPY, size = 44.dp)
                val text = message.text()
                Text(
                    text,
                    // Pronunciation tips are long; keep them readable without pushing the Next button off screen.
                    style = if (text.length > 40) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun LessonComplete(
    state: LessonUiState,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    onNextLesson: (String) -> Unit,
) {
    val say = rememberSay()
    val sounds = LocalSoundFx.current
    val revisionOnly = state.lessonId == null
    LaunchedEffect(Unit) {
        sounds.playCelebrate()
        delay(900)
        say(UiStrings.lessonDoneSay)
    }
    // Once the result is saved: a lesson with 1 star asks (kindly) for one more go.
    LaunchedEffect(state.needsRetry) {
        if (state.needsRetry) {
            delay(2500)
            say(UiStrings.tryOnceMore)
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Tinku(mood = TinkuMood.CHEER, size = 160.dp)
        Text(
            (if (revisionOnly) UiStrings.revisionDone else UiStrings.lessonDone).text(),
            style = MaterialTheme.typography.headlineLarge,
        )
        if (!revisionOnly) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { i -> Star(earned = i < state.stars, delayMs = 400L + i * 350L) }
            }
            Spacer(Modifier.height(12.dp))
            if (state.questions > 0) {
                Text(
                    UiStrings.firstTryScore.text().format(state.rightFirstTime, state.questions),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        if (!state.saved) return@Column

        val next = state.nextLessonId
        when {
            state.needsRetry -> {
                Text(UiStrings.tryOnceMore.text(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                BigButton(text = UiStrings.playAgain.text(), emoji = "🔁", onClick = onPlayAgain)
                Spacer(Modifier.height(14.dp))
                BigButton(text = UiStrings.goHome.text(), emoji = "🏠", color = Teal, onClick = onHome)
            }
            next != null -> {
                BigButton(text = UiStrings.nextLesson.text(), emoji = "👉", color = Leaf, onClick = { onNextLesson(next) })
                Spacer(Modifier.height(14.dp))
                BigButton(text = UiStrings.goHome.text(), emoji = "🏠", onClick = onHome)
            }
            else -> {
                BigButton(text = UiStrings.goHome.text(), emoji = "🏠", onClick = onHome)
                Spacer(Modifier.height(14.dp))
                BigButton(text = UiStrings.playAgain.text(), emoji = "🔁", color = Teal, onClick = onPlayAgain)
            }
        }
    }
    if (revisionOnly || state.stars >= 2) Confetti()
    }
}

@Composable
private fun Star(earned: Boolean, delayMs: Long) {
    val scale = remember { Animatable(0f) }
    val sounds = LocalSoundFx.current
    LaunchedEffect(Unit) {
        delay(delayMs)
        if (earned) sounds.playStar()
        scale.animateTo(1f, spring(dampingRatio = 0.35f))
    }
    Text(
        "⭐",
        fontSize = 56.sp,
        modifier = Modifier
            .scale(scale.value)
            .alpha(if (earned) 1f else 0.2f),
    )
}

/** Tinku explains why there's no lesson to show, with a way back. */
@Composable
private fun TinkuNotice(message: L, onBack: () -> Unit) {
    val say = rememberSay()
    LaunchedEffect(message) { say(message) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        TinkuSays(message = message.text(), onListen = { say(message) })
        Spacer(Modifier.height(24.dp))
        BigButton(text = UiStrings.back.text(), emoji = "⬅️", onClick = onBack)
    }
}
