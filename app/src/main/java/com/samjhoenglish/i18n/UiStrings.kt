package com.samjhoenglish.i18n

/**
 * All app UI text. Follow the Hindi style guide in PLAN.md (section 4):
 * everyday spoken Hindi, "तुम" for the child, "आप" for parents, short sentences.
 * Lesson text lives in assets/content, not here.
 */
object UiStrings {
    val appName = L(
        hi = "समझो इंग्लिश",
        hinglish = "Samjho English",
        en = "Samjho English",
    )
    val tagline = L(
        hi = "रट्टा नहीं, समझ!",
        hinglish = "Ratta nahi, samajh!",
        en = "Don't memorise, understand!",
    )

    // Language picker. Each language option is always shown in its own language.
    const val CHOOSE_LANGUAGE_ALL = "भाषा चुनो  •  Bhasha chuno  •  Choose a language"
    const val CHOOSE_LANGUAGE_SPOKEN = "भाषा चुनो"
    const val HINDI_OPTION = "मैं हिंदी में समझाऊँगा"
    const val HINGLISH_OPTION = "Main Hinglish mein samjhaunga"
    const val HINGLISH_OPTION_SPOKEN = "मैं हिंग्लिश में समझाऊँगा"
    const val ENGLISH_OPTION = "I will explain in English"
    val letsGo = L(
        hi = "चलो आगे!",
        hinglish = "Chalo aage!",
        en = "Let's go!",
    )

    // Home
    val tinkuIntro = L(
        hi = "नमस्ते! मैं टिंकू हूँ। चलो, साथ में इंग्लिश सीखते हैं!",
        hinglish = "Namaste! Main Tinku hoon. Chalo, saath mein English seekhte hain!",
        en = "Hello! I am Tinku. Let's learn English together!",
    )
    val todayTask = L(
        hi = "आज का काम",
        hinglish = "Aaj ka kaam",
        en = "Today's lesson",
    )
    val parentsComingSoon = L(
        hi = "यह हिस्सा मम्मी-पापा के लिए है। यह जल्दी आ रहा है!",
        hinglish = "Yeh hissa Mummy-Papa ke liye hai. Yeh jaldi aa raha hai!",
        en = "This part is for parents. It is coming soon!",
    )
    val yourJourney = L(
        hi = "तुम्हारा सफ़र",
        hinglish = "Tumhara safar",
        en = "Your journey",
    )
    val locked = L(
        hi = "पहले पिछला लेवल पूरा करो",
        hinglish = "Pehle pichhla level poora karo",
        en = "Finish the level before this first",
    )
    val lockedSay = L(
        hi = "यह लेवल अभी बंद है। पहले पिछला लेवल पूरा करो!",
        hinglish = "Yeh level abhi band hai. Pehle pichhla level poora karo!",
        en = "This level is locked. Finish the level before it first!",
    )
    val levelComingSoon = L(
        hi = "यह लेवल अभी बन रहा है। जल्दी आएगा!",
        hinglish = "Yeh level abhi ban raha hai. Jaldi aayega!",
        en = "This level is still being made. Coming soon!",
    )
    val comingSoonShort = L(hi = "जल्दी आ रहा है", hinglish = "Jaldi aa raha hai", en = "Coming soon")
    val lockedLessonSay = L(
        hi = "यह लेसन अभी बंद है। पहले पिछला लेसन पूरा करो!",
        hinglish = "Yeh lesson abhi band hai. Pehle pichhla lesson poora karo!",
        en = "This lesson is locked. Finish the one before it first!",
    )
    val level = L(hi = "लेवल", hinglish = "Level", en = "Level")
    val lesson = L(hi = "लेसन", hinglish = "Lesson", en = "Lesson")

    // Lesson player
    val next = L(hi = "आगे चलो", hinglish = "Aage chalo", en = "Next")
    val listenAndPick = L(
        hi = "सुनो और सही तस्वीर चुनो",
        hinglish = "Suno aur sahi tasveer chuno",
        en = "Listen and pick the right picture",
    )
    val pickWord = L(
        hi = "तस्वीर देखो और सही इंग्लिश शब्द चुनो",
        hinglish = "Tasveer dekho aur sahi English shabd chuno",
        en = "Look at the picture and pick the right English word",
    )
    val findEnglish = L(
        hi = "इस लाइन में इंग्लिश के सारे शब्द छुओ",
        hinglish = "Is line mein English ke saare shabd chhuo",
        en = "This is a Hindi sentence. Tap all the English words in it",
    )
    val inHindi = L(hi = "हिंदी में", hinglish = "Hindi mein", en = "In Hindi")
    val tapToListen = L(hi = "छूकर सुनो", hinglish = "Chhookar suno", en = "Tap to listen")
    val praise = listOf(
        L(hi = "शाबाश!", hinglish = "Shabaash!", en = "Well done!"),
        L(hi = "बहुत बढ़िया!", hinglish = "Bahut badhiya!", en = "Great job!"),
        L(hi = "एकदम सही!", hinglish = "Ekdum sahi!", en = "Perfect!"),
        L(hi = "कमाल कर दिया!", hinglish = "Kamaal kar diya!", en = "Amazing!"),
    )
    val tryAgain = listOf(
        L(hi = "कोई बात नहीं, फिर से करो!", hinglish = "Koi baat nahi, phir se karo!", en = "No problem, try again!"),
        L(hi = "अरे, एक बार और कोशिश करो!", hinglish = "Arre, ek baar aur koshish karo!", en = "Oops, try one more time!"),
    )
    val revealed = L(
        hi = "सही जवाब यह है। अगली बार पक्का!",
        hinglish = "Sahi jawab yeh hai. Agli baar pakka!",
        en = "This is the right answer. You'll get it next time!",
    )
    val lessonDone = L(hi = "लेसन पूरा! 🎉", hinglish = "Lesson poora! 🎉", en = "Lesson complete! 🎉")
    val lessonDoneSay = L(
        hi = "शाबाश! तुमने लेसन पूरा कर लिया!",
        hinglish = "Shabaash! Tumne lesson poora kar liya!",
        en = "Well done! You finished the lesson!",
    )

    /** Format with (rightFirstTime, totalQuestions). */
    val firstTryScore = L(
        hi = "%2\$d में से %1\$d सवाल पहली बार में सही!",
        hinglish = "%2\$d mein se %1\$d sawaal pehli baar mein sahi!",
        en = "%1\$d of %2\$d questions right on the first try!",
    )
    val playAgain = L(hi = "फिर से करो", hinglish = "Phir se karo", en = "Play again")
    val goHome = L(hi = "होम पर चलो", hinglish = "Home par chalo", en = "Go home")
    val leaveTitle = L(
        hi = "लेसन बीच में छोड़ना है?",
        hinglish = "Lesson beech mein chhodna hai?",
        en = "Leave this lesson?",
    )
    val leaveYes = L(hi = "हाँ, बाद में करेंगे", hinglish = "Haan, baad mein karenge", en = "Yes, later")
    val leaveNo = L(hi = "नहीं, लेसन करते हैं", hinglish = "Nahi, lesson karte hain", en = "No, let's continue")
    val loadError = L(
        hi = "अरे! यह लेसन खुल नहीं पाया।",
        hinglish = "Arre! Yeh lesson khul nahi paaya.",
        en = "Oops! This lesson could not open.",
    )

    // Speaking and phonics
    val sayThis = L(hi = "अब तुम बोलो", hinglish = "Ab tum bolo", en = "Now you say it")
    val saySentence = L(hi = "यह लाइन बोलो", hinglish = "Yeh line bolo", en = "Say this sentence")
    val tapMic = L(hi = "🎤 दबाओ और बोलो", hinglish = "🎤 dabao aur bolo", en = "Tap 🎤 and speak")
    val micStarting = L(hi = "एक सेकंड… 🎤", hinglish = "Ek second… 🎤", en = "One second… 🎤")
    val listening = L(
        hi = "अब बोलो! 👂 बोलकर 🎤 फिर से दबा सकते हो",
        hinglish = "Ab bolo! 👂 Bolkar 🎤 phir se daba sakte ho",
        en = "Speak now! 👂 Tap 🎤 again when you're done",
    )
    val notHeard = L(
        hi = "कुछ सुनाई नहीं दिया। 🎤 दबाकर ज़ोर से बोलो!",
        hinglish = "Kuch sunaai nahi diya. 🎤 dabakar zor se bolo!",
        en = "I couldn't hear you. Tap 🎤 and speak louder!",
    )
    val noInternet = L(
        hi = "बोलने वाले सवालों के लिए इंटरनेट चाहिए। इंटरनेट चेक करो।",
        hinglish = "Bolne waale sawaalon ke liye internet chahiye. Internet check karo.",
        en = "Speaking questions need the internet. Please check it.",
    )
    val micUnavailable = L(
        hi = "इस फ़ोन में आवाज़ पहचानने वाला ऐप नहीं मिला। अभी इसे छोड़ दो।",
        hinglish = "Is phone mein awaaz pehchaanne waala app nahi mila. Abhi ise chhod do.",
        en = "This phone can't recognise speech. Skip this one for now.",
    )
    val heard = L(hi = "मैंने सुना", hinglish = "Maine suna", en = "I heard")
    val skipSpeaking = L(
        hi = "अभी नहीं बोल सकते? छोड़ो",
        hinglish = "Abhi nahi bol sakte? Chhodo",
        en = "Can't speak now? Skip",
    )
    val skipped = L(
        hi = "कोई बात नहीं, अगली बार बोलेंगे!",
        hinglish = "Koi baat nahi, agli baar bolenge!",
        en = "No problem, we'll speak next time!",
    )
    val micPermission = L(
        hi = "टिंकू को तुम्हारी आवाज़ सुननी है! नीचे वाला बटन दबाओ और ‘Allow’ चुनो।",
        hinglish = "Tinku ko tumhari awaaz sunni hai! Neeche waala button dabao aur ‘Allow’ chuno.",
        en = "Tinku wants to hear your voice! Tap the button below and choose ‘Allow’.",
    )
    val micPermissionButton = L(hi = "माइक चालू करो", hinglish = "Mic chaalu karo", en = "Turn on the mic")
    val micDenied = L(
        hi = "माइक बंद है। मम्मी-पापा से फ़ोन की सेटिंग्स में चालू करवाओ।",
        hinglish = "Mic band hai. Mummy-Papa se phone ki settings mein chaalu karwao.",
        en = "The mic is off. Ask a parent to turn it on in the phone settings.",
    )
    val whichWord = L(hi = "सुनो, कौन सा शब्द है?", hinglish = "Suno, kaun sa shabd hai?", en = "Listen. Which word is it?")
    val blendThis = L(
        hi = "हर अक्षर छूकर उसकी आवाज़ सुनो। फिर पूरा शब्द बोलो!",
        hinglish = "Har akshar chhookar uski awaaz suno. Phir poora shabd bolo!",
        en = "Tap each letter to hear its sound. Then say the whole word!",
    )
    val tapLetter = L(
        hi = "👆 अक्षर छूकर आवाज़ सुनो",
        hinglish = "👆 Akshar chhookar awaaz suno",
        en = "👆 Tap the letter to hear its sound",
    )
    val soundsLike = L(hi = "हिंदी में", hinglish = "Hindi mein", en = "Like Hindi")

    // Words and sentences
    val pickMeaning = L(hi = "इस शब्द का मतलब चुनो", hinglish = "Is shabd ka matlab chuno", en = "Pick what this word means in Hindi")
    val listenRead = L(hi = "सुनो और सही शब्द चुनो", hinglish = "Suno aur sahi shabd chuno", en = "Listen and pick the right word")
    val trainInstruction = L(
        hi = "हिंदी लाइन को इंग्लिश ट्रेन में बदलो! डिब्बे सही क्रम में लगाओ।",
        hinglish = "Hindi line ko English train mein badlo! Dibbe sahi kram mein lagao.",
        en = "Turn the Hindi line into an English train! Put the coaches in the right order.",
    )
    val roleWho = L(hi = "कौन", hinglish = "Kaun", en = "Who")
    val roleAction = L(hi = "काम / है", hinglish = "Kaam / hai", en = "Action / is")
    val roleRest = L(hi = "बाकी", hinglish = "Baaki", en = "The rest")
    val check = L(hi = "चेक करो", hinglish = "Check karo", en = "Check")
    val fillBlank = L(hi = "खाली जगह में सही शब्द लगाओ", hinglish = "Khaali jagah mein sahi shabd lagao", en = "Fill the gap with the right word")
    val spotMistake = L(
        hi = "गलती ढूँढो! जो शब्द गलत है, उसे छुओ 🔍",
        hinglish = "Galti dhoondho! Jo shabd galat hai, use chhuo 🔍",
        en = "Find the mistake! Tap the wrong word 🔍",
    )

    // Parents: shown on Home when the phone lacks a text-to-speech voice. Addressed to adults ("आप").
    val voiceMissing = L(
        hi = "📢 इस फ़ोन में हिंदी या इंग्लिश की आवाज़ नहीं है, इसलिए टिंकू बोल नहीं पाएगा।",
        hinglish = "📢 Is phone mein Hindi ya English ki awaaz nahi hai, isliye Tinku bol nahi paayega.",
        en = "📢 This phone is missing the Hindi or English voice, so Tinku can't speak.",
    )
    val voiceMissingButton = L(hi = "आवाज़ डाउनलोड करें", hinglish = "Awaaz download karein", en = "Download the voice")

    // Profiles
    val setupHello = L(
        hi = "हाय! मैं टिंकू हूँ। पहले थोड़ी जान-पहचान कर लेते हैं!",
        hinglish = "Hi! Main Tinku hoon. Pehle thodi jaan-pehchaan kar lete hain!",
        en = "Hi! I'm Tinku. Let's get to know each other first!",
    )
    val whatsYourName = L(hi = "तुम्हारा नाम क्या है?", hinglish = "Tumhara naam kya hai?", en = "What is your name?")
    val typeName = L(hi = "यहाँ नाम लिखो", hinglish = "Yahan naam likho", en = "Type your name")
    val pickBuddy = L(hi = "अपना दोस्त चुनो", hinglish = "Apna dost chuno", en = "Pick your buddy")
    val whichClass = L(hi = "तुम कौन सी क्लास में हो?", hinglish = "Tum kaun si class mein ho?", en = "Which class are you in?")
    val whoIsLearning = L(hi = "आज कौन पढ़ेगा?", hinglish = "Aaj kaun padhega?", en = "Who is learning today?")
    val addChild = L(hi = "नया बच्चा जोड़ें", hinglish = "Naya bachcha jodein", en = "Add a child")
    val streakDays = L(hi = "दिन लगातार", hinglish = "din lagaataar", en = "day streak")

    // Placement game
    val placementIntro = L(
        hi = "चलो एक छोटा सा खेल खेलते हैं, ताकि मुझे पता चले तुम्हें कितनी इंग्लिश आती है। गलत हो जाए तो कोई बात नहीं!",
        hinglish = "Chalo ek chhota sa khel khelte hain, taaki mujhe pata chale tumhe kitni English aati hai. Galat ho jaaye to koi baat nahi!",
        en = "Let's play a small game so I can see how much English you know. It's okay to get things wrong!",
    )
    val placementStart = L(hi = "खेल शुरू करो", hinglish = "Khel shuru karo", en = "Start the game")
    val placementSkip = L(hi = "शुरू से सीखना है", hinglish = "Shuru se seekhna hai", en = "Start from the beginning")

    /** Format with (levelNumber, levelTitle). */
    val placementResult = L(
        hi = "बढ़िया! तुम्हारा सफ़र लेवल %1\$d से शुरू होगा: %2\$s",
        hinglish = "Badhiya! Tumhara safar Level %1\$d se shuru hoga: %2\$s",
        en = "Great! Your journey starts at Level %1\$d: %2\$s",
    )
    val letsBegin = L(hi = "चलो शुरू करें!", hinglish = "Chalo shuru karein!", en = "Let's begin!")

    // Today's task and revision
    val revisionPlusLesson = L(
        hi = "पुराने सवालों की प्रैक्टिस + नया लेसन",
        hinglish = "Purane sawaalon ki practice + naya lesson",
        en = "Quick revision + a new lesson",
    )
    val allDoneRevise = L(
        hi = "सब लेसन पूरे! अब रिविज़न करो 🔁",
        hinglish = "Sab lesson poore! Ab revision karo 🔁",
        en = "All lessons done! Time to revise 🔁",
    )
    val reviewBadge = L(hi = "🔁 पुरानी प्रैक्टिस", hinglish = "🔁 Purani practice", en = "🔁 Quick revision")
    val nextLesson = L(hi = "अगला लेसन", hinglish = "Agla lesson", en = "Next lesson")
    val tryOnceMore = L(
        hi = "एक बार और करो, इस बार और भी अच्छा होगा!",
        hinglish = "Ek baar aur karo, is baar aur bhi achha hoga!",
        en = "Try once more. It will go even better this time!",
    )
    val revisionDone = L(hi = "रिविज़न पूरा! 🎉", hinglish = "Revision poora! 🎉", en = "Revision done! 🎉")
    val nothingToRevise = L(
        hi = "अभी रिविज़न के लिए कुछ नहीं है। कल फिर आना!",
        hinglish = "Abhi revision ke liye kuch nahi hai. Kal phir aana!",
        en = "Nothing to revise right now. Come back tomorrow!",
    )

    // Settings
    val settings = L(hi = "सेटिंग्स", hinglish = "Settings", en = "Settings")
    val appLanguage = L(
        hi = "ऐप की भाषा",
        hinglish = "App ki bhasha",
        en = "App language",
    )
    val forParents = L(
        hi = "मम्मी-पापा के लिए",
        hinglish = "Mummy-Papa ke liye",
        en = "For parents",
    )
    val back = L(hi = "वापस", hinglish = "Wapas", en = "Back")
    val englishVoiceTitle = L(hi = "इंग्लिश बोलने वाली आवाज़", hinglish = "English bolne waali awaaz", en = "English voice")
    val voiceAmerican = L(
        hi = "अमेरिकन — ‘r’ साफ़ सुनाई देता है (सबसे अच्छा)",
        hinglish = "American — ‘r’ saaf sunaai deta hai (sabse achha)",
        en = "American — clear ‘r’ sound (recommended)",
    )
    val voiceIndian = L(hi = "इंडियन", hinglish = "Indian", en = "Indian")
    val voiceBritish = L(hi = "ब्रिटिश", hinglish = "British", en = "British")
}
