package com.samjhoenglish.ui.language

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.ui.theme.LevelColors

private data class LanguageOption(
    val language: AppLanguage,
    val badge: String,
    val title: String,
    val subtitle: String,
)

private val options = listOf(
    LanguageOption(AppLanguage.HINDI, "अ", "हिंदी", UiStrings.HINDI_OPTION),
    LanguageOption(AppLanguage.HINGLISH, "अA", "Hinglish", UiStrings.HINGLISH_OPTION),
    LanguageOption(AppLanguage.ENGLISH, "A", "English", UiStrings.ENGLISH_OPTION),
)

/** Says the option's sentence in that option's own language, so a child who can't read still knows what it means. */
fun Speaker.speakLanguageOption(language: AppLanguage) = when (language) {
    AppLanguage.HINDI -> speak(UiStrings.HINDI_OPTION, Speaker.HINDI)
    AppLanguage.HINGLISH -> speak(UiStrings.HINGLISH_OPTION_SPOKEN, Speaker.HINDI)
    AppLanguage.ENGLISH -> speak(UiStrings.ENGLISH_OPTION, Speaker.ENGLISH)
}

@Composable
fun LanguageOptions(
    selected: AppLanguage?,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        options.forEachIndexed { index, option ->
            LanguageCard(
                option = option,
                isSelected = option.language == selected,
                badgeColor = LevelColors[index],
                onClick = { onSelect(option.language) },
            )
        }
    }
}

@Composable
private fun LanguageCard(
    option: LanguageOption,
    isSelected: Boolean,
    badgeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (isSelected) 3.dp else 2.dp, if (isSelected) colors.primary else colors.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(60.dp)
                    .background(badgeColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(option.badge, style = MaterialTheme.typography.headlineSmall)
            }
            Column(Modifier.weight(1f)) {
                Text(option.title, style = MaterialTheme.typography.headlineSmall)
                Text(option.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            if (isSelected) Text("✅", style = MaterialTheme.typography.titleLarge)
        }
    }
}
