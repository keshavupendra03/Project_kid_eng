package com.samjhoenglish.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.LocalAppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** The accent used whenever the app speaks English. */
enum class EnglishVoice(val locale: Locale) {
    /** Default: its clear ‘r’ keeps "arm" from sounding like "आम" (the Indian/British voices drop the r). */
    AMERICAN(Locale.forLanguageTag("en-US")),
    INDIAN(Locale.forLanguageTag("en-IN")),
    BRITISH(Locale.forLanguageTag("en-GB")),
}

/** Speaks text using the phone's free built-in text-to-speech. */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var pending: Request? = null
    private var onDone: (() -> Unit)? = null

    /** Which accent [ENGLISH] text is spoken in. Set from the parent's setting. */
    var englishVoice: EnglishVoice = EnglishVoice.AMERICAN

    /** The best installed voice per locale, picked once. */
    private val bestVoices = mutableMapOf<Locale, Voice?>()

    private val missing = MutableStateFlow<Set<Locale>>(emptySet())

    /** Voices the phone doesn't have installed, so a parent can be asked to download them. */
    val missingVoices: StateFlow<Set<Locale>> = missing.asStateFlow()

    private class Request(val parts: List<Pair<String, Locale>>, val slow: Boolean, val onDone: (() -> Unit)?)

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == LAST) main.post { finished() }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = onDone(utteranceId)
            })
            missing.value = setOf(EnglishVoice.AMERICAN.locale, HINDI).filter { tts.isLanguageAvailable(it) < 0 }.toSet()
        } else {
            missing.value = setOf(EnglishVoice.AMERICAN.locale, HINDI)
        }
        pending?.let { request -> if (ready) speakNow(request) else request.onDone?.invoke() }
        pending = null
    }

    /**
     * Speaks UI text in the app language (Hindi and Hinglish both use the Devanagari text),
     * optionally followed by an English word in the English voice.
     */
    fun say(text: L, language: AppLanguage, thenEnglish: String? = null, onDone: (() -> Unit)? = null) {
        val first = if (language == AppLanguage.ENGLISH) text.en to ENGLISH else text.hi to HINDI
        speakAll(listOfNotNull(first, thenEnglish?.let { it to ENGLISH }), onDone = onDone)
    }

    fun speak(text: String, locale: Locale, slow: Boolean = false) = speakAll(listOf(text to locale), slow)

    /** Speaks several parts one after another, each in its own voice, then calls [onDone]. */
    fun speakAll(parts: List<Pair<String, Locale>>, slow: Boolean = false, onDone: (() -> Unit)? = null) {
        val clean = parts
            .map { (text, locale) -> text.replace(EMOJI, "").trim() to locale }
            .filter { it.first.isNotEmpty() }
        val request = Request(clean, slow, onDone)
        if (ready) speakNow(request) else pending = request
    }

    fun stop() {
        pending = null
        onDone = null
        if (ready) tts.stop()
    }

    private fun speakNow(request: Request) {
        onDone = request.onDone
        tts.setSpeechRate(if (request.slow) 0.55f else 0.9f)
        val playable = request.parts
            .map { (text, locale) -> text to if (locale == ENGLISH) englishVoice.locale else locale }
            // Phones without this voice skip it rather than crash; Home asks parents to install it.
            .filter { (_, locale) -> tts.isLanguageAvailable(locale) >= 0 }
        if (playable.isEmpty()) {
            tts.stop()
            finished()
            return
        }
        playable.forEachIndexed { i, (text, locale) ->
            useVoice(locale)
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(text, mode, null, if (i == playable.lastIndex) LAST else "part$i")
        }
    }

    /** Uses the highest-quality offline voice for [locale] (some phones default to a robotic one). */
    private fun useVoice(locale: Locale) {
        val voice = bestVoices.getOrPut(locale) {
            runCatching { tts.voices }.getOrNull().orEmpty()
                .filter { v ->
                    v.locale.language == locale.language && v.locale.country == locale.country &&
                        !v.isNetworkConnectionRequired &&
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in v.features
                }
                .maxWithOrNull(compareBy<Voice> { it.quality }.thenByDescending { it.latency })
        }
        if (voice == null || tts.setVoice(voice) != TextToSpeech.SUCCESS) tts.setLanguage(locale)
    }

    private fun finished() {
        val callback = onDone
        onDone = null
        callback?.invoke()
    }

    companion object {
        /** "Speak this in English" — the accent comes from [englishVoice]. */
        val ENGLISH: Locale = Locale.forLanguageTag("en")
        val HINDI: Locale = Locale.forLanguageTag("hi-IN")
        private const val LAST = "samjho-last"
        private val EMOJI = Regex("[\\p{So}\\x{FE0F}\\x{200D}]")
    }
}

val LocalSpeaker = staticCompositionLocalOf<Speaker> { error("Speaker not provided") }

/** Returns a function that speaks UI text in the current app language. */
@Composable
fun rememberSay(): (L) -> Unit {
    val speaker = LocalSpeaker.current
    val language = LocalAppLanguage.current
    return remember(speaker, language) { { text -> speaker.say(text, language) } }
}
