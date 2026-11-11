package com.samjhoenglish.lab

import androidx.compose.runtime.Composable

/** The read-along test screen exists only in test (debug) builds. */
object Lab {
    const val AVAILABLE = false
}

@Composable
fun ReadAlongLab(onBack: () -> Unit) = Unit
