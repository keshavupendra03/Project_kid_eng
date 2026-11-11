package com.samjhoenglish.data

import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.content.ListenRead
import com.samjhoenglish.content.ListenTap
import com.samjhoenglish.content.MatchMeaning
import com.samjhoenglish.content.SayWord
import com.samjhoenglish.content.Step
import com.samjhoenglish.content.WordPick
import com.samjhoenglish.content.answerSpeech
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.data.db.DayMillis
import com.samjhoenglish.data.db.MissionState
import com.samjhoenglish.data.db.ProgressDao
import com.samjhoenglish.data.db.SkillState
import com.samjhoenglish.learning.Learning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Everything behind the parent section: dashboard numbers, missions, time limit. */
class ParentRepository(
    private val dao: ProgressDao,
    private val content: ContentRepository,
    private val settings: SettingsRepository,
    private val timer: StudyTimer,
) {
    fun dailyMillis(childId: Long, fromDay: Long): Flow<List<DayMillis>> = dao.dailyMillis(childId, fromDay)

    fun skills(childId: Long): Flow<List<SkillState>> = dao.skillsFlow(childId)

    fun sentencesSpoken(childId: Long): Flow<Int> = dao.sentencesSpoken(childId)

    /** How many different words the child has answered correctly at least once. */
    fun wordsLearned(childId: Long): Flow<Int> = dao.correctSteps(childId).map { ids ->
        val index = content.stepIndex()
        ids.mapNotNull { index[it]?.step }
            .filter(::isWordQuestion)
            .mapNotNull { it.answerSpeech?.lowercase() }
            .toSet()
            .size
    }

    private fun isWordQuestion(step: Step): Boolean =
        (step is ListenTap || step is WordPick || step is MatchMeaning || step is ListenRead || step is SayWord) &&
            step.skills.any { it.startsWith("vocab.") }

    // ---------- Missions ----------

    fun missionStates(childId: Long): Flow<List<MissionState>> = dao.missions(childId)

    /** The child says they did it; it waits for a parent. */
    suspend fun markMissionDone(childId: Long, missionId: String) {
        dao.upsertMission(MissionState(childId, missionId, approved = false, updatedAt = System.currentTimeMillis()))
    }

    suspend fun approveMission(childId: Long, missionId: String) {
        dao.upsertMission(MissionState(childId, missionId, approved = true, updatedAt = System.currentTimeMillis()))
    }

    /** "Not yet": the mission goes back to the child. */
    suspend fun returnMission(childId: Long, missionId: String) {
        dao.deleteMission(childId, missionId)
    }

    // ---------- Daily time limit ----------

    suspend fun setDailyLimit(childId: Long, minutes: Int) = dao.setDailyLimit(childId, minutes)

    /** True when today's study time has used up the limit (plus any extra time granted today). */
    suspend fun timeIsUp(child: Child): Boolean {
        if (child.dailyLimitMinutes == 0) return false
        timer.flush()
        val today = localDay(System.currentTimeMillis())
        return Learning.timeIsUp(
            spentMillis = dao.millisOn(child.id, today),
            limitMinutes = child.dailyLimitMinutes,
            extraMinutes = settings.extraMinutes(child.id, today),
        )
    }

    suspend fun addExtraTime(childId: Long, minutes: Int) {
        settings.addExtraMinutes(childId, localDay(System.currentTimeMillis()), minutes)
    }

    // ---------- Deleting a child ----------

    /** Removes the child and all their progress. */
    suspend fun deleteChild(childId: Long) = dao.deleteChildCompletely(childId)
}
