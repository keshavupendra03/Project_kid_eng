package com.samjhoenglish.speech

import com.samjhoenglish.i18n.L

/** A specific pronunciation slip, e.g. "thin" said as "tin", with a tip for fixing it. */
data class Mistake(val skill: String, val tip: L)

sealed interface SpeechVerdict {
    data object Correct : SpeechVerdict
    data class Wrong(val heard: String, val mistake: Mistake?) : SpeechVerdict
}

/**
 * Decides whether what the recogniser heard matches what the child was asked to say.
 *
 * Lenient on purpose (kids' voices and Indian accents confuse recognisers), except for the
 * classic Hindi-speaker sound swaps: if the recogniser's best guess is exactly such a swap
 * ("three" → "tree"), we call it out with a tip, because that's the habit we want to fix.
 */
object SpeechChecker {

    private data class Swap(val from: String, val to: String, val skill: String)

    private val swaps = listOf(
        Swap("th", "t", SKILL_TH), Swap("th", "d", SKILL_TH), Swap("th", "f", SKILL_TH),
        Swap("v", "w", SKILL_V), Swap("v", "b", SKILL_V),
        Swap("w", "v", SKILL_W),
        Swap("z", "j", SKILL_Z),
        Swap("ph", "p", SKILL_F), Swap("f", "p", SKILL_F),
        Swap("sh", "s", SKILL_SH),
    )

    /**
     * @param target what the child should say ("thin", "Thank you")
     * @param heard the recogniser's guesses, best first
     * @param accept other spellings that also count ("3" for "three")
     * @param strict no near-miss leniency: "pump" is not "jump". For reading along, where the
     *   listener already expects the paragraph's words and a near miss is a real misreading.
     */
    fun check(target: String, heard: List<String>, accept: List<String> = emptyList(), strict: Boolean = false): SpeechVerdict {
        val goals = (listOf(target) + accept).map(::normalize).filter { it.isNotEmpty() }
        val guesses = heard.map(::normalize).filter { it.isNotEmpty() }
        if (guesses.isEmpty()) return SpeechVerdict.Wrong("", null)
        val best = guesses.first()
        val goal = goals.first()

        if (goals.any { sameWords(it, best) || soundsSame(it, best) }) return SpeechVerdict.Correct
        findSwap(goal, best)?.let { return SpeechVerdict.Wrong(best, mistakeFor(it)) }
        if (guesses.any { g -> goals.any { sameWords(it, g) || soundsSame(it, g) } }) return SpeechVerdict.Correct
        if (!strict && goals.any { closeEnough(it, best) }) return SpeechVerdict.Correct
        return SpeechVerdict.Wrong(best, null)
    }

    /**
     * How a Hindi speaker might say [word] instead ("three" → "tree", "van" → "wan", "thank" → "tank").
     * Small, quickly-said words are left out: a listener can't tell "with" from "wit" reliably,
     * and offering both made it miss real words in tests.
     */
    fun slipVariants(word: String): List<String> {
        val w = normalize(word)
        if (w.length < 3 || w in SMALL_WORDS || " " in w) return emptyList()
        return variantSwaps.flatMap { swap ->
            generateSequence(w.indexOf(swap.from)) { i -> w.indexOf(swap.from, i + 1).takeIf { it >= 0 } }
                .takeWhile { it >= 0 }
                .map { i -> w.substring(0, i) + swap.to + w.substring(i + swap.from.length) }
                .toList()
        }.filter { it != w }.distinct()
    }

    // "th" said as "f" is rare for Hindi speakers, so it isn't offered to the listener.
    private val variantSwaps get() = swaps.filterNot { it.from == "th" && it.to == "f" }

    private val SMALL_WORDS = setOf(
        "the", "then", "they", "them", "this", "that", "with", "we", "for", "there", "their", "was", "were",
        "have", "of", "very", "from", "will", "what", "who", "when", "why", "so",
    )

    /** Lowercase letters/digits and single spaces only: "T.V." → "t v", "I'm" → "i am". */
    fun normalize(text: String): String = text.lowercase()
        .replace("’", "'")
        .replace(Regex("\\bi'?m\\b"), "i am")
        .replace(Regex("\\b(he|she|it|that|what|where|who|there)'s\\b"), "$1 is")
        .replace(Regex("\\b(you|we|they)'re\\b"), "$1 are")
        .replace(Regex("\\b(is|are|do|does)n't\\b"), "$1 not")
        .replace(Regex("\\bcan't\\b"), "cannot")
        .replace("'", "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    /**
     * [heard] counts as [goal]: identical, sound-alike, or a small accent slip.
     * A Hindi-speaker sound swap ("tank" for "thank") doesn't count, because that's what we teach.
     */
    internal fun wordsAlike(goal: String, heard: String): Boolean =
        soundsSame(goal, heard) || (similarWord(goal, heard) && wordSwap(goal, heard) == null)

    private fun compact(text: String) = text.replace(" ", "")

    // "ice cream" = "icecream", "t v" = "tv"
    private fun sameWords(a: String, b: String) = compact(a) == compact(b)

    /**
     * Words that sound identical. Recognisers often return a letter or a homophone for a short
     * word said on its own: "eye" → "I", "tea" → "T", "the" → "di", "two" → "2".
     */
    private val soundAlikes: List<Set<String>> = listOf(
        setOf("eye", "i", "aye", "ai"),
        setOf("tea", "tee", "t", "ti"),
        setOf("key", "k", "ki", "kee", "ke"),
        // "the" is said "thuh" or "thee"; on its own it's often heard as d-sounds.
        setOf("the", "di", "de", "da", "duh", "dee", "thee", "tha", "d"),
        setOf("a", "ay", "ae", "eh"),
        setOf("bee", "be", "b"),
        setOf("see", "sea", "c"),
        setOf("you", "u", "yu"),
        setOf("why", "y"),
        setOf("are", "r"),
        setOf("oh", "o", "owe"),
        setOf("pea", "pee", "p"),
        setOf("zero", "0"), setOf("one", "won", "1"), setOf("two", "to", "too", "2"), setOf("three", "3"),
        setOf("four", "for", "4"), setOf("five", "5"), setOf("six", "6"), setOf("seven", "7"),
        setOf("eight", "ate", "8"), setOf("nine", "9"), setOf("ten", "10"), setOf("hundred", "100"),
        setOf("sun", "son"), setOf("ant", "aunt"), setOf("nose", "knows"), setOf("night", "knight"),
        setOf("new", "knew"), setOf("no", "know"), setOf("write", "right"), setOf("red", "read"),
        setOf("blue", "blew"), setOf("flower", "flour"), setOf("bear", "bare"), setOf("hi", "high"),
        setOf("by", "bye", "buy"), setOf("meet", "meat"), setOf("hour", "our"), setOf("week", "weak"),
        setOf("pair", "pear"), setOf("tail", "tale"), setOf("sail", "sale"),
    )

    private val soundGroup: Map<String, Set<String>> =
        soundAlikes.flatMap { group -> group.map { it to group } }.toMap()

    private fun soundsSame(a: String, b: String): Boolean {
        if (a == b) return true
        val aWords = a.split(" ")
        val bWords = b.split(" ")
        if (aWords.size != bWords.size) return false
        return aWords.zip(bWords).all { (x, y) -> x == y || soundGroup[x]?.contains(y) == true }
    }

    /** The skill of the Hindi-speaker swap that turns [goal] into [heard], if any. */
    private fun findSwap(goal: String, heard: String): String? {
        val goalWords = goal.split(" ")
        val heardWords = heard.split(" ")
        if (goalWords.size == 1) return wordSwap(goal, heard)
        if (goalWords.size != heardWords.size) return null
        // In a sentence, report the first word that has a swap.
        return goalWords.zip(heardWords).firstNotNullOfOrNull { (g, h) -> if (g == h) null else wordSwap(g, h) }
    }

    private fun wordSwap(goal: String, heard: String): String? {
        val said = compact(heard)
        // "school" said as "is school" / "iskool": an extra vowel before s + consonant.
        if (Regex("^s[ckmnptlw]").containsMatchIn(goal) && (said == "is$goal" || said == "es$goal")) return SKILL_S_CLUSTER
        // Recognisers snap to real words ("wery" comes back as "wary"), so for longer words a
        // near-miss of the swapped form counts too — as long as it isn't already close to the goal.
        val fuzzy = said.length >= 4 && !similarWord(goal, said)
        for (swap in swaps) {
            var index = goal.indexOf(swap.from)
            while (index >= 0) {
                val variant = goal.substring(0, index) + swap.to + goal.substring(index + swap.from.length)
                if (variant == said || (fuzzy && similarWord(variant, said))) return swap.skill
                index = goal.indexOf(swap.from, index + 1)
            }
        }
        return null
    }

    private fun closeEnough(goal: String, heard: String): Boolean {
        val goalWords = goal.split(" ")
        val heardWords = heard.split(" ")
        if (goalWords.size == 1 && heardWords.size == 1) return similarWord(goal, heard)
        // Sentences: allow about one slip per four words.
        val errors = editDistance(goalWords, heardWords) { a, b -> similarWord(a, b) || soundsSame(a, b) }
        return errors <= goalWords.size / 4
    }

    private fun similarWord(a: String, b: String): Boolean {
        if (a == b) return true
        val longest = maxOf(a.length, b.length)
        // Short words must match exactly: "bus" vs "boss" is a different word.
        if (longest <= 3) return false
        val distance = editDistance(a.toList(), b.toList()) { x, y -> x == y }
        return if (longest <= 5) distance <= 1 else distance <= longest / 4
    }

    private fun <T> editDistance(a: List<T>, b: List<T>, same: (T, T) -> Boolean): Int {
        var previous = IntArray(b.size + 1) { it }
        for (i in 1..a.size) {
            val current = IntArray(b.size + 1)
            current[0] = i
            for (j in 1..b.size) {
                val cost = if (same(a[i - 1], b[j - 1])) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.size]
    }

    private fun mistakeFor(skill: String) = Mistake(skill, tips.getValue(skill))

    const val SKILL_TH = "sound.th"
    const val SKILL_V = "sound.v"
    const val SKILL_W = "sound.w"
    const val SKILL_Z = "sound.z"
    const val SKILL_F = "sound.f"
    const val SKILL_SH = "sound.sh"
    const val SKILL_S_CLUSTER = "sound.s_cluster"

    private val tips = mapOf(
        SKILL_TH to L(
            hi = "‘th’ बोलते समय जीभ को दाँतों के बीच हल्का सा बाहर निकालो और हवा छोड़ो।",
            hinglish = "‘th’ bolte samay jeebh ko daanton ke beech halka sa bahar nikaalo aur hawa chhodo.",
            en = "For ‘th’, put your tongue a little between your teeth and blow air.",
        ),
        SKILL_V to L(
            hi = "‘v’ के लिए ऊपर के दाँत नीचे वाले होंठ पर रखो और बोलो: व्व्व।",
            hinglish = "‘v’ ke liye upar ke daant neeche waale honth par rakho aur bolo: vvv.",
            en = "For ‘v’, put your top teeth on your bottom lip and say: vvv.",
        ),
        SKILL_W to L(
            hi = "‘w’ के लिए होंठ गोल करो, जैसे ‘ऊ’ बोलते हैं। दाँत होंठ पर नहीं लगने चाहिए।",
            hinglish = "‘w’ ke liye honth gol karo, jaise ‘oo’ bolte hain. Daant honth par nahi lagne chahiye.",
            en = "For ‘w’, make your lips round like saying ‘oo’. Your teeth don't touch your lip.",
        ),
        SKILL_Z to L(
            hi = "‘z’ में मधुमक्खी जैसी आवाज़ करो: ज़्ज़्ज़। यह ‘ज’ नहीं है।",
            hinglish = "‘z’ mein madhumakkhi jaisi awaaz karo: zzz. Yeh ‘ja’ nahi hai.",
            en = "For ‘z’, buzz like a bee: zzz. It is not ‘j’.",
        ),
        SKILL_F to L(
            hi = "‘f’ में होंठ आपस में नहीं मिलते। ऊपर के दाँत नीचे वाले होंठ पर रखकर हवा छोड़ो: फ़्फ़्फ़।",
            hinglish = "‘f’ mein honth aapas mein nahi milte. Upar ke daant neeche waale honth par rakhkar hawa chhodo: fff.",
            en = "For ‘f’, your lips don't touch. Put your top teeth on your bottom lip and blow: fff.",
        ),
        SKILL_SH to L(
            hi = "‘sh’ के लिए होंठ थोड़े आगे करो और बोलो: श, जैसे ‘शेर’ में।",
            hinglish = "‘sh’ ke liye honth thode aage karo aur bolo: sh, jaise ‘sher’ mein.",
            en = "For ‘sh’, push your lips forward a little and say ‘sh’, like asking for quiet.",
        ),
        SKILL_S_CLUSTER to L(
            hi = "शुरू में ‘इ’ मत लगाओ। सीधे ‘स्’ से शुरू करो: स्कूल, इस्कूल नहीं।",
            hinglish = "Shuru mein ‘i’ mat lagao. Seedhe ‘s’ se shuru karo: school, ischool nahi.",
            en = "Don't add ‘i’ at the start. Begin straight with ‘s’: school, not ischool.",
        ),
    )
}
