package com.samjhoenglish.content

import com.samjhoenglish.i18n.L
import kotlinx.serialization.Serializable

/** assets/content/express.json — Tinku Express: passages that scroll up while the child reads them aloud. */
@Serializable
data class ExpressFile(val levels: List<ExpressLevel>)

/**
 * One train. [wpm] is the scroll speed in words per minute. With [lives], a round fails when
 * more than half the sentences escape unread.
 */
@Serializable
data class ExpressLevel(
    val number: Int,
    val emoji: String,
    val title: L,
    val wpm: Int,
    val lives: Boolean = false,
    val passages: List<ExpressPassage>,
)

/** [golden] are the tricky words that glow gold and earn a bonus when read cleanly. */
@Serializable
data class ExpressPassage(
    val id: String,
    val emoji: String,
    val title: L,
    val text: String,
    val golden: List<String> = emptyList(),
)

/** Express rounds are stored with the lessons for stars (X01.P1 …), but aren't lessons. */
val String.isExpressRound: Boolean get() = startsWith("X")
