package com.samjhoenglish.lab

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.ReadAlongTracker
import com.samjhoenglish.speech.WordMark
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.theme.Leaf
import com.samjhoenglish.ui.theme.Sunny

/** Test builds only: compares listeners on a paragraph read aloud. */
object Lab {
    const val AVAILABLE = true
}

private val PARAGRAPHS = listOf(
    "Easy" to "I have a red ball. It is very big. I play with my ball in the park. My friend Ravi plays with me. " +
        "We run and we jump. Then we sit under a tree. We see three birds. It is a happy day.",
    "Harder" to "Every morning, Meera walks to school with her brother. They carry their bags and water bottles. " +
        "On the way, they see a big van and a white cow. Their teacher says, thank you for coming on time.",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadAlongLab(onBack: () -> Unit) {
    val context = LocalContext.current
    val speaker = LocalSpeaker.current
    val zipformer = remember { ZipformerEngine(context) }
    val vosk = remember { VoskEngine(context) }
    val engines = remember { listOf(zipformer, vosk, GoogleEngine(context)) }
    var engine by remember { mutableStateOf<ListenEngine>(zipformer) }
    var grammar by remember { mutableStateOf(VoskGrammar.SENTENCES) }
    var saved by remember { mutableStateOf<String?>(null) }
    var paragraph by remember { mutableStateOf(PARAGRAPHS.first()) }
    val words = remember(paragraph) { paragraph.second.split(" ") }
    var tracker by remember(paragraph) { mutableStateOf(ReadAlongTracker(words)) }
    var state by remember(paragraph) { mutableStateOf(tracker.state) }
    var listening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Pick a listener, then tap Start and read aloud.") }
    var heard by remember { mutableStateOf(listOf<String>()) }
    var partial by remember { mutableStateOf("") }
    var startedAt by remember { mutableLongStateOf(0L) }
    var seconds by remember { mutableStateOf<Long?>(null) }

    fun stopAll() {
        val wasListening = listening
        engines.forEach { it.stop() }
        listening = false
        if (wasListening) {
            saved = engine.lastFile?.name
            val read = tracker.state.marks.count { it != WordMark.WAITING }
            android.util.Log.i(LOG_TAG, "result ${engine.name} ${grammar.name}: $read/${words.size} saved=${engine.lastFile?.name}")
        }
    }

    fun reset() {
        stopAll()
        tracker = ReadAlongTracker(words)
        state = tracker.state
        heard = emptyList()
        partial = ""
        seconds = null
        status = "Ready. Tap Start and read aloud."
    }

    fun start() {
        speaker.stop()
        reset()
        listening = true
        saved = null
        startedAt = System.currentTimeMillis()
        vosk.grammar = grammar
        engine.start(
            paragraph.second,
            onText = { guesses, final ->
                state = tracker.hearAny(guesses, final)
                val text = guesses.firstOrNull().orEmpty()
                if (final) {
                    if (text.isNotBlank()) heard = (heard + text).takeLast(8)
                    partial = ""
                } else {
                    partial = text
                }
                if (tracker.done && seconds == null) {
                    seconds = (System.currentTimeMillis() - startedAt) / 1000
                    stopAll()
                    status = "Finished!"
                }
            },
            onStatus = { status = it },
        )
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else status = "Microphone permission is needed."
    }

    BackHandler {
        stopAll()
        onBack()
    }
    DisposableEffect(Unit) {
        onDispose {
            engines.forEach { it.stop() }
            vosk.release()
            zipformer.release()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmojiButton(emoji = "⬅️", label = "Back", onClick = {
                stopAll()
                onBack()
            })
            Text("🧪 Read-along test", style = MaterialTheme.typography.headlineSmall)
        }

        Text("Listener", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            engines.forEach { e ->
                Choice(e.name, selected = e == engine, modifier = Modifier.fillMaxWidth()) {
                    if (!listening) {
                        engine = e
                        reset()
                    }
                }
            }
        }
        if (engine == vosk) {
            Text("Vosk expects", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoskGrammar.entries.forEach { g ->
                    Choice(g.label, selected = g == grammar, modifier = Modifier.weight(1f)) { if (!listening) grammar = g }
                }
            }
        }
        Text("Paragraph", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PARAGRAPHS.forEach { p ->
                Choice(p.first, selected = p == paragraph, modifier = Modifier.weight(1f)) {
                    if (!listening) paragraph = p
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FlowRow(
                Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                words.forEachIndexed { i, word ->
                    val mark = state.marks[i]
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (mark) {
                            WordMark.READ -> Leaf.copy(alpha = 0.35f)
                            WordMark.SLIP -> Sunny.copy(alpha = 0.6f)
                            WordMark.WAITING -> MaterialTheme.colorScheme.surface
                        },
                    ) {
                        Text(
                            word,
                            style = MaterialTheme.typography.headlineSmall,
                            textDecoration = if (i == state.next && listening) TextDecoration.Underline else null,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }

        val read = state.marks.count { it != WordMark.WAITING }
        Text(
            "$read of ${words.size} words read" + (seconds?.let { " in $it seconds" } ?: "") +
                if (state.slips.isNotEmpty()) " · ${state.slips.size} slips (yellow)" else "",
            style = MaterialTheme.typography.titleMedium,
        )
        state.slips.entries.sortedBy { it.key }.forEach { (i, mistake) ->
            Text("“${words[i]}”: ${mistake.tip.text()}", style = MaterialTheme.typography.bodyMedium)
        }
        Text(status, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        saved?.let { Text("Recording kept on this phone: $it", style = MaterialTheme.typography.bodySmall) }

        if (listening) {
            BigButton(text = "Stop", emoji = "⏹️", onClick = { stopAll(); status = "Stopped." })
        } else {
            BigButton(text = "Start", emoji = "🎤", onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (granted) start() else permission.launch(Manifest.permission.RECORD_AUDIO)
            })
        }
        BigButton(text = "Reset", emoji = "🔄", color = MaterialTheme.colorScheme.secondary, onClick = ::reset)

        Spacer(Modifier.height(4.dp))
        Text("What the listener heard (for checking):", style = MaterialTheme.typography.titleSmall)
        if (partial.isNotBlank()) Text("… $partial", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        heard.asReversed().forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) colors.primary else colors.outline),
        modifier = modifier,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(10.dp))
    }
}
