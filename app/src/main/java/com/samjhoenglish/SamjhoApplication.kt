package com.samjhoenglish

import android.app.Application
import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.data.ParentRepository
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.data.SchoolSync
import com.samjhoenglish.data.SettingsRepository
import com.samjhoenglish.data.StudyTimer
import com.samjhoenglish.data.db.SamjhoDatabase
import com.samjhoenglish.kidmode.KidMode
import com.samjhoenglish.speech.PhonicsPlayer
import com.samjhoenglish.speech.SoundFx
import com.samjhoenglish.speech.Speaker
import com.samjhoenglish.speech.VoiceListener
import com.samjhoenglish.speech.ZipformerReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

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
    val parent = ParentRepository(database.progress(), content, settings, studyTimer)

    init {
        // The app no longer keeps any recording of a child's voice. Older versions saved weekly
        // Voice Diary clips here; remove them so they don't take up space on the phone.
        appScope.launch(Dispatchers.IO) { File(app.filesDir, "voice_diary").deleteRecursively() }
    }

    val kidMode = KidMode(app, settings)
    val school = SchoolSync(database.progress(), content)

    val speaker = Speaker(app)
    val sounds = SoundFx()
    val phonics = PhonicsPlayer(app.assets)
    val listener = VoiceListener(app)
    val reader = ZipformerReader(app)

    /** Silences everything, e.g. when the app goes to the background. */
    fun stopAudio() {
        speaker.stop()
        phonics.stop()
        listener.cancel()
        reader.stop()
    }
}
