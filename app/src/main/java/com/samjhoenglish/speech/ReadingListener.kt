package com.samjhoenglish.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.io.File
import java.io.RandomAccessFile

/** How the read-along listener is doing. */
sealed interface ReadingStatus {
    data object Loading : ReadingStatus
    data object Listening : ReadingStatus
    data class Failed(val reason: String) : ReadingStatus
}

/**
 * Listens while a child reads a known paragraph aloud and reports what it hears, utterance by
 * utterance: partial text while the child speaks, then the final text for that stretch.
 */
interface ReadingListener {
    /** [recordTo] keeps the audio as a .wav (test screen only); null for lessons. */
    fun start(paragraph: String, recordTo: File?, onText: (guesses: List<String>, final: Boolean) -> Unit, onStatus: (ReadingStatus) -> Unit)
    fun stop()

    /** While true, incoming sound is ignored, so Tinku's own voice isn't heard as the child's reading. */
    var muted: Boolean

    /** Loads the model ahead of time, so the first reading doesn't wait. */
    fun warmUp() = Unit
}

const val READING_SAMPLE_RATE = 16000

/**
 * Zipformer (sherpa-onnx), offline, listening openly but nudged towards the paragraph's
 * sentences and their Indian-slip versions. On test recordings it caught the most words,
 * never marked a misread word as read, and heard most slips. Model files are in assets.
 */
class ZipformerReader(private val context: Context) : ReadingListener {
    override var muted = false
    private val main = Handler(Looper.getMainLooper())
    private val mic = MicLoop()
    private var recognizer: OnlineRecognizer? = null
    private var loading = false
    private val waiting = mutableListOf<(OnlineRecognizer?) -> Unit>()

    override fun warmUp() = load { }

    private fun load(then: (OnlineRecognizer?) -> Unit) {
        recognizer?.let { return then(it) }
        waiting += then
        if (loading) return
        loading = true
        Thread {
            val result = runCatching {
                OnlineRecognizer(
                    assetManager = context.assets,
                    config = OnlineRecognizerConfig(
                        featConfig = FeatureConfig(sampleRate = READING_SAMPLE_RATE, featureDim = 80),
                        modelConfig = OnlineModelConfig(
                            transducer = OnlineTransducerModelConfig(
                                encoder = "$MODEL_DIR/encoder-epoch-99-avg-1.int8.onnx",
                                decoder = "$MODEL_DIR/decoder-epoch-99-avg-1.int8.onnx",
                                joiner = "$MODEL_DIR/joiner-epoch-99-avg-1.int8.onnx",
                            ),
                            tokens = "$MODEL_DIR/tokens.txt",
                            numThreads = 2,
                            modelType = "zipformer",
                            modelingUnit = "bpe",
                            bpeVocab = "$MODEL_DIR/bpe.vocab",
                        ),
                        enableEndpoint = true,
                        decodingMethod = "modified_beam_search",
                        maxActivePaths = 4,
                        hotwordsScore = BIAS,
                    ),
                )
            }.onFailure { Log.e(TAG, "model load failed", it) }.getOrNull()
            main.post {
                recognizer = result
                loading = false
                val callbacks = waiting.toList()
                waiting.clear()
                callbacks.forEach { it(result) }
            }
        }.start()
    }

    override fun start(paragraph: String, recordTo: File?, onText: (List<String>, Boolean) -> Unit, onStatus: (ReadingStatus) -> Unit) {
        if (recognizer == null) onStatus(ReadingStatus.Loading)
        load { loaded ->
            if (loaded == null) {
                onStatus(ReadingStatus.Failed("model"))
                return@load
            }
            listen(loaded, paragraph, recordTo, onText, onStatus)
        }
    }

    private fun listen(recognizer: OnlineRecognizer, paragraph: String, recordTo: File?, onText: (List<String>, Boolean) -> Unit, onStatus: (ReadingStatus) -> Unit) {
        val hotwords = ReadAlongHints.sentenceVersions(paragraph).joinToString("\n") { it.joinToString(" ").uppercase() }
        val stream = recognizer.createStream(hotwords)
        val floats = FloatArray(MicLoop.CHUNK)
        var lastPartial = ""
        var chunks = 0
        var decodeNanos = 0L
        val error = mic.start(
            recordTo = recordTo,
            onChunk = { buffer, n ->
                if (muted || n > floats.size) return@start
                for (i in 0 until n) floats[i] = buffer[i] / 32768f
                val began = System.nanoTime()
                stream.acceptWaveform(if (n == floats.size) floats else floats.copyOf(n), READING_SAMPLE_RATE)
                while (recognizer.isReady(stream)) recognizer.decode(stream)
                decodeNanos += System.nanoTime() - began
                if (++chunks % 100 == 0) Log.i(TAG, "speed: ${decodeNanos / chunks / 1_000_000} ms per 0.1 s of audio")
                val text = recognizer.getResult(stream).text.lowercase().trim()
                if (recognizer.isEndpoint(stream)) {
                    if (text.isNotEmpty()) main.post { onText(listOf(text), true) }
                    recognizer.reset(stream)
                    lastPartial = ""
                } else if (text.isNotEmpty() && text != lastPartial) {
                    lastPartial = text
                    main.post { onText(listOf(text), false) }
                }
            },
            onDone = {
                stream.inputFinished()
                while (recognizer.isReady(stream)) recognizer.decode(stream)
                val text = recognizer.getResult(stream).text.lowercase().trim()
                if (text.isNotEmpty()) main.post { onText(listOf(text), true) }
                stream.release()
            },
        )
        if (error != null) {
            stream.release()
            onStatus(ReadingStatus.Failed(error))
        } else {
            onStatus(ReadingStatus.Listening)
        }
    }

    override fun stop() = mic.stop()

    fun release() {
        stop()
        recognizer?.release()
        recognizer = null
    }

    companion object {
        private const val TAG = "ReadingListener"
        private const val MODEL_DIR = "zipformer-en-20m"

        /** How strongly the paragraph's sentences are favoured. 5 caught the most words on test recordings without hiding misreadings. */
        const val BIAS = 5f
    }
}

/**
 * Our own microphone loop: 16 kHz mono chunks of 0.1 s handed to [onChunk] on a background
 * thread, optionally saved as a .wav.
 */
class MicLoop {
    @Volatile private var running = false
    private var thread: Thread? = null

    /** Returns an error message, or null when the microphone is open. */
    @SuppressLint("MissingPermission") // Callers ask for the microphone before starting.
    fun start(recordTo: File?, onChunk: (ShortArray, Int) -> Unit, onDone: () -> Unit): String? {
        val minBuffer = AudioRecord.getMinBufferSize(READING_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, READING_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuffer, CHUNK * 4),
            )
        }.getOrNull()
        if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder?.release()
            return "microphone"
        }
        running = true
        thread = Thread {
            val buffer = ShortArray(CHUNK)
            val out = recordTo?.let { WavWriter(it) }
            recorder.startRecording()
            try {
                while (running) {
                    val n = recorder.read(buffer, 0, buffer.size)
                    if (n <= 0) continue
                    out?.write(buffer, n)
                    onChunk(buffer, n)
                }
                onDone()
            } finally {
                recorder.stop()
                recorder.release()
                out?.close()
            }
        }.also { it.start() }
        return null
    }

    fun stop() {
        running = false
        thread?.join(3000)
        thread = null
    }

    companion object {
        const val CHUNK = 1600 // 0.1 s
    }
}

/** Writes 16 kHz mono 16-bit audio as a .wav file, fixing the header sizes when closed. */
class WavWriter(file: File) {
    private val raf = RandomAccessFile(file, "rw")
    private var dataBytes = 0

    init {
        raf.setLength(0)
        raf.write(ByteArray(44))
    }

    fun write(samples: ShortArray, count: Int) {
        val bytes = ByteArray(count * 2)
        for (i in 0 until count) {
            bytes[i * 2] = (samples[i].toInt() and 0xff).toByte()
            bytes[i * 2 + 1] = (samples[i].toInt() shr 8 and 0xff).toByte()
        }
        raf.write(bytes)
        dataBytes += bytes.size
    }

    fun close() {
        raf.seek(0)
        raf.writeBytes("RIFF")
        raf.writeIntLE(36 + dataBytes)
        raf.writeBytes("WAVEfmt ")
        raf.writeIntLE(16)
        raf.writeShortLE(1)
        raf.writeShortLE(1)
        raf.writeIntLE(READING_SAMPLE_RATE)
        raf.writeIntLE(READING_SAMPLE_RATE * 2)
        raf.writeShortLE(2)
        raf.writeShortLE(16)
        raf.writeBytes("data")
        raf.writeIntLE(dataBytes)
        raf.close()
    }

    private fun RandomAccessFile.writeIntLE(v: Int) = write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
    private fun RandomAccessFile.writeShortLE(v: Int) = write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
}

val LocalReadingListener = staticCompositionLocalOf<ReadingListener> { error("ReadingListener not provided") }
