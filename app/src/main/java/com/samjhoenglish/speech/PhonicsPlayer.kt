package com.samjhoenglish.speech

import android.content.res.AssetManager
import android.media.MediaPlayer
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Plays the recorded phonics sounds in assets/audio/phonics (c13_s.m4a = the /s/ sound).
 * Text-to-speech can't say a bare sound like /s/, so these are human recordings.
 * Empty placeholder files are treated as "not recorded yet" and skipped.
 */
class PhonicsPlayer(private val assets: AssetManager) {
    private var player: MediaPlayer? = null

    /** Sound id (file name without extension) → asset path, for recorded sounds only. */
    private val recorded: Map<String, String> by lazy {
        assets.list(DIR).orEmpty()
            .map { "$DIR/$it" }
            .filter(::hasAudio)
            .associateBy { it.substringAfterLast('/').substringBeforeLast('.') }
    }

    fun isRecorded(sound: String): Boolean = sound in recorded

    /** Plays [sound], then calls [onDone]. If it isn't recorded yet, calls [onDone] straight away. */
    fun play(sound: String, onDone: () -> Unit = {}) {
        stop()
        val path = recorded[sound] ?: return onDone()
        runCatching {
            assets.openFd(path).use { fd ->
                MediaPlayer().apply {
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    setOnCompletionListener {
                        stop()
                        onDone()
                    }
                    prepare()
                    start()
                }
            }
        }.onSuccess { player = it }.onFailure { onDone() }
    }

    fun stop() {
        player?.release()
        player = null
    }

    private fun hasAudio(path: String): Boolean =
        runCatching { assets.openFd(path).use { it.length > MIN_BYTES } }.getOrDefault(false)

    private companion object {
        const val DIR = "audio/phonics"
        const val MIN_BYTES = 512L
    }
}

val LocalPhonics = staticCompositionLocalOf<PhonicsPlayer> { error("PhonicsPlayer not provided") }
