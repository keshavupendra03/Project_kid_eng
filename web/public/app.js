// The school website: one page that shows the owner, headmaster or teacher view.
// All text goes in through h(), which uses textContent, so names can never inject HTML.

const WORDS = {
  appName: ["Samjho English School", "समझो इंग्लिश स्कूल"],
  username: ["Username", "यूज़रनेम"],
  password: ["Password", "पासवर्ड"],
  login: ["Log in", "लॉग इन करें"],
  logout: ["Log out", "लॉग आउट"],
  changePassword: ["Set a new password", "नया पासवर्ड बनाएँ"],
  currentPassword: ["Current password", "अभी का पासवर्ड"],
  newPassword: ["New password (8 or more characters)", "नया पासवर्ड (कम से कम 8 अक्षर)"],
  save: ["Save", "सेव करें"],
  add: ["Add", "जोड़ें"],
  remove: ["Remove", "हटाएँ"],
  back: ["Back", "वापस"],
  overview: ["Overview", "सारांश"],
  classes: ["Classes", "क्लास"],
  teachers: ["Teachers", "टीचर"],
  students: ["Students", "बच्चे"],
  parents: ["Parents", "माता-पिता"],
  schools: ["Schools", "स्कूल"],
  addSchool: ["Add a school", "नया स्कूल जोड़ें"],
  schoolName: ["School name", "स्कूल का नाम"],
  headmasterName: ["Headmaster's name", "हेडमास्टर का नाम"],
  tempPassword: ["Temporary password", "अस्थायी पासवर्ड"],
  tempNote: ["They will be asked to change it at first login.", "पहली बार लॉग इन पर इसे बदलना होगा।"],
  headmaster: ["Headmaster", "हेडमास्टर"],
  linked: ["Connected", "जुड़े हुए"],
  resetPassword: ["Reset password", "पासवर्ड बदलें"],
  loginAs: ["Login as", "इनके नाम से लॉग इन"],
  actingBanner: ["Owner view: you are logged in as this school's headmaster. Changes you make here are real.", "ओनर व्यू: आप इस स्कूल के हेडमास्टर के नाम से लॉग इन हैं। यहाँ किए बदलाव असली हैं।"],
  backToOwner: ["Back to owner", "ओनर पर वापस"],
  promptPassword: ["New temporary password (8 or more characters):", "नया अस्थायी पासवर्ड (कम से कम 8 अक्षर):"],
  grade: ["Class", "क्लास"],
  section: ["Section", "सेक्शन"],
  addClass: ["Add a class", "नई क्लास जोड़ें"],
  addTeacher: ["Add a teacher", "नया टीचर जोड़ें"],
  addStudent: ["Add a student", "नया बच्चा जोड़ें"],
  name: ["Name", "नाम"],
  studentName: ["Student's name", "बच्चे का नाम"],
  parentName: ["Parent's name", "माता/पिता का नाम"],
  parentPhone: ["Parent's phone", "माता/पिता का फ़ोन"],
  siblingNote: ["Use the same phone number for brothers and sisters. They will be linked to one parent, even in different classes.", "भाई-बहनों के लिए एक ही फ़ोन नंबर डालें। वे एक ही माता-पिता से जुड़ जाएँगे, चाहे क्लास अलग हो।"],
  assignedClasses: ["Classes", "क्लास"],
  joinCode: ["App code", "ऐप कोड"],
  codeNote: ["Give this code to the parent. They enter it in the app under 🔒 and give consent. It works once.", "यह कोड माता-पिता को दें। वे ऐप में 🔒 के अंदर इसे डालकर सहमति देंगे। कोड एक बार ही चलता है।"],
  newCode: ["New code", "नया कोड"],
  confirmNewCode: ["A new code disconnects the current phone and removes its shared progress. Continue?", "नया कोड बनाने से अभी का फ़ोन हट जाएगा और उसकी भेजी जानकारी मिट जाएगी। आगे बढ़ें?"],
  confirmRemove: ["This permanently removes the student and their progress. Continue?", "इससे बच्चा और उसकी पूरी जानकारी हमेशा के लिए मिट जाएगी। आगे बढ़ें?"],
  confirmRemoveTeacher: ["Remove this teacher?", "इस टीचर को हटाएँ?"],
  confirmRemoveClass: ["Remove this class?", "यह क्लास हटाएँ?"],
  children: ["Children", "बच्चे"],
  phone: ["Phone", "फ़ोन"],
  noneYet: ["Nothing here yet.", "अभी यहाँ कुछ नहीं है।"],
  myClasses: ["My classes", "मेरी क्लास"],
  classWord: ["Class", "क्लास"],
  connectedOf: ["Connected", "जुड़े हुए"],
  activeWeek: ["Active this week", "इस हफ़्ते सक्रिय"],
  minutesWeek: ["Minutes this week", "इस हफ़्ते के मिनट"],
  lessons: ["Lessons passed", "पूरे लेसन"],
  stars: ["Stars", "स्टार"],
  level: ["Level", "लेवल"],
  lastUpdate: ["Last update", "आख़िरी अपडेट"],
  notConnected: ["Not connected", "नहीं जुड़ा"],
  words: ["Words learned", "सीखे शब्द"],
  sentences: ["Sentences spoken", "बोली गई लाइनें"],
  streak: ["Day streak", "लगातार दिन"],
  needHelpClass: ["What to reteach", "क्या दोबारा पढ़ाएँ"],
  needHelpSchool: ["Where the school needs help", "स्कूल में कहाँ मदद चाहिए"],
  needHelpStudent: ["Needs help with", "इनमें मदद चाहिए"],
  nothingWeak: ["No weak areas showing yet.", "अभी कोई कमज़ोर जगह नहीं दिखी।"],
  caution: ["A low number can simply mean no phone at home. Speech checking is sometimes wrong. Please use this to help children, not to rank them.", "कम नंबर का मतलब यह भी हो सकता है कि घर पर फ़ोन नहीं है। आवाज़ की जाँच कभी-कभी गलत होती है। इसे बच्चों की मदद के लिए इस्तेमाल करें, उनकी तुलना के लिए नहीं।"],
  sevenDays: ["Minutes in the last 7 days", "पिछले 7 दिनों के मिनट"],
  notConnectedHelp: ["This child's phone isn't connected yet, so there's no progress to show.", "इस बच्चे का फ़ोन अभी जुड़ा नहीं है, इसलिए दिखाने को कुछ नहीं है।"],
  loading: ["Loading…", "लोड हो रहा है…"],
  lessonsTitle: ["📘 Lessons", "📘 लेसन"],
  readingTitle: ["📖 Reading", "📖 पढ़ना"],
  readingLevel: ["Reading level", "पढ़ने का लेवल"],
  passagesRead: ["Stories read", "पढ़ी गई कहानियाँ"],
  readingAccuracy: ["Words read right", "सही पढ़े शब्द"],
  wordsPerMinute: ["Words per minute", "शब्द प्रति मिनट"],
  expressBest: ["🚂 Tinku Express best (words per minute)", "🚂 टिंकू एक्सप्रेस सबसे तेज़ (शब्द प्रति मिनट)"],
  readingNote: ["Accuracy and speed are from the last 10 readings aloud.", "सही शब्द और स्पीड पिछली 10 बार ज़ोर से पढ़ने से निकाले गए हैं।"],
  wordPowerTitle: ["📚 Word Power", "📚 वर्ड पावर"],
  powerWords: ["Word Power words", "वर्ड पावर शब्द"],
  powerLearned: ["Words learned", "सीखे शब्द"],
  powerMastered: ["Words remembered well", "अच्छे से याद शब्द"],
  monthlyTest: ["Last monthly test", "पिछला मासिक टेस्ट"],
  oldApp: ["This child's phone has an older version of the app. Reading and Word Power will show here after it updates.", "इस बच्चे के फ़ोन में ऐप का पुराना वर्ज़न है। ऐप अपडेट होने के बाद पढ़ना और वर्ड पावर यहाँ दिखेंगे।"],
  hello: ["Namaste", "नमस्ते"],
  newTest: ["Make a new test", "नया टेस्ट बनाएँ"],
  testsResults: ["Tests & results", "टेस्ट और नतीजे"],
  syllabus: ["Syllabus progress", "सिलेबस की प्रगति"],
  progress: ["Children's progress", "बच्चों की प्रगति"],
  recentTests: ["Recent tests", "हाल के टेस्ट"],
  tests: ["Tests", "टेस्ट"],
  noTestsYet: ["No tests yet. Press “Make a new test” to start.", "अभी कोई टेस्ट नहीं। शुरू करने के लिए “नया टेस्ट बनाएँ” दबाएँ।"],
  bookTest: ["Book test", "किताब का टेस्ट"],
  courseTest: ["App course test", "ऐप कोर्स का टेस्ट"],
  bookTestHelp: ["Questions from the MP Board English textbook exercises", "MP Board इंग्लिश किताब के अभ्यास के सवाल"],
  courseTestHelp: ["Questions from the app's own lessons (not in the book)", "ऐप के अपने लेसन के सवाल (किताब से अलग)"],
  bookOnly68: ["Book tests are for Classes 6, 7 and 8", "किताब के टेस्ट कक्षा 6, 7 और 8 के लिए हैं"],
  stepKind: ["Which kind of test?", "किस तरह का टेस्ट?"],
  stepLessons: ["Pick the lessons", "लेसन चुनें"],
  stepLessonsHelp: ["Tick one lesson or several. Questions are picked from the lessons you tick.", "एक या कई लेसन पर ✔ लगाएँ। सवाल इन्हीं लेसन से चुने जाएँगे।"],
  stepSettings: ["Questions, timer and last date", "सवाल, समय और आख़िरी तारीख़"],
  stepCheck: ["Check the questions and send", "सवाल देखें और भेजें"],
  stepCheckHelp: ["These questions were picked for you. Change any one with 🔄, or remove it.", "ये सवाल अपने आप चुने गए हैं। किसी को 🔄 से बदलें या हटा दें।"],
  questionsWord: ["questions", "सवाल"],
  questionsReady: ["Questions available", "उपलब्ध सवाल"],
  howManyQuestions: ["How many questions", "कितने सवाल"],
  timer: ["Time for the test", "टेस्ट का समय"],
  timerHelp: ["The timer starts when the child opens the test. When time is up, the test is handed in by itself.", "बच्चा टेस्ट खोलेगा तब समय शुरू होगा। समय ख़त्म होने पर टेस्ट अपने आप जमा हो जाएगा।"],
  min: ["min", "मिनट"],
  lastDate: ["Last date", "आख़िरी तारीख़"],
  testName: ["Test name", "टेस्ट का नाम"],
  testType: ["Type", "प्रकार"],
  seeQuestions: ["See the questions", "सवाल देखें"],
  swap: ["Change", "बदलें"],
  newQuestions: ["Pick all new questions", "सारे नए सवाल चुनें"],
  publish: ["Send test to children", "टेस्ट बच्चों को भेजें"],
  confirmPublish: ["Send this test of {n} questions ({m} minutes) to all children of the class? Their phones will get a notification.", "क्या {n} सवाल ({m} मिनट) का यह टेस्ट क्लास के सब बच्चों को भेजें? उनके फ़ोन पर सूचना जाएगी।"],
  published: ["Test sent! Children's phones will show it within about an hour, or as soon as the app is opened.", "टेस्ट भेज दिया! बच्चों के फ़ोन पर लगभग एक घंटे में, या ऐप खोलते ही दिखेगा।"],
  lessonWord: ["Lesson", "पाठ"],
  lessonsWord: ["lessons", "लेसन"],
  testWord: ["test", "टेस्ट"],
  done: ["Done", "दिया"],
  average: ["Average", "औसत"],
  notYet: ["Not yet", "अभी नहीं दिया"],
  open: ["Open", "चालू"],
  closed: ["Closed", "बंद"],
  seeResults: ["See results", "नतीजे देखें"],
  score: ["Score", "नंबर"],
  timeTaken: ["Time taken", "लिया गया समय"],
  submitted: ["Handed in", "जमा किया"],
  seeAnswers: ["See answers", "जवाब देखें"],
  hardQuestions: ["Questions the class found hard", "क्लास को कौन से सवाल कठिन लगे"],
  hardQuestionsHelp: ["Hardest first. Explain these again in class.", "सबसे कठिन पहले। इन्हें क्लास में दोबारा समझाएँ।"],
  gotRight: ["right", "सही"],
  allQuestions: ["All questions in this test", "इस टेस्ट के सारे सवाल"],
  deleteTest: ["Delete this test", "यह टेस्ट मिटाएँ"],
  confirmDeleteTest: ["Delete this test and all its results?", "यह टेस्ट और इसके सारे नतीजे मिटाएँ?"],
  rightAnswer: ["Right answer", "सही जवाब"],
  childAnswer: ["Child's answer", "बच्चे का जवाब"],
  noAnswer: ["(not answered: time ran out)", "(जवाब नहीं दिया: समय ख़त्म)"],
  notStarted: ["Not started", "शुरू नहीं किया"],
  syllabusHelp: ["How far each child has practised each chapter of the textbook in the app. Children practise at home, so use this to help, not to rank.", "हर बच्चे ने किताब के हर पाठ का कितना अभ्यास ऐप में किया है। बच्चे घर पर अभ्यास करते हैं, इसे मदद के लिए इस्तेमाल करें, तुलना के लिए नहीं।"],
  chapterSummary: ["Chapters", "पाठ"],
  finished: ["Finished", "पूरा किया"],
  started: ["Started", "शुरू किया"],
  eachChild: ["Each child", "हर बच्चा"],
  syllabusLegend: ["✅ chapter finished · 3/9 = 3 of 9 parts done · ★ stars in the chapter test · – not started · · phone not connected", "✅ पाठ पूरा · 3/9 = 9 में से 3 हिस्से पूरे · ★ पाठ के टेस्ट के स्टार · – शुरू नहीं · · फ़ोन नहीं जुड़ा"],
  bookProgress: ["📕 Textbook chapters", "📕 किताब के पाठ"],
  testsTaken: ["📝 School tests", "📝 स्कूल के टेस्ट"],
  pending: ["Not taken yet", "अभी नहीं दिया"],
  oldConsent: ["Tests and chapter progress show after the parent agrees to the updated consent in the app.", "माता-पिता के ऐप में नई सहमति देने के बाद टेस्ट और पाठ की प्रगति यहाँ दिखेगी।"],
};

const ERRORS = {
  bad_login: ["Wrong username or password.", "यूज़रनेम या पासवर्ड गलत है।"],
  locked: ["Too many wrong tries. Please wait 15 minutes.", "बहुत बार गलत कोशिश। 15 मिनट रुकिए।"],
  wrong_password: ["The current password is wrong.", "अभी का पासवर्ड गलत है।"],
  weak_password: ["The password needs 8 or more characters.", "पासवर्ड में कम से कम 8 अक्षर चाहिए।"],
  username_taken: ["That username is already in use.", "यह यूज़रनेम पहले से है।"],
  bad_username: ["Username: 3 or more letters or numbers, no spaces.", "यूज़रनेम: कम से कम 3 अक्षर या अंक, बिना स्पेस।"],
  bad_phone: ["Please enter a valid phone number.", "सही फ़ोन नंबर डालें।"],
  class_exists: ["This class already exists.", "यह क्लास पहले से है।"],
  class_not_empty: ["Move or remove its students first.", "पहले इसके बच्चों को हटाएँ या दूसरी क्लास में डालें।"],
  invalid: ["Please check what you entered.", "भरी गई जानकारी जाँच लें।"],
  too_many_questions: ["A test can have 1 to 50 questions.", "टेस्ट में 1 से 50 सवाल हो सकते हैं।"],
  bad_date: ["Please pick a last date from today up to 60 days ahead.", "आख़िरी तारीख़ आज से 60 दिन के अंदर चुनें।"],
};

let lang = 1; // 0 English, 1 Hindi
try { lang = localStorage.getItem("lang") === "en" ? 0 : 1; } catch { /* private mode: keep the default */ }
const t = (key) => WORDS[key][lang];
const locale = () => (lang ? "hi-IN" : "en-IN");

let skills = { names: {}, broad: [] };
const skillName = (id) => skills.names[id]?.[lang ? "hi" : "en"] ?? id;
const specificFirst = (list, get) => [...list].sort((a, b) => skills.broad.includes(get(a)) - skills.broad.includes(get(b)));

function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs)) {
    if (key.startsWith("on")) el.addEventListener(key.slice(2), value);
    else if (key === "class") el.className = value;
    else if (value === true) el.setAttribute(key, "");
    else if (value !== false && value != null) el.setAttribute(key, value);
  }
  for (const child of children.flat()) if (child != null && child !== false) el.append(child);
  return el;
}

const root = document.getElementById("app");
function show(...nodes) { root.replaceChildren(...nodes); window.scrollTo(0, 0); }

// One spinner for the whole site: it appears whenever the server is being asked for something
// (opening a page, saving, logging in), and blocks clicks so nothing is pressed twice.
// It waits a moment before showing, so quick answers don't flash it.
const loader = h("div", { class: "loading", role: "status", "aria-live": "polite", hidden: true },
  h("div", { class: "loading-box" }, h("div", { class: "spin", "aria-hidden": "true" }), h("span", { class: "loading-text" })));
document.body.append(loader);
let waiting = 0;
let loaderTimer = null;
function busy(change) {
  waiting = Math.max(0, waiting + change);
  if (waiting > 0 && loader.hidden && !loaderTimer) {
    loaderTimer = setTimeout(() => {
      loaderTimer = null;
      if (waiting > 0) { loader.querySelector(".loading-text").textContent = t("loading"); loader.hidden = false; }
    }, 150);
  } else if (waiting === 0) {
    clearTimeout(loaderTimer);
    loaderTimer = null;
    loader.hidden = true;
  }
}

async function api(method, path, body) {
  busy(1);
  try {
    const response = await fetch(path, {
      method,
      headers: body ? { "content-type": "application/json" } : {},
      body: body ? JSON.stringify(body) : undefined,
    });
    const data = await response.json();
    if (!response.ok) throw Object.assign(new Error(data.error), { code: data.error, status: response.status });
    return data;
  } finally {
    busy(-1);
  }
}

const errorText = (error) => (ERRORS[error.code] ?? ERRORS.invalid)[lang];

/** A form that posts its fields and shows any error next to the button. */
function form(fields, submitLabel, onSubmit, extra) {
  const message = h("p", { class: "error", role: "alert" });
  const el = h("form", { class: "stack" },
    h("div", { class: "row" }, fields),
    extra,
    h("div", {}, h("button", { class: "primary" }, submitLabel)),
    message,
  );
  el.addEventListener("submit", async (event) => {
    event.preventDefault();
    message.textContent = "";
    try {
      await onSubmit(Object.fromEntries(new FormData(el)));
    } catch (error) {
      message.textContent = errorText(error);
    }
  });
  return el;
}

const field = (label, name, attrs = {}) => h("label", {}, label, h("input", { name, required: true, ...attrs }));

function classSelect(classes, name = "classId", selected) {
  return h("select", { name, required: true }, classes.map((c) => h("option", { value: c.id, selected: c.id === selected }, classLabel(c))));
}
const classLabel = (c) => `${c.grade} ${c.section}`;

function table(headers, rows) {
  if (!rows.length) return h("p", { class: "muted" }, t("noneYet"));
  return h("div", { class: "table-wrap" },
    h("table", {},
      h("thead", {}, h("tr", {}, headers.map((header) => h("th", { scope: "col", class: header.num ? "num" : undefined }, header.label ?? header)))),
      h("tbody", {}, rows.map((row) => h("tr", {}, row.map((cell, i) => {
        const wrapped = typeof cell === "object" && !(cell instanceof Node);
        return h("td", { class: [headers[i].num ? "num" : "", wrapped ? "wrap" : ""].join(" ").trim() || undefined }, wrapped ? cell.wrap : cell);
      })))),
    ),
  );
}

const card = (title, ...body) => h("section", { class: "card stack" }, title ? h("h2", {}, title) : null, body);
const tile = (value, label) => h("div", { class: "tile" }, h("div", { class: "value" }, String(value ?? "–")), h("div", { class: "label" }, label));
const orDash = (value, format = String) => (value == null ? "–" : format(value));
const starsLabel = (n) => "★".repeat(n) + "☆".repeat(Math.max(0, 3 - n));
const dateLabel = (ms) => (ms ? new Date(ms).toLocaleDateString(locale(), { day: "numeric", month: "short" }) : "–");

function header(me, tabs, active, onTab) {
  return [
    me.actingOwner ? h("div", { class: "acting", role: "status" },
      h("span", {}, t("actingBanner")),
      h("button", { onclick: async () => { await api("POST", "/api/owner/return"); start(); } }, t("backToOwner")),
    ) : null,
    h("header", { class: "top" },
      h("span", { class: "title" }, me.school ?? t("appName")),
      h("span", { class: "spacer" }),
      h("span", { class: "muted small" }, me.name),
      h("button", { onclick: () => { lang = lang ? 0 : 1; try { localStorage.setItem("lang", lang ? "hi" : "en"); } catch { /* not saved */ } start(); } }, lang ? "English" : "हिंदी"),
      me.actingOwner ? null : h("button", { onclick: async () => { await api("POST", "/api/logout"); start(); } }, t("logout")),
    ),
    tabs ? h("nav", { class: "tabs" }, tabs.map(([key, label]) => h("button", { class: key === active ? "on" : undefined, onclick: () => onTab(key) }, label))) : null,
  ].filter(Boolean);
}

/** Shared bar list: which skills how many children are weak in. */
function weakMeters(list) {
  if (!list.length) return h("p", { class: "muted" }, t("nothingWeak"));
  return h("div", { class: "meter" }, specificFirst(list, (w) => w.skill).slice(0, 8).flatMap((w) => [
    h("span", {}, skillName(w.skill)),
    h("div", { class: "track", role: "img", "aria-label": `${w.students} / ${w.total}` }, h("div", { class: "fill", style: `width:${Math.round((w.students / Math.max(w.total, 1)) * 100)}%` })),
    h("span", { class: "small" }, `${w.students} / ${w.total}`),
  ]));
}

function weekChart(days) {
  const top = Math.max(1, ...days.map((d) => d.minutes));
  const weekday = new Intl.DateTimeFormat(locale(), { weekday: "short", timeZone: "UTC" });
  return h("div", {},
    h("div", { class: "week" }, days.map((d) => h("div", { class: "col", title: `${d.minutes}` },
      d.minutes > 0 ? h("span", {}, String(d.minutes)) : null,
      d.minutes > 0 ? h("div", { class: "bar", style: `height:${Math.max(3, Math.round((d.minutes / top) * 80))}%` }) : null,
    ))),
    h("div", { class: "week-labels" }, days.map((d) => h("span", {}, weekday.format(new Date(d.day * 86_400_000))))),
  );
}

// ---------- Reports (headmaster and teacher) ----------

/** Reading ladder, Tinku Express and Word Power for one child. Phones with an older app don't send these. */
function newTrackers(p) {
  const r = p.reading ?? {};
  const w = p.wordPower ?? {};
  if (r.passagesRead == null && w.learned == null) return card(null, h("p", { class: "muted" }, t("oldApp")));
  return [
    card(t("readingTitle"),
      h("div", { class: "tiles" },
        tile(orDash(r.level, (n) => `R${n}`), t("readingLevel")),
        tile(r.passagesRead, t("passagesRead")),
        tile(orDash(r.accuracy, (n) => `${n}%`), t("readingAccuracy")),
        tile(r.wordsPerMinute, t("wordsPerMinute")),
        tile(r.expressBestWpm, t("expressBest")),
      ),
      h("p", { class: "muted small" }, t("readingNote")),
    ),
    card(t("wordPowerTitle"),
      h("div", { class: "tiles" },
        tile(w.learned, t("powerLearned")),
        tile(w.mastered, t("powerMastered")),
        tile(orDash(w.monthlyTestStars, starsLabel), t("monthlyTest")),
      ),
    ),
  ];
}

async function studentPage(me, id, onBack) {
  const again = () => studentPage(me, id, onBack);
  const r = await api("GET", `/api/students/${id}/report`);
  const p = r.progress;
  const book = await bookCatalog(r.student.grade);
  show(...header(me), h("main", {},
    h("div", {}, h("button", { onclick: onBack }, `← ${t("back")}`)),
    h("h1", {}, `${r.student.name} · ${t("classWord")} ${r.student.grade} ${r.student.section}`),
    p ? card(t("lessonsTitle"), h("div", { class: "tiles" },
      tile(p.lessonsPassed, t("lessons")), tile(p.stars, t("stars")), tile(p.wordsLearned, t("words")),
      tile(p.sentencesSpoken, t("sentences")), tile(p.streak, t("streak")), tile(p.level, t("level")),
    )) : card(null, h("p", { class: "muted" }, t("notConnectedHelp"))),
    p ? newTrackers(p) : null,
    p ? h("p", { class: "muted small" }, `${t("lastUpdate")}: ${dateLabel(p.lastSync)}`) : null,
    r.days && p ? card(t("sevenDays"), weekChart(r.days)) : null,
    p && r.weakSkills ? card(t("needHelpStudent"),
      r.weakSkills.length ? h("ul", {}, specificFirst(r.weakSkills, (s) => s).slice(0, 8).map((s) => h("li", {}, skillName(s)))) : h("p", { class: "muted" }, t("nothingWeak"))) : null,
    p && book && r.chapters ? card(t("bookProgress"),
      Object.keys(r.chapters).length ? table([t("lessonWord"), { label: t("finished"), num: true }, { label: t("stars"), num: true }],
        book.chapters.map((ch) => [
          { wrap: `${ch.emoji} ${ch.number}. ${ch.title}` },
          chapterCell(r.chapters[ch.id]),
          r.chapters[ch.id]?.testStars == null ? "–" : starsLabel(r.chapters[ch.id].testStars),
        ])) : h("p", { class: "muted" }, t("oldConsent"))) : null,
    r.tests ? card(t("testsTaken"), table(
      [t("testName"), t("testType"), { label: t("score"), num: true }, t("submitted"), ""],
      r.tests.map((x) => [
        { wrap: x.title }, kindLabel(x.kind),
        x.result ? `${x.result.score} / ${x.result.total} (${percent(x.result.score, x.result.total)}%)` : "–",
        x.result ? dateLabel(x.result.submittedAt) : (x.closesAt > Date.now() ? `⏳ ${t("pending")}` : t("notYet")),
        x.result ? h("button", { onclick: () => answersPage(me, x.id, id, r.student.grade, again) }, t("seeAnswers")) : "",
      ]),
    )) : null,
    r.parents?.length ? card(t("parents"), table([t("name"), t("phone")], r.parents.map((x) => [x.name, x.phone]))) : null,
  ));
}

async function classPage(me, id, onBack) {
  const again = () => classPage(me, id, onBack);
  const r = await api("GET", `/api/classes/${id}/report`);
  const linked = r.students.filter((s) => s.linked).length;
  show(...header(me), h("main", {},
    h("div", {}, h("button", { onclick: onBack }, `← ${t("back")}`)),
    h("h1", {}, `${t("classWord")} ${classLabel(r.class)}`),
    classActions(me, r.class, again, { progress: false }),
    h("div", { class: "tiles" },
      tile(`${linked} / ${r.students.length}`, t("connectedOf")),
      tile(r.students.filter((s) => s.weekMinutes > 0).length, t("activeWeek")),
    ),
    card(t("needHelpClass"), weakMeters(r.weakSkills)),
    card(t("students"),
      table(
        [
          t("name"), { label: t("lessons"), num: true }, { label: t("stars"), num: true },
          { label: t("readingLevel"), num: true }, { label: t("wordsPerMinute"), num: true }, { label: t("powerWords"), num: true },
          { label: t("minutesWeek"), num: true }, t("lastUpdate"),
        ],
        r.students.map((s) => [
          h("button", { class: "link", onclick: () => studentPage(me, s.id, again) }, s.name),
          s.linked ? String(s.lessonsPassed) : "–",
          s.linked ? String(s.stars) : "–",
          s.linked ? orDash(s.readingLevel, (n) => `R${n}`) : "–",
          s.linked ? orDash(s.wordsPerMinute) : "–",
          s.linked ? orDash(s.powerWords) : "–",
          s.linked ? String(s.weekMinutes) : "–",
          s.linked ? dateLabel(s.lastSync) : t("notConnected"),
        ]),
      ),
      h("p", { class: "muted small" }, t("caution")),
    ),
  ));
}

// ---------- Syllabus and tests (teacher and headmaster) ----------

// The questions a test can use. book-<class>.json has the textbook's own questions by chapter,
// course.json the app's main course by level. Loaded once, only when a test page needs them.
const catalogs = new Map();
async function catalog(name) {
  if (!catalogs.has(name)) {
    busy(1);
    try {
      const response = await fetch(`/catalog/${name}.json`);
      catalogs.set(name, response.ok ? await response.json() : null);
    } catch {
      catalogs.set(name, null);
    } finally {
      busy(-1);
    }
  }
  return catalogs.get(name);
}
const hasBook = (grade) => grade >= 6 && grade <= 8;
const bookCatalog = (grade) => (hasBook(grade) ? catalog(`book-${grade}`) : Promise.resolve(null));

/** Every question by its step id, from whichever catalog a test uses, to show its text and right answer. */
async function questionLookup(kind, grade) {
  const data = kind === "book" ? await bookCatalog(grade) : await catalog("course");
  const map = new Map();
  if (!data) return map;
  const groups = kind === "book" ? data.chapters : data.levels.flatMap((l) => l.lessons);
  for (const g of groups) for (const q of g.questions) map.set(q.s, q);
  return map;
}

const kindLabel = (kind) => (kind === "book" ? t("bookTest") : t("courseTest"));
const percent = (score, total) => (total ? Math.round((score / total) * 100) : 0);
const isOpen = (test) => test.closesAt > Date.now();
const minutesLabel = (seconds) => `${Math.max(1, Math.round(seconds / 60))} ${t("min")}`;

/** Big buttons for everything a teacher does with one class. */
function classActions(me, c, back, { progress = true } = {}) {
  return h("div", { class: "actions" },
    h("button", { class: "action primary", onclick: () => testWizard(me, c, back) }, h("span", { class: "big" }, "📝"), t("newTest")),
    h("button", { class: "action", onclick: () => classTestsPage(me, c, back) }, h("span", { class: "big" }, "📋"), t("testsResults")),
    hasBook(c.grade) ? h("button", { class: "action", onclick: () => syllabusPage(me, c, back) }, h("span", { class: "big" }, "📕"), t("syllabus")) : null,
    progress ? h("button", { class: "action", onclick: () => classPage(me, c.id, back) }, h("span", { class: "big" }, "📊"), t("progress")) : null,
  );
}

/** A list of tests with how many children took each and the average score. */
function testsTable(me, tests, back, showClass) {
  return table(
    [
      t("testName"), ...(showClass ? [t("classWord")] : []), t("testType"), { label: t("done"), num: true }, { label: t("average"), num: true }, t("lastDate"), "",
    ],
    tests.map((x) => [
      { wrap: h("span", {}, x.title, " ", h("span", { class: isOpen(x) ? "badge open" : "badge" }, isOpen(x) ? t("open") : t("closed"))) },
      ...(showClass ? [`${x.grade} ${x.section}`] : []),
      kindLabel(x.kind),
      `${x.done} / ${x.students}`,
      x.average == null ? "–" : `${x.average}%`,
      dateLabel(x.closesAt),
      h("button", { onclick: () => testPage(me, x.id, back) }, t("seeResults")),
    ]),
  );
}

async function classTestsPage(me, c, back) {
  const again = () => classTestsPage(me, c, back);
  const { tests } = await api("GET", `/api/classes/${c.id}/tests`);
  show(...header(me), h("main", {},
    h("div", {}, h("button", { onclick: back }, `← ${t("back")}`)),
    h("h1", {}, `📋 ${t("testsResults")} · ${t("classWord")} ${classLabel(c)}`),
    h("div", {}, h("button", { class: "primary", onclick: () => testWizard(me, c, again) }, `📝 ${t("newTest")}`)),
    card(null, tests.length ? testsTable(me, tests, again, false) : h("p", { class: "muted" }, t("noTestsYet"))),
  ));
}

// ----- Making a test: four numbered steps on one page -----

const shuffle = (list) => {
  const a = [...list];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
};

function isoDate(daysFromToday) {
  const d = new Date(Date.now() + daysFromToday * 86_400_000);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

async function testWizard(me, c, back, preset = {}) {
  const [book, course] = await Promise.all([bookCatalog(c.grade), catalog("course")]);
  const state = {
    kind: preset.kind ?? (book ? null : "course"),
    picked: new Set(preset.picked ?? []), // chapter or lesson ids
    count: 10,
    minutes: 15,
    lastDate: isoDate(3),
    title: "",
    questions: null, // the drawn questions, once the teacher asks to see them
  };

  // Every group (chapter or course lesson) the teacher can tick, for the chosen kind.
  const groups = () => (state.kind === "book"
    ? book.chapters.map((ch) => ({ id: ch.id, label: `${ch.emoji} ${t("lessonWord")} ${ch.number} · ${ch.title}`, questions: ch.questions }))
    : course.levels.flatMap((lv) => lv.lessons.map((l) => ({
      id: l.id, level: lv, label: `${l.emoji} ${l.title[lang ? "hi" : "en"]}`, questions: l.questions,
    }))).filter((g) => g.questions.length));
  const pool = () => groups().filter((g) => state.picked.has(g.id)).flatMap((g) => g.questions);
  const autoTitle = () => {
    const chosen = groups().filter((g) => state.picked.has(g.id));
    if (state.kind === "book") {
      const numbers = book.chapters.filter((ch) => state.picked.has(ch.id)).map((ch) => ch.number);
      return `${t("lessonWord")} ${numbers.join(", ")} ${t("testWord")}`;
    }
    return chosen.length === 1 ? `${chosen[0].label.replace(/^\S+\s/, "")} ${t("testWord")}` : `${t("courseTest")} (${chosen.length} ${t("lessonsWord")})`;
  };
  const draw = () => { state.questions = shuffle(pool()).slice(0, state.count); };

  const step = (n, title, done, ...body) => h("section", { class: `card stack step ${done ? "done" : ""}` },
    h("h2", {}, h("span", { class: "num" }, String(n)), title), body);

  function render() {
    const message = h("p", { class: "error", role: "alert" });
    const available = pool().length;
    const parts = [
      h("div", {}, h("button", { onclick: back }, `← ${t("back")}`)),
      h("h1", {}, `📝 ${t("newTest")} · ${t("classWord")} ${classLabel(c)}`),

      step(1, t("stepKind"), state.kind != null,
        h("div", { class: "choices" },
          h("button", {
            class: `choice ${state.kind === "book" ? "on" : ""}`, disabled: !book,
            onclick: () => { state.kind = "book"; state.picked.clear(); state.questions = null; render(); },
          }, h("span", { class: "big" }, "📕"), h("strong", {}, t("bookTest")), h("span", { class: "muted small" }, book ? t("bookTestHelp") : t("bookOnly68"))),
          h("button", {
            class: `choice ${state.kind === "course" ? "on" : ""}`,
            onclick: () => { state.kind = "course"; state.picked.clear(); state.questions = null; render(); },
          }, h("span", { class: "big" }, "⭐"), h("strong", {}, t("courseTest")), h("span", { class: "muted small" }, t("courseTestHelp"))),
        ),
      ),
    ];

    if (state.kind) {
      const list = groups();
      const box = (g) => h("label", { class: `pick ${state.picked.has(g.id) ? "on" : ""}` },
        h("input", {
          type: "checkbox", checked: state.picked.has(g.id),
          onchange: (e) => { if (e.target.checked) state.picked.add(g.id); else state.picked.delete(g.id); state.questions = null; render(); },
        }),
        h("span", {}, g.label), h("span", { class: "muted small" }, `${g.questions.length} ${t("questionsWord")}`));
      const picker = state.kind === "book"
        ? h("div", { class: "picks" }, list.map(box))
        : h("div", { class: "stack" }, course.levels.map((lv) => {
          const inLevel = list.filter((g) => g.level === lv);
          if (!inLevel.length) return null;
          return h("details", { open: inLevel.some((g) => state.picked.has(g.id)) || undefined },
            h("summary", {}, `${t("level")} ${lv.number} · ${lv.title[lang ? "hi" : "en"]} (${inLevel.length})`),
            h("div", { class: "picks" }, inLevel.map(box)));
        }));
      parts.push(step(2, t("stepLessons"), state.picked.size > 0,
        h("p", { class: "muted small" }, t("stepLessonsHelp")),
        picker,
        h("p", { class: "strong" }, `${t("questionsReady")}: ${available}`),
      ));
    }

    if (state.kind && state.picked.size) {
      const counts = [5, 10, 15, 20, 25, 30, 40, 50].filter((n) => n <= available);
      if (!counts.includes(state.count)) state.count = counts.length ? counts[counts.length - 1] : available;
      if (!counts.length) counts.push(available);
      parts.push(step(3, t("stepSettings"), true,
        h("div", { class: "row" },
          h("label", {}, t("howManyQuestions"),
            h("select", { onchange: (e) => { state.count = Number(e.target.value); state.questions = null; render(); } },
              counts.map((n) => h("option", { value: n, selected: n === state.count }, String(n))))),
          h("label", {}, `⏱️ ${t("timer")}`,
            h("select", { onchange: (e) => { state.minutes = Number(e.target.value); } },
              [5, 10, 15, 20, 30, 45, 60, 90].map((n) => h("option", { value: n, selected: n === state.minutes }, `${n} ${t("min")}`)))),
          h("label", {}, `📅 ${t("lastDate")}`,
            h("input", { type: "date", value: state.lastDate, min: isoDate(0), max: isoDate(60), onchange: (e) => { state.lastDate = e.target.value; } })),
        ),
        h("label", {}, t("testName"),
          h("input", { value: state.title || autoTitle(), maxlength: 80, oninput: (e) => { state.title = e.target.value; } })),
        h("p", { class: "muted small" }, t("timerHelp")),
        state.questions ? null : h("div", {}, h("button", { class: "primary", onclick: () => { draw(); render(); } }, `👀 ${t("seeQuestions")}`)),
      ));
    }

    if (state.questions) {
      const used = new Set(state.questions.map((q) => q.s));
      const spare = () => pool().filter((q) => !used.has(q.s));
      parts.push(step(4, t("stepCheck"), true,
        h("p", { class: "muted small" }, t("stepCheckHelp")),
        h("ol", { class: "questions" }, state.questions.map((q, i) => h("li", {},
          h("div", { class: "q" }, q.q),
          h("div", { class: "a" }, `✔ ${q.a}`),
          h("div", { class: "q-actions" },
            spare().length ? h("button", { onclick: () => { state.questions[i] = shuffle(spare())[0]; render(); } }, `🔄 ${t("swap")}`) : null,
            state.questions.length > 1 ? h("button", { class: "danger", onclick: () => { state.questions.splice(i, 1); render(); } }, `✖ ${t("remove")}`) : null,
          ),
        ))),
        h("div", { class: "row" },
          h("button", { onclick: () => { draw(); render(); } }, `🔀 ${t("newQuestions")}`),
        ),
        h("div", { class: "publish" },
          h("button", {
            class: "primary huge",
            onclick: async () => {
              message.textContent = "";
              const title = (state.title || autoTitle()).trim();
              if (!confirm(t("confirmPublish").replace("{n}", state.questions.length).replace("{m}", state.minutes))) return;
              try {
                const { id } = await api("POST", `/api/classes/${c.id}/tests`, {
                  kind: state.kind, title, lessons: [...state.picked], questions: state.questions.map((q) => [q.l, q.s]),
                  minutes: state.minutes, lastDate: state.lastDate,
                });
                alert(t("published"));
                testPage(me, id, back);
              } catch (error) {
                message.textContent = errorText(error);
              }
            },
          }, `✅ ${t("publish")} (${state.questions.length} ${t("questionsWord")}, ${state.minutes} ${t("min")})`),
          message,
        ),
      ));
    }
    show(...header(me), h("main", {}, parts));
  }
  render();
}

// ----- Results -----

async function testPage(me, id, back) {
  const again = () => testPage(me, id, back);
  const r = await api("GET", `/api/tests/${id}`);
  const x = r.test;
  const lookup = await questionLookup(x.kind, x.grade);
  const took = r.students.filter((s) => s.result);
  const average = took.length ? Math.round(took.reduce((sum, s) => sum + percent(s.result.score, s.result.total), 0) / took.length) : null;
  const hard = [...r.perQuestion].sort((a, b) => a.right - b.right);
  show(...header(me), h("main", {},
    h("div", {}, h("button", { onclick: back }, `← ${t("back")}`)),
    h("h1", {}, `${x.title}`),
    h("p", { class: "muted" },
      `${t("classWord")} ${x.grade} ${x.section} · ${kindLabel(x.kind)} · ${x.questions.length} ${t("questionsWord")} · ⏱️ ${x.minutes} ${t("min")} · 📅 ${t("lastDate")}: ${dateLabel(x.closesAt)} · `,
      h("span", { class: isOpen(x) ? "badge open" : "badge" }, isOpen(x) ? t("open") : t("closed"))),
    h("div", { class: "tiles" },
      tile(`${took.length} / ${r.students.length}`, t("done")),
      tile(average == null ? "–" : `${average}%`, t("average")),
      tile(r.students.length - took.length, t("notYet")),
    ),
    card(t("students"), table(
      [t("name"), { label: t("score"), num: true }, { label: t("timeTaken"), num: true }, t("submitted"), ""],
      r.students.map((s) => s.result ? [
        s.name,
        h("span", { class: percent(s.result.score, s.result.total) >= 60 ? "good" : "warn" }, `${s.result.score} / ${s.result.total} (${percent(s.result.score, s.result.total)}%)`),
        minutesLabel(s.result.seconds),
        dateLabel(s.result.submittedAt),
        h("button", { onclick: () => answersPage(me, x.id, s.id, x.grade, again) }, t("seeAnswers")),
      ] : [
        s.name, "–", "–", s.linked ? `⏳ ${t("notYet")}` : t("notConnected"), "",
      ]),
    )),
    took.length ? card(t("hardQuestions"),
      h("p", { class: "muted small" }, t("hardQuestionsHelp")),
      h("div", { class: "meter" }, hard.flatMap((q) => {
        const info = lookup.get(q.step);
        return [
          h("span", { class: "wrap-text" }, info ? info.q : q.step, info ? h("span", { class: "muted small" }, ` (✔ ${info.a})`) : null),
          h("div", { class: "track", role: "img", "aria-label": `${q.right} / ${q.answered}` }, h("div", { class: "fill", style: `width:${percent(q.right, q.answered)}%` })),
          h("span", { class: "small" }, `${q.right} / ${q.answered} ${t("gotRight")}`),
        ];
      })),
    ) : null,
    card(t("allQuestions"), h("ol", { class: "questions" }, x.questions.map(([, s]) => {
      const info = lookup.get(s);
      return h("li", {}, h("div", { class: "q" }, info ? info.q : s), info ? h("div", { class: "a" }, `✔ ${info.a}`) : null);
    }))),
    h("div", {}, h("button", { class: "danger", onclick: async () => {
      if (!confirm(t("confirmDeleteTest"))) return;
      await api("DELETE", `/api/tests/${x.id}`);
      back();
    } }, `🗑️ ${t("deleteTest")}`)),
  ));
}

/** One child's answers beside the right ones. */
async function answersPage(me, testId, studentId, grade, back) {
  const r = await api("GET", `/api/tests/${testId}/students/${studentId}`);
  const lookup = await questionLookup(r.test.kind, grade);
  const res = r.result;
  show(...header(me), h("main", {},
    h("div", {}, h("button", { onclick: back }, `← ${t("back")}`)),
    h("h1", {}, `${r.student.name} · ${r.test.title}`),
    h("div", { class: "tiles" },
      tile(`${res.score} / ${res.total}`, t("score")),
      tile(`${percent(res.score, res.total)}%`, t("average")),
      tile(minutesLabel(res.seconds), t("timeTaken")),
    ),
    card(null, h("ol", { class: "questions" }, res.answers.map((a) => {
      const info = lookup.get(a.s);
      return h("li", { class: a.ok ? "right" : "wrong" },
        h("div", { class: "q" }, `${a.ok ? "✅" : "❌"} ${info ? info.q : a.s}`),
        h("div", { class: "a" }, `${t("rightAnswer")}: ${info ? info.a : "–"}`),
        a.ok ? null : h("div", { class: "given" }, `${t("childAnswer")}: ${a.given || t("noAnswer")}`),
      );
    }))),
  ));
}

// ----- Syllabus: each child's progress through the textbook, chapter by chapter -----

function chapterCell(p) {
  if (!p || p.done === 0) return h("span", { class: "cell none", title: t("notStarted") }, "–");
  const full = p.done >= p.total;
  return h("span", { class: `cell ${full ? "full" : "part"}`, title: `${p.done} / ${p.total}` },
    full ? "✅" : `${p.done}/${p.total}`,
    p.testStars != null ? h("span", { class: "stars" }, "★".repeat(p.testStars) || "☆") : null);
}

async function syllabusPage(me, c, back) {
  const again = () => syllabusPage(me, c, back);
  const [r, book] = await Promise.all([api("GET", `/api/classes/${c.id}/syllabus`), bookCatalog(c.grade)]);
  const linked = r.students.filter((s) => s.linked);
  show(...header(me), h("main", {},
    h("div", {}, h("button", { onclick: back }, `← ${t("back")}`)),
    h("h1", {}, `📕 ${t("syllabus")} · ${t("classWord")} ${classLabel(c)}`),
    h("p", { class: "muted small" }, t("syllabusHelp")),
    card(t("chapterSummary"), table(
      [t("lessonWord"), { label: t("finished"), num: true }, { label: t("started"), num: true }, ""],
      book.chapters.map((ch) => {
        const rows = linked.map((s) => s.chapters[ch.id]).filter(Boolean);
        const finished = rows.filter((p) => p.done >= p.total).length;
        const started = rows.filter((p) => p.done > 0 && p.done < p.total).length;
        return [
          { wrap: `${ch.emoji} ${ch.number}. ${ch.title}` },
          `${finished} / ${linked.length}`,
          String(started),
          h("button", { onclick: () => testWizard(me, c, again, { kind: "book", picked: [ch.id] }) }, `📝 ${t("testWord")}`),
        ];
      }),
    )),
    card(t("eachChild"),
      h("div", { class: "table-wrap" }, h("table", { class: "grid" },
        h("thead", {}, h("tr", {}, h("th", { scope: "col" }, t("name")), book.chapters.map((ch) => h("th", { scope: "col", title: ch.title, class: "num" }, String(ch.number))))),
        h("tbody", {}, r.students.map((s) => h("tr", {},
          h("td", {}, h("button", { class: "link", onclick: () => studentPage(me, s.id, again) }, s.name)),
          book.chapters.map((ch) => h("td", { class: "num" }, s.linked ? chapterCell(s.chapters[ch.id]) : h("span", { class: "cell none" }, "·"))),
        ))),
      )),
      h("p", { class: "muted small" }, t("syllabusLegend")),
    ),
  ));
}

// ---------- Teacher ----------

async function teacherHome(me) {
  const home = () => teacherHome(me);
  const [{ classes }, { tests }] = await Promise.all([api("GET", "/api/classes"), api("GET", "/api/tests")]);
  show(...header(me), h("main", {},
    h("h1", {}, `🙏 ${t("hello")}, ${me.name}`),
    classes.length ? classes.map((c) => h("section", { class: "card stack" },
      h("h2", {}, `${t("classWord")} ${classLabel(c)} · ${c.students} ${t("students")}`),
      classActions(me, c, home),
    )) : card(null, h("p", { class: "muted" }, t("noneYet"))),
    card(`📋 ${t("recentTests")}`, tests.length ? testsTable(me, tests, home, true) : h("p", { class: "muted" }, t("noTestsYet"))),
  ));
}

// ---------- Headmaster ----------

const HEAD_TABS = () => [["overview", t("overview")], ["tests", `📝 ${t("tests")}`], ["classes", t("classes")], ["teachers", t("teachers")], ["students", t("students")], ["parents", t("parents")]];

async function headmaster(me, tab = "overview") {
  const go = (next) => headmaster(me, next);
  const again = () => go(tab);
  let content;
  if (tab === "overview") {
    const o = await api("GET", "/api/overview");
    const sum = (key) => o.classes.reduce((total, c) => total + c[key], 0);
    content = [
      h("div", { class: "tiles" },
        tile(sum("students"), t("students")), tile(sum("linked"), t("connectedOf")),
        tile(sum("activeThisWeek"), t("activeWeek")), tile(sum("weekMinutes"), t("minutesWeek")),
      ),
      card(t("needHelpSchool"), weakMeters(o.weakSkills)),
      card(t("classes"),
        table(
          [
            t("classWord"), { label: t("students"), num: true }, { label: t("connectedOf"), num: true }, { label: t("activeWeek"), num: true },
            { label: t("minutesWeek"), num: true }, { label: t("lessons"), num: true }, { label: t("passagesRead"), num: true }, { label: t("powerWords"), num: true },
          ],
          o.classes.map((c) => [
            h("button", { class: "link", onclick: () => classPage(me, c.id, again) }, classLabel(c)),
            String(c.students), String(c.linked), String(c.activeThisWeek), String(c.weekMinutes), String(c.lessons),
            String(c.passagesRead ?? 0), String(c.powerWords ?? 0),
          ]),
        ),
        h("p", { class: "muted small" }, t("caution")),
      ),
    ];
  } else if (tab === "tests") {
    const [{ classes }, { tests }] = await Promise.all([api("GET", "/api/classes"), api("GET", "/api/tests")]);
    content = [
      card(`📋 ${t("recentTests")}`, tests.length ? testsTable(me, tests, again, true) : h("p", { class: "muted" }, t("noTestsYet"))),
      ...classes.map((c) => h("section", { class: "card stack" },
        h("h2", {}, `${t("classWord")} ${classLabel(c)} · ${c.students} ${t("students")}`),
        classActions(me, c, again),
      )),
    ];
  } else if (tab === "classes") {
    const { classes } = await api("GET", "/api/classes");
    content = [
      card(t("addClass"), form(
        [field(t("grade"), "grade", { type: "number", min: 1, max: 12 }), field(t("section"), "section", { maxlength: 10, placeholder: "A" })],
        t("add"),
        async (v) => { await api("POST", "/api/classes", { grade: Number(v.grade), section: v.section }); again(); },
      )),
      card(t("classes"), table([t("classWord"), { label: t("students"), num: true }, ""], classes.map((c) => [
        classLabel(c), String(c.students),
        h("button", { class: "danger", onclick: async () => {
          if (!confirm(t("confirmRemoveClass"))) return;
          try { await api("DELETE", `/api/classes/${c.id}`); again(); } catch (error) { alert(errorText(error)); }
        } }, t("remove")),
      ]))),
    ];
  } else if (tab === "teachers") {
    const [{ teachers }, { classes }] = await Promise.all([api("GET", "/api/teachers"), api("GET", "/api/classes")]);
    content = [
      card(t("addTeacher"), form(
        [field(t("name"), "name"), field(t("username"), "username", { autocomplete: "off" }), field(t("tempPassword"), "password", { minlength: 8, autocomplete: "new-password" })],
        t("add"),
        async (v) => { await api("POST", "/api/teachers", v); again(); },
        h("p", { class: "muted small" }, t("tempNote")),
      )),
      card(t("teachers"), table([t("name"), t("username"), t("assignedClasses"), ""], teachers.map((teacher) => {
        const boxes = classes.map((c) => h("label", {}, h("input", {
          type: "checkbox",
          checked: teacher.classIds.includes(c.id),
          "data-class": c.id,
          onchange: async (event) => {
            const group = event.target.closest(".checks");
            const classIds = [...group.querySelectorAll("input:checked")].map((box) => Number(box.dataset.class));
            await api("PUT", `/api/teachers/${teacher.id}/classes`, { classIds });
          },
        }), classLabel(c)));
        return [
          teacher.name, teacher.username, { wrap: h("div", { class: "checks" }, boxes) },
          h("span", {},
            h("button", { onclick: async () => {
              const value = prompt(t("promptPassword"));
              if (!value) return;
              try { await api("POST", `/api/teachers/${teacher.id}/password`, { password: value }); } catch (error) { alert(errorText(error)); }
            } }, t("resetPassword")), " ",
            h("button", { class: "danger", onclick: async () => { if (confirm(t("confirmRemoveTeacher"))) { await api("DELETE", `/api/teachers/${teacher.id}`); again(); } } }, t("remove")),
          ),
        ];
      }))),
    ];
  } else if (tab === "students") {
    const [{ students }, { classes }] = await Promise.all([api("GET", "/api/students"), api("GET", "/api/classes")]);
    content = [
      card(t("addStudent"), classes.length ? form(
        [
          field(t("studentName"), "name"),
          h("label", {}, t("grade"), classSelect(classes)),
          field(t("parentName"), "parentName"),
          field(t("parentPhone"), "parentPhone", { inputmode: "tel" }),
        ],
        t("add"),
        async (v) => {
          await api("POST", "/api/students", { name: v.name, classId: Number(v.classId), parents: [{ name: v.parentName, phone: v.parentPhone }] });
          again();
        },
        h("p", { class: "muted small" }, t("siblingNote")),
      ) : h("p", { class: "muted" }, t("addClass"))),
      card(t("students"),
        table([t("name"), t("classWord"), t("parents"), t("joinCode"), ""], students.map((s) => [
          h("button", { class: "link", onclick: () => studentPage(me, s.id, again) }, s.name),
          h("select", { "aria-label": t("classWord"), onchange: async (event) => { await api("PUT", `/api/students/${s.id}`, { classId: Number(event.target.value) }); } },
            classes.map((c) => h("option", { value: c.id, selected: c.id === s.classId }, classLabel(c)))),
          { wrap: s.parents.map((p) => `${p.name} (${p.phone})`).join(", ") },
          s.joinCode ? h("code", { class: "join" }, s.joinCode) : h("span", { class: "good" }, t("linked")),
          h("span", {},
            h("button", { onclick: async () => { if (confirm(t("confirmNewCode"))) { await api("POST", `/api/students/${s.id}/code`); again(); } } }, t("newCode")), " ",
            h("button", { class: "danger", onclick: async () => { if (confirm(t("confirmRemove"))) { await api("DELETE", `/api/students/${s.id}`); again(); } } }, t("remove")),
          ),
        ])),
        h("p", { class: "muted small" }, t("codeNote")),
      ),
    ];
  } else {
    const { parents } = await api("GET", "/api/parents");
    content = [card(t("parents"), table([t("name"), t("phone"), t("children")], parents.map((p) => [
      p.name, p.phone, { wrap: p.children.map((c) => `${c.name} (${classLabel(c)})`).join(", ") },
    ])))];
  }
  show(...header(me, HEAD_TABS(), tab, go), h("main", {}, content));
}

// ---------- Owner ----------

async function ownerHome(me) {
  const again = () => ownerHome(me);
  const { schools } = await api("GET", "/api/owner/schools");
  show(...header(me), h("main", {},
    h("h1", {}, t("schools")),
    card(t("addSchool"), form(
      [
        field(t("schoolName"), "schoolName"), field(t("headmasterName"), "headmasterName"),
        field(t("username"), "username", { autocomplete: "off" }), field(t("tempPassword"), "password", { minlength: 8, autocomplete: "new-password" }),
      ],
      t("add"),
      async (v) => { await api("POST", "/api/owner/schools", v); again(); },
      h("p", { class: "muted small" }, t("tempNote")),
    )),
    card(null, table(
      [t("schools"), t("headmaster"), { label: t("teachers"), num: true }, { label: t("students"), num: true }, { label: t("linked"), num: true }, ""],
      schools.map((s) => [
        s.name, s.headmaster_username ?? "–", String(s.teachers), String(s.students), String(s.linked),
        s.headmaster_id ? h("span", {},
          h("button", { onclick: async () => {
            try { await api("POST", `/api/owner/schools/${s.id}/login-as`); start(); } catch (error) { alert(errorText(error)); }
          } }, t("loginAs")), " ",
          h("button", { onclick: async () => {
            const value = prompt(t("promptPassword"));
            if (!value) return;
            try { await api("POST", `/api/owner/users/${s.headmaster_id}/password`, { password: value }); } catch (error) { alert(errorText(error)); }
          } }, t("resetPassword")),
        ) : "",
      ]),
    )),
  ));
}

// ---------- Login ----------

function loginPage() {
  show(h("main", { class: "narrow" },
    h("h1", {}, t("appName")),
    h("div", { class: "card" }, form(
      [field(t("username"), "username", { autocomplete: "username" }), field(t("password"), "password", { type: "password", autocomplete: "current-password" })],
      t("login"),
      async (v) => { await api("POST", "/api/login", v); start(); },
    )),
    h("div", {}, h("button", { onclick: () => { lang = lang ? 0 : 1; try { localStorage.setItem("lang", lang ? "hi" : "en"); } catch { /* not saved */ } loginPage(); } }, lang ? "English" : "हिंदी")),
  ));
}

function passwordPage() {
  show(h("main", { class: "narrow" },
    h("h1", {}, t("changePassword")),
    h("div", { class: "card" }, form(
      [
        field(t("currentPassword"), "current", { type: "password", autocomplete: "current-password" }),
        field(t("newPassword"), "password", { type: "password", minlength: 8, autocomplete: "new-password" }),
      ],
      t("save"),
      async (v) => { await api("POST", "/api/password", v); start(); },
    )),
  ));
}

async function start() {
  document.documentElement.lang = lang ? "hi" : "en";
  let me;
  try {
    me = await api("GET", "/api/me");
  } catch {
    return loginPage();
  }
  if (me.mustChangePassword) return passwordPage();
  if (me.role === "owner") return ownerHome(me);
  if (me.role === "headmaster") return headmaster(me);
  return teacherHome(me);
}

// A login that expired while the page was open goes back to the login screen.
window.addEventListener("unhandledrejection", (event) => {
  if (event.reason?.status === 401 || event.reason?.status === 403) start();
});

busy(1);
try { skills = await (await fetch("/skills.json")).json(); } catch { /* skill ids are shown instead of names */ } finally { busy(-1); }
start();
