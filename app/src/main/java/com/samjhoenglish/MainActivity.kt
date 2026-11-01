package com.samjhoenglish

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.samjhoenglish.ui.SamjhoApp
import com.samjhoenglish.ui.theme.SamjhoTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as SamjhoApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            SamjhoTheme {
                SamjhoApp(container)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.studyTimer.resume()
    }

    override fun onStop() {
        super.onStop()
        container.stopAudio()
        container.studyTimer.pause()
    }
}
