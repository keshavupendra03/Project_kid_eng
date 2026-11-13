package com.samjhoenglish

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.samjhoenglish.data.TestCheckWorker
import com.samjhoenglish.ui.SamjhoApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container get() = (application as SamjhoApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            SamjhoApp(container)
        }
        // Looks for new school tests about once an hour (does nothing without a school connection).
        TestCheckWorker.schedule(this)
    }

    override fun onStart() {
        super.onStart()
        container.studyTimer.resume()
        syncSchool()
    }

    override fun onStop() {
        super.onStop()
        container.stopAudio()
        container.studyTimer.pause()
        syncSchool()
    }

    /** Does nothing for children not connected to a school. Runs after study time has been saved. */
    private fun syncSchool() {
        container.appScope.launch {
            delay(1_500)
            // A failed send must never disturb a child's lesson. It is retried next time.
            runCatching { container.school.syncAll() }
            // The app is open, so new tests show on Home; no notification needed.
            runCatching { container.school.checkTests() }
        }
    }
}
