# Samjho English — Build Plan (v1)

> "Ratta nahi, samajh!" — An Android app that helps Hindi-medium kids (Class 1–8)
> truly understand English: reading, words, sentence building, speaking.

Status: **Approved. M0–M4 ✅ (built; awaiting on-phone testing + phonics recordings) — next: M5 (parent section).** Last updated 2026-10-31.

### How content is made
- Lesson sources live in `tools/content/` (`curriculum.json`, `phonics.json`, `words.json`, `patterns.json`).
- `python tools/content/generate.py` builds the lesson files in `app/src/main/assets/content/` from them.
  L0.U01–03 and L1.U01–03 are hand-written JSON and are not touched by the generator.
- `gradlew test` then checks every lesson (answers in options, all 3 languages present, sounds exist, roles valid…).
- To add words: append to `words.json` (8 words per lesson), add the lesson to `curriculum.json`, run both commands.

Current content: 58 lessons, ~1,090 steps — Level 0 (3), Level 1 phonics (12), Level 2 vocabulary (28 lessons, 223 words), Level 3 sentence patterns (15).

---

## 1. Decisions (final)

| Topic | Decision |
|---|---|
| Users | Hindi-medium kids, Class 1–8. Junior look (Class 1–4), Senior look (Class 5–8) |
| App language | Choice of **हिंदी / Hinglish / English** (asked on first launch, changeable anytime). Changes buttons, instructions and explanations — the English being *taught* stays English |
| Hindi style | Everyday spoken Hindi, not shuddh/textbook Hindi (see section 4) |
| Cost | Free. **No paid AI or paid APIs** at runtime |
| Internet | Assumed available (needed for Google speech recognition) |
| Content | AI-generated during development, stored as JSON files inside the app |
| Platform | Android only. Kotlin + Jetpack Compose |
| Data storage | On the phone only (Room database). No server |
| Distribution | Direct APK file sharing (no Play Store for now) |
| Builder | Built entirely with Claude Code |

---

## 2. Teaching principles (how this app is different from school)

1. **Hindi is the bridge.** Every new idea is explained by comparing it with Hindi the child already knows.
2. **Understand, don't memorise.** Teach *patterns* (sound families, sentence patterns), not lists.
3. **Confidence first.** Level 0 shows the child they already know ~150 English words (bus, phone, TV, cricket, doctor…).
4. **Out-of-the-box methods** that the classroom does not use:
   - **Sentence Train:** Hindi sentence is a train; the child rearranges the compartments into English order (SOV → SVO).
   - **Teach Tinku:** the tutor robot makes mistakes and the child corrects it (learning by teaching).
   - **Mistake Detective:** spot the wrong word in a sentence ("She are happy").
   - **Sound Families:** cat, hat, mat, bat — learn one, read ten.
   - **Ear Training with word pairs:** thin/tin, vet/wet, ship/sip — trains both listening and speaking.
   - **Real-life Missions:** "Aaj mummy ko 'Good morning' bolo!" — parent ticks it in the parent section.
   - **Desi stories & examples:** village, mela, cricket, chai stall, school — never foreign settings.
5. **Small daily sessions.** 10–15 minutes a day beats 2 hours once a week.
6. **Never shame.** After 3 failed tries, show the answer kindly and bring it back later.

---

## 3. App identity

- **Name:** Samjho English (alternatives: English Dost, Seekho Samjho)
- **Tagline:** Ratta nahi, samajh!
- **Tutor character:** **Tinku**, a small friendly robot who came to Earth and is also learning English.
  Tinku learns *with* the child, and sometimes the child teaches Tinku.
  (A parrot was avoided on purpose — "tota ratna" = rote learning, which is the opposite of our idea. An owl was avoided because "ullu" is an insult in Hindi.)
- **Visuals (MVP):** emoji + simple vector shapes (free, works on all phones). Proper illustrations later.

---

## 4. App language & Hindi style

### Three language modes
| Mode | Who it's for | Example instruction |
|---|---|---|
| हिंदी | Kids & parents most comfortable reading Hindi | "सुनो और सही तस्वीर चुनो" |
| Hinglish | Kids who read Roman letters better | "Suno aur sahi tasveer chuno" |
| English | Older kids / parents comfortable in English | "Listen and pick the right picture" |

- Chosen on first launch with big buttons + audio ("हिंदी", "Hinglish", "English"), changeable in settings.
- Every explanation and UI text exists in all three versions (AI-generated into the content files).
- Spoken explanations use the Hindi (Devanagari) text so the Hindi voice sounds natural, in both हिंदी and Hinglish modes.
- Parent section follows the same language setting.

### Hindi style guide (must follow for all Hindi/Hinglish content)
- Use **bolchaal ki Hindi** — the Hindi people speak at home, not textbook Hindi.
- Keep English words that are already normal in Hindi: स्कूल, टीचर, फ़ोन, लेवल, गेम, प्रैक्टिस.
- Talk to the child with **"तुम"** (warm, friendly). Talk to parents with **"आप"**.
- Short sentences (max ~12 words). One idea per sentence.
- No grammar jargon. Say "नाम वाले शब्द" not "संज्ञा", "काम वाले शब्द" not "क्रिया".

| ❌ Don't use | ✅ Use |
|---|---|
| प्रश्न | सवाल |
| उत्तर | जवाब |
| पुनः प्रयास करें | एक बार फिर बोलो |
| अभ्यास करें | प्रैक्टिस करो |
| उच्चारण | बोलने का तरीका |
| चित्र | तस्वीर |
| विद्यालय | स्कूल |
| उत्कृष्ट! | बहुत बढ़िया! / शाबाश! |
| त्रुटि | गलती |
| आगे बढ़ें | चलो आगे! |

---

## 5. UI / UX rules (child-friendly)

1. **Audio-first:** every instruction is spoken automatically; a 🔊 button repeats it. A child who can't read yet can still use the whole app.
2. **One thing per screen.** No clutter, no menus inside lessons.
3. **Big touch targets:** minimum 64dp (Junior 80dp). Main action button always in the same place (bottom).
4. **Few words, more pictures:** icons + emoji + colour carry meaning.
5. **Kind feedback:** correct → Tinku dances + happy sound + stars. Wrong → no red ✗ or buzzer; Tinku says "कोई बात नहीं, फिर से करते हैं!" and gives a hint.
6. **No time pressure.** No countdown timers in learning exercises.
7. **Always know where you are:** progress bar on top of each lesson; a map of levels on home.
8. **Rewards that motivate, not distract:** stars, streak, Tinku stickers/outfits unlocked by learning.
9. **Font:** Baloo 2 (free, rounded, child-friendly, supports both Hindi and English letters). Large text sizes.
10. **Colours:** bright but soft, high contrast for readability; Junior theme more colourful, Senior theme calmer.
11. **Portrait only**, designed for small 5–6" screens first.
12. **Parent area hidden behind a PIN** so kids don't change settings.
13. **No ads, no external links, no in-app purchases.**
14. **Accessibility:** works with large system font; sound can be muted (vibration feedback instead).

---

## 6. Curriculum (MVP = Level 0–3)

### Level 0 — "Tum English Pehle Se Jaante Ho!"
English words already used in Hindi (bus, train, phone, TV, doctor, school, pen, ticket…). Pure confidence boost.

### Level 1 — Sounds & Reading (Phonics)
1. Letter sounds (not letter names): a = "ae" as in apple, b = "b"…
2. Blending CVC words: c-a-t → cat (sound families: -at, -an, -ig, -op, -ug…)
3. Two-letter sounds: sh, ch, th, ph, wh, ck, ng
4. Long vowels & "magic e": cap → cape, kit → kite
5. Vowel teams: ee, oa, ai, oo, ou…
6. Sight words (cannot be sounded out): the, is, are, was, you, said, what…
7. Hindi-speaker trouble sounds: v/w, z/j, th, f/ph, "s" + consonant start (school, not "iskool")

### Level 2 — Words (~500 everyday words)
Themes: Me & my body · Family · Home · School · Food · Animals · Colours & numbers · Clothes · Actions (verbs) · Places · Feelings · Time & days · Opposites.
Each word: emoji/picture · English audio · Hinglish meaning · speak it · use it in a tiny sentence.

### Level 3 — Sentence Patterns (~15 patterns)
| # | Pattern | Hinglish idea |
|---|---|---|
| 1 | This is a ___ / That is a ___ | Paas = this, door = that |
| 2 | I am ___ | Main = I + am |
| 3 | He / She / It is ___ | Woh (ek) = is |
| 4 | We / You / They are ___ | Ek se zyada / tum = are |
| 5 | a / an / the | a = koi ek, an = vowel sound se pehle, the = woh wala |
| 6 | I have ___ / He has ___ | Mere paas hai |
| 7 | I like ___ | Mujhe pasand hai |
| 8 | I can ___ | Main kar sakta/sakti hoon |
| 9 | There is / There are ___ | Wahan hai |
| 10 | Where is ___? in / on / under | Kahan hai? — "par" comes *before* the noun in English |
| 11 | I want ___ | Mujhe chahiye |
| 12 | not (I am not / It is not) | Nahi |
| 13 | Yes/No questions: Is it ___? Are you ___? | Sawal mein is/are aage aata hai |
| 14 | What / Who / Where questions | Kya / Kaun / Kahan |
| 15 | I ___ every day (simple present) | Roz ki baatein |

### Role-plays (MVP: 3)
1. Introduce yourself (name, class, age, likes)
2. At the shop (asking for things, numbers, "How much?")
3. At school (greetings, asking the teacher, "May I…?")

### Later levels
Level 4 Reading stories · Level 5 Tenses (past/future with timeline) · Level 6 Longer conversations & writing.

---

## 7. Exercise types

| Type | What the child does | Skills trained |
|---|---|---|
| `learn_card` | Sees + hears a concept explained by Tinku | Understanding |
| `listen_tap` | Hears a word, taps the right picture | Listening, vocabulary |
| `say_word` | Says the word; app checks | Pronunciation |
| `say_sentence` | Says a full sentence; app checks | Speaking, fluency |
| `minimal_pair` | Hears one of two similar words, picks which | Ear training |
| `blend` | Taps letter sounds one by one, then says the word | Phonics, reading |
| `match_meaning` | Matches English word ↔ Hinglish meaning | Vocabulary |
| `sentence_train` | Arranges word blocks into the right English order | Sentence building |
| `fill_blank` | Picks the right word (is/am/are, a/an) | Grammar |
| `spot_mistake` | Finds and fixes the wrong word | Grammar, attention |
| `teach_tinku` | Tinku says something wrong; child corrects it by speaking | All, confidence |
| `roleplay` | Scripted conversation with Tinku, by voice | Conversation |

Every exercise is tagged with one or more **skill IDs** (e.g. `sound.th`, `grammar.is_am_are`, `vocab.food`). This is how we track exactly where a child struggles.

---

## 8. Speech (all free)

### Listening (app speaks)
- Android built-in **TextToSpeech** — English (en-IN) for English text, Hindi (hi-IN) for explanations.
- Explanations stored in three versions: `hi` (Devanagari — shown in हिंदी mode and always used for speaking), `hinglish`, `en`.
- **Phonics sounds** (/k/, /sh/, /th/…) are recorded by us (44 short clips) and bundled, because TTS cannot speak single sounds properly. Recording list: [docs/phonics-sounds.md](docs/phonics-sounds.md).
- Slow-speed replay button on every word/sentence.

### Checking the child's voice
- Android **SpeechRecognizer** (Google, free, en-IN), asking for up to 5 guesses.
- **Correct** if any guess matches the target after cleanup (lowercase, remove punctuation), or is very close (fuzzy match, edit distance).
- **Error detection with a confusion map:** if target is "thin" and the app heard "tin" → tag `sound.th`. Examples:
  `th→t/d`, `v→w/b`, `w→v`, `z→j`, `f→ph/p`, `sh→s`, `s+consonant → is+consonant`.
- The tagged error triggers a short Hinglish tip + mouth-position hint ("Jeebh ko daanton ke beech rakho").
- Up to 3 tries, then show the answer and schedule it for review.
- **Fallback (later milestone):** Vosk offline recogniser (free, Indian-English model) for phones without Google speech services.

### Voice Diary (for parents)
Once a week the child reads the same 5 sentences; the app records them (MediaRecorder) so parents can hear progress over weeks.

---

## 9. Adaptive learning engine (rule-based, simple, on-device)

- **Placement test** (5 min, game-style): finds the starting level and skips what the child already knows.
- **Skill mastery** (0–100%) per skill, based on recent answers (recent attempts count more).
- **Spaced review (Leitner boxes):** items move up a box when correct, back to box 1 when wrong.
  Review gaps: box 1 = same day, 2 = 1 day, 3 = 3 days, 4 = 7 days, 5 = 16 days.
- **Lesson mix:** ~70% new content, ~30% review of due items and weak skills.
- **Mastery gate:** next lesson unlocks when the lesson's skills reach ≥ 70%.
- **Stuck detection:** if a skill stays < 50% after 2 tries of a lesson → show a *different* explanation (each concept has 2 explanation variants) and easier practice.
- **Hint ladder:** try again → hint → slow audio → Hindi meaning → show answer.
- **Daily plan:** a "Aaj ka kaam" button builds a 10–15 minute session automatically.

---

## 10. Parent section (PIN protected, in the chosen app language)

- Today / this week time spent (simple bar chart) and daily streak
- Progress per level (progress bars)
- **Top 3 weak areas** in plain words + a home tip
  (e.g. "is / am / are mein confusion hai. Ghar pe poochhiye: 'Tum kaise ho?' aur jawab English mein dilwaiye.")
- Words learned count, sentences spoken count
- Voice Diary playback (week 1 vs latest)
- Real-life Missions to approve
- Daily time limit setting and parent PIN change
- Multiple child profiles on one phone

---

## 11. Technical architecture

- **Language/UI:** Kotlin, Jetpack Compose, Material 3, Navigation Compose
- **Pattern:** MVVM (ViewModel + StateFlow), single app module to start
- **Storage:** Room (progress, attempts, sessions), DataStore (settings)
- **Content:** JSON files in `assets/content/` loaded with kotlinx.serialization
- **Speech:** Android TextToSpeech, SpeechRecognizer, MediaRecorder
- **Min Android version:** Android 7.0 (API 24) so it runs on old cheap phones
- **App size target:** under 30 MB

### Data model (Room)
- `ChildProfile` — id, name, avatar, classGrade, ageGroup (junior/senior), createdAt
- `SkillState` — childId, skillId, mastery, attempts, correct, lastPracticed
- `ItemState` — childId, itemId, box, nextReviewAt
- `Attempt` — childId, exerciseId, skillIds, correct, heardText, errorTag, tries, durationMs, timestamp
- `StudySession` — childId, startAt, endAt
- `VoiceDiaryEntry` — childId, filePath, createdAt
- `MissionState` — childId, missionId, status

### Content file format (example)
```json
{
  "id": "L3.U02",
  "title": "I am ___",
  "level": 3,
  "skills": ["grammar.i_am"],
  "explain": [
    {
      "hi": "जब हम अपने बारे में बोलते हैं, तो 'I' के साथ 'am' लगाते हैं। मैं = I, हूँ = am.",
      "hinglish": "Jab hum apne baare mein bolte hain, toh 'I' ke saath 'am' lagate hain. Main = I, hoon = am.",
      "en": "When we talk about ourselves, we use 'am' with 'I'.",
      "example_en": "I am happy.",
      "example_hi": "मैं खुश हूँ।",
      "example_hinglish": "Main khush hoon."
    }
  ],
  "exercises": [
    {
      "id": "L3.U02.E05",
      "type": "sentence_train",
      "skills": ["grammar.i_am", "grammar.word_order"],
      "prompt": { "hi": "मैं खुश हूँ", "hinglish": "Main khush hoon", "en": "Main khush hoon (I am happy)" },
      "blocks": ["happy", "I", "am"],
      "answer": ["I", "am", "happy"]
    },
    {
      "id": "L3.U02.E09",
      "type": "say_sentence",
      "skills": ["speak.sentence", "grammar.i_am"],
      "target": "I am a student",
      "accept": ["i am a student", "i'm a student"]
    }
  ]
}
```

### Folder structure (planned)
```
app/src/main/
  assets/content/        lesson JSON files (level0/ level1/ level2/ level3/ roleplay/)
  assets/audio/phonics/  recorded phonics sound clips
  java/.../
    data/      Room database, DAOs, content loader
    domain/    adaptive engine, speech checker, confusion map
    speech/    TTS + recogniser wrappers
    ui/        screens (home, lesson player, exercises, parent, onboarding)
    ui/theme/  junior + senior themes
```

---

## 12. Build milestones

| # | Milestone | Done when… |
|---|---|---|
| M0 | Project setup | App with language picker (हिंदी / Hinglish / English) and Tinku home screen runs on your phone |
| M1 | Content format + lesson player | Level 0 playable with tap-type exercises |
| M2 | Speech | TTS + voice checking + confusion-map tips working in `say_word` / `minimal_pair` |
| M3 | Progress + adaptive engine | Attempts saved, mastery, spaced review, placement test, "Aaj ka kaam" |
| M4 | Full content | Level 1–3 content generated, checked by script for errors |
| M5 | Parent section | PIN, dashboard, weak areas, time limit, voice diary, missions |
| M6 | Conversation + polish | 3 role-plays, Teach Tinku, junior/senior themes, sounds & animations |
| M7 | Real-kid testing + release | 3–5 real kids test it; fix issues; signed release APK to share |

Each milestone ends with: build → install on your phone → you test → we fix → next milestone.

---

## 13. Risks & how we handle them

| Risk | Plan |
|---|---|
| Speech recognition weak with kids' voices | Fuzzy matching, accept close answers, never block progress; test with real kids at M2 |
| Some phones lack Google speech | Vosk offline fallback (later milestone) |
| TTS voice quality varies per phone | Check on first launch; guide parent to install Google TTS voice data if missing |
| AI content mistakes | Content validation script + you review a few lessons per level while testing |
| Kids lose interest | Short sessions, streaks, Tinku reactions, stars, missions |
