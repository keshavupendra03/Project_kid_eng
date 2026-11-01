package com.samjhoenglish.ui.language

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.TinkuMood
import com.samjhoenglish.ui.components.TinkuSays

/** First-launch screen. Shown in all three languages because we don't know the user's language yet. */
@Composable
fun LanguagePickerScreen(onChosen: (AppLanguage) -> Unit) {
    val speaker = LocalSpeaker.current
    var selected by rememberSaveable { mutableStateOf<AppLanguage?>(null) }

    LaunchedEffect(Unit) { speaker.speak(UiStrings.CHOOSE_LANGUAGE_SPOKEN, Speaker.HINDI) }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TinkuSays(
            message = UiStrings.CHOOSE_LANGUAGE_ALL.replace("  •  ", "\n"),
            onListen = { speaker.speak(UiStrings.CHOOSE_LANGUAGE_SPOKEN, Speaker.HINDI) },
            mood = if (selected == null) TinkuMood.HAPPY else TinkuMood.CHEER,
        )
        Spacer(Modifier.height(20.dp))
        LanguageOptions(
            selected = selected,
            onSelect = {
                selected = it
                speaker.speakLanguageOption(it)
            },
        )
        Spacer(Modifier.height(24.dp))
        BigButton(
            text = selected?.let { UiStrings.letsGo.get(it) } ?: "👆",
            emoji = if (selected != null) "🚀" else null,
            enabled = selected != null,
            onClick = { selected?.let(onChosen) },
        )
    }
}
