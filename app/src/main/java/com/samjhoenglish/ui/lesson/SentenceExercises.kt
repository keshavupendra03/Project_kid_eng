package com.samjhoenglish.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samjhoenglish.content.BLANK
import com.samjhoenglish.content.FillBlank
import com.samjhoenglish.content.ListenRead
import com.samjhoenglish.content.MatchMeaning
import com.samjhoenglish.content.Meaning
import com.samjhoenglish.content.SentenceTrain
import com.samjhoenglish.content.SpotMistake
import com.samjhoenglish.content.corrected
import com.samjhoenglish.content.filled
import com.samjhoenglish.content.sentence
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.SpeakButton
import com.samjhoenglish.ui.theme.Leaf
import com.samjhoenglish.ui.theme.RoleAction
import com.samjhoenglish.ui.theme.RoleRest
import com.samjhoenglish.ui.theme.RoleWho
import com.samjhoenglish.ui.theme.roleColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Hindi meanings show in Roman letters in Hinglish mode, otherwise in Devanagari. */
@Composable
private fun Meaning.text(): String = if (LocalAppLanguage.current == AppLanguage.HINGLISH) hinglish else hi

// ---------- English word → Hindi meaning ----------

@Composable
internal fun MatchMeaningStep(step: MatchMeaning, feedback: Feedback, onAnswer: OnAnswer) {
    val logAnswer = LocalAnswerLog.current
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val options = remember(step) { step.options.shuffled() }
    var wrong by remember(step) { mutableStateOf(setOf<Meaning>()) }

    LaunchedEffect(step) { speaker.say(UiStrings.pickMeaning, language, thenEnglish = step.word.english) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.pickMeaning)
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = { speaker.speak(step.word.english, Speaker.ENGLISH) },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (step.word.emoji.isNotEmpty()) Text(step.word.emoji, fontSize = 56.sp)
                Text("🔊 ${step.word.english}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val isAnswer = option == step.answer
                OptionTile(
                    state = optionState(isAnswer, option in wrong, feedback),
                    onClick = {
                        if (feedback.isDone || option in wrong) return@OptionTile
                        logAnswer(option.hi)
                        if (isAnswer) onAnswer(true, null) else {
                            wrong = wrong + option
                            onAnswer(false, null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                ) {
                    Text(option.text(), style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

// ---------- Hear a word, pick how it's written ----------

@Composable
internal fun ListenReadStep(step: ListenRead, feedback: Feedback, onAnswer: OnAnswer) {
    val logAnswer = LocalAnswerLog.current
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val options = remember(step) { step.options.shuffled() }
    var wrong by remember(step) { mutableStateOf(setOf<String>()) }

    LaunchedEffect(step) { speaker.say(UiStrings.listenRead, language, thenEnglish = step.answer) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.listenRead)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            SpeakButton(onClick = { speaker.speak(step.answer, Speaker.ENGLISH) }, size = 88.dp)
            SlowButton(onClick = { speaker.speak(step.answer, Speaker.ENGLISH, slow = true) })
        }
        Spacer(Modifier.height(20.dp))
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

// ---------- Sentence Train ----------

/** A word block with its index in the pool, so repeated words ("the ... the") stay distinct. */
private data class Block(val index: Int, val text: String, val role: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SentenceTrainStep(step: SentenceTrain, feedback: Feedback, onAnswer: OnAnswer) {
    val logAnswer = LocalAnswerLog.current
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val pool = remember(step) {
        (step.answer.map { it.text to it.role } + step.extra.map { it to "x" })
            .shuffled()
            .mapIndexed { i, (text, role) -> Block(i, text, role) }
    }
    var placed by remember(step) { mutableStateOf(listOf<Block>()) }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(step) {
        val instruction = if (language == AppLanguage.ENGLISH) UiStrings.trainInstruction.en to Speaker.ENGLISH
        else UiStrings.trainInstruction.hi to Speaker.HINDI
        speaker.speakAll(listOf(instruction, step.hindi.joinToString(" ") { it.hi } to Speaker.HINDI))
    }

    // When finished (or revealed), the train shows the right sentence.
    val shown = if (feedback.isDone) step.answer.mapIndexed { i, w -> Block(-1 - i, w.text, w.role) } else placed

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.trainInstruction)
        Spacer(Modifier.height(8.dp))
        RoleLegend()
        Spacer(Modifier.height(14.dp))

        // The Hindi sentence, coloured by role.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            step.hindi.forEach { word ->
                WordChip(if (language == AppLanguage.HINDI) word.hi else word.hinglish, roleColor(word.role))
            }
        }
        Spacer(Modifier.height(14.dp))

        // The train: engine + coaches.
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, if (feedback == Feedback.CORRECT) Leaf else MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shake.value * density },
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp).heightIn(min = 56.dp),
            ) {
                Text("🚂", fontSize = 34.sp)
                shown.forEach { block ->
                    WordChip(block.text, roleColor(block.role), onClick = {
                        if (!feedback.isDone) placed = placed - block
                    })
                }
                repeat((step.answer.size - shown.size).coerceAtLeast(0)) { EmptyCoach() }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Blocks still waiting at the station.
        if (!feedback.isDone) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                pool.filter { it !in placed }.forEach { block ->
                    WordChip(block.text, roleColor(block.role), big = true, onClick = {
                        if (placed.size < step.answer.size) {
                            speaker.speak(block.text, Speaker.ENGLISH)
                            placed = placed + block
                        }
                    })
                }
            }
            if (placed.size == step.answer.size) {
                Spacer(Modifier.height(16.dp))
                BigButton(text = UiStrings.check.text(), emoji = "✔️", color = MaterialTheme.colorScheme.secondary, onClick = {
                    val right = placed.map { it.text } == step.answer.map { it.text }
                    logAnswer(placed.joinToString(" ") { it.text })
                    if (right) onAnswer(true, null) else {
                        onAnswer(false, null)
                        scope.launch {
                            repeat(2) {
                                shake.animateTo(12f, tween(50))
                                shake.animateTo(-12f, tween(50))
                            }
                            shake.animateTo(0f, tween(50))
                            delay(300)
                            placed = emptyList()
                        }
                    }
                })
            }
        }
    }
}

@Composable
private fun RoleLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(RoleWho, UiStrings.roleWho.text())
        LegendDot(RoleAction, UiStrings.roleAction.text())
        LegendDot(RoleRest, UiStrings.roleRest.text())
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
    }
}

@Composable
private fun WordChip(text: String, color: Color, big: Boolean = false, onClick: (() -> Unit)? = null) {
    val content: @Composable () -> Unit = {
        Text(
            text,
            style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = if (big) 18.dp else 12.dp, vertical = if (big) 8.dp else 4.dp),
        )
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = color, shadowElevation = 2.dp) { content() }
    } else {
        Surface(shape = RoundedCornerShape(14.dp), color = color) { content() }
    }
}

@Composable
private fun EmptyCoach() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(width = 56.dp, height = 40.dp),
    ) {}
}

// ---------- Fill the gap ----------

@Composable
internal fun FillBlankStep(step: FillBlank, feedback: Feedback, onAnswer: OnAnswer) {
    val logAnswer = LocalAnswerLog.current
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val options = remember(step) { step.options.shuffled() }
    var wrong by remember(step) { mutableStateOf(setOf<String>()) }

    LaunchedEffect(step) { speaker.say(UiStrings.fillBlank, language) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.fillBlank)
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, if (feedback == Feedback.CORRECT) Leaf else MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (feedback.isDone) step.filled else step.sentence.replace(BLANK, "_____"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                step.meaning?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("= ${it.text()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        val tile: @Composable (String, Modifier) -> Unit = { option, modifier ->
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
                modifier = modifier.heightIn(min = 64.dp),
            ) {
                Text(
                    option,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        // Short words (is / am / are) sit side by side; phrases like "at one go" get a full-width row each.
        if (options.any { it.contains(' ') || it.length > 7 }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { tile(it, Modifier.fillMaxWidth()) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { tile(it, Modifier.weight(1f)) }
            }
        }
    }
}

// ---------- Mistake Detective ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpotMistakeStep(step: SpotMistake, feedback: Feedback, onAnswer: OnAnswer) {
    val logAnswer = LocalAnswerLog.current
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    var wrongTaps by remember(step) { mutableStateOf(setOf<Int>()) }
    var lastWrong by remember(step) { mutableIntStateOf(-1) }

    LaunchedEffect(step) { speaker.say(UiStrings.spotMistake, language, thenEnglish = step.words.joinToString(" ")) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.spotMistake)
        Spacer(Modifier.height(20.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            step.words.forEachIndexed { index, word ->
                val isWrongWord = index == step.wrong
                val found = isWrongWord && feedback.isDone
                OptionTile(
                    state = when {
                        found -> OptionState.CORRECT
                        index in wrongTaps -> OptionState.WRONG
                        else -> OptionState.NORMAL
                    },
                    onClick = {
                        if (feedback.isDone || index in wrongTaps) return@OptionTile
                        logAnswer(step.words[index])
                        if (isWrongWord) onAnswer(true, null) else {
                            wrongTaps = wrongTaps + index
                            lastWrong = index
                            onAnswer(false, null)
                        }
                    },
                    modifier = Modifier.widthIn(min = 64.dp),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            word,
                            style = MaterialTheme.typography.headlineSmall,
                            textDecoration = if (found) TextDecoration.LineThrough else null,
                            color = if (found) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        // The fix appears right under the crossed-out word: are → is
                        if (found) Text(step.fix, style = MaterialTheme.typography.headlineSmall, color = Leaf)
                    }
                }
            }
        }
        step.meaning?.let {
            Spacer(Modifier.height(16.dp))
            Text("= ${it.text()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (feedback.isDone) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("✅ ${step.corrected}", style = MaterialTheme.typography.headlineSmall, color = Leaf, textAlign = TextAlign.Center)
            }
        }
    }
}
