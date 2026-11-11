package com.samjhoenglish.learning

import com.samjhoenglish.content.ContentJson
import com.samjhoenglish.content.FillBlank
import com.samjhoenglish.content.Lesson
import com.samjhoenglish.content.ListenTap
import com.samjhoenglish.content.MatchMeaning
import com.samjhoenglish.content.Meaning
import com.samjhoenglish.content.MinimalPair
import com.samjhoenglish.content.RoleWord
import com.samjhoenglish.content.SaySentence
import com.samjhoenglish.content.SayWord
import com.samjhoenglish.content.SentenceTrain
import com.samjhoenglish.content.Step
import com.samjhoenglish.content.Word
import com.samjhoenglish.content.WordPick
import com.samjhoenglish.content.answerSpeech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HintsTest {
    private val fourPictures = ListenTap(
        id = "t",
        answer = "apple",
        options = listOf(Word("apple", "🍎", "सेब", "seb"), Word("bus", "🚌"), Word("cat", "🐱"), Word("dog", "🐶")),
    )
    private val lessons = File("src/main/assets/content/lessons").listFiles().orEmpty()
        .map { ContentJson.decodeFromString<Lesson>(it.readText()) }
    private val allSteps: List<Step> = lessons.flatMap { it.steps }

    @Test
    fun answerIsShownOnceOnlyTheAnswerIsLeftToTap() {
        assertEquals(3, Hints.maxTries(fourPictures))
        assertEquals(1, Hints.maxTries(MinimalPair(id = "m", answer = "vet", options = listOf("vet", "wet"))))
    }

    @Test
    fun speakingGetsLongerLadderBecauseNoChoicesRunOut() {
        assertEquals(4, Hints.maxTries(SayWord(id = "s", word = Word("apple"))))
    }

    @Test
    fun listeningHelpGrowsFromSlowAudioToHindiMeaning() {
        val first = Hints.forTry(fourPictures, 1)!!
        assertEquals(listOf("apple"), first.slowEnglish)
        val second = Hints.forTry(fourPictures, 2)!!
        assertTrue(second.text.hi.contains("सेब"))
    }

    @Test
    fun firstLetterIsNotOfferedWhenAnotherOptionSharesIt() {
        val shared = WordPick(id = "w", emoji = "🍎", answer = "apple", options = listOf("apple", "ant", "bus"))
        assertTrue(!Hints.forTry(shared, 1)!!.text.en.contains("starts with"))
        val unique = shared.copy(options = listOf("apple", "cat", "bus"))
        assertTrue(Hints.forTry(unique, 1)!!.text.en.contains("‘a’"))
    }

    @Test
    fun speakingHelpStartsPlainThenSlowsDownThenBreaksUp() {
        val step = SaySentence(id = "s", sentence = "I am happy", meaning = com.samjhoenglish.i18n.L("a", "b", "c"))
        assertNull(Hints.forTry(step, 1))
        assertEquals(listOf("I am happy"), Hints.forTry(step, 2)!!.slowEnglish)
        assertEquals(listOf("I", "am", "happy"), Hints.forTry(step, 3)!!.slowEnglish)
    }

    @Test
    fun specificGrammarRuleBeatsGeneralWordOrderRule() {
        val step = SentenceTrain(
            id = "t",
            skills = listOf("grammar.word_order", "grammar.i_am"),
            hindi = emptyList(),
            answer = listOf(RoleWord("I"), RoleWord("am"), RoleWord("happy")),
        )
        assertEquals(SkillHelps.of("grammar.i_am")!!.hint, Hints.forTry(step, 2)!!.text)
    }

    @Test
    fun minimalPairRevealPlaysBothWithTheAnswerLast() {
        val step = MinimalPair(id = "m", answer = "vet", options = listOf("vet", "wet"))
        assertEquals(listOf("wet", "vet"), Hints.forReveal(step)!!.slowEnglish)
    }

    @Test
    fun hintsNeverGiveAwayTheAnswerOfATapQuestion() {
        val tapQuestions = allSteps.filter { it is WordPick || it is FillBlank || it is MatchMeaning }
        assertTrue(tapQuestions.size > 100)
        tapQuestions.forEach { step ->
            for (tries in 1 until Hints.maxTries(step)) {
                val hint = Hints.forTry(step, tries) ?: continue
                val answer = when (step) {
                    is FillBlank -> step.answer
                    is MatchMeaning -> step.answer.hi
                    else -> step.answerSpeech!!
                }
                // A one-word grammar rule may name "is" or "am" among several; only a plain "the answer is X" would be a leak.
                assertTrue("${step.id} gives away $answer", !hint.text.en.endsWith(": $answer") && !hint.text.hi.endsWith(": $answer"))
            }
        }
    }

    @Test
    fun everySpecificSkillInTheLessonsHasASecondExplanation() {
        val skills = allSteps.flatMap { it.skills }.toSet() - SkillCatalog.broad
        assertTrue(skills.size > 50)
        val missing = skills.filter { SkillHelps.of(it) == null }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun helpTextIsInTheRightLanguageAndHintsFitTheFeedbackPanel() {
        val devanagari = Regex("[\\u0900-\\u097F]")
        val skills = allSteps.flatMap { it.skills }.toSet() - SkillCatalog.broad
        skills.mapNotNull { SkillHelps.of(it) }.forEach { help ->
            val texts = listOfNotNull(help.hint, help.explain)
            texts.forEach { text ->
                assertNotNull(devanagari.find(text.hi))
                assertNull(text.hinglish, devanagari.find(text.hinglish))
                assertNull(text.en, devanagari.find(text.en))
            }
            help.hint?.let { assertTrue(it.hi, it.hi.length <= 70) }
        }
    }

    @Test
    fun easierPracticeKeepsTheAnswerAndCutsChoices() {
        val easier = StuckHelp.easier(fourPictures) as ListenTap
        assertEquals(2, easier.options.size)
        assertTrue(easier.options.any { it.english == "apple" })

        val meaning = MatchMeaning(
            id = "m",
            word = Word("apple"),
            answer = Meaning("सेब", "seb"),
            options = listOf(Meaning("केला", "kela"), Meaning("सेब", "seb"), Meaning("आम", "aam")),
        )
        assertTrue((StuckHelp.easier(meaning) as MatchMeaning).options.contains(meaning.answer))

        val train = SentenceTrain(id = "t", hindi = emptyList(), answer = listOf(RoleWord("I")), extra = listOf("is"))
        assertEquals(emptyList<String>(), (StuckHelp.easier(train) as SentenceTrain).extra)
    }

    @Test
    fun stuckChildGetsTheNewExplanationFirstAndEasierQuestions() {
        val hard = fourPictures.copy(skills = listOf("grammar.i_am"))
        val other = fourPictures.copy(id = "other", skills = listOf("vocab.food"))
        val adapted = StuckHelp.adapt(listOf(hard, other), reviewCount = 0, stuck = listOf("grammar.i_am"))

        assertEquals("help.grammar.i_am", adapted.steps.first().id)
        assertEquals(2, (adapted.steps[1] as ListenTap).options.size)
        assertEquals(4, (adapted.steps[2] as ListenTap).options.size)
        assertEquals(0, adapted.reviewCount)
    }

    @Test
    fun explanationCardsCountAsPartOfRevision() {
        val review = fourPictures.copy(skills = listOf("grammar.i_am"))
        val adapted = StuckHelp.adapt(listOf(review), reviewCount = 1, stuck = listOf("grammar.i_am"))
        assertEquals(2, adapted.reviewCount)
    }

    @Test
    fun onlyWeakSpecificSkillsCountAsStuck() {
        val scores = listOf(
            Learning.SkillScore("grammar.i_am", 0.2f, 5),
            Learning.SkillScore("grammar.not", 0.3f, 2), // too few tries to judge
            Learning.SkillScore("sound.th", 0.9f, 8),
            Learning.SkillScore("speak.word", 0.1f, 9), // broad
            Learning.SkillScore("vocab.food", 0.1f, 9), // not in this session
        )
        val stuck = Learning.stuckSkills(setOf("grammar.i_am", "grammar.not", "sound.th", "speak.word"), scores, SkillCatalog.broad)
        assertEquals(listOf("grammar.i_am"), stuck)
    }
}
