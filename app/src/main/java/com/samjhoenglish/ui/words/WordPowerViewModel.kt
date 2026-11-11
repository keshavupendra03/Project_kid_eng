package com.samjhoenglish.ui.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.content.WORD_MONTHLY_PREFIX
import com.samjhoenglish.content.WORD_PLACEMENT
import com.samjhoenglish.content.WordBand
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.data.localDay
import com.samjhoenglish.learning.WordPower
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class PackRow(val id: String, val testId: String, val emoji: String, val title: com.samjhoenglish.i18n.L, val open: Boolean, val learned: Boolean, val testStars: Int)

data class BandRow(val band: WordBand, val open: Boolean, val home: Boolean, val packs: List<PackRow>, val testsPassed: Int)

data class WordPowerUiState(
    val loaded: Boolean = false,
    val bands: List<BandRow> = emptyList(),
    val wordsLearned: Int = 0,
    val mastered: Int = 0,
    val due: Int = 0,
    val placementDone: Boolean = false,
    val placementId: String = "",
    val monthlyOpen: Boolean = false,
    /** Stars of the most recent monthly test, if one was taken. */
    val lastMonthly: Int? = null,
)

class WordPowerViewModel(childId: Long, classGrade: Int, content: ContentRepository, progress: ProgressRepository) : ViewModel() {
    private val file = flow { emit(content.wordPower()) }

    val uiState: StateFlow<WordPowerUiState> = combine(
        file,
        progress.lessonStatus(childId),
        progress.wordsMastered(childId),
        progress.wordsDue(childId),
        progress.attemptsFor(childId, WORD_PLACEMENT),
    ) { wp, status, mastered, due, placementAttempts ->
        val home = WordPower.homeBand(classGrade, wp.bands.map { it.minClass to it.maxClass })
        val testsPassed = wp.bands.map { band -> band.packs.count { WordPower.testPassed(status[it.testId]?.bestStars ?: 0) } }
        val placementRight = wp.bands.map { band -> placementAttempts.count { it.firstTry && it.stepId.contains(".${band.id}.") } }
        val open = WordPower.openBands(home, testsPassed, placementRight)
        val wordsLearned = wp.bands.flatMap { it.packs }.count { status[it.id]?.completed == true } * 10
        // Monthly ids are numbered by period, so the highest number is the latest test.
        val monthly = status.filterKeys { it.startsWith(WORD_MONTHLY_PREFIX) }
            .maxByOrNull { it.key.removePrefix(WORD_MONTHLY_PREFIX).toIntOrNull() ?: 0 }
        WordPowerUiState(
            loaded = true,
            bands = wp.bands.mapIndexed { i, band ->
                val learned = band.packs.map { status[it.id]?.completed == true }
                BandRow(
                    band = band,
                    open = i in open,
                    home = i == home,
                    packs = band.packs.mapIndexed { p, pack ->
                        PackRow(pack.id, pack.testId, pack.emoji, pack.title, WordPower.packOpen(p, learned), learned[p], status[pack.testId]?.bestStars ?: 0)
                    },
                    testsPassed = testsPassed[i],
                )
            },
            wordsLearned = wordsLearned,
            mastered = mastered,
            due = due,
            placementDone = placementAttempts.isNotEmpty(),
            placementId = wp.placement,
            monthlyOpen = wordsLearned >= WordPower.MONTHLY_AFTER_WORDS,
            lastMonthly = monthly?.value?.bestStars,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordPowerUiState())

    companion object {
        fun factory(childId: Long, classGrade: Int, content: ContentRepository, progress: ProgressRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { WordPowerViewModel(childId, classGrade, content, progress) } }

        /** Today's monthly test id, so a finished one isn't offered again this month. */
        fun monthlyIdToday(): String = WordPower.monthlyId(localDay(System.currentTimeMillis()))
    }
}
