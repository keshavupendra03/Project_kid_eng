package com.samjhoenglish.ui.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samjhoenglish.data.SettingsRepository
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.ui.components.EmojiButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class GateStep { CREATE, CONFIRM, VERIFY, FORGOT }

private const val MAX_WRONG_TRIES = 5
private const val LOCK_MILLIS = 30_000L

/**
 * Keeps children out of the parent section. Creates a PIN the first time, otherwise asks for it.
 * A forgotten PIN can be replaced after answering a sum: this is a child gate, not a bank vault.
 *
 * @param createNew skip asking for the current PIN (used by "Change PIN", when already unlocked)
 */
@Composable
fun ParentGate(
    settings: SettingsRepository,
    onDone: () -> Unit,
    onClose: () -> Unit,
    createNew: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val hasPin by settings.hasPin.collectAsStateWithLifecycle(initialValue = null)
    var step by remember { mutableStateOf(if (createNew) GateStep.CREATE else null) }
    var firstPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<L?>(null) }
    var attempt by remember { mutableIntStateOf(0) } // new key clears the pad
    var wrongTries by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var sum by remember { mutableStateOf(newSum()) }

    BackHandler(onBack = onClose)
    LaunchedEffect(hasPin) {
        val known = hasPin
        if (step == null && known != null) step = if (known) GateStep.VERIFY else GateStep.CREATE
    }
    LaunchedEffect(locked) {
        if (locked) {
            delay(LOCK_MILLIS)
            locked = false
            wrongTries = 0
            message = null
        }
    }

    fun clearPad() {
        attempt++
    }

    fun onEntered(entered: String) {
        when (step) {
            GateStep.CREATE -> {
                firstPin = entered
                message = null
                step = GateStep.CONFIRM
                clearPad()
            }
            GateStep.CONFIRM -> {
                if (entered == firstPin) {
                    scope.launch {
                        settings.setPin(entered)
                        onDone()
                    }
                } else {
                    message = UiStrings.pinMismatch
                    step = GateStep.CREATE
                    clearPad()
                }
            }
            GateStep.VERIFY -> scope.launch {
                if (settings.checkPin(entered)) {
                    onDone()
                } else {
                    wrongTries++
                    locked = wrongTries >= MAX_WRONG_TRIES
                    message = if (locked) UiStrings.pinLocked else UiStrings.pinWrong
                    clearPad()
                }
            }
            GateStep.FORGOT -> {
                if (entered == sum.answer.toString()) {
                    message = null
                    step = GateStep.CREATE
                } else {
                    sum = newSum()
                }
                clearPad()
            }
            null -> Unit
        }
    }

    val current = step ?: return Box(Modifier.fillMaxSize())
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.align(Alignment.Start)) {
            EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onClose)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when (current) {
                GateStep.CREATE -> UiStrings.pinCreate
                GateStep.CONFIRM -> UiStrings.pinConfirm
                GateStep.VERIFY -> UiStrings.pinEnter
                GateStep.FORGOT -> UiStrings.pinForgotQuestion
            }.text(),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (current == GateStep.CREATE && message == null) {
            Text(
                UiStrings.pinCreateNote.text(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (current == GateStep.FORGOT) {
            Spacer(Modifier.height(8.dp))
            Text("${sum.a} × ${sum.b} = ?", style = MaterialTheme.typography.displaySmall)
        }
        message?.let {
            Spacer(Modifier.height(4.dp))
            Text(it.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        key(attempt, current) {
            NumberPad(
                length = if (current == GateStep.FORGOT) sum.answer.toString().length else 4,
                masked = current != GateStep.FORGOT,
                enabled = !locked,
                onComplete = ::onEntered,
            )
        }
        if (current == GateStep.VERIFY) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                message = null
                sum = newSum()
                step = GateStep.FORGOT
            }) {
                Text(UiStrings.pinForgot.text(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private data class Sum(val a: Int, val b: Int) {
    val answer: Int get() = a * b
}

private fun newSum() = Sum(Random.nextInt(13, 49), Random.nextInt(6, 10))

@Composable
private fun NumberPad(length: Int, masked: Boolean, enabled: Boolean, onComplete: (String) -> Unit) {
    var entered by remember { mutableStateOf("") }

    fun press(digit: String) {
        if (!enabled || entered.length >= length) return
        entered += digit
        if (entered.length == length) onComplete(entered)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(length) { i ->
                val filled = i < entered.length
                Box(
                    Modifier
                        .size(28.dp)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (filled && !masked) {
                        Text(entered[i].toString(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(72.dp))
                        "⌫" -> PadKey(key) { entered = entered.dropLast(1) }
                        else -> PadKey(key) { press(key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PadKey(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp, modifier = Modifier.size(72.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
