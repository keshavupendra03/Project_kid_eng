package com.samjhoenglish.learning

import com.samjhoenglish.i18n.L

/** A skill in words a parent understands, with something they can do at home. */
data class SkillInfo(val name: L, val tip: L)

/** Turns skill ids from the lessons ("sound.th") into plain language for the parent section. */
object SkillCatalog {
    /** Broad skills. They say less than specific ones, so they are only shown if nothing specific is weak. */
    val broad = setOf("listen.word", "read.word", "speak.word", "speak.sentence", "vocab.meaning", "phonics.blend")

    fun describe(skill: String): SkillInfo? = named[skill] ?: vocab(skill) ?: letterSound(skill)

    private fun info(hi: String, hinglish: String, en: String, tipHi: String, tipHinglish: String, tipEn: String) =
        SkillInfo(L(hi, hinglish, en), L(tipHi, tipHinglish, tipEn))

    private val vocabThemes = mapOf(
        "body" to L("शरीर", "Shareer", "Body"),
        "family" to L("परिवार", "Parivaar", "Family"),
        "home" to L("घर", "Ghar", "Home"),
        "school" to L("स्कूल", "School", "School"),
        "food" to L("खाने", "Khaane", "Food"),
        "animals" to L("जानवरों", "Jaanwaron", "Animal"),
        "colours" to L("रंगों", "Rangon", "Colour"),
        "numbers" to L("गिनती", "Ginti", "Number"),
        "clothes" to L("कपड़ों", "Kapdon", "Clothes"),
        "actions" to L("काम वाले", "Kaam waale", "Action"),
        "places" to L("जगहों", "Jagahon", "Place"),
        "nature" to L("बाहर की दुनिया", "Baahar ki duniya", "Nature"),
        "feelings" to L("मन की बातों", "Mann ki baaton", "Feeling"),
        "time" to L("समय", "Samay", "Time"),
        "opposites" to L("उल्टे", "Ulte", "Opposite"),
        "health" to L("सेहत", "Sehat", "Health"),
        "jobs" to L("लोगों के काम", "Logon ke kaam", "Job"),
        "vehicles" to L("गाड़ियों", "Gaadiyon", "Vehicle"),
        "manners" to L("अच्छी बातों", "Achhi baaton", "Polite"),
        "loanwords" to L("रोज़ के इंग्लिश", "Roz ke English", "Everyday English"),
    )

    private fun vocab(skill: String): SkillInfo? {
        val theme = vocabThemes[skill.removePrefix("vocab.")] ?: return null
        if (!skill.startsWith("vocab.")) return null
        return SkillInfo(
            L("${theme.hi} के शब्द", "${theme.hinglish} ke shabd", "${theme.en} words"),
            L(
                "रोज़ की बातचीत में ये शब्द इंग्लिश में पूछिए। चीज़ दिखाकर पूछना सबसे अच्छा है।",
                "Roz ki baatcheet mein yeh shabd English mein poochhiye. Cheez dikhakar poochhna sabse achha hai.",
                "Ask for these words in English during the day. Pointing at the real thing works best.",
            ),
        )
    }

    private fun letterSound(skill: String): SkillInfo? {
        val letters = skill.removePrefix("phonics.")
        if (!skill.startsWith("phonics.") || letters.length > 3) return null
        return SkillInfo(
            L("‘$letters’ की आवाज़", "‘$letters’ ki awaaz", "The ‘$letters’ sound"),
            L(
                "घर की चीज़ों के नाम में ‘$letters’ की आवाज़ ढूँढने का खेल खेलिए।",
                "Ghar ki cheezon ke naam mein ‘$letters’ ki awaaz dhoondhne ka khel kheliye.",
                "Play a game of finding the ‘$letters’ sound in the names of things at home.",
            ),
        )
    }

    private val named: Map<String, SkillInfo> = mapOf(
        // Pronunciation
        "sound.th" to info(
            "‘th’ बोलना", "‘th’ bolna", "Saying ‘th’",
            "शीशे के सामने three और thank you बुलवाइए। जीभ दाँतों के बीच दिखनी चाहिए।",
            "Sheeshe ke saamne three aur thank you bulwaiye. Jeebh daanton ke beech dikhni chahiye.",
            "Practise three and thank you at a mirror. The tongue should show between the teeth.",
        ),
        "sound.v" to info(
            "‘v’ बोलना", "‘v’ bolna", "Saying ‘v’",
            "van और very बुलवाइए। ऊपर के दाँत नीचे वाले होंठ को छूने चाहिए।",
            "van aur very bulwaiye. Upar ke daant neeche waale honth ko chhoone chahiye.",
            "Practise van and very. The top teeth should touch the bottom lip.",
        ),
        "sound.w" to info(
            "‘w’ बोलना", "‘w’ bolna", "Saying ‘w’",
            "water और web बुलवाइए। होंठ गोल हों, दाँत होंठ को न छुएँ।",
            "water aur web bulwaiye. Honth gol hon, daant honth ko na chhuein.",
            "Practise water and web. Lips round, teeth not touching the lip.",
        ),
        "sound.z" to info(
            "‘z’ बोलना", "‘z’ bolna", "Saying ‘z’",
            "zoo और zebra में मधुमक्खी जैसी आवाज़ करवाइए, ‘ज’ नहीं।",
            "zoo aur zebra mein madhumakkhi jaisi awaaz karwaiye, ‘ja’ nahi.",
            "In zoo and zebra, make a buzzing sound like a bee, not ‘j’.",
        ),
        "sound.f" to info(
            "‘f’ बोलना", "‘f’ bolna", "Saying ‘f’",
            "fish और five बुलवाइए। होंठ आपस में न मिलें।",
            "fish aur five bulwaiye. Honth aapas mein na milein.",
            "Practise fish and five. The lips should not touch each other.",
        ),
        "sound.sh" to info(
            "‘sh’ बोलना", "‘sh’ bolna", "Saying ‘sh’",
            "ship और shoe बुलवाइए। ‘स’ और ‘श’ का फ़र्क सुनाइए।",
            "ship aur shoe bulwaiye. ‘S’ aur ‘sh’ ka farq sunaiye.",
            "Practise ship and shoe, and listen for the difference between s and sh.",
        ),
        "sound.s_cluster" to info(
            "‘स्कूल’ को ‘इस्कूल’ बोलना", "‘School’ ko ‘ischool’ bolna", "Adding ‘i’ before s (ischool)",
            "school, star, spoon बुलवाइए। शुरू में ‘इ’ न लगे।",
            "school, star, spoon bulwaiye. Shuru mein ‘i’ na lage.",
            "Practise school, star and spoon with no ‘i’ at the start.",
        ),
        // Sentences
        "grammar.this_that" to info(
            "This / That", "This / That", "This / That",
            "चीज़ों की तरफ़ इशारा करके बुलवाइए: This is a pen. That is a door.",
            "Cheezon ki taraf ishaara karke bulwaiye: This is a pen. That is a door.",
            "Point at things and have your child say: This is a pen. That is a door.",
        ),
        "grammar.i_am" to info(
            "I am", "I am", "I am",
            "पूछिए ‘तुम कैसे हो?’ और जवाब इंग्लिश में दिलवाइए: I am happy.",
            "Poochhiye ‘Tum kaise ho?’ aur jawab English mein dilwaiye: I am happy.",
            "Ask how your child is, and get the answer in English: I am happy.",
        ),
        "grammar.he_she_it" to info(
            "He / She / It", "He / She / It", "He / She / It",
            "घर के लोगों के बारे में बुलवाइए: He is my brother. She is my sister.",
            "Ghar ke logon ke baare mein bulwaiye: He is my brother. She is my sister.",
            "Talk about family: He is my brother. She is my sister.",
        ),
        "grammar.we_you_they" to info(
            "We / You / They are", "We / You / They are", "We / You / They are",
            "बुलवाइए: We are friends. They are at home.",
            "Bulwaiye: We are friends. They are at home.",
            "Practise: We are friends. They are at home.",
        ),
        "grammar.articles" to info(
            "a / an / the", "a / an / the", "a / an / the",
            "फल दिखाकर बुलवाइए: a mango, an apple.",
            "Phal dikhakar bulwaiye: a mango, an apple.",
            "Show fruit and practise: a mango, an apple.",
        ),
        "grammar.have_has" to info(
            "have / has", "have / has", "have / has",
            "पूछिए ‘तुम्हारे पास क्या है?’ जवाब: I have a bag.",
            "Poochhiye ‘Tumhare paas kya hai?’ Jawab: I have a bag.",
            "Ask what your child has. Answer: I have a bag.",
        ),
        "grammar.like" to info(
            "I like / She likes", "I like / She likes", "I like / She likes",
            "खाने के समय पूछिए क्या पसंद है: I like rice.",
            "Khaane ke samay poochhiye kya pasand hai: I like rice.",
            "At mealtimes, ask what your child likes: I like rice.",
        ),
        "grammar.can" to info(
            "I can", "I can", "I can",
            "पूछिए क्या-क्या कर सकते हो: I can run. I can read.",
            "Poochhiye kya-kya kar sakte ho: I can run. I can read.",
            "Ask what your child can do: I can run. I can read.",
        ),
        "grammar.there_is" to info(
            "There is / There are", "There is / There are", "There is / There are",
            "कमरे की चीज़ें गिनते हुए बुलवाइए: There are two chairs.",
            "Kamre ki cheezein ginte hue bulwaiye: There are two chairs.",
            "Count things in the room: There are two chairs.",
        ),
        "grammar.where_prepositions" to info(
            "Where, in, on, under", "Where, in, on, under", "Where, in, on, under",
            "कोई चीज़ छिपाकर पूछिए: Where is the pen? जवाब: under the bed.",
            "Koi cheez chhipakar poochhiye: Where is the pen? Jawab: under the bed.",
            "Hide something and ask: Where is the pen? Answer: under the bed.",
        ),
        "grammar.want" to info(
            "I want", "I want", "I want",
            "कुछ माँगते समय बुलवाइए: I want water, please.",
            "Kuch maangte samay bulwaiye: I want water, please.",
            "When asking for something: I want water, please.",
        ),
        "grammar.not" to info(
            "not (नहीं)", "not (nahi)", "not",
            "बुलवाइए: I am not sad. It is not cold.",
            "Bulwaiye: I am not sad. It is not cold.",
            "Practise: I am not sad. It is not cold.",
        ),
        "grammar.yes_no_questions" to info(
            "Is it…? Are you…?", "Is it…? Are you…?", "Is it…? Are you…?",
            "छोटे सवाल पूछिए और पूछवाइए: Are you happy? Is it hot?",
            "Chhote sawaal poochhiye aur poochhwaiye: Are you happy? Is it hot?",
            "Take turns asking: Are you happy? Is it hot?",
        ),
        "grammar.wh_questions" to info(
            "What, Who, Where", "What, Who, Where", "What, Who, Where",
            "रोज़ एक सवाल पूछवाइए: What is your name?",
            "Roz ek sawaal poochhwaiye: What is your name?",
            "Have your child ask one question a day: What is your name?",
        ),
        "grammar.simple_present" to info(
            "रोज़ की बातें (He plays)", "Roz ki baatein (He plays)", "Every-day sentences (He plays)",
            "घर के लोगों के रोज़ के काम बुलवाइए: Papa reads. I play.",
            "Ghar ke logon ke roz ke kaam bulwaiye: Papa reads. I play.",
            "Talk about daily routines: Papa reads. I play.",
        ),
        "grammar.word_order" to info(
            "इंग्लिश में शब्दों का क्रम", "English mein shabdon ka kram", "English word order",
            "हिंदी की छोटी लाइन बोलकर पूछिए कि इंग्लिश में क्या होगा।",
            "Hindi ki chhoti line bolkar poochhiye ki English mein kya hoga.",
            "Say a short Hindi line and ask how it goes in English.",
        ),
        "grammar.spot_mistake" to info(
            "गलती पहचानना", "Galti pehchaanna", "Spotting mistakes",
            "जानबूझकर गलत लाइन बोलिए, जैसे ‘She are happy’, और गलती पकड़वाइए।",
            "Jaanboojhkar galat line boliye, jaise ‘She are happy’, aur galti pakadwaiye.",
            "Say a wrong line on purpose, like ‘She are happy’, and let your child catch it.",
        ),
        "grammar.uncountable" to info(
            "न गिने जाने वाले शब्द (bread, soap)", "Na gine jaane waale shabd (bread, soap)", "Words we don't count (bread, soap)",
            "रसोई की चीज़ों से पूछिए: a slice of bread, a cake of soap. ‘two breads’ नहीं, ‘two slices of bread’।",
            "Rasoi ki cheezon se poochhiye: a slice of bread, a cake of soap. ‘two breads’ nahi, ‘two slices of bread’.",
            "Use things in the kitchen: a slice of bread, a cake of soap. Not ‘two breads’ but ‘two slices of bread’.",
        ),
        "grammar.comparison" to info(
            "तुलना (tall, taller, tallest)", "Tulna (tall, taller, tallest)", "Comparing (tall, taller, tallest)",
            "घर की चीज़ों की तुलना करवाइए: This bag is bigger than that bag.",
            "Ghar ki cheezon ki tulna karwaiye: This bag is bigger than that bag.",
            "Compare things at home: This bag is bigger than that bag.",
        ),
        "grammar.relative" to info(
            "who / which से वाक्य जोड़ना", "who / which se vaakya jodna", "Joining sentences with who / which",
            "दो छोटी बातें जोड़कर बुलवाइए: The man who sells milk is kind.",
            "Do chhoti baatein jodkar bulwaiye: The man who sells milk is kind.",
            "Join two short ideas aloud: The man who sells milk is kind.",
        ),
        "grammar.should" to info(
            "should / should not (चाहिए)", "should / should not (chahiye)", "should / should not",
            "घर के नियम इंग्लिश में बुलवाइए: We should wash our hands. We should not waste water.",
            "Ghar ke niyam English mein bulwaiye: We should wash our hands. We should not waste water.",
            "Say house rules in English: We should wash our hands. We should not waste water.",
        ),
        "grammar.to_for" to info(
            "to / for का इस्तेमाल", "to / for ka istemaal", "Using to / for",
            "बुलवाइए: This gift is for you. I went to buy milk.",
            "Bulwaiye: This gift is for you. I went to buy milk.",
            "Practise aloud: This gift is for you. I went to buy milk.",
        ),
        "grammar.few_little" to info(
            "few / a few / little / a little", "few / a few / little / a little", "few / a few / little / a little",
            "रसोई में पूछिए: a little milk (गिन नहीं सकते), a few biscuits (गिन सकते हैं)।",
            "Rasoi mein poochhiye: a little milk (gin nahi sakte), a few biscuits (gin sakte hain).",
            "In the kitchen, ask: a little milk (can't count), a few biscuits (can count).",
        ),
        "grammar.did_questions" to info(
            "Did से सवाल बनाना", "Did se sawaal banaana", "Questions with Did",
            "कल की बातें पूछिए: Did you eat lunch? Did you play? जवाब पूरे वाक्य में दिलवाइए।",
            "Kal ki baatein poochhiye: Did you eat lunch? Did you play? Jawab poore vaakya mein dilwaiye.",
            "Ask about yesterday: Did you eat lunch? Did you play? Ask for a full-sentence answer.",
        ),
        "grammar.prefix_suffix" to info(
            "re, un, ful से नए शब्द", "re, un, ful se naye shabd", "New words with re, un, ful",
            "शब्द बोलिए और नया शब्द बनवाइए: happy → unhappy, write → rewrite, help → helpful।",
            "Shabd boliye aur naya shabd banwaiye: happy → unhappy, write → rewrite, help → helpful.",
            "Say a word and ask for a new one: happy → unhappy, write → rewrite, help → helpful.",
        ),
        "grammar.tenses" to info(
            "क्रिया का सही रूप (tense)", "Kriya ka sahi roop (tense)", "Using the right verb form (tenses)",
            "पूछिए कल क्या किया, अभी क्या कर रहे हो, कल क्या करोगे, और जवाब इंग्लिश में दिलवाइए।",
            "Poochhiye kal kya kiya, abhi kya kar rahe ho, kal kya karoge, aur jawab English mein dilwaiye.",
            "Ask what your child did yesterday, is doing now and will do tomorrow, and help them answer in English.",
        ),
        "grammar.indirect_speech" to info(
            "किसी की कही बात बताना (indirect speech)", "Kisi ki kahi baat bataana (indirect speech)", "Reporting what someone said (indirect speech)",
            "पूछिए: दादी ने क्या कहा? और इंग्लिश में बुलवाइए: Grandmother said that she was tired.",
            "Poochhiye: daadi ne kya kaha? Aur English mein bulwaiye: Grandmother said that she was tired.",
            "Ask: what did Grandmother say? Help your child answer: Grandmother said that she was tired.",
        ),
        "grammar.gender" to info(
            "he / she / it", "he / she / it", "he / she / it",
            "घर के लोगों और चीज़ों की ओर इशारा करके पूछिए: he, she या it?",
            "Ghar ke logon aur cheezon ki or ishaara karke poochhiye: he, she ya it?",
            "Point at people and things at home and ask: he, she or it?",
        ),
        "grammar.punctuation" to info(
            "विराम चिह्न (capital, full stop, comma)", "Viraam chihn (capital, full stop, comma)", "Punctuation (capitals, full stops, commas)",
            "बच्चे की कॉपी में देखिए: हर वाक्य बड़े अक्षर से शुरू और full stop पर ख़त्म हो।",
            "Bachche ki copy mein dekhiye: har vaakya bade akshar se shuru aur full stop par khatm ho.",
            "Check your child's notebook: every sentence should start with a capital and end with a full stop.",
        ),
        "grammar.too_so" to info(
            "too ... to / so ... that", "too ... to / so ... that", "too ... to / so ... that",
            "चीज़ें दिखाकर बुलवाइए: The box is too heavy to lift. = The box is so heavy that I cannot lift it.",
            "Cheezein dikhaakar bulwaiye: The box is too heavy to lift. = The box is so heavy that I cannot lift it.",
            "Show things and say: The box is too heavy to lift. = The box is so heavy that I cannot lift it.",
        ),
        "grammar.conjunctions" to info(
            "and / but / because से जोड़ना", "and / but / because se jodna", "Joining with and / but / because",
            "दो बातें जोड़कर बुलवाइए: I like tea but my sister likes milk.",
            "Do baatein jodkar bulwaiye: I like tea but my sister likes milk.",
            "Join two ideas aloud: I like tea but my sister likes milk.",
        ),
        "grammar.plurals" to info(
            "एक से अनेक (plurals)", "Ek se anek (plurals)", "Plurals (one and many)",
            "घर की चीज़ें गिनवाइए: one cup, two cups; one child, two children।",
            "Ghar ki cheezein ginwaiye: one cup, two cups; one child, two children.",
            "Count things at home: one cup, two cups; one child, two children.",
        ),
        "grammar.adverbs" to info(
            "काम कैसे हुआ (-ly शब्द)", "Kaam kaise hua (-ly shabd)", "Adverbs: how it was done (-ly)",
            "बच्चे से पूछिए काम कैसे किया: She sang beautifully. He ran quickly.",
            "Bachche se poochhiye kaam kaise kiya: She sang beautifully. He ran quickly.",
            "Ask how something was done: She sang beautifully. He ran quickly.",
        ),
        "grammar.contractions" to info(
            "छोटे रूप (I'm, don't)", "Chhote roop (I'm, don't)", "Short forms (I'm, don't)",
            "बोलचाल में छोटे रूप बुलवाइए: I'm hungry. I don't know.",
            "Bolchaal mein chhote roop bulwaiye: I'm hungry. I don't know.",
            "Use short forms when talking: I'm hungry. I don't know.",
        ),
        // School textbook chapters (what the teacher sees, section by section)
        "book.words" to info(
            "किताब के नए शब्द", "Kitaab ke naye shabd", "New words from the textbook",
            "पाठ के नए शब्द पूछिए और हर शब्द से एक छोटी लाइन बुलवाइए।",
            "Paath ke naye shabd poochhiye aur har shabd se ek chhoti line bulwaiye.",
            "Ask the chapter's new words and have your child make a short line with each.",
        ),
        "book.comprehension" to info(
            "पाठ समझकर जवाब देना", "Paath samajhkar jawab dena", "Answering questions on the chapter",
            "पाठ साथ में पढ़िए और पूछिए: क्या हुआ, क्यों हुआ?",
            "Paath saath mein padhiye aur poochhiye: kya hua, kyon hua?",
            "Read the chapter together and ask: what happened, and why?",
        ),
        "book.grammar" to info(
            "किताब की ग्रामर", "Kitaab ki grammar", "Textbook grammar",
            "पाठ के ‘Grammar in Use’ वाले उदाहरण बोलकर अपनी लाइनें बनवाइए।",
            "Paath ke ‘Grammar in Use’ waale udaharan bolkar apni lines banwaiye.",
            "Say the chapter's Grammar in Use examples aloud and make new lines together.",
        ),
        "book.talk" to info(
            "किताब के सवालों पर बोलना", "Kitaab ke sawaalon par bolna", "Speaking on the chapter's questions",
            "‘Let's Talk’ के सवाल इंग्लिश में पूछिए और जवाब इंग्लिश में दिलवाइए।",
            "‘Let's Talk’ ke sawaal English mein poochhiye aur jawab English mein dilwaiye.",
            "Ask the Let's Talk questions in English and help your child answer in English.",
        ),
        "book.writing" to info(
            "किताब के हिसाब से लिखना", "Kitaab ke hisaab se likhna", "Writing tasks from the textbook",
            "‘Let's Write’ वाले जवाब कॉपी में लिखवाइए और ज़ोर से पढ़वाइए।",
            "‘Let's Write’ waale jawab copy mein likhwaiye aur zor se padhwaiye.",
            "Have your child write the Let's Write answers in a notebook and read them aloud.",
        ),
        "book.activity" to info(
            "किताब की गतिविधियाँ", "Kitaab ki gatividhiyaan", "Textbook activities",
            "‘Let's Do It’ की गतिविधि घर पर साथ में करिए।",
            "‘Let's Do It’ ki gatividhi ghar par saath mein kariye.",
            "Do the chapter's Let's Do It activity together at home.",
        ),
        // Reading and sounds
        "phonics.blend" to info(
            "आवाज़ें जोड़कर पढ़ना", "Awaazein jodkar padhna", "Joining sounds to read",
            "तीन अक्षर के शब्द धीरे-धीरे पढ़वाइए: c-a-t, cat.",
            "Teen akshar ke shabd dheere-dheere padhwaiye: c-a-t, cat.",
            "Read three-letter words slowly together: c-a-t, cat.",
        ),
        "phonics.magic_e" to info(
            "जादुई e (kit, kite)", "Jaadui e (kit, kite)", "Magic e (kit, kite)",
            "जोड़ियाँ पढ़वाइए: kit-kite, tap-tape.",
            "Jodiyaan padhwaiye: kit-kite, tap-tape.",
            "Read pairs together: kit-kite, tap-tape.",
        ),
        "phonics.name_vs_sound" to info(
            "अक्षर का नाम और उसकी आवाज़", "Akshar ka naam aur uski awaaz", "Letter names and sounds",
            "पूछिए: ‘s’ का नाम क्या है और उसकी आवाज़ क्या है?",
            "Poochhiye: ‘s’ ka naam kya hai aur uski awaaz kya hai?",
            "Ask: what is the name of ‘s’, and what is its sound?",
        ),
        "phonics.oo_short" to info(
            "छोटा ‘oo’ (book)", "Chhota ‘oo’ (book)", "Short ‘oo’ (book)",
            "book और moon बुलवाकर फ़र्क सुनिए।",
            "book aur moon bulwaakar farq suniye.",
            "Say book and moon, and listen for the difference.",
        ),
        "phonics.zh" to info(
            "नरम ‘si’ (television)", "Naram ‘si’ (television)", "Soft ‘si’ (television)",
            "television धीरे-धीरे बुलवाइए।", "television dheere-dheere bulwaiye.", "Say television slowly together.",
        ),
        "phonics.schwa" to info(
            "हल्का ‘a’ (banana)", "Halka ‘a’ (banana)", "Soft ‘a’ (banana)",
            "banana और about बुलवाइए। ‘a’ बहुत हल्का बोला जाता है।",
            "banana aur about bulwaiye. ‘a’ bahut halka bola jaata hai.",
            "Say banana and about. The ‘a’ is very soft.",
        ),
        "read.sight_words" to info(
            "देखते ही पहचानने वाले शब्द", "Dekhte hi pehchaanne waale shabd", "Words to know on sight",
            "किताब में the, is, you ढूँढने का खेल खेलिए।",
            "Kitaab mein the, is, you dhoondhne ka khel kheliye.",
            "Play a game of finding the, is and you in a book.",
        ),
        "vocab.power" to info(
            "नए शब्द (शब्द शक्ति)", "Naye shabd (Word Power)", "New words (Word Power)",
            "रोज़ सीखे हुए एक शब्द से वाक्य बनवाइए। खाने के समय पूछिए: आज कौन सा नया शब्द सीखा?",
            "Roz seekhe hue ek shabd se vaakya banwaiye. Khaane ke samay poochhiye: aaj kaun sa naya shabd seekha?",
            "Ask for one sentence a day using a new word. At mealtimes, ask: which new word did you learn today?",
        ),
        "spell.word" to info(
            "स्पेलिंग", "Spelling", "Spelling",
            "शब्द बोलिए और बच्चे से हवा में या कागज़ पर उसकी स्पेलिंग लिखवाइए।",
            "Shabd boliye aur bachche se hawa mein ya kaagaz par uski spelling likhwaiye.",
            "Say a word and have your child spell it in the air or on paper.",
        ),
        "read.passage" to info(
            "कहानी पढ़ना", "Kahani padhna", "Reading a passage",
            "रोज़ पाँच मिनट एक छोटी कहानी ज़ोर से पढ़वाइए। गलती पर टोकिए मत, आख़िर में साथ पढ़िए।",
            "Roz paanch minute ek chhoti kahani zor se padhwaiye. Galti par tokiye mat, aakhir mein saath padhiye.",
            "Five minutes of reading a short story aloud every day. Don't interrupt mistakes; read it together at the end.",
        ),
        "read.understand" to info(
            "पढ़कर समझना", "Padhkar samajhna", "Understanding what is read",
            "कहानी के बाद पूछिए: क्या हुआ? कौन था? कहाँ था? जवाब हिंदी में भी चलेगा।",
            "Kahani ke baad poochhiye: kya hua? Kaun tha? Kahan tha? Jawab Hindi mein bhi chalega.",
            "After a story, ask: what happened? Who was it? Where? An answer in Hindi is fine too.",
        ),
        "read.word" to info(
            "इंग्लिश शब्द पढ़ना", "English shabd padhna", "Reading English words",
            "बोर्ड और पैकेट पर लिखे शब्द साथ में पढ़िए।",
            "Board aur packet par likhe shabd saath mein padhiye.",
            "Read words on signs and packets together.",
        ),
        "listen.word" to info(
            "इंग्लिश शब्द सुनकर समझना", "English shabd sunkar samajhna", "Understanding spoken words",
            "चीज़ का इंग्लिश नाम बोलिए और बच्चे से वह चीज़ लाने को कहिए।",
            "Cheez ka English naam boliye aur bachche se woh cheez laane ko kahiye.",
            "Say a thing's English name and ask your child to bring it.",
        ),
        "listen.minimal_pair" to info(
            "मिलती-जुलती आवाज़ें पहचानना", "Milti-julti awaazein pehchaanna", "Telling similar sounds apart",
            "जोड़ियाँ बोलकर पूछिए कौन सा शब्द था: thin-tin, vet-wet.",
            "Jodiyaan bolkar poochhiye kaun sa shabd tha: thin-tin, vet-wet.",
            "Say pairs and ask which word it was: thin-tin, vet-wet.",
        ),
        "speak.word" to info(
            "शब्द बोलना", "Shabd bolna", "Saying words",
            "रोज़ 5 शब्द ज़ोर से बुलवाइए। शांत जगह पर बोलने से मदद मिलती है।",
            "Roz 5 shabd zor se bulwaiye. Shaant jagah par bolne se madad milti hai.",
            "Say 5 words aloud each day. A quiet place helps.",
        ),
        "speak.sentence" to info(
            "पूरी लाइन बोलना", "Poori line bolna", "Saying whole sentences",
            "रोज़ एक छोटी लाइन साथ में बोलिए, जैसे I am happy.",
            "Roz ek chhoti line saath mein boliye, jaise I am happy.",
            "Say one short sentence together each day, like I am happy.",
        ),
        "notice.english_in_hindi" to info(
            "हिंदी में छिपे इंग्लिश शब्द", "Hindi mein chhipe English shabd", "English words inside Hindi",
            "बातचीत में इंग्लिश शब्द पकड़ने का खेल खेलिए: बस, स्कूल, फ़ोन।",
            "Baatcheet mein English shabd pakadne ka khel kheliye: bus, school, phone.",
            "Play a game of catching English words in everyday Hindi: bus, school, phone.",
        ),
        "vocab.meaning" to info(
            "शब्दों के मतलब", "Shabdon ke matlab", "Word meanings",
            "एक इंग्लिश शब्द बोलिए और उसका हिंदी मतलब पूछिए।",
            "Ek English shabd boliye aur uska Hindi matlab poochhiye.",
            "Say an English word and ask what it means in Hindi.",
        ),
    )
}
