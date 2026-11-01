package com.samjhoenglish.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.staticCompositionLocalOf

sealed interface ListenResult {
    /** The recogniser's guesses, best first. */
    data class Heard(val guesses: List<String>) : ListenResult
    data object NothingHeard : ListenResult
    data object NoInternet : ListenResult
    data object NoPermission : ListenResult
    data object Unavailable : ListenResult
}

/**
 * Listens to the child once using the phone's free built-in speech recogniser (Indian English).
 *
 * Tuned for children: it signals [listen]'s onReady only when the mic is really open (so the first
 * sound isn't lost), waits longer for silence, keeps partial results (a half-heard word still
 * counts), and quietly retries once when the recogniser fails while starting up.
 */
class VoiceListener(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var session = 0

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    /** Must be called on the main thread. */
    fun listen(onReady: () -> Unit, onLevel: (Float) -> Unit, onResult: (ListenResult) -> Unit) {
        cancel()
        if (!isAvailable) {
            onResult(ListenResult.Unavailable)
            return
        }
        start(++session, retriesLeft = 1, onReady, onLevel, onResult)
    }

    /** The child has finished: stop recording now and get the result straight away. */
    fun finish() {
        recognizer?.stopListening()
    }

    fun cancel() {
        session++
        main.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        recognizer = null
    }

    private fun start(
        id: Int,
        retriesLeft: Int,
        onReady: () -> Unit,
        onLevel: (Float) -> Unit,
        onResult: (ListenResult) -> Unit,
    ) {
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        val startedAt = SystemClock.elapsedRealtime()
        var speechStarted = false
        var partial = ""
        var finished = false

        fun deliver(result: ListenResult) {
            if (finished || id != session) return
            finished = true
            recognizer?.destroy()
            recognizer = null
            onResult(result)
        }

        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (id == session) onReady()
            }

            override fun onBeginningOfSpeech() {
                speechStarted = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (id == session) onLevel(rmsdB)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull { it.isNotBlank() }
                    ?.let { partial = it }
            }

            override fun onResults(results: Bundle?) {
                val guesses = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    .filter { it.isNotBlank() }
                // Some phones give an empty final result even though the partial result had the word.
                val all = (guesses + listOf(partial)).filter { it.isNotBlank() }.distinct()
                deliver(if (all.isEmpty()) ListenResult.NothingHeard else ListenResult.Heard(all))
            }

            override fun onError(error: Int) {
                if (partial.isNotBlank()) return deliver(ListenResult.Heard(listOf(partial)))
                val startupGlitch = error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                    (error == SpeechRecognizer.ERROR_NO_MATCH && !speechStarted &&
                        SystemClock.elapsedRealtime() - startedAt < QUICK_FAIL_MS)
                if (startupGlitch && retriesLeft > 0 && id == session) {
                    // Recogniser wasn't ready yet (common right after the previous one closed): try again quietly.
                    finished = true
                    recognizer?.destroy()
                    recognizer = null
                    main.postDelayed({ if (id == session) start(id, retriesLeft - 1, onReady, onLevel, onResult) }, RETRY_DELAY_MS)
                    return
                }
                deliver(
                    when (error) {
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                        SpeechRecognizer.ERROR_SERVER -> ListenResult.NoInternet
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> ListenResult.NoPermission
                        else -> ListenResult.NothingHeard
                    },
                )
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // Children pause and speak slowly: don't cut them off after a short silence.
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_MS)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_MS)
                .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, MIN_LENGTH_MS),
        )
    }

    private companion object {
        const val SILENCE_MS = 1500L
        const val MIN_LENGTH_MS = 1000L
        const val QUICK_FAIL_MS = 1200L
        const val RETRY_DELAY_MS = 350L
    }
}

val LocalVoiceListener = staticCompositionLocalOf<VoiceListener> { error("VoiceListener not provided") }
