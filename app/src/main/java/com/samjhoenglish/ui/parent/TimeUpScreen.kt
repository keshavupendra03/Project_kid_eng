package com.samjhoenglish.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.samjhoenglish.data.SettingsRepository
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.rememberSay
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.TinkuMood
import com.samjhoenglish.ui.components.TinkuSays

/** Shown instead of a new lesson once today's time is used up. A parent can give more time with the PIN. */
@Composable
fun TimeUpScreen(settings: SettingsRepository, onHome: () -> Unit, onExtraTime: () -> Unit) {
    val say = rememberSay()
    var askingPin by remember { mutableStateOf(false) }

    if (askingPin) {
        ParentGate(settings, onDone = onExtraTime, onClose = { askingPin = false })
        return
    }

    LaunchedEffect(Unit) { say(UiStrings.timeUp) }
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TinkuSays(message = UiStrings.timeUp.text(), onListen = { say(UiStrings.timeUp) }, mood = TinkuMood.CHEER)
        Spacer(Modifier.height(24.dp))
        BigButton(text = UiStrings.goHome.text(), emoji = "🏠", onClick = onHome)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { askingPin = true }) {
            Text(UiStrings.timeUpMore.text(), style = MaterialTheme.typography.titleMedium)
        }
    }
}
