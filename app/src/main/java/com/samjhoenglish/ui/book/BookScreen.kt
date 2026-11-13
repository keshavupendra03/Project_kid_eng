package com.samjhoenglish.ui.book

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samjhoenglish.content.BookChapter
import com.samjhoenglish.content.BookSection
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.rememberSay
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.components.TinkuSays
import com.samjhoenglish.ui.home.JourneyUiState
import com.samjhoenglish.ui.theme.Leaf
import com.samjhoenglish.ui.theme.Sunny

/** The child's English textbook: one card per chapter, with how many of its sections are done. */
@Composable
fun BookScreen(journey: JourneyUiState, onBack: () -> Unit, onOpenChapter: (String) -> Unit) {
    val say = rememberSay()
    LaunchedEffect(Unit) { say(UiStrings.bookPick) }
    val book = journey.myBook

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Header("📕 ${UiStrings.myBook.text()}", onBack)
        Spacer(Modifier.height(12.dp))
        TinkuSays(message = UiStrings.bookPick.text(), onListen = { say(UiStrings.bookPick) }, tinkuSize = 80.dp)
        Spacer(Modifier.height(20.dp))
        book?.chapters?.forEach { chapter ->
            val done = chapter.sections.count { (journey.stars[it.id] ?: 0) > 0 }
            Surface(
                onClick = { onOpenChapter(chapter.id) },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(chapter.emoji, style = MaterialTheme.typography.headlineLarge)
                    Column(Modifier.weight(1f)) {
                        Text("${UiStrings.lessonNumber.text()} ${chapter.number}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(chapter.title, style = MaterialTheme.typography.titleLarge)
                        Text("${UiStrings.bookPages.text()} ${chapter.pages}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("$done/${chapter.sections.size}", style = MaterialTheme.typography.titleMedium, color = if (done == chapter.sections.size) Leaf else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(UiStrings.moreChaptersSoon.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One chapter: read it in the book, then its exercise sections in the book's own order, then the test. */
@Composable
fun ChapterScreen(journey: JourneyUiState, chapterId: String, onBack: () -> Unit, onOpenSection: (String) -> Unit) {
    val chapter: BookChapter = journey.myBook?.chapters?.firstOrNull { it.id == chapterId } ?: return
    val say = rememberSay()
    val next = chapter.sections.firstOrNull { (journey.stars[it.id] ?: 0) == 0 }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Header("${chapter.emoji} ${chapter.title}", onBack)
        Spacer(Modifier.height(8.dp))
        Text(
            "${UiStrings.lessonNumber.text()} ${chapter.number} · ${UiStrings.bookPages.text()} ${chapter.pages}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        TinkuSays(message = UiStrings.bookPick.text(), onListen = { say(UiStrings.bookPick) }, tinkuSize = 70.dp)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chapter.sections.forEachIndexed { index, section ->
                SectionCard(section, number = index + 1, stars = journey.stars[section.id] ?: 0, isNext = section == next, onClick = { onOpenSection(section.id) })
            }
        }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onBack)
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun SectionCard(section: BookSection, number: Int, stars: Int, isNext: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isNext) colors.primaryContainer else colors.surface,
        border = BorderStroke(2.dp, if (isNext) colors.primary else colors.outline),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(section.emoji, style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(50), color = if (section.book) Sunny.copy(alpha = 0.45f) else colors.primaryContainer) {
                    Text(
                        when {
                            section.test -> UiStrings.chapterTest.text()
                            section.book -> UiStrings.fromBook.text()
                            else -> UiStrings.extraPractice.text()
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    )
                }
                Text("$number. ${section.title.text()}", style = MaterialTheme.typography.titleMedium)
                if (stars > 0) Text("⭐".repeat(stars) + "☆".repeat(3 - stars), style = MaterialTheme.typography.bodyLarge)
            }
            Box(Modifier.size(44.dp).background(if (stars > 0) Leaf else colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text(if (stars > 0) "✓" else "▶", style = MaterialTheme.typography.titleMedium, color = colors.onPrimary)
            }
        }
    }
}
