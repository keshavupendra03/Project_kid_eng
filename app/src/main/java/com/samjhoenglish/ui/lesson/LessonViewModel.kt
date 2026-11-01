package com.samjhoenglish.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samjhoenglish.content.Step
import com.samjhoenglish.content.isQuestion
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.learning.Learning
import com.samjhoenglish.speech.Mistake
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Feedback { NONE, CORRECT, TRY_AGAIN, REVEALED }

data class LessonUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    /** The lesson being played; null for a revision-only session. */
    val lessonId: String? = null,
    /** Revision questions, the lesson, then a second chance for anything answered wrongly. */
    val steps: List<Step> = emptyList(),
    /** Steps before this index are the planned session; after it are second chances. */
    val lessonSize: Int = 0,
    /** The first [reviewCount] steps are revision from earlier lessons. */
    val reviewCount: Int = 0,
    val position: Int = 0,
    val feedback: Feedback = Feedback.NONE,
    val wrongTries: Int = 0,
    /** The pronunciation slip behind the last wrong answer, if one was recognised. */
    val mistake: Mistake? = null,
    /** The child chose "can't speak now" for this step. */
    val skipped: Boolean = false,
    /** Questions in the lesson part (revision doesn't affect stars). */
    val questions: Int = 0,
    val rightFirstTime: Int = 0,
    /** Set once the result is saved. */
    val saved: Boolean = false,
    /** What to do next: may be this same lesson if it needs another go. */
    val nextLessonId: String? = null,
) {
    val current: Step? get() = steps.getOrNull(position)
    val empty: Boolean get() = !loading && !error && steps.isEmpty()
    val finished: Boolean get() = !loading && !error && steps.isNotEmpty() && position >= steps.size
    val inReview: Boolean get() = position < reviewCount
    val progress: Float get() = if (steps.isEmpty()) 0f else position.toFloat() / steps.size
    val stars: Int get() = Learning.stars(rightFirstTime, questions)
    val needsRetry: Boolean get() = saved && lessonId != null && nextLessonId == lessonId
}

/**
 * Plays one session. With a [lessonId] it's that lesson; with null it's "today's task",
 * which the progress repository plans (revision first, then the next lesson).
 */
class LessonViewModel(
    private val childId: Long,
    private var lessonId: String?,
    private val progress: ProgressRepository,
) : ViewModel() {
    private val state = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = state.asStateFlow()

    private var stepLessons: Map<String, String> = emptyMap()

    init {
        load()
    }

    private fun load() {
        state.value = LessonUiState()
        viewModelScope.launch {
            state.value = runCatching { progress.planSession(childId, lessonId) }.fold(
                onSuccess = { plan ->
                    stepLessons = plan.stepLessons
                    LessonUiState(
                        loading = false,
                        lessonId = plan.lessonId,
                        steps = plan.steps,
                        lessonSize = plan.steps.size,
                        reviewCount = plan.reviewCount,
                        questions = plan.steps.drop(plan.reviewCount).count { it.isQuestion },
                    )
                },
                onFailure = { LessonUiState(loading = false, error = true) },
            )
        }
    }

    fun answer(correct: Boolean, mistake: Mistake? = null) {
        val s = state.value
        val step = s.current ?: return
        if (s.feedback == Feedback.CORRECT || s.feedback == Feedback.REVEALED) return

        if (correct) {
            val inLesson = s.position >= s.reviewCount && s.position < s.lessonSize
            state.value = s.copy(
                feedback = Feedback.CORRECT,
                rightFirstTime = s.rightFirstTime + if (inLesson && s.wrongTries == 0) 1 else 0,
            )
            record(step, if (s.wrongTries == 0) Learning.Outcome.FIRST_TRY else Learning.Outcome.AFTER_RETRY, s.wrongTries, s.mistake)
        } else {
            val tries = s.wrongTries + 1
            // Give a second chance at the end — but only once per question.
            val needsSecondChance = s.position < s.lessonSize && s.wrongTries == 0
            val revealed = tries >= MAX_TRIES
            state.value = s.copy(
                steps = if (needsSecondChance) s.steps + step else s.steps,
                wrongTries = tries,
                mistake = mistake,
                feedback = if (revealed) Feedback.REVEALED else Feedback.TRY_AGAIN,
            )
            if (revealed) record(step, Learning.Outcome.MISSED, tries, mistake)
        }
    }

    /** "Can't speak right now" (noisy room, shy child): show the answer without counting it wrong. */
    fun skip() {
        val s = state.value
        val step = s.current ?: return
        if (s.feedback == Feedback.CORRECT || s.feedback == Feedback.REVEALED) return
        state.value = s.copy(feedback = Feedback.REVEALED, skipped = true)
        record(step, Learning.Outcome.SKIPPED, s.wrongTries, null)
    }

    fun next() {
        val s = state.value
        state.value = s.copy(
            position = s.position + 1,
            feedback = Feedback.NONE,
            wrongTries = 0,
            mistake = null,
            skipped = false,
        )
        if (state.value.finished) finish()
    }

    /** Play the same lesson again (or the same revision session). */
    fun restart() {
        lessonId = state.value.lessonId ?: lessonId
        load()
    }

    private fun finish() {
        val s = state.value
        val id = s.lessonId
        viewModelScope.launch {
            val next = if (id != null) progress.finishLesson(childId, id, s.stars) else null
            state.value = state.value.copy(saved = true, nextLessonId = next)
        }
    }

    private fun record(step: Step, outcome: Learning.Outcome, wrongTries: Int, mistake: Mistake?) {
        val s = state.value
        val lesson = stepLessons[step.id] ?: return
        val countsForReview = s.position < s.lessonSize
        viewModelScope.launch {
            progress.recordAnswer(childId, lesson, step, outcome, wrongTries, mistake?.skill, countsForReview)
        }
    }

    companion object {
        const val MAX_TRIES = 3

        fun factory(childId: Long, lessonId: String?, progress: ProgressRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { LessonViewModel(childId, lessonId, progress) } }
    }
}
