package com.samjhoenglish.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.EnglishVoice
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.language.LanguageOptions
import com.samjhoenglish.ui.language.speakLanguageOption

private val voiceLabels = mapOf(
    EnglishVoice.AMERICAN to UiStrings.voiceAmerican,
    EnglishVoice.INDIAN to UiStrings.voiceIndian,
    EnglishVoice.BRITISH to UiStrings.voiceBritish,
)

/** Words with an ‘r’ after a vowel, where the accents differ most (arm vs "आम"). */
private const val VOICE_SAMPLE = "Arm. Car. Three. Water."

@Composable
fun SettingsScreen(
    current: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    englishVoice: EnglishVoice,
    onEnglishVoiceChange: (EnglishVoice) -> Unit,
    onBack: () -> Unit,
) {
    val speaker = LocalSpeaker.current
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onBack)
            Text(UiStrings.settings.text(), style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(28.dp))
        Text(UiStrings.appLanguage.text(), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        LanguageOptions(
            selected = current,
            onSelect = {
                onLanguageChange(it)
                speaker.speakLanguageOption(it)
            },
        )

        Spacer(Modifier.height(32.dp))
        Text(UiStrings.englishVoiceTitle.text(), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EnglishVoice.entries.forEach { voice ->
                VoiceOption(
                    label = voiceLabels.getValue(voice),
                    selected = voice == englishVoice,
                    onClick = {
                        // Apply straight away so the sample plays in the chosen accent.
                        speaker.englishVoice = voice
                        onEnglishVoiceChange(voice)
                        speaker.speak(VOICE_SAMPLE, Speaker.ENGLISH)
                    },
                )
            }
        }
    }
}

@Composable
private fun VoiceOption(label: L, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 3.dp else 2.dp, if (selected) colors.primary else colors.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔊  ${label.text()}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (selected) Text("✅", style = MaterialTheme.typography.titleLarge)
        }
    }
}
