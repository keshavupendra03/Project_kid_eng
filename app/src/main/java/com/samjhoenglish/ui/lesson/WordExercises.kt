package com.samjhoenglish.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samjhoenglish.content.LearnWord
import com.samjhoenglish.content.SpellWord
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.SpeakButton
import com.samjhoenglish.ui.theme.Leaf

// ---------- A new word ----------

/**
 * A new word: picture, the word, its Hindi meaning, a simple English meaning and an example.
 * Tinku says the word, its Hindi meaning, then the example, each in its own voice.
 */
@Composable
internal fun LearnWordStep(step: LearnWord) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val hindi = if (language == AppLanguage.HINGLISH) step.word.hinglish ?: step.word.hindi else step.word.hindi
    val exampleHindi = step.exampleMeaning?.let { if (language == AppLanguage.HINGLISH) it.hinglish else it.hi }
    val playAll = {
        speaker.speakAll(
            listOfNotNull(
                step.word.english to Speaker.ENGLISH,
                step.word.hindi?.takeIf { language != AppLanguage.ENGLISH }?.let { it to Speaker.HINDI },
                step.example to Speaker.ENGLISH,
                // The Hindi voice reads Devanagari, also in Hinglish mode.
                step.exampleMeaning?.hi?.takeIf { language != AppLanguage.ENGLISH }?.let { it to Speaker.HINDI },
            ),
        )
    }
    LaunchedEffect(step) { playAll() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(UiStrings.newWord.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(step.word.emoji, fontSize = 64.sp)
                // The word gets its own line, so long words (government, deforestation) can't squeeze the buttons.
                Text(
                    step.word.english,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SpeakButton(onClick = { speaker.speak(step.word.english, Speaker.ENGLISH) }, size = 52.dp)
                    SlowButton(onClick = { speaker.speak(step.word.english, Speaker.ENGLISH, slow = true) }, size = 52.dp)
                }
                if (hindi != null && language != AppLanguage.ENGLISH) {
                    Text("= $hindi", style = MaterialTheme.typography.headlineSmall)
                }
                Text(step.meaning, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = {
                speaker.speakAll(
                    listOfNotNull(
                        step.example to Speaker.ENGLISH,
                        step.exampleMeaning?.hi?.takeIf { language != AppLanguage.ENGLISH }?.let { it to Speaker.HINDI },
                    ),
                )
            },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("💬 ${UiStrings.forExample.text()}", style = MaterialTheme.typography.labelLarge)
                Text(highlight(step.example, step.word.english), style = MaterialTheme.typography.titleLarge)
                if (exampleHindi != null && language != AppLanguage.ENGLISH) {
                    Text("= $exampleHindi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** The example sentence with the new word in bold. */
private fun highlight(sentence: String, word: String): AnnotatedString = buildAnnotatedString {
    val match = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE).find(sentence)
    if (match == null) {
        append(sentence)
        return@buildAnnotatedString
    }
    append(sentence.substring(0, match.range.first))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Leaf)) { append(match.value) }
    append(sentence.substring(match.range.last + 1))
}

// ---------- Spell the word ----------

/**
 * Hear the word (with its picture and Hindi meaning) and spell it: letter tiles for younger
 * children, the keyboard for Classes 5–8, who can switch to tiles if typing is hard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpellWordStep(step: SpellWord, feedback: Feedback, onAnswer: OnAnswer) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val target = step.word.english.lowercase()
    var useTiles by remember(step) { mutableStateOf(!step.typed) }
    var typed by remember(step) { mutableStateOf("") }
    // Tiles: the word's letters plus two extra, shuffled. Each tile can be used once.
    val tiles = remember(step) {
        val extra = ('a'..'z').filter { it !in target }.shuffled().take(2)
        (target.toList() + extra).shuffled().mapIndexed { i, c -> i to c }
    }
    var placed by remember(step) { mutableStateOf(listOf<Pair<Int, Char>>()) }

    fun check(answer: String) {
        if (feedback.isDone || answer.isBlank()) return
        if (answer.trim().lowercase() == target) onAnswer(true, null) else {
            onAnswer(false, null)
            placed = emptyList()
            typed = ""
        }
    }

    LaunchedEffect(step) { speaker.say(UiStrings.spellThis, language, thenEnglish = step.word.english) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.spellThis)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(step.word.emoji, fontSize = 56.sp)
            SpeakButton(onClick = { speaker.speak(step.word.english, Speaker.ENGLISH) }, size = 64.dp)
            SlowButton(onClick = { speaker.speak(step.word.english, Speaker.ENGLISH, slow = true) })
        }
        val hindi = if (language == AppLanguage.HINGLISH) step.word.hinglish ?: step.word.hindi else step.word.hindi
        if (hindi != null && language != AppLanguage.ENGLISH) {
            Text("= $hindi", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(14.dp))

        if (feedback.isDone) {
            Text(step.word.english, style = MaterialTheme.typography.displaySmall, color = Leaf)
            return@Column
        }

        if (useTiles) {
            // The answer so far, one box per letter.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                target.indices.forEach { i ->
                    val tile = placed.getOrNull(i)
                    Surface(
                        onClick = { if (tile != null) placed = placed - tile },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.size(width = 40.dp, height = 50.dp),
                    ) {
                        Text(
                            tile?.second?.toString() ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                tiles.filter { it !in placed }.forEach { tile ->
                    Surface(
                        onClick = {
                            if (placed.size < target.length) {
                                placed = placed + tile
                                if (placed.size == target.length) check(placed.map { it.second }.joinToString(""))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Text(tile.second.toString(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.filter { c -> c.isLetter() }.take(target.length + 4) },
                singleLine = true,
                label = { Text(UiStrings.typeTheWord.text()) },
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { check(typed) }),
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            )
            Spacer(Modifier.height(12.dp))
            BigButton(text = UiStrings.check.text(), emoji = "✔️", enabled = typed.isNotBlank(), onClick = { check(typed) })
            TextButton(onClick = { useTiles = true }) {
                Text("🔤 ${UiStrings.useLetterTiles.text()}", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
