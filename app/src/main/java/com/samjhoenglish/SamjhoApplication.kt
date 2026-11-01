package com.samjhoenglish

import android.app.Application
import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.data.SettingsRepository
import com.samjhoenglish.data.StudyTimer
import com.samjhoenglish.data.db.SamjhoDatabase
import com.samjhoenglish.speech.PhonicsPlayer
import com.samjhoenglish.speech.SoundFx
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.speech.VoiceListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SamjhoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** App-wide objects, created once. */
class AppContainer(app: Application) {
    /** For work that must finish even if the screen that started it closes (e.g. saving study time). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsRepository(app)
    val content = ContentRepository(app.assets)
    private val database = SamjhoDatabase.create(app)
    val progress = ProgressRepository(database.progress(), content)
    val studyTimer = StudyTimer(progress, appScope)

    val speaker = Speaker(app)
    val sounds = SoundFx()
    val phonics = PhonicsPlayer(app.assets)
    val listener = VoiceListener(app)

    /** Silences everything, e.g. when the app goes to the background. */
    fun stopAudio() {
        speaker.stop()
        phonics.stop()
        listener.cancel()
    }
}
