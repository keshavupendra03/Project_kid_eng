package com.samjhoenglish.kidmode

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samjhoenglish.MainActivity
import com.samjhoenglish.SamjhoApplication
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.TinkuMood
import com.samjhoenglish.ui.components.TinkuSays
import com.samjhoenglish.ui.theme.SamjhoTheme

/** Covers a locked app. Every way out leads back to learning, never back to the locked app. */
class LockedAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SamjhoApplication).container
        setContent {
            val saved by container.settings.appLanguage.collectAsState(initial = null)
            val language = saved ?: AppLanguage.HINGLISH
            SamjhoTheme {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalAppLanguage provides language, LocalSpeaker provides container.speaker) {
                        BackHandler(onBack = ::goLearn)
                        LaunchedEffect(language) { container.speaker.say(UiStrings.appLockedSay, language) }
                        Column(
                            Modifier
                                .fillMaxSize()
                                .safeDrawingPadding()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TinkuSays(
                                message = UiStrings.appLockedSay.text(),
                                onListen = { container.speaker.say(UiStrings.appLockedSay, language) },
                                mood = TinkuMood.HAPPY,
                            )
                            Spacer(Modifier.height(24.dp))
                            BigButton(text = UiStrings.letsLearn.text(), emoji = "🚀", onClick = ::goLearn)
                        }
                    }
                }
            }
        }
    }

    private fun goLearn() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"

        fun intent(context: Context, lockedPackage: String): Intent =
            Intent(context, LockedAppActivity::class.java)
                .putExtra(EXTRA_PACKAGE, lockedPackage)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
