package com.samjhoenglish.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samjhoenglish.content.BookClass
import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.content.LessonRef
import com.samjhoenglish.content.LevelInfo
import com.samjhoenglish.content.Mission
import com.samjhoenglish.content.PowerWord
import com.samjhoenglish.content.WordPowerFile
import com.samjhoenglish.learning.WordPower
import com.samjhoenglish.content.lessonOrder
import com.samjhoenglish.data.ParentRepository
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.data.localDay
import com.samjhoenglish.learning.Learning
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A child's journey: which lessons are open, passed, how many stars, and what to do next. */
data class JourneyUiState(
    val loaded: Boolean = false,
    val levels: List<LevelInfo> = emptyList(),
    val unlocked: Set<String> = emptySet(),
    val passed: Set<String> = emptySet(),
    val stars: Map<String, Int> = emptyMap(),
    val nextLesson: LessonRef? = null,
    val dueCount: Int = 0,
    val streak: Int = 0,
    val mission: Mission? = null,
    /** The child has said the mission is done; it's waiting for a parent. */
    val missionPending: Boolean = false,
    val approvedMissions: Int = 0,
    /** The reading ladder: levels of passages, which are open, and the next one to read. */
    val readingLevels: List<LevelInfo> = emptyList(),
    val readingUnlocked: Set<String> = emptySet(),
    val nextPassage: LessonRef? = null,
    /** A word from the child's Word Power band, different every day. */
    val wordOfDay: PowerWord? = null,
    /** The child's own school textbook, if its chapters are in the app (Classes 6–8). */
    val myBook: BookClass? = null,
) {
    /** "R2 · 3/6": the level the child is reading at and how far through it they are. */
    val readingLevelNow: LevelInfo? get() = readingLevels.lastOrNull { level -> level.lessons.firstOrNull()?.id in readingUnlocked }

    /** Lesson stars, plus one star for every mission a parent has approved. */
    val totalStars: Int get() = stars.values.sum() + approvedMissions

    fun isOpen(level: LevelInfo): Boolean = level.lessons.firstOrNull()?.id in unlocked

    fun passedCount(level: LevelInfo): Int = level.lessons.count { it.id in passed }
}

class JourneyViewModel(
    private val childId: Long,
    content: ContentRepository,
    progress: ProgressRepository,
    private val parent: ParentRepository,
    private val classGrade: Int = 1,
) : ViewModel() {
    /** Words the child has learned in their own band; before any, the band's first pack. */
    private fun wordOfDay(file: WordPowerFile, status: Map<String, Learning.LessonStatus>): PowerWord? {
        val band = file.bands.getOrNull(WordPower.homeBand(classGrade, file.bands.map { it.minClass to it.maxClass })) ?: return null
        val learned = band.packs.filter { status[it.id]?.completed == true }.flatMap { it.words }
        return WordPower.wordOfDay(learned.ifEmpty { band.packs.first().words }, localDay(System.currentTimeMillis()))
    }

    private val lessons = combine(
        flow { emit(content.levels()) },
        flow { emit(content.readingLevels()) },
        progress.lessonStatus(childId),
        progress.dueCount(childId),
        progress.streak(childId),
    ) { levels, reading, status, due, streak ->
        val order = levels.lessonOrder
        val nextId = Learning.nextLesson(order, status)
        val readingOrder = reading.map { level -> level.lessons.map { it.id } }
        val nextPassageId = Learning.nextPassage(readingOrder, status)
        JourneyUiState(
            loaded = true,
            levels = levels,
            unlocked = Learning.unlockedLessons(order, status),
            passed = status.filterValues { Learning.isPassed(it) }.keys,
            stars = status.mapValues { it.value.bestStars },
            nextLesson = levels.flatMap { it.lessons }.firstOrNull { it.id == nextId },
            dueCount = due,
            streak = streak,
            readingLevels = reading,
            readingUnlocked = Learning.unlockedPassages(readingOrder, status),
            nextPassage = reading.flatMap { it.lessons }.firstOrNull { it.id == nextPassageId },
        )
    }

    val uiState: StateFlow<JourneyUiState> = combine(
        lessons,
        parent.missionStates(childId),
        flow { emit(content.missions()) },
        combine(flow { emit(content.wordPower()) }, progress.lessonStatus(childId), ::wordOfDay),
        flow { emit(content.book().classes.firstOrNull { it.grade == classGrade && it.chapters.isNotEmpty() }) },
    ) { state, missionStates, missions, word, book ->
        val approved = missionStates.filter { it.approved }.map { it.missionId }.toSet()
        val currentId = Learning.currentMission(missions.map { it.id to it.needLessons }, approved, state.passed.size)
        state.copy(
            myBook = book,
            wordOfDay = word,
            mission = missions.firstOrNull { it.id == currentId },
            missionPending = missionStates.any { it.missionId == currentId && !it.approved },
            approvedMissions = approved.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JourneyUiState())

    fun missionDone(missionId: String) {
        viewModelScope.launch { parent.markMissionDone(childId, missionId) }
    }

    companion object {
        fun factory(
            childId: Long,
            content: ContentRepository,
            progress: ProgressRepository,
            parent: ParentRepository,
            classGrade: Int = 1,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { JourneyViewModel(childId, content, progress, parent, classGrade) }
        }
    }
}
