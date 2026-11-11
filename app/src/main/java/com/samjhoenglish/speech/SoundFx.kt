package com.samjhoenglish.speech

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Short feedback chimes, synthesised in code so the app needs no sound files.
 * The "try again" sound is deliberately soft — mistakes should never feel like punishment.
 */
class SoundFx {
    private val correct = chime(listOf(1046.5 to 0.08, 1318.5 to 0.08, 1568.0 to 0.22), volume = 0.45)
    private val tryAgain = chime(listOf(523.3 to 0.10, 440.0 to 0.20), volume = 0.25)
    private val celebrate = chime(
        listOf(784.0 to 0.10, 988.0 to 0.10, 1174.7 to 0.10, 1568.0 to 0.12, 1318.5 to 0.10, 1568.0 to 0.35),
        volume = 0.45,
    )

    /** Parents can turn the chimes off in Settings. The mic's "speak now" ding always plays. */
    var chimesOn = true

    /** A short "ding": the mic is open, speak now. */
    private val micReady = chime(listOf(880.0 to 0.06, 1318.5 to 0.10), volume = 0.3)

    private val star = chime(listOf(1568.0 to 0.06, 2093.0 to 0.14), volume = 0.3)

    fun playMicReady() = play(micReady)
    fun playStar() = if (chimesOn) play(star) else Unit
    fun playCorrect() = if (chimesOn) play(correct) else Unit
    fun playTryAgain() = if (chimesOn) play(tryAgain) else Unit
    fun playCelebrate() = if (chimesOn) play(celebrate) else Unit

    private fun chime(notes: List<Pair<Double, Double>>, volume: Double): ShortArray {
        val samples = ArrayList<Short>()
        for ((freq, seconds) in notes) {
            val n = (SAMPLE_RATE * seconds).toInt()
            for (i in 0 until n) {
                val t = i.toDouble() / SAMPLE_RATE
                val attack = min(1.0, t / 0.005) // avoid clicks
                val decay = exp(-t * 6)
                // A little second harmonic makes it sound like a bell instead of a beep.
                val wave = sin(2 * PI * freq * t) + 0.3 * sin(4 * PI * freq * t)
                samples += (wave / 1.3 * attack * decay * volume * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return samples.toShortArray()
    }

    private fun play(data: ShortArray) {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(data.size * 2)
                .build()
            track.write(data, 0, data.size)
            track.play()
            val durationMs = data.size * 1000L / SAMPLE_RATE
            Handler(Looper.getMainLooper()).postDelayed({ track.release() }, durationMs + 200)
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22050
    }
}

val LocalSoundFx = staticCompositionLocalOf<SoundFx> { error("SoundFx not provided") }
