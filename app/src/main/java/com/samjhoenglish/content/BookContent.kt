package com.samjhoenglish.content

import com.samjhoenglish.i18n.L
import kotlinx.serialization.Serializable

/**
 * assets/content/book.json — the school textbook (MP Board English, Classes 6–8), chapter by chapter.
 * Only the exercises are in the app; children read the lesson itself in their own book.
 */
@Serializable
data class BookFile(val classes: List<BookClass>)

@Serializable
data class BookClass(val grade: Int, val chapters: List<BookChapter>)

@Serializable
data class BookChapter(
    /** B7.04 = Class 7, chapter 4. */
    val id: String,
    val number: Int,
    val emoji: String,
    /** The chapter's title as printed in the book. */
    val title: String,
    /** Where the lesson is in the book, e.g. "18–25". */
    val pages: String,
    val sections: List<BookSection>,
)

/** One part of a chapter's exercises, played as a lesson (B7.04.1 …, the test is B7.04.T). */
@Serializable
data class BookSection(
    val id: String,
    val emoji: String,
    val title: L,
    /** True when the section is the book's own exercise; false for our extra practice. */
    val book: Boolean,
    val test: Boolean = false,
)

/** Textbook chapter sections: B<class>.<chapter>.<section>. */
val String.isBookLesson: Boolean get() = startsWith("B") && count { it == '.' } == 2

/** The chapter a section belongs to: B7.04.3 → B7.04. */
val String.bookChapterId: String get() = substringBeforeLast('.')

/** The grid squares this word fills, in order. */
val CrosswordEntry.cells: List<Pair<Int, Int>>
    get() = answer.indices.map { i -> if (across) row to col + i else row + i to col }

/** The section after [sectionId] in its chapter, or null after the last one. */
fun BookFile.nextSection(sectionId: String): String? {
    val sections = classes.flatMap { it.chapters }.firstOrNull { it.id == sectionId.bookChapterId }?.sections ?: return null
    val at = sections.indexOfFirst { it.id == sectionId }
    return sections.getOrNull(at + 1)?.id
}
