package com.samjhoenglish.ui.lesson

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.samjhoenglish.content.Blend
import com.samjhoenglish.content.SaySentence
import com.samjhoenglish.content.SayWord
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.ListenResult
import com.samjhoenglish.speech.LocalPhonics
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.LocalVoiceListener
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.speech.SpeechChecker
import com.samjhoenglish.speech.SpeechVerdict
import com.samjhoenglish.ui.components.SpeakButton
import com.samjhoenglish.ui.theme.LevelColors
import com.samjhoenglish.ui.theme.Saffron
import com.samjhoenglish.ui.theme.Sunny
import com.samjhoenglish.speech.LocalSoundFx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------- Say a word ----------

@Composable
internal fun SayWordStep(step: SayWord, feedback: Feedback, onAnswer: OnAnswer, onSkip: () -> Unit) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    LaunchedEffect(step) { speaker.say(UiStrings.sayThis, language, thenEnglish = step.word.english) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.sayThis)
        Spacer(Modifier.height(12.dp))
        ModelCard(english = step.word.english) {
            if (step.word.emoji.isNotEmpty()) Text(step.word.emoji, fontSize = 60.sp)
            Text(step.word.english, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        SpeakPanel(step.word.english, step.accept, feedback, onAnswer, onSkip)
    }
}

// ---------- Say a sentence ----------

@Composable
internal fun SaySentenceStep(step: SaySentence, feedback: Feedback, onAnswer: OnAnswer, onSkip: () -> Unit) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    LaunchedEffect(step) { speaker.say(UiStrings.saySentence, language, thenEnglish = step.sentence) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.saySentence)
        Spacer(Modifier.height(12.dp))
        ModelCard(english = step.sentence) {
            Text(
                step.sentence,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            if (language != AppLanguage.ENGLISH) {
                Text(
                    "= ${step.meaning.text()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        SpeakPanel(step.sentence, step.accept, feedback, onAnswer, onSkip)
    }
}

// ---------- Blend sounds into a word: t-a-p → tap ----------

@Composable
internal fun BlendStep(step: Blend, feedback: Feedback, onAnswer: OnAnswer, onSkip: () -> Unit) {
    val speaker = LocalSpeaker.current
    val phonics = LocalPhonics.current
    val language = LocalAppLanguage.current
    var heardParts by remember(step) { mutableStateOf(setOf<Int>()) }
    val allHeard = heardParts.size == step.parts.size

    LaunchedEffect(step) { speaker.say(UiStrings.blendThis, language) }
    DisposableEffect(step) { onDispose { phonics.stop() } }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.blendThis)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            step.parts.forEachIndexed { index, part ->
                val tapped = index in heardParts
                Surface(
                    onClick = {
                        speaker.stop()
                        phonics.play(part.sound)
                        heardParts = heardParts + index
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (tapped) LevelColors[index % LevelColors.size] else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(width = 78.dp, height = 92.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(part.letters, style = MaterialTheme.typography.displaySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Once every sound has been heard, show the picture and let the child hear the whole word.
        if (allHeard || feedback.isDone) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(step.emoji, fontSize = 52.sp)
                SpeakButton(onClick = { speaker.speak(step.word, Speaker.ENGLISH) }, size = 56.dp)
            }
        } else {
            Text(
                UiStrings.tapLetter.text(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        SpeakPanel(step.word, step.accept, feedback, onAnswer, onSkip)
    }
}

// ---------- Shared pieces ----------

/** The thing to say, with buttons to hear it at normal and slow speed. */
@Composable
private fun ModelCard(english: String, content: @Composable () -> Unit) {
    val speaker = LocalSpeaker.current
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            content()
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                SpeakButton(onClick = { speaker.speak(english, Speaker.ENGLISH) }, size = 56.dp)
                SlowButton(onClick = { speaker.speak(english, Speaker.ENGLISH, slow = true) }, size = 56.dp)
            }
        }
    }
}

/** STARTING = recogniser is opening (don't speak yet); LISTENING = mic is open, speak now. */
private enum class MicState { IDLE, STARTING, LISTENING }

/** A mic that checks what was said against one target with [SpeechChecker]. */
@Composable
internal fun SpeakPanel(
    target: String,
    accept: List<String>,
    feedback: Feedback,
    onAnswer: OnAnswer,
    onSkip: () -> Unit,
) {
    MicPanel(
        enabled = !feedback.isDone,
        showHeard = feedback != Feedback.CORRECT,
        onHeard = { guesses ->
            when (val verdict = SpeechChecker.check(target, guesses, accept)) {
                SpeechVerdict.Correct -> onAnswer(true, null)
                is SpeechVerdict.Wrong -> onAnswer(false, verdict.mistake)
            }
        },
        onSkip = onSkip,
    )
}

/**
 * The microphone: asks for permission kindly, listens once per tap, hands over what it heard,
 * and always offers a no-penalty "skip" for noisy rooms or shy children.
 */
@Composable
internal fun MicPanel(
    enabled: Boolean,
    showHeard: Boolean,
    onHeard: (List<String>) -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val listener = LocalVoiceListener.current
    val speaker = LocalSpeaker.current
    val phonics = LocalPhonics.current
    val sounds = LocalSoundFx.current
    val scope = rememberCoroutineScope()

    var hasMic by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var askedOnce by remember { mutableStateOf(false) }
    var mic by remember { mutableStateOf(MicState.IDLE) }
    var status by remember { mutableStateOf<L?>(UiStrings.tapMic) }
    var heard by remember { mutableStateOf<String?>(null) }
    var level by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) { onDispose { listener.cancel() } }

    fun startListening() {
        speaker.stop()
        phonics.stop()
        heard = null
        status = UiStrings.micStarting
        mic = MicState.STARTING
        scope.launch {
            // Let Tinku's last word fade so the mic doesn't hear the app itself.
            delay(150)
            listener.listen(
            onReady = {
                // Only now is the mic really open: turn red, ding, "speak now".
                mic = MicState.LISTENING
                status = UiStrings.listening
                sounds.playMicReady()
            },
            onLevel = { level = it },
            onResult = { result ->
                mic = MicState.IDLE
                level = 0f
                status = when (result) {
                    is ListenResult.Heard -> {
                        heard = result.guesses.first()
                        onHeard(result.guesses)
                        null
                    }
                    ListenResult.NothingHeard -> UiStrings.notHeard
                    ListenResult.NoInternet -> UiStrings.noInternet
                    ListenResult.Unavailable -> UiStrings.micUnavailable
                    ListenResult.NoPermission -> {
                        hasMic = false
                        UiStrings.micDenied
                    }
                }
            },
            )
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMic = granted
        askedOnce = true
        if (granted) startListening() else status = UiStrings.micDenied
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (!hasMic && !askedOnce) {
            Text(
                UiStrings.micPermission.text(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
        }

        MicButton(
            state = mic,
            level = level,
            enabled = enabled,
            onClick = {
                when {
                    // Tapping while listening means "I'm done": get the result right away.
                    mic == MicState.LISTENING -> listener.finish()
                    mic == MicState.STARTING -> Unit
                    hasMic -> startListening()
                    else -> permission.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        )
        Spacer(Modifier.height(8.dp))

        val heardText = heard
        when {
            !hasMic && !askedOnce -> Text(
                UiStrings.micPermissionButton.text(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            // Showing what was heard helps the child notice their own slip ("tin" instead of "thin").
            heardText != null && showHeard -> Text(
                "${UiStrings.heard.text()}: “$heardText”",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            status != null && enabled -> Text(
                status!!.text(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (enabled) {
            TextButton(onClick = {
                listener.cancel()
                mic = MicState.IDLE
                onSkip()
            }) {
                Text(
                    UiStrings.skipSpeaking.text(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Big round mic. Yellow while starting; red, pulsing and growing with the child's voice while listening. */
@Composable
private fun MicButton(state: MicState, level: Float, enabled: Boolean, onClick: () -> Unit) {
    val listening = state == MicState.LISTENING
    val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse",
    )
    // Recogniser loudness is roughly -2..10 dB.
    val voice by animateFloatAsState(1f + (level.coerceIn(0f, 10f) / 10f) * 0.3f, label = "voice")
    val ringScale = if (listening) maxOf(pulse, voice) else 1f

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
        if (listening) {
            Box(
                Modifier
                    .size(110.dp)
                    .scale(ringScale * 1.15f)
                    .background(Saffron.copy(alpha = 0.18f), CircleShape),
            )
        }
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = when {
                !enabled -> MaterialTheme.colorScheme.outline
                listening -> Saffron
                state == MicState.STARTING -> Sunny
                else -> MaterialTheme.colorScheme.secondary
            },
            shadowElevation = if (enabled) 6.dp else 0.dp,
            modifier = Modifier.size(104.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🎤", fontSize = 46.sp)
            }
        }
    }
}
