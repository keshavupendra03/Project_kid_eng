package com.samjhoenglish.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samjhoenglish.content.Blend
import com.samjhoenglish.content.BuildSentence
import com.samjhoenglish.content.Choice
import com.samjhoenglish.content.Crossword
import com.samjhoenglish.content.MatchPairs
import com.samjhoenglish.content.SortGroups
import com.samjhoenglish.content.FillBlank
import com.samjhoenglish.content.FindEnglish
import com.samjhoenglish.content.ListenRead
import com.samjhoenglish.content.MatchMeaning
import com.samjhoenglish.content.LearnWord
import com.samjhoenglish.content.ReadAlong
import com.samjhoenglish.content.Roleplay
import com.samjhoenglish.content.SpellWord
import com.samjhoenglish.content.StoryQuestion
import com.samjhoenglish.content.SentenceTrain
import com.samjhoenglish.content.SpotMistake
import com.samjhoenglish.content.TeachTinku
import com.samjhoenglish.content.LearnCard
import com.samjhoenglish.content.LearnSound
import com.samjhoenglish.content.ListenTap
import com.samjhoenglish.content.MinimalPair
import com.samjhoenglish.content.SaySentence
import com.samjhoenglish.content.SayWord
import com.samjhoenglish.content.Step
import com.samjhoenglish.content.Word
import com.samjhoenglish.content.WordPick
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.LocalPhonics
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.Mistake
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.ui.components.SpeakButton
import com.samjhoenglish.ui.components.TinkuSays
import com.samjhoenglish.ui.theme.Leaf

/** Reports an answer: right or wrong, plus the pronunciation slip if one was recognised. */
typealias OnAnswer = (correct: Boolean, mistake: Mistake?) -> Unit

/**
 * Hears what the child picked or built, just before the answer is reported. Lessons ignore it;
 * a school test keeps it so the parent and teacher can see the child's answer beside the right one.
 */
val LocalAnswerLog = androidx.compose.runtime.staticCompositionLocalOf<(String) -> Unit> { {} }

@Composable
fun StepContent(step: Step, feedback: Feedback, onAnswer: OnAnswer, onSkip: () -> Unit) {
    when (step) {
        is LearnCard -> LearnCardStep(step)
        is ListenTap -> ListenTapStep(step, feedback, onAnswer)
        is WordPick -> WordPickStep(step, feedback, onAnswer)
        is FindEnglish -> FindEnglishStep(step, feedback, onAnswer)
        is LearnSound -> LearnSoundStep(step)
        is MinimalPair -> MinimalPairStep(step, feedback, onAnswer)
        is SayWord -> SayWordStep(step, feedback, onAnswer, onSkip)
        is SaySentence -> SaySentenceStep(step, feedback, onAnswer, onSkip)
        is Blend -> BlendStep(step, feedback, onAnswer, onSkip)
        is MatchMeaning -> MatchMeaningStep(step, feedback, onAnswer)
        is ListenRead -> ListenReadStep(step, feedback, onAnswer)
        is SentenceTrain -> SentenceTrainStep(step, feedback, onAnswer)
        is FillBlank -> FillBlankStep(step, feedback, onAnswer)
        is SpotMistake -> SpotMistakeStep(step, feedback, onAnswer)
        is TeachTinku -> TeachTinkuStep(step, feedback, onAnswer, onSkip)
        is Roleplay -> RoleplayStep(step, feedback, onAnswer, onSkip)
        is ReadAlong -> ReadAlongStep(step, feedback, onAnswer, onSkip)
        is StoryQuestion -> StoryQuestionStep(step, feedback, onAnswer)
        is LearnWord -> LearnWordStep(step)
        is SpellWord -> SpellWordStep(step, feedback, onAnswer)
        is Choice -> ChoiceStep(step, feedback, onAnswer)
        is MatchPairs -> MatchPairsStep(step, feedback, onAnswer)
        is Crossword -> CrosswordStep(step, feedback, onAnswer)
        is SortGroups -> SortGroupsStep(step, feedback, onAnswer)
        is BuildSentence -> BuildSentenceStep(step, feedback, onAnswer)
    }
}

internal val Feedback.isDone get() = this == Feedback.CORRECT || this == Feedback.REVEALED

// ---------- Learn card ----------

@Composable
private fun LearnCardStep(step: LearnCard) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val play = { speaker.say(step.text, language, thenEnglish = step.word?.english) }
    LaunchedEffect(Unit) { play() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TinkuSays(message = step.text.text(), onListen = play, tinkuSize = 110.dp)
        step.word?.let { word ->
            Spacer(Modifier.height(20.dp))
            WordCard(word, onClick = { speaker.speak(word.english, Speaker.ENGLISH) })
        }
    }
}

@Composable
private fun WordCard(word: Word, onClick: () -> Unit) {
    val language = LocalAppLanguage.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (word.emoji.isNotEmpty()) Text(word.emoji, fontSize = 72.sp)
            Text(word.english, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            // The Hindi word is the bridge: the child already knows "सेब", now sees "apple".
            val hindi = if (language == AppLanguage.HINGLISH) word.hinglish ?: word.hindi else word.hindi
            if (hindi != null) {
                Text(
                    "${UiStrings.inHindi.text()}: $hindi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "🔊 ${UiStrings.tapToListen.text()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------- Listen and tap the picture ----------

@Composable
private fun ListenTapStep(step: ListenTap, feedback: Feedback, onAnswer: OnAnswer) {
    val logAnswer = LocalAnswerLog.current
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val options = remember(step) { step.options.shuffled() }
    var wrong by remember(step) { mutableStateOf(setOf<String>()) }

    LaunchedEffect(step) { speaker.say(UiStrings.listenAndPick, language, thenEnglish = step.answer) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.listenAndPick)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            SpeakButton(onClick = { speaker.speak(step.answer, Speaker.ENGLISH) }, size = 80.dp)
            SlowButton(onClick = { speaker.speak(step.answer, Speaker.ENGLISH, slow = true) })
        }
        Spacer(Modifier.height(16.dp))
        // Compact enough that the grid stays visible above the feedback panel on small phones.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { option ->
                        val isAnswer = option.english == step.answer
                        val state = optionState(isAnswer, option.english in wrong, feedback)
                        OptionTile(
                            state = state,
                            onClick = {
                                if (feedback.isDone || option.english in wrong) return@OptionTile
                                logAnswer(option.english)
                                if (isAnswer) onAnswer(true, null) else {
                                    wrong = wrong + option.english
                                    onAnswer(false, null)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.3f),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(option.emoji, fontSize = 52.sp)
                                // Show the word only once it's answered, so this stays a listening exercise.
                                if (state == OptionState.CORRECT) {
                                    Text(option.english, style = MaterialTheme.typography.titleLarge, color = Leaf)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- See the picture, pick the word ----------

@Composable
private fun WordPickStep(step: WordPick, feedback: Feedback, onAnswer: OnAnswer) {
    val logAnswer = LocalAnswerLog.current
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val options = remember(step) { step.options.shuffled() }
    var wrong by remember(step) { mutableStateOf(setOf<String>()) }

    LaunchedEffect(step) { speaker.say(UiStrings.pickWord, language) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.pickWord)
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(step.emoji, fontSize = 88.sp, modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp))
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val isAnswer = option == step.answer
                OptionTile(
                    state = optionState(isAnswer, option in wrong, feedback),
                    onClick = {
                        if (feedback.isDone || option in wrong) return@OptionTile
                        logAnswer(option)
                        if (isAnswer) onAnswer(true, null) else {
                            wrong = wrong + option
                            onAnswer(false, null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp),
                ) {
                    Text(option, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

// ---------- Find the English words inside a Hindi sentence ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FindEnglishStep(step: FindEnglish, feedback: Feedback, onAnswer: OnAnswer) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    var found by remember(step) { mutableStateOf(setOf<Int>()) }
    var wrong by remember(step) { mutableStateOf(setOf<Int>()) }
    val englishCount = step.words.count { it.en != null }

    LaunchedEffect(step) {
        val sentence = step.words.joinToString(" ") { it.hi }
        val instruction = if (language == AppLanguage.ENGLISH) UiStrings.findEnglish.en to Speaker.ENGLISH
        else UiStrings.findEnglish.hi to Speaker.HINDI
        speaker.speakAll(listOf(instruction, sentence to Speaker.HINDI))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.findEnglish)
        Spacer(Modifier.height(28.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            step.words.forEachIndexed { index, word ->
                val isEnglish = word.en != null
                val shown = isEnglish && (index in found || feedback.isDone)
                OptionTile(
                    state = when {
                        shown -> OptionState.CORRECT
                        index in wrong -> OptionState.WRONG
                        else -> OptionState.NORMAL
                    },
                    onClick = {
                        if (feedback.isDone || index in found || index in wrong) return@OptionTile
                        if (isEnglish) {
                            speaker.speak(word.en!!, Speaker.ENGLISH)
                            found = found + index
                            if (found.size == englishCount) onAnswer(true, null)
                        } else {
                            wrong = wrong + index
                            onAnswer(false, null)
                        }
                    },
                ) {
                    Column(
                        Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (language == AppLanguage.HINDI) word.hi else word.hinglish,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        // Reveal the English spelling under each found word: बस → bus
                        if (shown) Text(word.en!!, style = MaterialTheme.typography.titleMedium, color = Leaf)
                    }
                }
            }
        }
    }
}

// ---------- Phonics: letter-sound card ----------

@Composable
private fun LearnSoundStep(step: LearnSound) {
    val speaker = LocalSpeaker.current
    val phonics = LocalPhonics.current
    val language = LocalAppLanguage.current
    val playSound = { phonics.play(step.sound) { speaker.speak(step.example.english, Speaker.ENGLISH) } }

    // Tinku explains, then the recorded sound, then the example word: "s… sun".
    DisposableEffect(step) {
        var alive = true
        speaker.say(step.text, language, onDone = { if (alive) playSound() })
        onDispose {
            alive = false
            phonics.stop()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TinkuSays(message = step.text.text(), onListen = { speaker.say(step.text, language) }, tinkuSize = 90.dp)
        Spacer(Modifier.height(16.dp))
        Surface(
            onClick = { playSound() },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(step.letters, fontSize = 88.sp, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.displayLarge)
                if (language != AppLanguage.ENGLISH) {
                    Text(
                        "${UiStrings.soundsLike.text()}: ${step.hint}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(step.example.emoji, fontSize = 44.sp)
                    Text(step.example.english, style = MaterialTheme.typography.headlineMedium)
                }
                Text(
                    UiStrings.tapLetter.text(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------- Which word did you hear? (vet / wet) ----------

@Composable
private fun MinimalPairStep(step: MinimalPair, feedback: Feedback, onAnswer: OnAnswer) {
    val logAnswer = LocalAnswerLog.current
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val options = remember(step) { step.options.shuffled() }
    var wrong by remember(step) { mutableStateOf(setOf<String>()) }

    LaunchedEffect(step) { speaker.say(UiStrings.whichWord, language, thenEnglish = step.answer) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.whichWord)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            SpeakButton(onClick = { speaker.speak(step.answer, Speaker.ENGLISH) }, size = 96.dp)
            SlowButton(onClick = { speaker.speak(step.answer, Speaker.ENGLISH, slow = true) })
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            options.forEach { option ->
                val isAnswer = option == step.answer
                OptionTile(
                    state = optionState(isAnswer, option in wrong, feedback),
                    onClick = {
                        if (feedback.isDone || option in wrong) return@OptionTile
                        logAnswer(option)
                        if (isAnswer) onAnswer(true, null) else {
                            wrong = wrong + option
                            onAnswer(false, null)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 110.dp),
                ) {
                    Text(option, style = MaterialTheme.typography.displaySmall)
                }
            }
        }
    }
}

// ---------- Shared pieces ----------

@Composable
internal fun Instruction(text: L) {
    Text(
        text.text(),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun SlowButton(onClick: () -> Unit, size: Dp = 64.dp) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(size),
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🐢", fontSize = 28.sp)
        }
    }
}

enum class OptionState { NORMAL, CORRECT, WRONG }

internal fun optionState(isAnswer: Boolean, isWrong: Boolean, feedback: Feedback) = when {
    isAnswer && feedback.isDone -> OptionState.CORRECT
    isWrong -> OptionState.WRONG
    else -> OptionState.NORMAL
}

/** An answer button: pops green when right, shakes gently and fades when wrong. */
@Composable
internal fun OptionTile(
    state: OptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == OptionState.WRONG) {
            repeat(2) {
                shake.animateTo(12f, tween(50))
                shake.animateTo(-12f, tween(50))
            }
            shake.animateTo(0f, tween(50))
        }
    }
    val pop by animateFloatAsState(
        targetValue = if (state == OptionState.CORRECT) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "pop",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = when (state) {
            OptionState.CORRECT -> Leaf.copy(alpha = 0.12f)
            else -> colors.surface
        },
        border = BorderStroke(
            if (state == OptionState.CORRECT) 3.dp else 2.dp,
            if (state == OptionState.CORRECT) Leaf else colors.outline,
        ),
        shadowElevation = if (state == OptionState.NORMAL) 2.dp else 0.dp,
        modifier = modifier
            .graphicsLayer { translationX = shake.value * density }
            .scale(pop)
            .alpha(if (state == OptionState.WRONG) 0.4f else 1f),
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            content()
        }
    }
}
