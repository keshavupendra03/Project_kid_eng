package com.samjhoenglish.ui.placement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.LocalSoundFx
import com.samjhoenglish.speech.rememberSay
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.TinkuMood
import com.samjhoenglish.ui.components.TinkuSays
import com.samjhoenglish.ui.lesson.Feedback
import com.samjhoenglish.ui.lesson.StepContent
import com.samjhoenglish.ui.theme.Leaf
import kotlinx.coroutines.delay

@Composable
fun PlacementScreen(viewModel: PlacementViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        when (state.phase) {
            PlacementPhase.INTRO -> Intro(onStart = viewModel::start, onSkip = viewModel::skip)
            PlacementPhase.QUESTIONS -> Questions(state, onAnswer = viewModel::answer, onNext = viewModel::next)
            PlacementPhase.RESULT -> Result(state, onDone)
        }
    }
}

@Composable
private fun Intro(onStart: () -> Unit, onSkip: () -> Unit) {
    val say = rememberSay()
    LaunchedEffect(Unit) { say(UiStrings.placementIntro) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        TinkuSays(message = UiStrings.placementIntro.text(), onListen = { say(UiStrings.placementIntro) })
        Spacer(Modifier.height(32.dp))
        BigButton(text = UiStrings.placementStart.text(), emoji = "🎮", onClick = onStart)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSkip) {
            Text(UiStrings.placementSkip.text(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * One question at a time, no retries and no praise or "wrong" messages: this is about finding
 * the right starting point, not testing. After an answer it quietly moves on.
 */
@Composable
private fun Questions(state: PlacementUiState, onAnswer: (Boolean) -> Unit, onNext: () -> Unit) {
    val step = state.current ?: return
    LaunchedEffect(state.section, state.position, state.answered) {
        if (state.answered) {
            delay(1200)
            onNext()
        }
    }
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outline),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(state.progress.coerceIn(0.03f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(Leaf),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            key(state.section, state.position) {
                StepContent(
                    step = step,
                    feedback = if (state.answered) Feedback.REVEALED else Feedback.NONE,
                    onAnswer = { correct, _ -> onAnswer(correct) },
                    onSkip = { onAnswer(false) },
                )
            }
        }
    }
}

@Composable
private fun Result(state: PlacementUiState, onDone: () -> Unit) {
    val say = rememberSay()
    val sounds = LocalSoundFx.current
    val level = state.startLevel
    val message = level?.let { l ->
        val template = UiStrings.placementResult
        L(
            hi = template.hi.format(l.number, l.title.hi),
            hinglish = template.hinglish.format(l.number, l.title.hinglish),
            en = template.en.format(l.number, l.title.en),
        )
    }
    LaunchedEffect(Unit) {
        sounds.playCelebrate()
        delay(800)
        message?.let(say)
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        TinkuSays(
            message = message?.text() ?: "",
            onListen = { message?.let(say) },
            mood = TinkuMood.CHEER,
        )
        Spacer(Modifier.height(32.dp))
        BigButton(text = UiStrings.letsBegin.text(), emoji = "🚀", onClick = onDone)
    }
}
