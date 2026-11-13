package com.samjhoenglish.learning

import com.samjhoenglish.content.Blend
import com.samjhoenglish.content.BuildSentence
import com.samjhoenglish.content.Choice
import com.samjhoenglish.content.Crossword
import com.samjhoenglish.content.FillBlank
import com.samjhoenglish.content.MatchPairs
import com.samjhoenglish.content.SortGroups
import com.samjhoenglish.content.FindEnglish
import com.samjhoenglish.content.LearnCard
import com.samjhoenglish.content.ListenRead
import com.samjhoenglish.content.ListenTap
import com.samjhoenglish.content.MatchMeaning
import com.samjhoenglish.content.MinimalPair
import com.samjhoenglish.content.ReadAlong
import com.samjhoenglish.content.StoryQuestion
import com.samjhoenglish.content.SpellWord
import com.samjhoenglish.content.SaySentence
import com.samjhoenglish.content.SayWord
import com.samjhoenglish.content.SentenceTrain
import com.samjhoenglish.content.SpotMistake
import com.samjhoenglish.content.Step
import com.samjhoenglish.content.TeachTinku
import com.samjhoenglish.content.WordPick
import com.samjhoenglish.content.sentence
import com.samjhoenglish.i18n.L

/** What Tinku says after a wrong answer, and any English to play slowly afterwards. */
data class Hint(val text: L, val slowEnglish: List<String> = emptyList())

/** The hint ladder: each wrong try gets more help, until the answer is shown. See PLAN.md section 9. */
object Hints {
    private const val SPEAKING_TRIES = 4
    private const val OTHER_TRIES = 3

    /** Stops once only the answer is left to tap, because tapping it then proves nothing. */
    fun maxTries(step: Step): Int = when (step) {
        is ListenTap -> step.options.size - 1
        is WordPick -> step.options.size - 1
        is ListenRead -> step.options.size - 1
        is MatchMeaning -> step.options.size - 1
        is MinimalPair -> step.options.size - 1
        is FillBlank -> step.options.size - 1
        is SayWord, is SaySentence, is Blend, is TeachTinku -> SPEAKING_TRIES
        is StoryQuestion -> step.options.size - 1
        is SpellWord -> OTHER_TRIES
        // A passage is long: one more go, then Tinku reads it to the child.
        is ReadAlong -> 2
        is SpotMistake -> minOf(OTHER_TRIES, step.words.size - 1)
        is FindEnglish -> minOf(OTHER_TRIES, step.words.count { it.en == null })
        is Choice -> step.options.size - 1
        // Many small answers in one exercise: one slip each is allowed before the rest are shown.
        is MatchPairs -> step.pairs.size
        is Crossword -> step.entries.count { !it.given }
        is SortGroups -> maxOf(OTHER_TRIES, step.items.size / 3)
        else -> OTHER_TRIES
    }.coerceAtLeast(1)

    /** Help after wrong try number [wrongTries]. Null means a plain "try again". */
    fun forTry(step: Step, wrongTries: Int): Hint? = when (step) {
        is ListenTap -> listenTap(step, wrongTries)
        is WordPick -> readWord(step.answer, step.options)
        is ListenRead -> readWord(step.answer, step.options)
        is MatchMeaning -> meaning(step)
        is FillBlank -> Hint(rule(step) ?: READ_WHOLE_LINE)
        is SpotMistake -> spotMistake(step, wrongTries)
        is FindEnglish -> findEnglish(step, wrongTries)
        is SentenceTrain -> sentenceTrain(step, wrongTries)
        is SayWord -> speaking(step.word.english, wrongTries) {
            val hindi = step.word.hindi ?: return@speaking null
            Hint(itMeans(hindi, step.word.hinglish ?: hindi), listOf(step.word.english))
        }
        is SaySentence -> speaking(step.sentence, wrongTries) {
            Hint(ONE_WORD_AT_A_TIME, step.sentence.split(" "))
        }
        is Blend -> speaking(step.word, wrongTries) {
            Hint(inParts(step.parts.joinToString(" - ") { it.letters }), listOf(step.word))
        }
        is TeachTinku -> when (wrongTries) {
            1 -> null
            2 -> Hint(rule(step) ?: SAY_WITH_ME, listOf(step.answer))
            else -> Hint(ONE_WORD_AT_A_TIME, step.answer.split(" "))
        }
        is StoryQuestion -> Hint(LOOK_IN_THE_STORY)
        is SpellWord -> spelling(step.word.english.lowercase(), wrongTries)
        is ReadAlong -> Hint(READ_ONCE_MORE)
        is Choice -> choice(step, wrongTries)
        is MatchPairs -> Hint(EASY_ONES_FIRST)
        is Crossword -> Hint(COUNT_THE_BOXES)
        is SortGroups -> Hint(rule(step) ?: WHICH_GROUP)
        is BuildSentence -> if (wrongTries == 1) {
            val first = step.tiles.first()
            Hint(L("पहला शब्द है: ‘$first’", "Pehla shabd hai: ‘$first’", "The first word is: ‘$first’"), listOf(first))
        } else {
            Hint(rule(step) ?: SAY_WITH_ME, listOf(step.answer))
        }
        else -> null
    }

    /** First the question in Hindi (if the child may not have understood it), then where to look. */
    private fun choice(step: Choice, wrongTries: Int): Hint {
        val meaning = step.questionMeaning
        if (wrongTries == 1 && meaning != null) {
            return Hint(L("सवाल का मतलब: ${meaning.hi}", "Sawaal ka matlab: ${meaning.hinglish}", "Read the question again slowly."), listOf(step.question))
        }
        return Hint(if ("book.comprehension" in step.skills) LOOK_IN_THE_BOOK else rule(step) ?: READ_WHOLE_LINE)
    }

    /** Extra teaching when the answer is shown. Null means the usual message. */
    fun forReveal(step: Step): Hint? = when (step) {
        is MinimalPair -> Hint(LISTEN_TO_BOTH, step.options.filter { it != step.answer } + step.answer)
        is ReadAlong -> Hint(TINKU_WILL_READ)
        is FillBlank, is SpotMistake, is SentenceTrain, is TeachTinku, is BuildSentence -> rule(step)?.let { Hint(it) }
        else -> null
    }

    private fun listenTap(step: ListenTap, wrongTries: Int): Hint {
        val slow = listOf(step.answer)
        if (wrongTries == 1) return Hint(LISTEN_SLOWLY, slow)
        val hindi = step.options.firstOrNull { it.english == step.answer }?.hindi
        val hinglish = step.options.firstOrNull { it.english == step.answer }?.hinglish
        return if (hindi != null) Hint(itMeans(hindi, hinglish ?: hindi), slow)
        else readWord(step.answer, step.options.map { it.english })
    }

    private fun readWord(answer: String, options: List<String>): Hint {
        val slow = listOf(answer)
        return if (startsDifferently(answer, options)) Hint(startsWith(answer.first().lowercase()), slow)
        else Hint(LISTEN_SLOWLY, slow)
    }

    private fun meaning(step: MatchMeaning): Hint {
        val slow = listOf(step.word.english)
        val others = step.options.filter { it != step.answer }
        val hiLetter = step.answer.hi.first().toString()
        val hinglishLetter = step.answer.hinglish.first().uppercase()
        val distinct = others.none { it.hi.first().toString() == hiLetter || it.hinglish.first().uppercase() == hinglishLetter }
        if (!distinct) return Hint(LISTEN_SLOWLY, slow)
        return Hint(
            L(
                "मतलब ‘$hiLetter’ से शुरू होता है।",
                "Matlab ‘$hinglishLetter’ se shuru hota hai.",
                "The meaning starts with ‘$hiLetter’.",
            ),
            slow,
        )
    }

    private fun spotMistake(step: SpotMistake, wrongTries: Int): Hint {
        val wrongWord = step.words[step.wrong]
        if (wrongTries >= 2 && startsDifferently(wrongWord, step.words)) {
            val letter = wrongWord.first().lowercase()
            return Hint(L("गलत शब्द ‘$letter’ से शुरू होता है।", "Galat shabd ‘$letter’ se shuru hota hai.", "The wrong word starts with ‘$letter’."))
        }
        return Hint(rule(step) ?: READ_WHOLE_LINE)
    }

    private fun findEnglish(step: FindEnglish, wrongTries: Int): Hint {
        if (wrongTries == 1) return Hint(LIKE_SCHOOL_BUS_PHONE)
        val count = step.words.count { it.en != null }
        return if (count == 1) Hint(L("इस लाइन में 1 इंग्लिश शब्द है।", "Is line mein 1 English shabd hai.", "This line has 1 English word."))
        else Hint(L("इस लाइन में $count इंग्लिश शब्द हैं।", "Is line mein $count English shabd hain.", "This line has $count English words."))
    }

    private fun sentenceTrain(step: SentenceTrain, wrongTries: Int): Hint {
        if (wrongTries == 1) {
            val first = step.answer.first().text
            return Hint(L("पहला डिब्बा है: ‘$first’", "Pehla dabba hai: ‘$first’", "The first coach is: ‘$first’"), listOf(first))
        }
        return Hint(rule(step) ?: SAY_WITH_ME, listOf(step.sentence))
    }

    /** First the opening letter, then the length with the first and last letters shown. */
    private fun spelling(word: String, wrongTries: Int): Hint {
        if (wrongTries == 1) return Hint(startsWith(word.take(1)), listOf(word))
        val pattern = word.mapIndexed { i, c -> if (i == 0 || i == word.lastIndex) c else '_' }.joinToString(" ")
        return Hint(
            L("${word.length} अक्षर: $pattern", "${word.length} akshar: $pattern", "${word.length} letters: $pattern"),
            listOf(word),
        )
    }

    private inline fun speaking(target: String, wrongTries: Int, thirdRung: () -> Hint?): Hint? = when (wrongTries) {
        1 -> null
        2 -> Hint(SAY_WITH_ME, listOf(target))
        else -> thirdRung() ?: Hint(SAY_WITH_ME, listOf(target))
    }

    /** The first letter only helps if no other option starts with it. */
    private fun startsDifferently(answer: String, options: List<String>): Boolean {
        val letter = answer.first().lowercaseChar()
        return options.none { it != answer && it.first().lowercaseChar() == letter }
    }

    // "word_order" and "spot_mistake" are general, so a step's more specific rule is used first.
    private val generalRules = setOf("grammar.word_order", "grammar.spot_mistake")

    private fun rule(step: Step): L? = step.skills
        .sortedBy { it in generalRules }
        .firstNotNullOfOrNull { SkillHelps.of(it)?.hint }

    private fun itMeans(hindi: String, hinglish: String) =
        L("इसका मतलब है: $hindi", "Iska matlab hai: $hinglish", "In Hindi it means: $hindi")

    private fun startsWith(letter: String) =
        L("यह शब्द ‘$letter’ से शुरू होता है।", "Yeh shabd ‘$letter’ se shuru hota hai.", "This word starts with ‘$letter’.")

    private fun inParts(parts: String) =
        L("टुकड़ों में बोलो: $parts", "Tukdon mein bolo: $parts", "Say it in parts: $parts")

    private val LISTEN_SLOWLY = L("ध्यान से सुनो, अब धीरे-धीरे।", "Dhyaan se suno, ab dheere-dheere.", "Listen carefully, nice and slow.")
    private val SAY_WITH_ME = L("धीरे से सुनो, फिर मेरे साथ बोलो।", "Dheere se suno, phir mere saath bolo.", "Listen slowly, then say it with me.")
    private val ONE_WORD_AT_A_TIME = L("एक-एक शब्द करके बोलो।", "Ek-ek shabd karke bolo.", "Say it one word at a time.")
    private val LISTEN_TO_BOTH = L("दोनों को ध्यान से सुनो।", "Dono ko dhyaan se suno.", "Listen carefully to both.")
    private val READ_WHOLE_LINE = L("पूरी लाइन पढ़कर सोचो, क्या ठीक लगता है?", "Poori line padhkar socho, kya theek lagta hai?", "Read the whole line and think: what sounds right?")
    private val LOOK_IN_THE_STORY = L("जवाब कहानी में छिपा है। याद करो, क्या पढ़ा था?", "Jawab kahani mein chhipa hai. Yaad karo, kya padha tha?", "The answer is in the story. Think back: what did you read?")
    private val READ_ONCE_MORE = L("बढ़िया कोशिश! एक बार फिर पढ़ो। मुश्किल शब्द को छूकर सुन लो।", "Badhiya koshish! Ek baar phir padho. Mushkil shabd ko chhookar sun lo.", "Good try! Read it once more. Tap a hard word to hear it first.")
    private val TINKU_WILL_READ = L("कोई बात नहीं। अब मैं पढ़ता हूँ, तुम साथ-साथ देखो।", "Koi baat nahi. Ab main padhta hoon, tum saath-saath dekho.", "That's okay. Now I'll read, and you follow along.")
    private val LOOK_IN_THE_BOOK = L("किताब में कहानी फिर से देखो। जवाब वहीं लिखा है।", "Kitaab mein kahani phir se dekho. Jawab wahin likha hai.", "Look at the story in your book again. The answer is written there.")
    private val EASY_ONES_FIRST = L("जो जोड़ी पक्की पता है, वो पहले जोड़ो। बाकी आसान हो जाएँगी।", "Jo jodi pakki pata hai, woh pehle jodo. Baaki aasaan ho jaayengi.", "Join the pairs you are sure of first. The rest get easier.")
    private val COUNT_THE_BOXES = L("खाली जगह वाली लाइन पढ़ो, और ग्रिड में डिब्बे गिनो। शब्द में उतने ही अक्षर होंगे।", "Khaali jagah waali line padho, aur grid mein dabbe gino. Shabd mein utne hi akshar honge.", "Read the line with the gap and count the boxes in the grid. The word has that many letters.")
    private val WHICH_GROUP = L("सोचो, यह कहाँ रहता है और कैसा है? फिर उसका ग्रुप चुनो।", "Socho, yeh kahan rehta hai aur kaisa hai? Phir uska group chuno.", "Think: where does it live, and what is it like? Then pick its group.")
    private val LIKE_SCHOOL_BUS_PHONE =L("सोचो: कौन सा शब्द स्कूल, बस या फ़ोन जैसा है?", "Socho: kaun sa shabd school, bus ya phone jaisa hai?", "Think: which word is like school, bus or phone?")
}

/** When a child stays weak in a skill: a different explanation first, then easier questions. */
object StuckHelp {
    /** The lesson's own wording is repeated by "try again", so this card opens with a new one. */
    fun card(skill: String): LearnCard? {
        val help = SkillHelps.of(skill) ?: return null
        return LearnCard(
            id = "help.$skill",
            skills = listOf(skill),
            text = L(
                "चलो, दूसरे तरीके से समझते हैं। ${help.explain.hi}",
                "Chalo, doosre tareeke se samajhte hain. ${help.explain.hinglish}",
                "Let's try another way. ${help.explain.en}",
            ),
        )
    }

    /** Fewer choices. The question and its answer stay the same. */
    fun easier(step: Step): Step = when (step) {
        is ListenTap -> step.copy(options = narrowed(step.options) { it.english == step.answer })
        is WordPick -> step.copy(options = narrowed(step.options) { it == step.answer })
        is ListenRead -> step.copy(options = narrowed(step.options) { it == step.answer })
        is MatchMeaning -> step.copy(options = narrowed(step.options) { it == step.answer })
        is FillBlank -> step.copy(options = narrowed(step.options) { it == step.answer })
        is StoryQuestion -> step.copy(options = narrowed(step.options) { it == step.answer })
        // Pictures must stay with their options, so a question with pictures keeps all of them.
        is Choice -> if (step.emojis.isEmpty()) step.copy(options = narrowed(step.options) { it == step.answer }) else step
        is BuildSentence -> step.copy(extra = emptyList())
        // Letter tiles instead of the keyboard.
        is SpellWord -> step.copy(typed = false)
        is SentenceTrain -> step.copy(extra = emptyList())
        else -> step
    }

    private fun <T> narrowed(options: List<T>, isAnswer: (T) -> Boolean): List<T> {
        if (options.size <= 2) return options
        return options.filter(isAnswer).take(1) + options.filterNot(isAnswer).take(1)
    }

    data class Adapted(val steps: List<Step>, val reviewCount: Int)

    /** Puts the new explanations first and makes practice on those skills easier. */
    fun adapt(steps: List<Step>, reviewCount: Int, stuck: List<String>): Adapted {
        if (stuck.isEmpty()) return Adapted(steps, reviewCount)
        val cards = stuck.mapNotNull(::card)
        val practice = steps.map { step -> if (step.skills.any { it in stuck }) easier(step) else step }
        return Adapted(cards + practice, if (reviewCount > 0) reviewCount + cards.size else 0)
    }
}
