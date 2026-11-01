package com.samjhoenglish.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.Serializable

/** The language used for buttons, instructions and explanations. The English being taught never changes. */
enum class AppLanguage { HINDI, HINGLISH, ENGLISH }

/**
 * One piece of text in all three app languages.
 *
 * [hi] is everyday spoken Hindi in Devanagari. It is also what the Hindi voice reads aloud
 * in both HINDI and HINGLISH modes, because the voice can't pronounce Roman-script Hindi well.
 */
@Serializable
data class L(val hi: String, val hinglish: String, val en: String) {
    fun get(language: AppLanguage): String = when (language) {
        AppLanguage.HINDI -> hi
        AppLanguage.HINGLISH -> hinglish
        AppLanguage.ENGLISH -> en
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.HINGLISH }

/** The text in the language the user picked. */
@Composable
@ReadOnlyComposable
fun L.text(): String = get(LocalAppLanguage.current)
