package com.samjhoenglish.ui.lesson

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samjhoenglish.content.BLANK
import com.samjhoenglish.content.BuildSentence
import com.samjhoenglish.content.Choice
import com.samjhoenglish.content.Crossword
import com.samjhoenglish.content.CrosswordEntry
import com.samjhoenglish.content.MatchPair
import com.samjhoenglish.content.MatchPairs
import com.samjhoenglish.content.Meaning
import com.samjhoenglish.content.SortGroups
import com.samjhoenglish.content.SortItem
import com.samjhoenglish.content.cells
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.SpeakButton
import com.samjhoenglish.ui.theme.Leaf
import com.samjhoenglish.ui.theme.Sunny
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Hindi in Devanagari, or in Roman letters in Hinglish mode. Hidden in English mode. */
@Composable
private fun Meaning.shown(): String? = when (LocalAppLanguage.current) {
    AppLanguage.HINDI -> hi
    AppLanguage.HINGLISH -> hinglish
    AppLanguage.ENGLISH -> null
}

// ---------- A written question with choices (comprehension, riddles, tables) ----------

@Composable
internal fun ChoiceStep(step: Choice, feedback: Feedback, onAnswer: OnAnswer) {
    val logAnswer = LocalAnswerLog.current
    val speaker = LocalSpeaker.current
    val options = remember(step) { step.options.indices.shuffled() }
    var wrong by remember(step) { mutableStateOf(setOf<String>()) }

    LaunchedEffect(step) { speaker.speak(step.question, Speaker.ENGLISH) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.pickOne)
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = { speaker.speak(step.question, Speaker.ENGLISH) },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🔊 ${step.question}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                step.questionMeaning?.shown()?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("= $it", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { i ->
                val option = step.options[i]
                val isAnswer = option == step.answer
                OptionTile(
                    state = optionState(isAnswer, option in wrong, feedback),
                    onClick = {
                        if (feedback.isDone || option in wrong) return@OptionTile
                        speaker.speak(option, Speaker.ENGLISH)
                        logAnswer(option)
                        if (isAnswer) onAnswer(true, null) else {
                            wrong = wrong + option
                            onAnswer(false, null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        step.emojis.getOrNull(i)?.takeIf { it.isNotEmpty() }?.let { Text(it, fontSize = 36.sp) }
                        Text(option, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        val model = step.modelAnswer
        if (feedback.isDone && model != null) {
            Spacer(Modifier.height(16.dp))
            ExamAnswer(model, step.modelMeaning)
        }
    }
}

/** The full sentence to write in the exam, with its Hindi. */
@Composable
private fun ExamAnswer(sentence: String, meaning: Meaning?) {
    val speaker = LocalSpeaker.current
    Surface(
        onClick = { speaker.speak(sentence, Speaker.ENGLISH) },
        shape = RoundedCornerShape(20.dp),
        color = Leaf.copy(alpha = 0.10f),
        border = BorderStroke(2.dp, Leaf),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(UiStrings.examAnswer.text(), style = MaterialTheme.typography.labelLarge, color = Leaf)
            Spacer(Modifier.height(4.dp))
            Text("🔊 $sentence", style = MaterialTheme.typography.titleLarge)
            meaning?.shown()?.let {
                Spacer(Modifier.height(4.dp))
                Text("= $it", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------- Match words with their meanings ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MatchPairsStep(step: MatchPairs, feedback: Feedback, onAnswer: OnAnswer) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val rights = remember(step) { step.pairs.indices.shuffled() }
    var matched by remember(step) { mutableStateOf(setOf<Int>()) }
    var picked by remember(step) { mutableStateOf<Int?>(null) }
    var wrongRight by remember(step) { mutableStateOf<Int?>(null) }

    LaunchedEffect(step) { speaker.say(step.instruction, language) }
    LaunchedEffect(wrongRight) {
        if (wrongRight != null) {
            delay(700)
            wrongRight = null
        }
    }
    val done = feedback.isDone
    val joined = if (done) step.pairs.indices.toSet() else matched

    fun tryRight(right: Int) {
        val left = picked ?: return
        if (left == right) {
            speaker.speak(step.pairs[left].left, Speaker.ENGLISH)
            matched = matched + left
            picked = null
            if (matched.size == step.pairs.size) onAnswer(true, null)
        } else {
            wrongRight = right
            picked = null
            onAnswer(false, null)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(step.instruction)
        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            step.pairs.forEachIndexed { i, pair ->
                val isJoined = i in joined
                val selected = picked == i
                Surface(
                    onClick = {
                        if (done || isJoined) return@Surface
                        speaker.speak(pair.left, Speaker.ENGLISH)
                        picked = if (selected) null else i
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        isJoined -> Leaf.copy(alpha = 0.15f)
                        selected -> Sunny
                        else -> MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(if (selected) 3.dp else 2.dp, if (isJoined) Leaf else if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    shadowElevation = if (isJoined) 0.dp else 2.dp,
                ) {
                    Text(
                        (if (isJoined) "✓ " else "") + pair.left,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rights.forEach { i ->
                val pair = step.pairs[i]
                val isJoined = i in joined
                OptionTile(
                    state = when {
                        isJoined -> OptionState.CORRECT
                        wrongRight == i -> OptionState.WRONG
                        else -> OptionState.NORMAL
                    },
                    onClick = { if (!done && !isJoined) tryRight(i) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    MeaningRow(pair, isJoined)
                }
            }
        }
    }
}

@Composable
private fun MeaningRow(pair: MatchPair, joined: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        if (joined) Text(pair.left, style = MaterialTheme.typography.labelLarge, color = Leaf, fontWeight = FontWeight.Bold)
        Text(pair.right, style = MaterialTheme.typography.titleMedium)
        if (joined) pair.meaning?.shown()?.let {
            Text("= $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------- Crossword ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CrosswordStep(step: Crossword, feedback: Feedback, onAnswer: OnAnswer) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val toSolve = remember(step) { step.entries.filter { !it.given }.sortedBy { it.number } }
    var solved by remember(step) { mutableStateOf(setOf<Int>()) }
    var current by remember(step) { mutableStateOf(toSolve.firstOrNull()?.number) }
    var wrongWord by remember(step) { mutableStateOf<String?>(null) }
    val bank = remember(step) { toSolve.map { it.answer.lowercase() }.shuffled() }

    LaunchedEffect(step) { speaker.say(UiStrings.crosswordInstruction, language) }
    LaunchedEffect(wrongWord) {
        if (wrongWord != null) {
            delay(700)
            wrongWord = null
        }
    }
    val done = feedback.isDone
    val filled = if (done) step.entries.map { it.number }.toSet() else solved + step.entries.filter { it.given }.map { it.number }
    val active = step.entries.firstOrNull { it.number == current && !done }

    // Letters in the grid: every square of a filled word.
    val letters = mutableMapOf<Pair<Int, Int>, Char>()
    step.entries.filter { it.number in filled }.forEach { e -> e.cells.forEachIndexed { i, cell -> letters[cell] = e.answer[i].uppercaseChar() } }
    val squares = step.entries.flatMap { it.cells }.toSet()
    val numbers = step.entries.associate { (it.row to it.col) to it.number }
    val highlighted = active?.cells?.toSet().orEmpty()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(UiStrings.crosswordInstruction)
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cell = minOf(34.dp, maxWidth / step.cols)
            Column {
                for (r in 0 until step.rows) {
                    Row {
                        for (c in 0 until step.cols) {
                            val here = r to c
                            if (here !in squares) {
                                Spacer(Modifier.size(cell))
                            } else {
                                Box(
                                    Modifier
                                        .size(cell)
                                        .background(if (here in highlighted) Sunny.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    numbers[here]?.let {
                                        Text(
                                            "$it",
                                            fontSize = 8.sp,
                                            modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    letters[here]?.let { Text("$it", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // The clue being solved, with its words to choose from.
        if (active != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Sunny.copy(alpha = 0.25f),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "${active.number}. ${active.clue.replace(BLANK, "_____")}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(14.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val left = bank.filter { word -> toSolve.none { it.number in solved && it.answer.equals(word, ignoreCase = true) } }
                left.forEach { word ->
                    OptionTile(
                        state = if (wrongWord == word) OptionState.WRONG else OptionState.NORMAL,
                        onClick = {
                            if (active.answer.equals(word, ignoreCase = true)) {
                                speaker.speak(active.clue.replace(BLANK, word), Speaker.ENGLISH)
                                val nowSolved = solved + active.number
                                solved = nowSolved
                                current = toSolve.firstOrNull { it.number !in nowSolved }?.number
                                if (nowSolved.size == toSolve.size) onAnswer(true, null)
                            } else {
                                wrongWord = word
                                onAnswer(false, null)
                            }
                        },
                    ) {
                        Text(word, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // All the clues: solved ones show the filled sentence; tap another to switch.
        Text(UiStrings.tapClue.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            step.entries.sortedBy { it.number }.forEach { entry -> ClueLine(entry, isFilled = entry.number in filled, isActive = entry == active) { if (!done && entry.number !in filled) current = entry.number } }
        }
    }
}

@Composable
private fun ClueLine(entry: CrosswordEntry, isFilled: Boolean, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Sunny.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            (if (isFilled) "✓ " else "") + "${entry.number}. " + entry.clue.replace(BLANK, if (isFilled) entry.answer else "_____"),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isFilled) Leaf else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

// ---------- Sort into groups ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SortGroupsStep(step: SortGroups, feedback: Feedback, onAnswer: OnAnswer) {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    var placed by remember(step) { mutableStateOf(mapOf<Int, String>()) }
    var wrongGroup by remember(step) { mutableStateOf<String?>(null) }

    LaunchedEffect(step) { speaker.say(step.instruction, language) }
    LaunchedEffect(wrongGroup) {
        if (wrongGroup != null) {
            delay(700)
            wrongGroup = null
        }
    }
    val done = feedback.isDone
    // Once shown, every item sits in its (first) right group.
    val where = if (done) step.items.indices.associateWith { placed[it] ?: step.items[it].groups.first() } else placed
    val nextIndex = step.items.indices.firstOrNull { it !in where }
    val next: SortItem? = nextIndex?.let { step.items[it] }
    LaunchedEffect(nextIndex) { next?.let { speaker.speak(it.text, Speaker.ENGLISH) } }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Instruction(step.instruction)
        Spacer(Modifier.height(12.dp))
        if (next != null) {
            Surface(
                onClick = { speaker.speak(next.text, Speaker.ENGLISH) },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shadowElevation = 2.dp,
            ) {
                Row(Modifier.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(next.emoji, fontSize = 48.sp)
                    Text(next.text, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(UiStrings.sortNow.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            step.groups.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { group ->
                        OptionTile(
                            state = if (wrongGroup == group.id) OptionState.WRONG else OptionState.NORMAL,
                            onClick = {
                                val i = nextIndex ?: return@OptionTile
                                if (done) return@OptionTile
                                if (group.id in step.items[i].groups) {
                                    val now = placed + (i to group.id)
                                    placed = now
                                    if (now.size == step.items.size) onAnswer(true, null)
                                } else {
                                    wrongGroup = group.id
                                    onAnswer(false, null)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 120.dp),
                        ) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${group.emoji} ${group.name}", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                                group.meaning.shown()?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Spacer(Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                                    where.filterValues { it == group.id }.keys.sorted().forEach { Text(step.items[it].emoji, fontSize = 26.sp) }
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------- Build a sentence from word tiles ----------

/** A tile with its place in the pool, so a word used twice ("the … the") stays two tiles. */
private data class Tile(val index: Int, val text: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BuildSentenceStep(step: BuildSentence, feedback: Feedback, onAnswer: OnAnswer) {
    val logAnswer = LocalAnswerLog.current
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val pool = remember(step) { (step.tiles + step.extra).shuffled().mapIndexed { i, w -> Tile(i, w) } }
    var placed by remember(step) { mutableStateOf(listOf<Tile>()) }
    val shake = remember { Animatable(0f) }
    val size = step.tiles.size

    LaunchedEffect(step) { speaker.say(step.prompt, language) }

    val shown = if (feedback.isDone) step.tiles.mapIndexed { i, w -> Tile(-1 - i, w) } else placed

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(step.prompt.text(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
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
                shown.forEach { tile ->
                    WordTile(tile.text, onClick = { if (!feedback.isDone) placed = placed - tile })
                }
                repeat((size - shown.size).coerceAtLeast(0)) {
                    Box(Modifier.size(width = 48.dp, height = 40.dp).border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)))
                }
            }
        }
        if (feedback.isDone) {
            step.meaning.shown()?.let {
                Spacer(Modifier.height(8.dp))
                Text("= $it", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(UiStrings.buildInstruction.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                pool.filter { it !in placed }.forEach { tile ->
                    WordTile(tile.text, big = true, onClick = {
                        if (placed.size < size) {
                            speaker.speak(tile.text, Speaker.ENGLISH)
                            placed = placed + tile
                        }
                    })
                }
            }
            if (placed.size == size) {
                Spacer(Modifier.height(16.dp))
                BigButton(text = UiStrings.check.text(), emoji = "✔️", color = MaterialTheme.colorScheme.secondary, onClick = {
                    val built = placed.joinToString(" ") { it.text }
                    logAnswer(built)
                    if (built == step.answer || built in step.also) onAnswer(true, null) else {
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
private fun WordTile(text: String, big: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 2.dp,
    ) {
        Text(
            text,
            style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = if (big) 16.dp else 12.dp, vertical = if (big) 8.dp else 4.dp),
        )
    }
}
