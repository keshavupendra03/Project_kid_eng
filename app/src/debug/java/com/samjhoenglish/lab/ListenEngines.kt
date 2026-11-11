package com.samjhoenglish.lab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.samjhoenglish.speech.MicLoop
import com.samjhoenglish.speech.READING_SAMPLE_RATE
import com.samjhoenglish.speech.ReadAlongHints
import com.samjhoenglish.speech.ReadingStatus
import com.samjhoenglish.speech.SpeechChecker
import com.samjhoenglish.speech.ZipformerReader
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A listener that reports what it hears, utterance by utterance, while the child reads. */
interface ListenEngine {
    val name: String

    /** [onText] gets the listener's guesses for the current utterance (best first) and whether it is final. */
    fun start(paragraph: String, onText: (guesses: List<String>, final: Boolean) -> Unit, onStatus: (String) -> Unit)
    fun stop()

    /** The recording of the last run, kept in the app's private storage on this phone. */
    val lastFile: File? get() = null
}

const val LOG_TAG = "ReadAlongLab"

fun newWavFile(context: Context, prefix: String): File {
    val dir = File(context.filesDir, "lab").apply { mkdirs() }
    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return File(dir, "$prefix-$stamp.wav")
}

/** The app's real read-along listener, recording each run for testing on the laptop. */
class ZipformerEngine(private val context: Context) : ListenEngine {
    override val name = "Zipformer (biased, offline)"
    val reader = ZipformerReader(context)
    override var lastFile: File? = null
        private set

    override fun start(paragraph: String, onText: (List<String>, Boolean) -> Unit, onStatus: (String) -> Unit) {
        val file = newWavFile(context, "zip")
        lastFile = file
        reader.start(
            paragraph, file,
            onText = { guesses, final ->
                if (final) Log.i(LOG_TAG, "zipformer final: $guesses")
                onText(guesses, final)
            },
            onStatus = { status ->
                onStatus(
                    when (status) {
                        ReadingStatus.Loading -> "Loading the Zipformer model (first time takes a few seconds)…"
                        ReadingStatus.Listening -> "Listening (Zipformer, bias ${ZipformerReader.BIAS}, slips it can hear: ${ReadAlongHints.slipWords(paragraph).joinToString().ifEmpty { "none" }})… read now"
                        is ReadingStatus.Failed -> "Couldn't start the Zipformer listener: ${status.reason}"
                    },
                )
            },
        )
    }

    override fun stop() = reader.stop()
    fun release() = reader.release()
}

/**
 * Android's built-in recogniser in Indian English. It stops after each pause, so it is
 * restarted straight away; a word said during the restart can be missed.
 */
class GoogleEngine(private val context: Context) : ListenEngine {
    override val name = "Google (en-IN)"
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private var onText: (List<String>, Boolean) -> Unit = { _, _ -> }
    private var onStatus: (String) -> Unit = {}

    override fun start(paragraph: String, onText: (List<String>, Boolean) -> Unit, onStatus: (String) -> Unit) {
        this.onText = onText
        this.onStatus = onStatus
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onStatus("This phone has no speech recogniser.")
            return
        }
        running = true
        listenOnce()
    }

    private fun listenOnce() {
        recognizer?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onStatus("Listening (Google)… read now")
            override fun onPartialResults(partialResults: Bundle?) {
                guesses(partialResults).takeIf { it.isNotEmpty() }?.let { onText(it, false) }
            }
            override fun onResults(results: Bundle?) {
                val all = guesses(results)
                Log.i(LOG_TAG, "google final: $all")
                if (all.isNotEmpty()) onText(all, true)
                restart(250)
            }
            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_CLIENT -> restart(250)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> restart(600)
                    else -> {
                        onStatus("Google stopped (error $error).")
                        stop()
                    }
                }
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        }
        r.startListening(intent)
    }

    private fun guesses(bundle: Bundle?): List<String> =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().filter { it.isNotBlank() }

    private fun restart(delayMs: Long) {
        if (!running) return
        main.postDelayed({ if (running) listenOnce() }, delayMs)
    }

    override fun stop() {
        running = false
        main.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }
}

/** How Vosk is told what to expect. */
enum class VoskGrammar(val label: String) {
    WORDS("Loose words"),
    SENTENCES("Whole sentences"),
}

/** Vosk with its small Indian English model, offline, restricted to the paragraph's own words. Kept for comparison. */
class VoskEngine(private val context: Context) : ListenEngine {
    override val name = "Vosk (Indian English, offline)"
    var grammar = VoskGrammar.SENTENCES
    override var lastFile: File? = null
        private set

    private val main = Handler(Looper.getMainLooper())
    private val mic = MicLoop()
    private var model: Model? = null

    override fun start(paragraph: String, onText: (List<String>, Boolean) -> Unit, onStatus: (String) -> Unit) {
        val loaded = model
        if (loaded != null) return listen(loaded, paragraph, onText, onStatus)
        onStatus("Loading the offline model (first time takes a few seconds)…")
        StorageService.unpack(
            context, "model-en-in", "model",
            { m ->
                model = m
                listen(m, paragraph, onText, onStatus)
            },
            { e -> onStatus("Couldn't load the model: ${e.message}") },
        )
    }

    private fun listen(model: Model, paragraph: String, onText: (List<String>, Boolean) -> Unit, onStatus: (String) -> Unit) {
        val knows = { word: String -> knows(model, word) }
        val rec = Recognizer(model, READING_SAMPLE_RATE.toFloat(), grammarFor(paragraph, grammar, knows))
        rec.setMaxAlternatives(ALTERNATIVES)
        val file = newWavFile(context, "vosk")
        lastFile = file
        val error = mic.start(
            recordTo = file,
            onChunk = { buffer, n ->
                if (rec.acceptWaveForm(buffer, n)) {
                    val guesses = parseFinal(rec.result)
                    Log.i(LOG_TAG, "vosk final: $guesses")
                    main.post { onText(guesses, true) }
                } else {
                    val partial = clean(JSONObject(rec.partialResult).optString("partial"))
                    if (partial.isNotEmpty()) main.post { onText(listOf(partial), false) }
                }
            },
            onDone = {
                val last = parseFinal(rec.finalResult)
                if (last.any { it.isNotEmpty() }) main.post { onText(last, true) }
                rec.close()
            },
        )
        if (error != null) {
            rec.close()
            onStatus("Couldn't open the microphone.")
            return
        }
        val slipNote = if (grammar == VoskGrammar.SENTENCES) ", slips it can hear: ${ReadAlongHints.slipWords(paragraph, knows).joinToString().ifEmpty { "none" }}" else ""
        onStatus("Listening (Vosk, ${grammar.label.lowercase()}$slipNote)… read now")
    }

    private fun parseFinal(json: String): List<String> {
        val obj = JSONObject(json)
        val alternatives = obj.optJSONArray("alternatives")
        val texts = if (alternatives != null) {
            (0 until alternatives.length()).map { clean(alternatives.getJSONObject(it).optString("text")) }
        } else {
            listOf(clean(obj.optString("text")))
        }
        return texts.filter { it.isNotEmpty() }.distinct()
    }

    private fun clean(text: String) = text.replace("[unk]", " ").replace(Regex("\\s+"), " ").trim()

    override fun stop() = mic.stop()

    fun release() {
        stop()
        model?.close()
        model = null
    }

    companion object {
        private const val ALTERNATIVES = 3

        /** Whether the model's dictionary has [word]; the Android binding doesn't expose this, so it is called directly. */
        private fun knows(model: Model, word: String): Boolean =
            runCatching { VoskWords.lib.vosk_model_find_word(model.pointer, word) >= 0 }.getOrDefault(false)

        /**
         * WORDS: any paragraph word in any order. SENTENCES: each sentence (and its slip versions),
         * and the rest of it from any word, because a child may restart mid-sentence.
         */
        fun grammarFor(paragraph: String, mode: VoskGrammar, knows: (String) -> Boolean = { true }): String {
            val phrases = when (mode) {
                VoskGrammar.WORDS -> SpeechChecker.normalize(paragraph).split(" ").filter { it.isNotEmpty() }.distinct()
                // Single words are left out: on test recordings they lowered the words caught (112 → 105 of 129).
                VoskGrammar.SENTENCES -> ReadAlongHints.sentenceVersions(paragraph, knows)
                    .flatMap { s -> s.indices.map { i -> s.drop(i).joinToString(" ") } }.distinct()
            }
            return JSONArray((phrases + "[unk]").distinct()).toString()
        }
    }
}

/** The one Vosk function the Android binding leaves out. */
private interface VoskWords : com.sun.jna.Library {
    fun vosk_model_find_word(model: com.sun.jna.Pointer, word: String): Int

    companion object {
        val lib: VoskWords by lazy { com.sun.jna.Native.load("vosk", VoskWords::class.java) }
    }
}
