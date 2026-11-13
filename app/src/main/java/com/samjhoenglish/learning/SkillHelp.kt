package com.samjhoenglish.learning

import com.samjhoenglish.i18n.L

/**
 * Teaching for a child who is stuck on a skill. [hint] is one short line shown after a wrong answer.
 * [explain] is a second explanation that takes a different route from the lesson's own:
 * something to do with the hands or mouth, or a link to Hindi the child already knows.
 */
data class SkillHelp(val hint: L?, val explain: L)

object SkillHelps {
    fun of(skill: String): SkillHelp? = named[skill] ?: book[skill] ?: vocab(skill) ?: letter(skill)

    private fun vocab(skill: String): SkillHelp? {
        if (!skill.startsWith("vocab.") || skill in SkillCatalog.broad) return null
        return SkillHelp(
            hint = null,
            explain = L(
                "एक खेल करो: घर में ये चीज़ें ढूँढो। हर चीज़ को हाथ लगाओ और उसका इंग्लिश नाम ज़ोर से बोलो।",
                "Ek khel karo: ghar mein yeh cheezein dhoondho. Har cheez ko haath lagao aur uska English naam zor se bolo.",
                "Play a game: find these things at home. Touch each one and say its English name out loud.",
            ),
        )
    }

    private fun letter(skill: String): SkillHelp? {
        val letters = skill.removePrefix("phonics.")
        if (!skill.startsWith("phonics.") || letters.length > 3) return null
        return SkillHelp(
            hint = null,
            explain = L(
                "एक खेल करो: उँगली से हवा में ‘$letters’ बड़ा-बड़ा लिखो और साथ में उसकी आवाज़ बोलो। तीन बार करो।",
                "Ek khel karo: ungli se hawa mein ‘$letters’ bada-bada likho aur saath mein uski awaaz bolo. Teen baar karo.",
                "Play a game: write a big ‘$letters’ in the air with your finger, and make its sound as you write. Do it three times.",
            ),
        )
    }

    private fun help(hi: String, hinglish: String, en: String, hi2: String, hinglish2: String, en2: String) =
        SkillHelp(L(hi, hinglish, en), L(hi2, hinglish2, en2))

    /** A second explanation with no short hint (the exercise's own hint is used after a wrong answer). */
    private fun explainOnly(hi: String, hinglish: String, en: String) = SkillHelp(null, L(hi, hinglish, en))

    // Textbook chapter sections. Their wrong-answer hints come from the exercise itself.
    private val book: Map<String, SkillHelp> = mapOf(
        "book.words" to explainOnly(
            "हर नए शब्द से अपनी ज़िंदगी की एक लाइन बनाओ, जैसे: I gobble my lunch when I am hungry. अपनी बनाई लाइन याद रहती है!",
            "Har naye shabd se apni zindagi ki ek line banao, jaise: I gobble my lunch when I am hungry. Apni banaayi line yaad rehti hai!",
            "Make a line about your own life with each new word, like: I gobble my lunch when I am hungry. A line you make yourself is easy to remember!",
        ),
        "book.comprehension" to explainOnly(
            "कहानी फिर से पढ़ो, पर हर पैराग्राफ़ के बाद रुककर खुद से पूछो: कौन? क्या किया? क्यों? सवाल का मुख्य शब्द (जैसे trunk या water) कहानी में ढूँढो। जवाब उसी लाइन के पास होता है।",
            "Kahani phir se padho, par har paragraph ke baad rukkar khud se poochho: kaun? kya kiya? kyon? Sawaal ka mukhya shabd (jaise trunk ya water) kahani mein dhoondho. Jawab usi line ke paas hota hai.",
            "Read the story again, but stop after each paragraph and ask: who? what did they do? why? Find the question's key word (like trunk or water) in the story. The answer is near that line.",
        ),
        "book.grammar" to explainOnly(
            "किताब के उदाहरण ज़ोर से तीन बार बोलो। फिर उसी ढाँचे में अपनी एक लाइन बनाओ, सिर्फ़ एक शब्द बदलकर: a slice of bread → a slice of cake।",
            "Kitaab ke udaharan zor se teen baar bolo. Phir usi dhaanche mein apni ek line banao, sirf ek shabd badalkar: a slice of bread → a slice of cake.",
            "Say the book's examples aloud three times. Then make your own line in the same pattern, changing just one word: a slice of bread → a slice of cake.",
        ),
        "book.talk" to explainOnly(
            "पहले जवाब हिंदी में सोचो, फिर छोटा इंग्लिश जवाब बोलो: Yes, I would. या No, I would not. छोटा जवाब भी सही जवाब है!",
            "Pehle jawab Hindi mein socho, phir chhota English jawab bolo: Yes, I would. Ya No, I would not. Chhota jawab bhi sahi jawab hai!",
            "Think of the answer first, then say a short English answer: Yes, I would. Or: No, I would not. A short answer is still a right answer!",
        ),
        "book.writing" to explainOnly(
            "लिखने से पहले नोट्स के हर हिस्से से एक लाइन बनाओ: home : trees → He lives on the trees. फिर सारी लाइनें जोड़ दो, पैराग्राफ़ बन जाएगा।",
            "Likhne se pehle notes ke har hisse se ek line banao: home : trees → He lives on the trees. Phir saari lines jod do, paragraph ban jaayega.",
            "Before writing, turn each part of the notes into one line: home : trees → He lives on the trees. Then put the lines together and you have a paragraph.",
        ),
        "book.activity" to explainOnly(
            "हर जानवर के बारे में दो सवाल पूछो: यह कहाँ रहता है, पानी में, जंगल में या हमारे घर में? क्या यह उड़ता है? जवाब से ग्रुप पता चल जाएगा।",
            "Har jaanwar ke baare mein do sawaal poochho: yeh kahan rehta hai, paani mein, jangal mein ya hamare ghar mein? Kya yeh udta hai? Jawab se group pata chal jaayega.",
            "Ask two questions about each animal: where does it live, in water, in the forest or in our homes? Does it fly? The answers tell you its group.",
        ),
    )

    private val named: Map<String, SkillHelp> = mapOf(
        // Sentences
        "grammar.this_that" to help(
            "पास की चीज़ = This, दूर की चीज़ = That।",
            "Paas ki cheez = This, door ki cheez = That.",
            "Near = This, far = That.",
            "एक खेल करो: पास की चीज़ पर हाथ रखो और बोलो This. अब दूर की चीज़ की तरफ़ उँगली करो और बोलो That. हाथ लगे तो This, न लगे तो That!",
            "Ek khel karo: paas ki cheez par haath rakho aur bolo This. Ab door ki cheez ki taraf ungli karo aur bolo That. Haath lage to This, na lage to That!",
            "Play a game: put your hand on something near and say This. Now point at something far and say That. If you can touch it, This. If not, That!",
        ),
        "grammar.i_am" to help(
            "‘I’ के साथ हमेशा ‘am’ आता है।",
            "‘I’ ke saath hamesha ‘am’ aata hai.",
            "‘I’ always goes with ‘am’.",
            "हिंदी में ‘मैं’ के साथ ‘हूँ’ आता है। ठीक वैसे ही इंग्लिश में ‘I’ के साथ ‘am’ आता है। मैं हूँ = I am. ये दोनों पक्के दोस्त हैं!",
            "Hindi mein ‘main’ ke saath ‘hoon’ aata hai. Theek waise hi English mein ‘I’ ke saath ‘am’ aata hai. Main hoon = I am. Yeh dono pakke dost hain!",
            "‘I’ and ‘am’ are best friends and always go together. Say it three times: I am, I am, I am!",
        ),
        "grammar.he_she_it" to help(
            "लड़का = He, लड़की = She, चीज़ या जानवर = It।",
            "Ladka = He, ladki = She, cheez ya jaanwar = It.",
            "Boy = He, girl = She, thing or animal = It.",
            "अपने घर को सोचो। पापा या भाई की बात हो तो He. मम्मी या बहन की बात हो तो She. पंखे या बिल्ली की बात हो तो It. तीनों के साथ ‘is’ आता है।",
            "Apne ghar ko socho. Papa ya bhai ki baat ho to He. Mummy ya behen ki baat ho to She. Pankhe ya billi ki baat ho to It. Teeno ke saath ‘is’ aata hai.",
            "Think of your home. For father or brother, He. For mother or sister, She. For a fan or a cat, It. All three go with ‘is’.",
        ),
        "grammar.we_you_they" to help(
            "We, You, They के साथ हमेशा ‘are’ आता है।",
            "We, You, They ke saath hamesha ‘are’ aata hai.",
            "We, You and They always go with ‘are’.",
            "हिंदी में याद करो: हम हैं, तुम हो, वे हैं। इंग्लिश में तीनों के लिए एक ही शब्द है, ‘are’. We are, You are, They are.",
            "Hindi mein yaad karo: hum hain, tum ho, woh hain. English mein teeno ke liye ek hi shabd hai, ‘are’. We are, You are, They are.",
            "English has just one word for all three: ‘are’. We are, You are, They are. Say them while pointing at your friends!",
        ),
        "grammar.articles" to help(
            "शब्द की पहली आवाज़ अ, ए, इ, ओ, उ जैसी हो तो ‘an’, वरना ‘a’।",
            "Shabd ki pehli awaaz a, e, i, o, u jaisi ho to ‘an’, warna ‘a’.",
            "If the word starts with a vowel sound, use ‘an’. Otherwise ‘a’.",
            "शब्द को ज़ोर से बोलो और पहली आवाज़ सुनो। apple की पहली आवाज़ ‘ऐ’ है, इसलिए an apple. mango की पहली आवाज़ ‘म’ है, इसलिए a mango.",
            "Shabd ko zor se bolo aur pehli awaaz suno. Apple ki pehli awaaz ‘ae’ hai, isliye an apple. Mango ki pehli awaaz ‘m’ hai, isliye a mango.",
            "Say the word out loud and listen to its first sound. Apple starts with a vowel sound, so an apple. Mango starts with ‘m’, so a mango.",
        ),
        "grammar.have_has" to help(
            "I, We, You, They के साथ have. He, She, It के साथ has.",
            "I, We, You, They ke saath have. He, She, It ke saath has.",
            "I, We, You, They go with have. He, She, It go with has.",
            "एक आसान तरीका: जहाँ ‘is’ आता है वहाँ ‘has’ आएगा। She is, तो She has. जहाँ ‘am’ या ‘are’ आता है वहाँ ‘have’. I am, तो I have.",
            "Ek aasaan tareeka: jahan ‘is’ aata hai wahan ‘has’ aayega. She is, to She has. Jahan ‘am’ ya ‘are’ aata hai wahan ‘have’. I am, to I have.",
            "An easy way: where ‘is’ goes, ‘has’ goes. She is, so She has. Where ‘am’ or ‘are’ goes, ‘have’ goes. I am, so I have.",
        ),
        "grammar.like" to help(
            "He या She के साथ अंत में s लगता है: She likes.",
            "He ya She ke saath ant mein s lagta hai: She likes.",
            "With He or She, add s at the end: She likes.",
            "दो लोगों को सोचो। तुम बोलो: I like mango. अब दोस्त के बारे में बोलो: Ravi likes mango. एक इंसान के नाम के साथ ‘likes’ में एक s जुड़ गया।",
            "Do logon ko socho. Tum bolo: I like mango. Ab dost ke baare mein bolo: Ravi likes mango. Ek insaan ke naam ke saath ‘likes’ mein ek s jud gaya.",
            "Think of two people. You say: I like mango. Now about a friend: Ravi likes mango. With one person's name, ‘likes’ gets an s.",
        ),
        "grammar.can" to help(
            "can के बाद का शब्द वैसा ही रहता है: She can run.",
            "Can ke baad ka shabd waisa hi rehta hai: She can run.",
            "The word after ‘can’ stays the same: She can run.",
            "‘can’ मतलब ‘सकना’। हिंदी में ‘सकना’ अंत में आता है, पर इंग्लिश में पहले: दौड़ सकते हैं = can run. अब बोलकर दौड़ने का नाटक करो: I can run!",
            "‘Can’ matlab ‘sakna’. Hindi mein ‘sakna’ ant mein aata hai, par English mein pehle: daud sakte hain = can run. Ab bolkar daudne ka naatak karo: I can run!",
            "‘Can’ comes first, then the action. Pretend to run and say: I can run! Pretend to jump and say: I can jump!",
        ),
        "grammar.there_is" to help(
            "एक चीज़ = There is, एक से ज़्यादा = There are.",
            "Ek cheez = There is, ek se zyada = There are.",
            "One thing = There is, more than one = There are.",
            "कमरे में देखो और गिनो। एक ही पंखा है? There is a fan. दो कुर्सियाँ हैं? There are two chairs. हिंदी की तरह: है = is, हैं = are.",
            "Kamre mein dekho aur gino. Ek hi pankha hai? There is a fan. Do kursiyan hain? There are two chairs. Hindi ki tarah: hai = is, hain = are.",
            "Look around the room and count. Just one fan? There is a fan. Two chairs? There are two chairs.",
        ),
        "grammar.where_prepositions" to help(
            "in = अंदर, on = ऊपर, under = नीचे।",
            "In = andar, on = upar, under = neeche.",
            "in = inside, on = on top, under = below.",
            "अपना पेन लेकर करके देखो। बैग के अंदर रखो और बोलो in the bag. मेज़ के ऊपर रखो और बोलो on the table. मेज़ के नीचे रखो और बोलो under the table.",
            "Apna pen lekar karke dekho. Bag ke andar rakho aur bolo in the bag. Mez ke upar rakho aur bolo on the table. Mez ke neeche rakho aur bolo under the table.",
            "Take your pen and try it. Put it inside your bag and say in the bag. Put it on the table and say on the table. Put it below the table and say under the table.",
        ),
        "grammar.want" to help(
            "चाहिए = want. मुझे पानी चाहिए = I want water.",
            "Chahiye = want. Mujhe paani chahiye = I want water.",
            "Say ‘I want’, then the thing: I want water.",
            "हिंदी में ‘चाहिए’ अंत में आता है। इंग्लिश में पहले I want, फिर चीज़। मुझे पानी चाहिए = I want water. साथ में please बोलो तो और अच्छा!",
            "Hindi mein ‘chahiye’ ant mein aata hai. English mein pehle I want, phir cheez. Mujhe paani chahiye = I want water. Saath mein please bolo to aur achha!",
            "First say I want, then the thing: I want water. Add please and it's even better!",
        ),
        "grammar.not" to help(
            "‘not’ हमेशा am, is, are के बाद आता है।",
            "‘Not’ hamesha am, is, are ke baad aata hai.",
            "‘not’ always comes after am, is or are.",
            "हिंदी में ‘नहीं’ लाइन के अंत के पास आता है: मैं खुश नहीं हूँ। इंग्लिश में पहले am, फिर not: I am not happy. सिर हिलाकर ‘ना’ करते हुए बोलो!",
            "Hindi mein ‘nahi’ line ke ant ke paas aata hai: Main khush nahi hoon. English mein pehle am, phir not: I am not happy. Sir hilakar ‘na’ karte hue bolo!",
            "First am, then not: I am not happy. Shake your head as you say it!",
        ),
        "grammar.yes_no_questions" to help(
            "सवाल में Is या Are सबसे पहले आता है।",
            "Sawaal mein Is ya Are sabse pehle aata hai.",
            "In a question, Is or Are comes first.",
            "लाइन को पलट दो! She is happy का सवाल बनेगा Is she happy? ‘is’ आगे आ गया। You are good का सवाल बनेगा Are you good?",
            "Line ko palat do! She is happy ka sawaal banega Is she happy? ‘Is’ aage aa gaya. You are good ka sawaal banega Are you good?",
            "Flip the line! She is happy becomes Is she happy? The word ‘is’ jumped to the front. You are good becomes Are you good?",
        ),
        "grammar.wh_questions" to help(
            "What = क्या, Who = कौन, Where = कहाँ।",
            "What = kya, Who = kaun, Where = kahan.",
            "What is for things, Who for people, Where for places.",
            "हिंदी में ‘क्या’ लाइन के अंत में आता है: तुम्हारा नाम क्या है? इंग्लिश में What सबसे पहले: What is your name?",
            "Hindi mein ‘kya’ line ke ant mein aata hai: Tumhara naam kya hai? English mein What sabse pehle: What is your name?",
            "The question word comes first in English: What is your name? Where is my bag? Who is he?",
        ),
        "grammar.uncountable" to help(
            "bread, soap में s नहीं लगता। गिनना हो तो: a slice of bread.",
            "bread, soap mein s nahi lagta. Ginna ho to: a slice of bread.",
            "Bread and soap don't take s. To count them: a slice of bread.",
            "हिंदी में भी हम ‘दो पानी’ नहीं बोलते, ‘दो गिलास पानी’ बोलते हैं। इंग्लिश में भी ऐसा ही है: two slices of bread, a cake of soap, a sheet of paper। पहले बर्तन या टुकड़ा, फिर of, फिर चीज़।",
            "Hindi mein bhi hum ‘do paani’ nahi bolte, ‘do gilaas paani’ bolte hain. English mein bhi aisa hi hai: two slices of bread, a cake of soap, a sheet of paper. Pehle bartan ya tukda, phir of, phir cheez.",
            "We don't say ‘two waters’, we say ‘two glasses of water’. It's the same with bread and soap: two slices of bread, a cake of soap, a sheet of paper. First the piece, then of, then the thing.",
        ),
        "grammar.comparison" to help(
            "दो की तुलना में -er और than, सब में -est: taller than, the tallest.",
            "Do ki tulna mein -er aur than, sab mein -est: taller than, the tallest.",
            "Two things: -er and than. All of them: the -est.",
            "तीन पेंसिल लो: छोटी, बड़ी, सबसे बड़ी। बोलो: This pencil is long. That one is longer. This is the longest! दो की बात में -er, सब में -est।",
            "Teen pencil lo: chhoti, badi, sabse badi. Bolo: This pencil is long. That one is longer. This is the longest! Do ki baat mein -er, sab mein -est.",
            "Take three pencils: short, long and longest. Say: This pencil is long. That one is longer. This is the longest! Two things use -er; all of them use -est.",
        ),
        "grammar.relative" to help(
            "लोगों के लिए who, जानवर और चीज़ों के लिए which।",
            "Logon ke liye who, jaanwar aur cheezon ke liye which.",
            "who for people, which for animals and things.",
            "दो वाक्यों में जो इंसान दोनों में है, उसके बाद ‘who’ लगाओ और दूसरी बात वहीं जोड़ दो। The boy is tall. The boy plays cricket. → The boy who plays cricket is tall. ‘who’ = ‘जो’।",
            "Do vaakyon mein jo insaan dono mein hai, uske baad ‘who’ lagao aur doosri baat wahin jod do. The boy is tall. The boy plays cricket. → The boy who plays cricket is tall. ‘who’ = ‘jo’.",
            "Find the person who is in both sentences. Put ‘who’ after them and add the other idea right there. The boy is tall. The boy plays cricket. → The boy who plays cricket is tall.",
        ),
        "grammar.should" to help(
            "should = चाहिए, should not = नहीं चाहिए। should के बाद ‘to’ नहीं लगता।",
            "should = chahiye, should not = nahi chahiye. should ke baad ‘to’ nahi lagta.",
            "should = right to do; should not = not right. No ‘to’ after should.",
            "अपने स्कूल के नियम सोचो। ‘हमें समय पर आना चाहिए’ = We should come on time. ‘हमें झूठ नहीं बोलना चाहिए’ = We should not tell lies. चाहिए = should!",
            "Apne school ke niyam socho. ‘Humein samay par aana chahiye’ = We should come on time. ‘Humein jhooth nahi bolna chahiye’ = We should not tell lies. Chahiye = should!",
            "Think of your school rules. We should come on time. We should not tell lies. Good things to do use should; things to avoid use should not.",
        ),
        "grammar.to_for" to help(
            "काम वाले शब्द से पहले to (to buy), किसी के लिए for (for you)।",
            "Kaam waale shabd se pehle to (to buy), kisi ke liye for (for you).",
            "to before an action word (to buy); for someone (for you).",
            "सोचो: ‘करने के लिए’ = to + काम (to eat, to play)। ‘किसी के लिए’ या ‘किसी के बदले’ = for (for my mother, for ten rupees)। काम है तो to, इंसान या चीज़ है तो for!",
            "Socho: ‘karne ke liye’ = to + kaam (to eat, to play). ‘Kisi ke liye’ ya ‘kisi ke badle’ = for (for my mother, for ten rupees). Kaam hai to to, insaan ya cheez hai to for!",
            "Think: to do something = to + action (to eat, to play). For a person or in exchange for a thing = for (for my mother, for ten rupees). An action takes to; a person or thing takes for!",
        ),
        "grammar.few_little" to help(
            "गिन सकते हो तो few, नहीं गिन सकते तो little।",
            "Gin sakte ho to few, nahi gin sakte to little.",
            "Can count it? few. Can't count it? little.",
            "पानी गिन नहीं सकते, तो a little water। पेंसिल गिन सकते हैं, तो a few pencils। और ‘a’ लगा हो तो मतलब ‘थोड़ा है’; ‘a’ न हो तो मतलब ‘लगभग नहीं है’।",
            "Paani gin nahi sakte, to a little water. Pencil gin sakte hain, to a few pencils. Aur ‘a’ laga ho to matlab ‘thoda hai’; ‘a’ na ho to matlab ‘lagbhag nahi hai’.",
            "You can't count water, so a little water. You can count pencils, so a few pencils. With ‘a’ it means there is some; without ‘a’ it means almost none.",
        ),
        "grammar.did_questions" to help(
            "Did पहले, फिर काम वाला शब्द सीधे रूप में: Did he go?",
            "Did pehle, phir kaam waala shabd seedhe roop mein: Did he go?",
            "Did first, then the base verb: Did he go?",
            "Did अपने साथ बीता हुआ समय ले आता है, इसलिए बाद वाला शब्द सीधा रहता है। He went. → Did he go? (went नहीं, go)। She played. → Did she play?",
            "Did apne saath beeta hua samay le aata hai, isliye baad waala shabd seedha rehta hai. He went. → Did he go? (went nahi, go). She played. → Did she play?",
            "Did already carries the past, so the next verb stays plain. He went. → Did he go? (not went). She played. → Did she play?",
        ),
        "grammar.prefix_suffix" to help(
            "re = फिर से, un = नहीं, ful = भरा हुआ।",
            "re = phir se, un = nahi, ful = bhara hua.",
            "re = again, un = not, ful = full of.",
            "तीन छोटे जादुई टुकड़े याद करो: re (फिर से) → retell = फिर से बताना। un (नहीं) → unkind = दयालु नहीं। ful (भरा) → careful = सावधानी से भरा।",
            "Teen chhote jaadui tukde yaad karo: re (phir se) → retell = phir se bataana. un (nahi) → unkind = dayaalu nahi. ful (bhara) → careful = saavdhaani se bhara.",
            "Remember three little magic parts: re (again) → retell = tell again. un (not) → unkind = not kind. ful (full of) → careful = full of care.",
        ),
        "grammar.tenses" to help(
            "पहले देखो: काम कब हुआ? बीता, चल रहा, या आगे होगा?",
            "Pehle dekho: kaam kab hua? Beeta, chal raha, ya aage hoga?",
            "First ask: when? Past, going on, or future?",
            "समय की एक लाइन सोचो: कल (played, ran), अभी (is playing), कल आने वाला (will play)। वाक्य में ‘yesterday’, ‘now’, ‘tomorrow’ जैसे शब्द ढूँढो, वही बताते हैं कौन सा रूप लगेगा।",
            "Samay ki ek line socho: kal (played, ran), abhi (is playing), kal aane waala (will play). Vaakya mein ‘yesterday’, ‘now’, ‘tomorrow’ jaise shabd dhoondho, wahi bataate hain kaun sa roop lagega.",
            "Picture a time line: yesterday (played, ran), now (is playing), tomorrow (will play). Look for words like yesterday, now and tomorrow in the sentence. They tell you which form to use.",
        ),
        "grammar.indirect_speech" to help(
            "कही हुई बात बताते समय समय पीछे जाता है: is → was, will → would।",
            "Kahi hui baat bataate samay samay peechhe jaata hai: is → was, will → would.",
            "When reporting, the time moves back: is → was, will → would.",
            "सोचो तुम कल की बात किसी और को बता रहे हो। गीता ने कहा था ‘मैं जा रही हूँ’ → Geeta said that she was going। I बदलकर she, am बदलकर was, क्योंकि बात बीत चुकी है।",
            "Socho tum kal ki baat kisi aur ko bata rahe ho. Geeta ne kaha tha ‘main ja rahi hoon’ → Geeta said that she was going. I badalkar she, am badalkar was, kyonki baat beet chuki hai.",
            "Imagine telling someone else what was said yesterday. Geeta said, ‘I am going’ → Geeta said that she was going. I becomes she, am becomes was, because it is in the past now.",
        ),
        "grammar.gender" to help(
            "लड़का/आदमी = he, लड़की/औरत = she, चीज़ = it।",
            "Ladka/aadmi = he, ladki/aurat = she, cheez = it.",
            "Boy or man = he, girl or woman = she, thing = it.",
            "अपने घर को सोचो: Papa = he, Mummy = she, TV = it। जो लड़का या लड़की दोनों हो सकते हैं, जैसे teacher या baby, उनके लिए he या she दोनों ठीक हैं।",
            "Apne ghar ko socho: Papa = he, Mummy = she, TV = it. Jo ladka ya ladki dono ho sakte hain, jaise teacher ya baby, unke liye he ya she dono theek hain.",
            "Think of your home: Papa = he, Mummy = she, TV = it. Words like teacher or baby can be a boy or a girl, so he or she both work.",
        ),
        "grammar.punctuation" to help(
            "वाक्य बड़े अक्षर से शुरू, full stop पर ख़त्म। I हमेशा बड़ा।",
            "Vaakya bade akshar se shuru, full stop par khatm. I hamesha bada.",
            "Start with a capital, end with a full stop. I is always capital.",
            "ज़ोर से पढ़ो: जहाँ आवाज़ पूरी रुकती है वहाँ full stop (.), जहाँ थोड़ा रुकती है वहाँ comma (,)। और हर नए वाक्य का पहला अक्षर बड़ा लिखो।",
            "Zor se padho: jahan awaaz poori rukti hai wahan full stop (.), jahan thoda rukti hai wahan comma (,). Aur har naye vaakya ka pehla akshar bada likho.",
            "Read it aloud: where your voice stops fully, put a full stop. Where it pauses a little, put a comma. And start every new sentence with a capital letter.",
        ),
        "grammar.too_so" to help(
            "too ... to = इतना कि नहीं हो सकता = so ... that ... cannot।",
            "too ... to = itna ki nahi ho sakta = so ... that ... cannot.",
            "too ... to = so ... that ... cannot.",
            "‘too’ में ‘नहीं’ छिपा होता है। Too hot to drink = इतनी गर्म कि पी नहीं सकते। इसलिए so ... that वाले वाक्य में cannot लिखना मत भूलो।",
            "‘too’ mein ‘nahi’ chhipa hota hai. Too hot to drink = itni garm ki pee nahi sakte. Isliye so ... that waale vaakya mein cannot likhna mat bhoolo.",
            "‘too’ hides a ‘not’ inside it. Too hot to drink = so hot that you cannot drink it. So don't forget ‘cannot’ in the so ... that sentence.",
        ),
        "grammar.conjunctions" to help(
            "मिलती बातें = and (और), उल्टी बातें = but (लेकिन)।",
            "Milti baatein = and (aur), ulti baatein = but (lekin).",
            "Similar ideas: and. Opposite ideas: but.",
            "दोनों बातें पढ़ो। अगर दोनों एक जैसी हैं (मुझे आम पसंद है, केले भी) तो and। अगर दूसरी बात पहली से उल्टी है (मुझे आम पसंद है, पर केले नहीं) तो but।",
            "Dono baatein padho. Agar dono ek jaisi hain (mujhe aam pasand hai, kele bhi) to and. Agar doosri baat pehli se ulti hai (mujhe aam pasand hai, par kele nahi) to but.",
            "Read both ideas. If they agree (I like mangoes, and bananas too), use and. If the second is the opposite (I like mangoes, but not bananas), use but.",
        ),
        "grammar.plurals" to help(
            "ज़्यादातर में s, s/sh/ch पर es, व्यंजन+y पर ies।",
            "Zyaadatar mein s, s/sh/ch par es, vyanjan+y par ies.",
            "Most add s; s/sh/ch add es; consonant + y becomes ies.",
            "शब्द का आख़िरी अक्षर देखो। bus में आख़िर में s है, तो buses (बोलने में ‘इज़’ जुड़ता है)। baby में y से पहले b है, तो babies। boy में y से पहले o है, तो बस boys।",
            "Shabd ka aakhiri akshar dekho. bus mein aakhir mein s hai, to buses (bolne mein ‘iz’ judta hai). baby mein y se pehle b hai, to babies. boy mein y se pehle o hai, to bas boys.",
            "Look at the last letter. bus ends in s, so buses (you hear an extra ‘iz’). In baby, y comes after b, so babies. In boy, y comes after o, so just boys.",
        ),
        "grammar.adverbs" to help(
            "काम कैसे हुआ? विशेषण + ly: quick → quickly।",
            "Kaam kaise hua? Visheshan + ly: quick → quickly.",
            "How was it done? Adjective + ly: quick → quickly.",
            "पहले सोचो वाक्य में क्या हुआ और कैसे हुआ। ख़ुश होकर गया → happily। ज़ोर से बोला → loudly। happy जैसे y वाले शब्द में y की जगह i: happily।",
            "Pehle socho vaakya mein kya hua aur kaise hua. Khush hokar gaya → happily. Zor se bola → loudly. happy jaise y waale shabd mein y ki jagah i: happily.",
            "First think what happened and how. Went in a happy way → happily. Spoke in a loud way → loudly. In words like happy, y becomes i: happily.",
        ),
        "grammar.contractions" to help(
            "’ बताता है कि अक्षर हटे हैं: do not → don't।",
            "’ bataata hai ki akshar hate hain: do not → don't.",
            "The ’ shows missing letters: do not → don't.",
            "छोटा रूप वहीं बनता है जहाँ से अक्षर हटे: I am में a हटा → I'm। do not में o हटा → don't। हटे अक्षर की जगह ’ लगता है।",
            "Chhota roop wahin banta hai jahan se akshar hate: I am mein a hata → I'm. do not mein o hata → don't. Hate akshar ki jagah ’ lagta hai.",
            "The short form puts ’ where letters were dropped: I am drops a → I'm. do not drops o → don't.",
        ),
        "grammar.simple_present" to help(
            "He या She के साथ काम वाले शब्द में s लगता है: He plays.",
            "He ya She ke saath kaam waale shabd mein s lagta hai: He plays.",
            "With He or She, the action word gets an s: He plays.",
            "अपने घर के लोगों को सोचो। I play. You play. लेकिन एक इंसान की बात हो तो s लगाओ: Papa plays. Didi reads. अंत की आवाज़ कान से सुनो!",
            "Apne ghar ke logon ko socho. I play. You play. Lekin ek insaan ki baat ho to s lagao: Papa plays. Didi reads. Ant ki awaaz kaan se suno!",
            "Think of your family. I play. You play. But when it's about one person, add s: Papa plays. Didi reads. Listen for the sound at the end!",
        ),
        "grammar.word_order" to help(
            "इंग्लिश में: पहले कौन, फिर काम, फिर बाकी।",
            "English mein: pehle kaun, phir kaam, phir baaki.",
            "In English: first who, then the action, then the rest.",
            "हिंदी में काम वाला शब्द अंत में आता है: मैं चावल खाता हूँ। इंग्लिश में वह बीच में कूद जाता है: I eat rice. कौन, काम, बाकी!",
            "Hindi mein kaam waala shabd ant mein aata hai: Main chawal khaata hoon. English mein woh beech mein kood jaata hai: I eat rice. Kaun, kaam, baaki!",
            "In English, the action word goes in the middle: I eat rice. Who, action, the rest!",
        ),
        "grammar.spot_mistake" to help(
            "I के साथ am, He या She के साथ is, बाकी के साथ are.",
            "I ke saath am, He ya She ke saath is, baaki ke saath are.",
            "I goes with am, He or She with is, the others with are.",
            "जासूस बन जाओ! हर शब्द पर उँगली रखकर धीरे-धीरे पढ़ो। जहाँ कान को अजीब लगे, वहीं गलती छिपी है।",
            "Jaasoos ban jao! Har shabd par ungli rakhkar dheere-dheere padho. Jahan kaan ko ajeeb lage, wahin galti chhipi hai.",
            "Be a detective! Put your finger on each word and read slowly. Where it sounds strange, the mistake is hiding.",
        ),
        // Pronunciation
        "sound.th" to help(
            "जीभ दाँतों के बीच रखकर हवा छोड़ो।",
            "Jeebh daanton ke beech rakhkar hawa chhodo.",
            "Put your tongue between your teeth and blow.",
            "शीशा लो। जीभ की नोक दाँतों के बीच रखो, थोड़ी बाहर दिखे। अब हवा छोड़ो। ‘ट’ या ‘थ’ नहीं निकलना चाहिए। अब three बोलो, जीभ दिखनी चाहिए!",
            "Sheesha lo. Jeebh ki nok daanton ke beech rakho, thodi baahar dikhe. Ab hawa chhodo. ‘T’ ya ‘th’ wali Hindi awaaz nahi nikalni chahiye. Ab three bolo, jeebh dikhni chahiye!",
            "Take a mirror. Put the tip of your tongue between your teeth so it shows. Now blow. Say three, and your tongue should show!",
        ),
        "sound.v" to help(
            "ऊपर के दाँत नीचे वाले होंठ पर रखो।",
            "Upar ke daant neeche waale honth par rakho.",
            "Rest your top teeth on your bottom lip.",
            "ऊपर के दाँतों से नीचे वाला होंठ हल्का सा दबाओ और आवाज़ निकालो। होंठ पर उँगली रखो, झनझनाहट महसूस होगी। अब van बोलो।",
            "Upar ke daanton se neeche waala honth halka sa dabao aur awaaz nikalo. Honth par ungli rakho, jhanjhanahat mehsoos hogi. Ab van bolo.",
            "Press your top teeth gently on your bottom lip and make a sound. Touch your lip and feel it buzz. Now say van.",
        ),
        "sound.w" to help(
            "होंठ गोल करो, जैसे सीटी बजानी हो।",
            "Honth gol karo, jaise seeti bajani ho.",
            "Make your lips round, as if to whistle.",
            "पहले होंठ गोल करो जैसे ‘ऊ’ बोलना हो, फिर खोल दो। दाँत होंठ को न छुएँ। अब water बोलो।",
            "Pehle honth gol karo jaise ‘oo’ bolna ho, phir khol do. Daant honth ko na chhuein. Ab water bolo.",
            "First make your lips round as if saying ‘oo’, then open them. Your teeth shouldn't touch your lip. Now say water.",
        ),
        "sound.z" to help(
            "मधुमक्खी जैसी आवाज़ करो: zzz, ‘ज’ नहीं।",
            "Madhumakkhi jaisi awaaz karo: zzz, ‘j’ nahi.",
            "Buzz like a bee: zzz, not ‘j’.",
            "दाँत हल्के से मिलाओ और मधुमक्खी बन जाओ: zzzz. गले पर हाथ रखो, झनझनाहट होगी। अब zoo बोलो।",
            "Daant halke se milao aur madhumakkhi ban jao: zzzz. Gale par haath rakho, jhanjhanahat hogi. Ab zoo bolo.",
            "Bring your teeth together and buzz like a bee: zzzz. Touch your throat and feel it buzz. Now say zoo.",
        ),
        "sound.f" to help(
            "ऊपर के दाँत नीचे वाले होंठ पर रखकर हवा छोड़ो।",
            "Upar ke daant neeche waale honth par rakhkar hawa chhodo.",
            "Top teeth on your bottom lip, and blow.",
            "‘प’ बोलने में दोनों होंठ मिलते हैं, पर ‘f’ में नहीं। ऊपर के दाँत नीचे वाले होंठ पर रखो और हवा छोड़ो। हथेली मुँह के सामने रखो, हवा लगेगी। अब fish बोलो।",
            "‘P’ bolne mein dono honth milte hain, par ‘f’ mein nahi. Upar ke daant neeche waale honth par rakho aur hawa chhodo. Hatheli munh ke saamne rakho, hawa lagegi. Ab fish bolo.",
            "For ‘p’ your lips touch, but not for ‘f’. Put your top teeth on your bottom lip and blow. Hold your palm in front and feel the air. Now say fish.",
        ),
        "sound.sh" to help(
            "होंठ आगे करो, जैसे चुप कराते हुए: shh.",
            "Honth aage karo, jaise chup karate hue: shh.",
            "Push your lips forward, as if saying shh for quiet.",
            "मुँह पर उँगली रखकर ‘शशश’ बोलो, जैसे किसी को चुप कराते हो। यही sh है। ‘स’ में हवा पतली निकलती है, sh में होंठ आगे आते हैं। अब ship बोलो।",
            "Munh par ungli rakhkar ‘shhh’ bolo, jaise kisi ko chup karate ho. Yahi sh hai. ‘S’ mein hawa patli nikalti hai, sh mein honth aage aate hain. Ab ship bolo.",
            "Put a finger on your lips and say shhh, as if asking for quiet. That is sh. Now say ship.",
        ),
        "sound.s_cluster" to help(
            "सीधे ‘स’ से शुरू करो, आगे ‘इ’ मत लगाओ।",
            "Seedhe ‘s’ se shuru karo, aage ‘i’ mat lagao.",
            "Start straight with ‘s’. Don't add ‘i’ before it.",
            "धीरे बोलो: sss, फिर kool. बीच में कोई ‘इ’ नहीं। sss-kool, अब जल्दी से: school. अब star बोलो: sss-tar.",
            "Dheere bolo: sss, phir kool. Beech mein koi ‘i’ nahi. Sss-kool, ab jaldi se: school. Ab star bolo: sss-tar.",
            "Say it slowly: sss, then kool. No ‘i’ at the start. Sss-kool, now quickly: school. Now say star: sss-tar.",
        ),
        // Listening and reading
        "listen.minimal_pair" to help(
            "दोनों शब्दों की पहली आवाज़ पर ध्यान दो।",
            "Dono shabdon ki pehli awaaz par dhyaan do.",
            "Listen closely to the first sound of each word.",
            "आँखें बंद करके सुनो। ये दोनों शब्द लगभग एक जैसे हैं, बस एक आवाज़ अलग है। पहले वह आवाज़ खुद बोलो, फिर शब्द सुनो।",
            "Aankhein band karke suno. Yeh dono shabd lagbhag ek jaise hain, bas ek awaaz alag hai. Pehle woh awaaz khud bolo, phir shabd suno.",
            "Close your eyes and listen. These two words are almost the same, just one sound is different. Say that sound yourself first, then listen.",
        ),
        "phonics.magic_e" to help(
            "अंत का e चुप रहता है, पर बीच वाला अक्षर अपना नाम बोलता है।",
            "Ant ka e chup rehta hai, par beech waala akshar apna naam bolta hai.",
            "The e at the end is silent, but the middle letter says its name.",
            "kit और kite देखो। अंत का e खुद नहीं बोलता, पर जादू कर देता है: i अपना नाम बोलने लगता है, ‘आइ’. kit से kite, tap से tape.",
            "Kit aur kite dekho. Ant ka e khud nahi bolta, par jaadu kar deta hai: i apna naam bolne lagta hai, ‘aai’. Kit se kite, tap se tape.",
            "Look at kit and kite. The e at the end stays silent, but does magic: the i says its name. Kit becomes kite, tap becomes tape.",
        ),
        "phonics.name_vs_sound" to help(
            "अक्षर का नाम अलग होता है, उसकी आवाज़ अलग।",
            "Akshar ka naam alag hota hai, uski awaaz alag.",
            "A letter's name and its sound are different.",
            "जैसे तुम्हारा नाम अलग है और तुम्हारी आवाज़ अलग, वैसे ही अक्षर का भी। ‘s’ का नाम ‘एस’ है, पर शब्दों में वह ‘स्’ बोलता है।",
            "Jaise tumhara naam alag hai aur tumhari awaaz alag, waise hi akshar ka bhi. ‘S’ ka naam ‘es’ hai, par shabdon mein woh ‘ss’ bolta hai.",
            "You have a name and a voice, and so does a letter. The letter s is called ‘ess’, but in words it says ‘sss’.",
        ),
        "phonics.oo_short" to help(
            "book में oo छोटा बोलो, moon में लंबा।",
            "Book mein oo chhota bolo, moon mein lamba.",
            "Say oo short in book, and long in moon.",
            "दोनों बोलकर देखो। moon: आवाज़ लंबी, ‘ऊ’ जैसी। book: आवाज़ छोटी, ‘उ’ जैसी। ताली बजाते हुए बोलो, moon पर लंबी, book पर छोटी।",
            "Dono bolkar dekho. Moon: awaaz lambi, ‘oo’ jaisi. Book: awaaz chhoti, ‘u’ jaisi. Taali bajate hue bolo, moon par lambi, book par chhoti.",
            "Try saying both. Moon has a long sound. Book has a short sound. Clap as you say them: a long clap for moon, a short one for book.",
        ),
        "phonics.zh" to help(
            "television में ‘si’ की आवाज़ नरम होती है।",
            "Television mein ‘si’ ki awaaz naram hoti hai.",
            "The ‘si’ in television has a soft sound.",
            "sh बोलो और साथ में गले से आवाज़ निकालो, यही नरम आवाज़ है। television को टुकड़ों में बोलो: te-le-vi-sion, फिर पूरा शब्द।",
            "Sh bolo aur saath mein gale se awaaz nikalo, yahi naram awaaz hai. Television ko tukdon mein bolo: te-le-vi-sion, phir poora shabd.",
            "Say sh and add your voice: that's the soft sound. Say television in parts: te-le-vi-sion, then the whole word.",
        ),
        "phonics.schwa" to help(
            "banana में पहला और आखिरी a बहुत हल्का बोलो।",
            "Banana mein pehla aur aakhiri a bahut halka bolo.",
            "In banana, the first and last a are very soft.",
            "banana को तीन टुकड़ों में बोलो: ba-NA-na. बीच वाला टुकड़ा ज़ोर से, बाकी दो बहुत हल्के। ताली भी बीच वाले पर ही बजाओ।",
            "Banana ko teen tukdon mein bolo: ba-NA-na. Beech waala tukda zor se, baaki do bahut halke. Taali bhi beech waale par hi bajao.",
            "Say banana in three parts: ba-NA-na. The middle part is strong, the other two are very soft. Clap only on the middle one.",
        ),
        "vocab.power" to help(
            "वाक्य को पूरा पढ़ो। कौन सा शब्द सही बैठता है?",
            "Vaakya ko poora padho. Kaun sa shabd sahi baithta hai?",
            "Read the whole sentence. Which word fits?",
            "एक शब्द याद करने की ट्रिक: उसका मतलब सोचो, फिर अपने घर या स्कूल की कोई बात उससे जोड़ो। जैसे ‘borrow’: मैंने भाई की पेंसिल borrow की। अपना वाक्य बोलो, शब्द पक्का हो जाएगा।",
            "Ek shabd yaad karne ki trick: uska matlab socho, phir apne ghar ya school ki koi baat usse jodo. Jaise ‘borrow’: maine bhai ki pencil borrow ki. Apna vaakya bolo, shabd pakka ho jaayega.",
            "A trick to remember a word: think of its meaning, then link it to something from your home or school. Like ‘borrow’: I borrowed my brother's pencil. Say your own sentence and the word will stick.",
        ),
        "spell.word" to help(
            "शब्द को धीरे बोलो और हर आवाज़ का अक्षर सोचो।",
            "Shabd ko dheere bolo aur har awaaz ka akshar socho.",
            "Say the word slowly and think of a letter for each sound.",
            "शब्द को टुकड़ों में तोड़ो: um-brel-la. हर टुकड़े की स्पेलिंग अलग से लिखो, फिर जोड़ दो। मुश्किल शब्द हवा में उँगली से तीन बार लिखो।",
            "Shabd ko tukdon mein todo: um-brel-la. Har tukde ki spelling alag se likho, phir jod do. Mushkil shabd hawa mein ungli se teen baar likho.",
            "Break the word into parts: um-brel-la. Spell each part, then join them. Write a hard word in the air three times.",
        ),
        "read.passage" to help(
            "उँगली शब्द पर रखो, धीरे पढ़ो। मुश्किल शब्द को छूकर सुनो।",
            "Ungli shabd par rakho, dheere padho. Mushkil shabd ko chhookar suno.",
            "Put your finger on the word and read slowly. Tap a hard word to hear it.",
            "पढ़ना दौड़ नहीं है। पहले मुझे सुनो, फिर एक-एक लाइन मेरे साथ बोलो। जो शब्द अटके, उसे छूकर सुनो और फिर बोलो। हर बार थोड़ा और आसान लगेगा।",
            "Padhna daud nahi hai. Pehle mujhe suno, phir ek-ek line mere saath bolo. Jo shabd atke, use chhookar suno aur phir bolo. Har baar thoda aur aasaan lagega.",
            "Reading is not a race. Listen to me first, then say each line with me. Tap any word that stops you, hear it, and say it. It gets a little easier every time.",
        ),
        "read.understand" to help(
            "जवाब कहानी में छिपा है। उस लाइन को फिर से पढ़ो।",
            "Jawab kahani mein chhipa hai. Us line ko phir se padho.",
            "The answer is hiding in the story. Read that line again.",
            "जासूस बनो! सवाल में जो शब्द है, वही शब्द कहानी में ढूँढो। जवाब उसके आस-पास ही मिलेगा।",
            "Jaasoos bano! Sawaal mein jo shabd hai, wahi shabd kahani mein dhoondho. Jawab uske aas-paas hi milega.",
            "Be a detective! Find the word from the question inside the story. The answer is right next to it.",
        ),
        "read.sight_words" to help(
            "इस शब्द को फ़ोटो की तरह पहचानो। पहला अक्षर देखो।",
            "Is shabd ko photo ki tarah pehchaano. Pehla akshar dekho.",
            "Know this word like a photo. Look at its first letter.",
            "कुछ शब्दों को जोड़कर नहीं, देखकर पहचानते हैं। इस शब्द को उँगली से हवा में तीन बार लिखो और हर बार बोलो।",
            "Kuch shabdon ko jodkar nahi, dekhkar pehchaante hain. Is shabd ko ungli se hawa mein teen baar likho aur har baar bolo.",
            "Some words you know by looking at them. Write this word in the air three times, and say it each time.",
        ),
        "notice.english_in_hindi" to help(
            "बस, स्कूल, फ़ोन जैसे शब्द ढूँढो।",
            "Bus, school, phone jaise shabd dhoondho.",
            "Look for words like bus, school and phone.",
            "तुम रोज़ बहुत सारी इंग्लिश बोलते हो! बस, टीवी, फ़ोन, स्कूल, पेन, ये सब इंग्लिश के शब्द हैं। अपने दिन की बातों में ऐसे शब्द ढूँढो।",
            "Tum roz bahut saari English bolte ho! Bus, TV, phone, school, pen, yeh sab English ke shabd hain. Apne din ki baaton mein aise shabd dhoondho.",
            "You speak a lot of English every day! Bus, TV, phone, school and pen are all English words. Look for them in your day.",
        ),
    )
}
