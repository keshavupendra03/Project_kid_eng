package com.samjhoenglish.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.content.LevelInfo
import com.samjhoenglish.content.Mission
import com.samjhoenglish.content.WORD_MONTHLY_PREFIX
import com.samjhoenglish.content.isWordPower
import com.samjhoenglish.data.ParentRepository
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.data.db.DayMillis
import com.samjhoenglish.data.db.MissionState
import com.samjhoenglish.data.db.ReadingResult
import com.samjhoenglish.data.db.SkillState
import com.samjhoenglish.data.localDay
import com.samjhoenglish.learning.Learning
import com.samjhoenglish.learning.SkillCatalog
import com.samjhoenglish.learning.SkillInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayBar(val day: Long, val minutes: Int)

data class LevelRow(val level: LevelInfo, val passed: Int)

data class ParentUiState(
    val loaded: Boolean = false,
    val days: List<DayBar> = emptyList(),
    val streak: Int = 0,
    val levels: List<LevelRow> = emptyList(),
    val wordsLearned: Int = 0,
    val sentencesSpoken: Int = 0,
    val weak: List<SkillInfo> = emptyList(),
    /** Enough answers have been given for "where help is needed" to mean something. */
    val hasPractised: Boolean = false,
    val pendingMissions: List<Mission> = emptyList(),
    val approvedMissions: Int = 0,
    /** Reading: passages read so far, the level the child reads at, and how the last readings went. */
    val passagesRead: Int = 0,
    val readingLevel: Int? = null,
    val readingAccuracy: Int? = null,
    val wordsPerMinute: Int? = null,
    /** Fastest reading speed in Tinku Express. */
    val expressBest: Int? = null,
    /** Word Power: words learned and mastered, and the latest monthly test's stars. */
    val powerWordsLearned: Int = 0,
    val powerWordsMastered: Int = 0,
    val lastMonthlyStars: Int? = null,
) {
    val todayMinutes: Int get() = days.lastOrNull()?.minutes ?: 0
    val weekMinutes: Int get() = days.sumOf { it.minutes }
}

class ParentViewModel(
    private val childId: Long,
    content: ContentRepository,
    progress: ProgressRepository,
    private val parent: ParentRepository,
) : ViewModel() {
    private val today = localDay(System.currentTimeMillis())

    private data class Activity(
        val daily: List<DayMillis>,
        val streak: Int,
        val status: Map<String, Learning.LessonStatus>,
        val words: Int,
        val sentences: Int,
    )

    private data class Details(
        val skills: List<SkillState>,
        val missions: List<MissionState>,
        val levels: List<LevelInfo>,
        val allMissions: List<Mission>,
    )

    private val activity = combine(
        parent.dailyMillis(childId, today - 6),
        progress.streak(childId),
        progress.lessonStatus(childId),
        parent.wordsLearned(childId),
        parent.sentencesSpoken(childId),
        ::Activity,
    )

    private val details = combine(
        parent.skills(childId),
        parent.missionStates(childId),
        flow { emit(content.levels()) },
        flow { emit(content.missions()) },
        ::Details,
    )

    private data class Reading(val results: List<ReadingResult>, val passages: Int, val levels: List<LevelInfo>, val expressBest: Int?, val mastered: Int)

    private val reading = combine(
        progress.readingResults(childId),
        progress.passagesRead(childId),
        flow { emit(content.readingLevels()) },
        progress.expressBestWpm(childId),
        progress.wordsMastered(childId),
        ::Reading,
    )

    val uiState: StateFlow<ParentUiState> = combine(activity, details, reading) { a, d, r ->
        val pending = d.missions.filter { !it.approved }.map { it.missionId }.toSet()
        val readingOrder = r.levels.map { level -> level.lessons.map { it.id } }
        val open = Learning.unlockedPassages(readingOrder, a.status)
        val wordsRead = r.results.sumOf { it.wordsRead }
        val wordsTotal = r.results.sumOf { it.wordsTotal }
        val seconds = r.results.sumOf { it.seconds }
        ParentUiState(
            passagesRead = r.passages,
            readingLevel = r.levels.lastOrNull { level -> level.lessons.firstOrNull()?.id in open }?.number,
            readingAccuracy = if (wordsTotal > 0) (100 * wordsRead / wordsTotal) else null,
            wordsPerMinute = if (seconds > 0 && wordsRead > 0) (60 * wordsRead / seconds) else null,
            expressBest = r.expressBest,
            // A Word Power pack is ten words; learn lessons are W34.P01 …, tests end in T.
            powerWordsLearned = a.status.count { (id, s) -> id.isWordPower && id.contains(".P") && !id.endsWith("T") && s.completed } * 10,
            powerWordsMastered = r.mastered,
            lastMonthlyStars = a.status.filterKeys { it.startsWith(WORD_MONTHLY_PREFIX) }
                .maxByOrNull { it.key.removePrefix(WORD_MONTHLY_PREFIX).toIntOrNull() ?: 0 }?.value?.bestStars,
            loaded = true,
            days = Learning.lastDays(a.daily.associate { it.day to it.millis }, today).map { (day, minutes) -> DayBar(day, minutes) },
            streak = a.streak,
            levels = d.levels.filter { it.lessons.isNotEmpty() }.map { level ->
                LevelRow(level, level.lessons.count { Learning.isPassed(a.status[it.id]) })
            },
            wordsLearned = a.words,
            sentencesSpoken = a.sentences,
            weak = Learning.weakestSkills(
                d.skills.map { Learning.SkillScore(it.skill, it.mastery, it.attempts) },
                SkillCatalog.broad,
            ).mapNotNull(SkillCatalog::describe),
            hasPractised = d.skills.any { it.attempts >= 3 },
            pendingMissions = d.allMissions.filter { it.id in pending },
            approvedMissions = d.missions.count { it.approved },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ParentUiState())

    fun approveMission(id: String) {
        viewModelScope.launch { parent.approveMission(childId, id) }
    }

    fun returnMission(id: String) {
        viewModelScope.launch { parent.returnMission(childId, id) }
    }

    fun setDailyLimit(minutes: Int) {
        viewModelScope.launch { parent.setDailyLimit(childId, minutes) }
    }

    companion object {
        fun factory(
            childId: Long,
            content: ContentRepository,
            progress: ProgressRepository,
            parent: ParentRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ParentViewModel(childId, content, progress, parent) }
        }
    }
}
