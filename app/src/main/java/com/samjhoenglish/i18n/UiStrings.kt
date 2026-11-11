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

    // Kid Mode. The locked screen is for the child; the rest is addressed to adults ("आप").
    val appLockedSay = L(
        hi = "यह ऐप अभी बंद है। चलो, इंग्लिश सीखते हैं!",
        hinglish = "Yeh app abhi band hai. Chalo, English seekhte hain!",
        en = "This app is locked right now. Let's learn English!",
    )
    val letsLearn = L(hi = "चलो सीखें", hinglish = "Chalo seekhein", en = "Let's learn")
    val kidMode = L(hi = "किड मोड", hinglish = "Kid Mode", en = "Kid Mode")
    val kidModeCardText = L(
        hi = "बच्चे को फ़ोन देने से पहले चुने हुए ऐप लॉक करें।",
        hinglish = "Bachche ko phone dene se pehle chune hue app lock karein.",
        en = "Lock the apps you choose before handing over the phone.",
    )

    /** Format with the child's name. */
    val kidModeOnFor = L(hi = "किड मोड चालू है: %s", hinglish = "Kid Mode chaalu hai: %s", en = "Kid Mode is on: %s")
    val kidModeStart = L(hi = "बच्चे को फ़ोन दें", hinglish = "Bachche ko phone dein", en = "Hand the phone to my child")
    val kidModeStop = L(hi = "किड मोड बंद करें", hinglish = "Kid Mode band karein", en = "Turn off Kid Mode")
    val kidModeSetup = L(hi = "एक बार की सेटिंग", hinglish = "Ek baar ki setting", en = "One-time setup")
    val setupUsage = L(hi = "कौन सा ऐप खुला है, यह देखना", hinglish = "Kaun sa app khula hai, yeh dekhna", en = "See which app is open")
    val setupOverlay = L(hi = "लॉक स्क्रीन दिखाना", hinglish = "Lock screen dikhana", en = "Show the lock screen")
    val setupBattery = L(hi = "बैटरी की पाबंदी हटाना", hinglish = "Battery ki paabandi hatana", en = "Remove battery limits")
    val setupNotifications = L(hi = "सूचना दिखाना", hinglish = "Notification dikhana", en = "Show a notification")
    val setupTurnOn = L(hi = "चालू करें", hinglish = "Chaalu karein", en = "Turn on")
    val setupAutostart = L(
        hi = "Realme, Oppo, Vivo या Xiaomi फ़ोन पर: फ़ोन की सेटिंग्स में Samjho English के लिए Auto-start भी चालू करें।",
        hinglish = "Realme, Oppo, Vivo ya Xiaomi phone par: phone ki settings mein Samjho English ke liye Auto-start bhi chaalu karein.",
        en = "On Realme, Oppo, Vivo or Xiaomi phones: also turn on Auto-start for Samjho English in the phone's settings.",
    )
    val chooseAppsToLock = L(hi = "कौन से ऐप लॉक करने हैं?", hinglish = "Kaun se app lock karne hain?", en = "Which apps should be locked?")
    val alwaysLockedNote = L(
        hi = "फ़ोन की सेटिंग्स और Play Store किड मोड में अपने आप लॉक रहते हैं। कॉल हमेशा चालू रहती है।",
        hinglish = "Phone ki settings aur Play Store Kid Mode mein apne aap lock rehte hain. Call hamesha chaalu rehti hai.",
        en = "Phone Settings and the Play Store are always locked in Kid Mode. Calls always work.",
    )

    // Reading ladder and read-along
    val reading = L(hi = "पढ़ो", hinglish = "Padho", en = "Reading")
    val readingCardText = L(hi = "कहानी पढ़ो, शब्द हरे होते जाएँगे!", hinglish = "Kahani padho, shabd hare hote jaayenge!", en = "Read a story and watch the words turn green!")
    val readingPick = L(hi = "आज कौन सी कहानी पढ़ें?", hinglish = "Aaj kaun si kahani padhein?", en = "Which story shall we read today?")
    val readingLevel = L(hi = "पढ़ने का लेवल", hinglish = "Padhne ka level", en = "Reading level")
    val passage = L(hi = "कहानी", hinglish = "Kahani", en = "Story")
    val readingLockedSay = L(hi = "पहले ऊपर वाली कहानियाँ पढ़ो, फिर यह खुलेगी!", hinglish = "Pehle upar waali kahaniyan padho, phir yeh khulegi!", en = "Read the stories above first, then this one opens!")
    val tinkuReadsFirst = L(hi = "पहले मैं पढ़ता हूँ। सुनो और उँगली से साथ चलो।", hinglish = "Pehle main padhta hoon. Suno aur ungli se saath chalo.", en = "I'll read first. Listen and follow with your finger.")
    val yourTurnToRead = L(hi = "अब तुम्हारी बारी! 🎤 दबाओ और ज़ोर से पढ़ो।", hinglish = "Ab tumhari baari! 🎤 dabao aur zor se padho.", en = "Your turn! Tap 🎤 and read aloud.")
    val readingListening = L(hi = "मैं सुन रहा हूँ… पढ़ो!", hinglish = "Main sun raha hoon… padho!", en = "I'm listening… read!")
    val readingLoading = L(hi = "एक पल, अपने कान तैयार कर रहा हूँ…", hinglish = "Ek pal, apne kaan taiyaar kar raha hoon…", en = "One moment, getting my ears ready…")
    val readingUnavailable = L(hi = "आज मेरे कान काम नहीं कर रहे। तुम ज़ोर से पढ़ो, फिर ✔ दबाओ।", hinglish = "Aaj mere kaan kaam nahi kar rahe. Tum zor se padho, phir ✔ dabao.", en = "My ears aren't working today. Read aloud, then tap ✔.")
    val letMeRead = L(hi = "मैं पढ़ूँगा", hinglish = "Main padhoonga", en = "Let me read")
    val hearItAgain = L(hi = "फिर सुनो", hinglish = "Phir suno", en = "Hear it again")
    val doneReading = L(hi = "पढ़ लिया", hinglish = "Padh liya", en = "I've finished")
    val stuckWhisper = L(hi = "यह शब्द है", hinglish = "Yeh shabd hai", en = "This word is")
    val tapWordHint = L(hi = "किसी भी शब्द को छूकर सुनो", hinglish = "Kisi bhi shabd ko chhookar suno", en = "Tap any word to hear it")

    /** Format with (total, read). */
    val wordsReadScore = L(hi = "%1\$d में से %2\$d शब्द पढ़े", hinglish = "%1\$d mein se %2\$d shabd padhe", en = "%2\$d of %1\$d words read")
    val practiseThese = L(hi = "इन्हें एक बार फिर बोलो:", hinglish = "Inhe ek baar phir bolo:", en = "Say these once more:")
    val readingSection = L(hi = "पढ़ना", hinglish = "Padhna", en = "Reading")
    val readingNone = L(hi = "अभी कोई कहानी नहीं पढ़ी। होम पर 📖 पढ़ो से शुरू करें।", hinglish = "Abhi koi kahani nahi padhi. Home par 📖 Padho se shuru karein.", en = "No stories read yet. Start from 📖 Reading on the home screen.")
    val passagesRead = L(hi = "कहानियाँ पढ़ीं", hinglish = "kahaniyan padhin", en = "stories read")
    val readingAccuracy = L(hi = "सही पढ़े शब्द", hinglish = "sahi padhe shabd", en = "words read correctly")
    val wordsPerMinute = L(hi = "शब्द प्रति मिनट", hinglish = "shabd prati minute", en = "words per minute")

    // Word Power (vocabulary by class)
    val wordPower = L(hi = "शब्द शक्ति", hinglish = "Shabd Shakti", en = "Word Power")
    val wordPowerCard = L(hi = "अपनी कक्षा के नए शब्द सीखो और टेस्ट दो", hinglish = "Apni kaksha ke naye shabd seekho aur test do", en = "Learn new words for your class and take tests")
    val wordPowerHello = L(hi = "रोज़ 10 नए शब्द, और वो हमेशा याद रहेंगे!", hinglish = "Roz 10 naye shabd, aur woh hamesha yaad rahenge!", en = "Ten new words a day, and they'll stay with you!")
    val newWord = L(hi = "नया शब्द", hinglish = "Naya shabd", en = "New word")
    val forExample = L(hi = "जैसे", hinglish = "Jaise", en = "For example")
    val spellThis = L(hi = "सुनो और स्पेलिंग बनाओ", hinglish = "Suno aur spelling banao", en = "Listen and spell it")
    val typeTheWord = L(hi = "यहाँ शब्द लिखो", hinglish = "Yahan shabd likho", en = "Type the word here")
    val useLetterTiles = L(hi = "अक्षरों के टुकड़ों से बनाओ", hinglish = "Aksharon ke tukdon se banao", en = "Use letter tiles")
    val wordsLearnedShort = L(hi = "शब्द सीखे", hinglish = "shabd seekhe", en = "words learned")
    val wordsMastered = L(hi = "पक्के शब्द", hinglish = "pakke shabd", en = "words mastered")
    val reviewWords = L(hi = "शब्द दोहराओ", hinglish = "Shabd dohrao", en = "Review words")

    /** Format with the number of words waiting. */
    val reviewWaiting = L(hi = "%d शब्द दोहराने हैं", hinglish = "%d shabd dohraane hain", en = "%d words to review")
    val findMyLevel = L(hi = "मेरा लेवल पता करो (3 मिनट)", hinglish = "Mera level pata karo (3 minute)", en = "Find my level (3 minutes)")
    val findMyLevelNote = L(
        hi = "जो शब्द पहले से आते हैं, वो दोबारा नहीं सीखने पड़ेंगे। ऊपर की कक्षा के शब्द भी खुल सकते हैं।",
        hinglish = "Jo shabd pehle se aate hain, woh dobaara nahi seekhne padenge. Upar ki kaksha ke shabd bhi khul sakte hain.",
        en = "Skip words you already know. It can open higher-class words too.",
    )
    val monthlyTest = L(hi = "महीने का शब्द टेस्ट", hinglish = "Mahine ka shabd test", en = "Monthly word test")
    val monthlyTestNote = L(hi = "अब तक सीखे शब्दों से 20 सवाल", hinglish = "Ab tak seekhe shabdon se 20 sawaal", en = "20 questions from the words you've learned")
    val yourClass = L(hi = "तुम्हारी कक्षा", hinglish = "Tumhari kaksha", en = "Your class")
    val learn = L(hi = "सीखो", hinglish = "Seekho", en = "Learn")
    val test = L(hi = "टेस्ट", hinglish = "Test", en = "Test")
    val bandLockedSay = L(
        hi = "इससे पहले वाली कक्षा के 8 टेस्ट पास करो, या लेवल टेस्ट दो। फिर ये शब्द खुलेंगे!",
        hinglish = "Isse pehle waali kaksha ke 8 test pass karo, ya level test do. Phir yeh shabd khulenge!",
        en = "Pass 8 tests in the band before, or take the level test. Then these words open!",
    )
    val packLockedSay = L(hi = "पहले ऊपर वाले शब्द सीखो!", hinglish = "Pehle upar waale shabd seekho!", en = "Learn the words above first!")
    val wordOfTheDay = L(hi = "आज का शब्द", hinglish = "Aaj ka shabd", en = "Word of the day")
    val useItToday = L(hi = "आज इस शब्द से एक वाक्य बोलो!", hinglish = "Aaj is shabd se ek vaakya bolo!", en = "Say a sentence with this word today!")
    val lastMonthlyScore = L(hi = "पिछला महीने का टेस्ट", hinglish = "Pichhla mahine ka test", en = "Last monthly test")

    // Tinku Express (the scrolling reading game)
    val express = L(hi = "टिंकू एक्सप्रेस", hinglish = "Tinku Express", en = "Tinku Express")
    val expressCard = L(hi = "लाइनें ऊपर चलेंगी, तुम साथ-साथ पढ़ो!", hinglish = "Lines upar chalengi, tum saath-saath padho!", en = "The lines move up. Read them before they go!")
    val expressPick = L(hi = "कौन सी ट्रेन चलाएँ?", hinglish = "Kaun si train chalayein?", en = "Which train shall we run?")
    val train = L(hi = "ट्रेन", hinglish = "Train", en = "Train")
    val smartSpeed = L(hi = "स्मार्ट स्पीड", hinglish = "Smart speed", en = "Smart speed")
    val smartSpeedNote = L(hi = "तुम्हारे साथ धीमी या तेज़ होगी", hinglish = "Tumhare saath dheemi ya tez hogi", en = "Slows down or speeds up with you")
    val challenge = L(hi = "चैलेंज", hinglish = "Challenge", en = "Challenge")
    val challengeNote = L(hi = "एक ही स्पीड, असली टेस्ट", hinglish = "Ek hi speed, asli test", en = "One speed, the real test")
    val expressLocked = L(
        hi = "पिछली ट्रेन की 2 कहानियों पर 2-2 स्टार लाओ, फिर यह ट्रेन खुलेगी!",
        hinglish = "Pichhli train ki 2 kahaniyon par 2-2 star lao, phir yeh train khulegi!",
        en = "Get 2 stars on 2 stories of the train before, then this train opens!",
    )
    val bonusTitle = L(hi = "बोनस: पढ़ी हुई कहानियाँ, अब और तेज़", hinglish = "Bonus: padhi hui kahaniyan, ab aur tez", en = "Bonus: stories you've read, now faster")
    val bonusNone = L(
        hi = "📖 पढ़ो में कहानी पूरी करो, फिर वह यहाँ तेज़ पढ़ने के लिए आएगी।",
        hinglish = "📖 Padho mein kahani poori karo, phir woh yahan tez padhne ke liye aayegi.",
        en = "Finish a story in 📖 Reading and it comes here to read faster.",
    )
    val bestSpeed = L(hi = "तुम्हारी सबसे तेज़ स्पीड", hinglish = "Tumhari sabse tez speed", en = "Your best speed")
    val getReady = L(hi = "तैयार?", hinglish = "Taiyaar?", en = "Ready?")
    val startsWhenYouRead = L(
        hi = "पढ़ना शुरू करो, लाइनें चलने लगेंगी।",
        hinglish = "Padhna shuru karo, lines chalne lagengi.",
        en = "Start reading and the lines start moving.",
    )
    val paused = L(hi = "रुकी हुई है", hinglish = "Ruki hui hai", en = "Paused")
    val resume = L(hi = "फिर से चलाओ", hinglish = "Phir se chalao", en = "Resume")
    val golden = L(hi = "सुनहरे शब्द", hinglish = "Sunehre shabd", en = "Golden words")

    /** Format with (total, caught). */
    val linesCaught = L(hi = "%1\$d में से %2\$d लाइनें पकड़ीं", hinglish = "%1\$d mein se %2\$d lines pakdin", en = "%2\$d of %1\$d lines caught")
    val bestStreak = L(hi = "सबसे ज़्यादा लगातार", hinglish = "Sabse zyada lagaataar", en = "Best streak")
    val yourSpeed = L(hi = "तुम्हारी स्पीड", hinglish = "Tumhari speed", en = "Your speed")
    val newBest = L(hi = "नया रिकॉर्ड! 🎉", hinglish = "Naya record! 🎉", en = "New best! 🎉")
    val roundFailed = L(
        hi = "आधी से ज़्यादा लाइनें निकल गईं। फिर से कोशिश करो, तुम कर सकते हो!",
        hinglish = "Aadhi se zyada lines nikal gayin. Phir se koshish karo, tum kar sakte ho!",
        en = "More than half the lines got away. Try again, you can do it!",
    )
    val roundDoneSay = L(hi = "वाह! ट्रेन स्टेशन पहुँच गई!", hinglish = "Waah! Train station pahunch gayi!", en = "Wow! The train reached the station!")
    val nextStory = L(hi = "अगली कहानी", hinglish = "Agli kahani", en = "Next story")
    val allTrains = L(hi = "सब ट्रेनें", hinglish = "Sab trainein", en = "All trains")
    val expressBest = L(hi = "एक्सप्रेस में सबसे तेज़", hinglish = "Express mein sabse tez", en = "Best Express speed")

    // School connection (addressed to adults, "आप")
    val school = L(hi = "स्कूल से जोड़ें", hinglish = "School se jodein", en = "Connect to school")
    val schoolCardText = L(
        hi = "बच्चे की पढ़ाई की जानकारी उसके स्कूल और टीचर को भेजें।",
        hinglish = "Bachche ki padhai ki jaankari uske school aur teacher ko bhejein.",
        en = "Share this child's learning progress with their school and teacher.",
    )
    val schoolCodeLabel = L(hi = "स्कूल से मिला कोड", hinglish = "School se mila code", en = "Code from the school")

    /** Keep in step with SchoolSync.CONSENT_VERSION: change the version when these words change. */
    val schoolConsentTitle = L(hi = "आपकी सहमति", hinglish = "Aapki sehmati", en = "Your consent")
    val schoolShared = L(
        hi = "स्कूल को यह दिखेगा: पूरे किए लेसन, स्टार, सीखे शब्द, पढ़ने का लेवल और स्पीड, वर्ड पावर के शब्द और टेस्ट, ऐप में बिताया समय, और किन चीज़ों में मदद चाहिए। यह हेडमास्टर और इस बच्चे के टीचर देख सकेंगे। समझो इंग्लिश चलाने वाले भी स्कूल की मदद के लिए इसे देख सकते हैं।",
        hinglish = "School ko yeh dikhega: poore kiye lesson, star, seekhe shabd, padhne ka level aur speed, Word Power ke shabd aur test, app mein bitaya samay, aur kin cheezon mein madad chahiye. Yeh headmaster aur is bachche ke teacher dekh sakenge. Samjho English chalane wale bhi school ki madad ke liye ise dekh sakte hain.",
        en = "The school will see: lessons finished, stars, words learned, reading level and speed, Word Power words and tests, time in this app, and what the child needs help with. The headmaster and this child's teachers can see it. The people who run Samjho English can also see it, to help the school.",
    )
    val schoolNotShared = L(
        hi = "स्कूल को यह कभी नहीं भेजा जाएगा: बच्चे की आवाज़ (ऐप इसे कभी सेव नहीं करता), फ़ोन के दूसरे ऐप की जानकारी, और किड मोड की जानकारी।",
        hinglish = "School ko yeh kabhi nahi bheja jaayega: bachche ki awaaz (app ise kabhi save nahi karta), phone ke doosre app ki jaankari, aur Kid Mode ki jaankari.",
        en = "Never sent to the school: the child's voice (the app never saves it), other apps on this phone, and Kid Mode details.",
    )
    val schoolStopAnytime = L(
        hi = "आप कभी भी यहीं से जुड़ाव हटा सकते हैं। तब स्कूल के पास भेजी गई जानकारी मिटा दी जाएगी।",
        hinglish = "Aap kabhi bhi yahin se judaav hata sakte hain. Tab school ke paas bheji gayi jaankari mita di jaayegi.",
        en = "You can disconnect here at any time. What was shared with the school will then be deleted.",
    )
    val schoolConsentCheck = L(
        hi = "मैं इस बच्चे का माता/पिता या अभिभावक हूँ और जानकारी भेजने की सहमति देता/देती हूँ।",
        hinglish = "Main is bachche ka mata/pita ya abhibhavak hoon aur jaankari bhejne ki sehmati deta/deti hoon.",
        en = "I am this child's parent or guardian and I agree to share this information.",
    )
    val schoolConnect = L(hi = "जोड़ें", hinglish = "Jodein", en = "Connect")
    val schoolWrongCode = L(hi = "कोड गलत है या पहले इस्तेमाल हो चुका है।", hinglish = "Code galat hai ya pehle istemaal ho chuka hai.", en = "This code is wrong or has already been used.")
    val schoolTooManyTries = L(hi = "बहुत बार गलत कोड। एक घंटे बाद कोशिश करें।", hinglish = "Bahut baar galat code. Ek ghante baad koshish karein.", en = "Too many wrong codes. Please try again in an hour.")
    val schoolNoConnection = L(hi = "इंटरनेट से जुड़ नहीं पाया। फिर कोशिश करें।", hinglish = "Internet se jud nahi paaya. Phir koshish karein.", en = "Couldn't connect to the internet. Please try again.")

    /** Format with (student name, school name, class). */
    val schoolConnected = L(hi = "%1\$s का जुड़ाव %2\$s की क्लास %3\$s से है।", hinglish = "%1\$s ka judaav %2\$s ki class %3\$s se hai.", en = "%1\$s is connected to %2\$s, class %3\$s.")
    val schoolLastSent = L(hi = "आख़िरी बार भेजा", hinglish = "Aakhiri baar bheja", en = "Last sent")
    val schoolNever = L(hi = "अभी नहीं", hinglish = "Abhi nahi", en = "Not yet")
    val schoolSendNow = L(hi = "अभी भेजें", hinglish = "Abhi bhejein", en = "Send now")
    val schoolDisconnect = L(hi = "स्कूल से जुड़ाव हटाएँ", hinglish = "School se judaav hatayein", en = "Disconnect from school")
    val schoolDisconnectConfirm = L(
        hi = "स्कूल के पास भेजी गई इस बच्चे की जानकारी मिट जाएगी। दोबारा जोड़ने के लिए स्कूल से नया कोड लेना होगा।",
        hinglish = "School ke paas bheji gayi is bachche ki jaankari mit jaayegi. Dobara jodne ke liye school se naya code lena hoga.",
        en = "What was shared about this child will be deleted from the school. You'll need a new code from the school to connect again.",
    )

    // Teach Tinku and role-plays
    val teachTinku = L(
        hi = "मैंने ऐसा बोला। इसमें गलती है! मेरे टीचर बनो और सही करके बोलो।",
        hinglish = "Maine aisa bola. Ismein galti hai! Mere teacher bano aur sahi karke bolo.",
        en = "I said this, but there's a mistake! Be my teacher and say it correctly.",
    )
    val teachTinkuThanks = L(hi = "थैंक यू, टीचर! अब मुझे समझ आ गया।", hinglish = "Thank you, teacher! Ab mujhe samajh aa gaya.", en = "Thank you, teacher! Now I understand.")
    val roleplayYourTurn = L(hi = "अब तुम बोलो, इनमें से कोई एक:", hinglish = "Ab tum bolo, inmein se koi ek:", en = "Your turn. Say one of these:")
    val roleplayTryLine = L(hi = "ऊपर वाली कोई एक लाइन बोलो।", hinglish = "Upar waali koi ek line bolo.", en = "Say one of the lines above.")
    val roleplayDone = L(hi = "वाह! तुमने इंग्लिश में पूरी बातचीत कर ली!", hinglish = "Waah! Tumne English mein poori baatcheet kar li!", en = "Wow! You had a whole conversation in English!")

    // Tinku's rewards
    val nextReward = L(hi = "अगला इनाम", hinglish = "Agla inaam", en = "Next reward")
    val rewardCap = L(hi = "टोपी", hinglish = "Topi", en = "Cap")
    val rewardBowTie = L(hi = "बो टाई", hinglish = "Bow tie", en = "Bow tie")
    val rewardGlasses = L(hi = "चश्मा", hinglish = "Chashma", en = "Glasses")
    val rewardCrown = L(hi = "मुकुट", hinglish = "Mukut", en = "Crown")

    // Missions on Home (for the child)
    val todaysMission = L(hi = "तुम्हारा मिशन", hinglish = "Tumhara mission", en = "Your mission")
    val iDidIt = L(hi = "मैंने कर लिया!", hinglish = "Maine kar liya!", en = "I did it!")
    val missionWaiting = L(
        hi = "शाबाश! अब मम्मी-पापा से ✅ करवाओ।",
        hinglish = "Shabaash! Ab Mummy-Papa se ✅ karwao.",
        en = "Well done! Now ask a parent to tick it ✅",
    )

    // Daily time limit (for the child, then the parent)
    val timeUp = L(
        hi = "आज की पढ़ाई पूरी! अब थोड़ा खेलो। कल फिर मिलेंगे!",
        hinglish = "Aaj ki padhai poori! Ab thoda khelo. Kal phir milenge!",
        en = "That's all for today! Go and play. See you tomorrow!",
    )
    val timeUpMore = L(hi = "मम्मी-पापा: 15 मिनट और दें", hinglish = "Mummy-Papa: 15 minute aur dein", en = "Parents: give 15 more minutes")

    // Parent PIN. Everything from here on is addressed to adults ("आप").
    val pinCreate = L(hi = "4 अंकों का PIN बनाइए", hinglish = "4 ankon ka PIN banaiye", en = "Create a 4-digit PIN")
    val pinCreateNote = L(
        hi = "इससे बच्चे यह हिस्सा नहीं खोल पाएँगे।",
        hinglish = "Isse bachche yeh hissa nahi khol paayenge.",
        en = "This keeps children out of this section.",
    )
    val pinConfirm = L(hi = "वही PIN दोबारा डालिए", hinglish = "Wahi PIN dobara daaliye", en = "Enter the same PIN again")
    val pinMismatch = L(hi = "दोनों PIN अलग हैं। फिर से बनाइए।", hinglish = "Dono PIN alag hain. Phir se banaiye.", en = "The PINs didn't match. Please start again.")
    val pinEnter = L(hi = "PIN डालिए", hinglish = "PIN daaliye", en = "Enter your PIN")
    val pinWrong = L(hi = "PIN गलत है।", hinglish = "PIN galat hai.", en = "Wrong PIN.")
    val pinLocked = L(hi = "बहुत बार गलत PIN। 30 सेकंड रुकिए।", hinglish = "Bahut baar galat PIN. 30 second rukiye.", en = "Too many wrong tries. Please wait 30 seconds.")
    val pinForgot = L(hi = "PIN भूल गए?", hinglish = "PIN bhool gaye?", en = "Forgot your PIN?")
    val pinForgotQuestion = L(hi = "बड़ों के लिए सवाल: जवाब डालिए", hinglish = "Badon ke liye sawaal: jawab daaliye", en = "A question for adults: enter the answer")
    val pinChange = L(hi = "PIN बदलें", hinglish = "PIN badlein", en = "Change PIN")

    // Parent dashboard
    val parentTitle = L(hi = "मम्मी-पापा के लिए", hinglish = "Mummy-Papa ke liye", en = "For parents")
    val minutes = L(hi = "मिनट", hinglish = "minute", en = "min")
    val timeSpent = L(hi = "पढ़ाई का समय", hinglish = "Padhai ka samay", en = "Time spent")
    val today = L(hi = "आज", hinglish = "Aaj", en = "Today")
    val thisWeek = L(hi = "इस हफ़्ते", hinglish = "Is hafte", en = "This week")
    val streak = L(hi = "लगातार दिन", hinglish = "Lagaataar din", en = "Day streak")
    val mostInDay = L(hi = "सबसे ज़्यादा", hinglish = "Sabse zyaada", en = "Most in a day")
    val progressTitle = L(hi = "कहाँ तक पहुँचे", hinglish = "Kahan tak pahunche", en = "Progress")
    val lessonsWord = L(hi = "लेसन", hinglish = "lesson", en = "lessons")
    val wordsLearned = L(hi = "शब्द सीखे", hinglish = "Shabd seekhe", en = "Words learned")
    val sentencesSpoken = L(hi = "लाइनें बोलीं", hinglish = "Lines boleen", en = "Sentences spoken")
    val weakTitle = L(hi = "कहाँ मदद चाहिए", hinglish = "Kahan madad chahiye", en = "Where help is needed")
    val weakNone = L(
        hi = "अभी कोई कमज़ोर जगह नहीं दिखी। ऐसे ही प्रैक्टिस करते रहने दीजिए!",
        hinglish = "Abhi koi kamzor jagah nahi dikhi. Aise hi practice karte rehne dijiye!",
        en = "No weak areas showing right now. Keep up the practice!",
    )
    val weakTooEarly = L(
        hi = "कुछ लेसन होने के बाद यहाँ दिखेगा कि कहाँ मदद चाहिए।",
        hinglish = "Kuch lesson hone ke baad yahan dikhega ki kahan madad chahiye.",
        en = "After a few lessons, this will show where help is needed.",
    )
    val homeTip = L(hi = "घर पर:", hinglish = "Ghar par:", en = "At home:")
    val missionsTitle = L(hi = "मिशन जो बच्चे ने किए", hinglish = "Mission jo bachche ne kiye", en = "Missions to check")
    val missionsNone = L(hi = "अभी कोई मिशन चेक करने को नहीं है।", hinglish = "Abhi koi mission check karne ko nahi hai.", en = "No missions to check right now.")
    val missionsApprovedCount = L(hi = "अब तक पूरे हुए", hinglish = "Ab tak poore hue", en = "Completed so far")
    val missionApprove = L(hi = "हाँ, किया ✅", hinglish = "Haan, kiya ✅", en = "Yes, done ✅")
    val missionNotYet = L(hi = "अभी नहीं", hinglish = "Abhi nahi", en = "Not yet")
    val limitTitle = L(hi = "रोज़ का समय", hinglish = "Roz ka samay", en = "Daily time limit")
    val limitNote = L(
        hi = "समय पूरा होने पर नया लेसन शुरू नहीं होगा। चल रहा लेसन पूरा हो जाएगा।",
        hinglish = "Samay poora hone par naya lesson shuru nahi hoga. Chal raha lesson poora ho jaayega.",
        en = "When time is up, no new lesson can start. A lesson in progress can finish.",
    )
    val limitOff = L(hi = "कोई सीमा नहीं", hinglish = "Koi seema nahi", en = "No limit")
    val deleteChild = L(hi = "इस बच्चे की प्रोफ़ाइल हटाएँ", hinglish = "Is bachche ki profile hatayein", en = "Delete this child's profile")

    /** Format with the child's name. */
    val deleteConfirm = L(
        hi = "%s की प्रोफ़ाइल और सारी प्रगति हमेशा के लिए मिट जाएगी। क्या आप पक्का हैं?",
        hinglish = "%s ki profile aur saari progress hamesha ke liye mit jaayegi. Kya aap pakka hain?",
        en = "%s's profile and all progress will be erased forever. Are you sure?",
    )
    val deleteYes = L(hi = "हाँ, हटाएँ", hinglish = "Haan, hatayein", en = "Yes, delete")
    val cancel = L(hi = "रहने दें", hinglish = "Rehne dein", en = "Cancel")

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
    val soundEffects = L(hi = "खुशी और इनाम की आवाज़ें", hinglish = "Khushi aur inaam ki awaazein", en = "Chimes and reward sounds")
    val on = L(hi = "चालू", hinglish = "Chaalu", en = "On")
    val off = L(hi = "बंद", hinglish = "Band", en = "Off")
    val voiceIndian = L(hi = "इंडियन", hinglish = "Indian", en = "Indian")
    val voiceBritish = L(hi = "ब्रिटिश", hinglish = "British", en = "British")
}
