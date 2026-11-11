import { VISIBILITY } from "./visibility.js";
import { hashPassword, newJoinCode, randomHex, sameSecret, sha256 } from "./security.js";

const SESSION_DAYS = 7;
const LOGIN_AS_MS = 60 * 60 * 1000;
const MAX_FAILED_LOGINS = 5;
const LOCK_MINUTES = 15;
const MAX_JOIN_FAILURES = 20;
const JOIN_WINDOW_MS = 60 * 60 * 1000;
const MIN_PASSWORD = 8;
// v3 adds reading and Word Power progress to what the parent agrees to share. v2 phones keep working
// and don't send those numbers.
const CONSENT_VERSIONS = new Set(["2026-11-v2", "2026-11-v3"]);
const DAY_MS = 86_400_000;
const IST_OFFSET_MS = 330 * 60_000; // Phones report local days; schools are in India.

class HttpError extends Error {
  constructor(status, code) {
    super(code);
    this.status = status;
    this.code = code;
  }
}

const notFound = () => new HttpError(404, "not_found");
const forbidden = () => new HttpError(403, "forbidden");
const invalid = (code = "invalid") => new HttpError(400, code);

const now = () => Date.now();
const today = () => Math.floor((now() + IST_OFFSET_MS) / DAY_MS);

function json(body, status = 200, headers = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json", "cache-control": "no-store", ...headers },
  });
}

async function readBody(request) {
  if (!(request.headers.get("content-type") ?? "").startsWith("application/json")) throw invalid("json_required");
  const text = await request.text();
  if (text.length > 200_000) throw new HttpError(413, "too_large");
  try {
    const body = JSON.parse(text);
    if (body === null || typeof body !== "object") throw new Error();
    return body;
  } catch {
    throw invalid("bad_json");
  }
}

function text(value, max = 80) {
  if (typeof value !== "string") throw invalid();
  const trimmed = value.trim();
  if (!trimmed || trimmed.length > max) throw invalid();
  return trimmed;
}

function int(value, min, max) {
  if (!Number.isInteger(value) || value < min || value > max) throw invalid();
  return value;
}

const username = (value) => {
  const name = text(value, 40);
  if (!/^[a-zA-Z0-9._-]{3,40}$/.test(name)) throw invalid("bad_username");
  return name;
};

function password(value) {
  if (typeof value !== "string" || value.length < MIN_PASSWORD || value.length > 128) throw invalid("weak_password");
  return value;
}

// ---------- Sessions ----------

function readCookie(request, name) {
  for (const part of (request.headers.get("cookie") ?? "").split(";")) {
    const [key, ...rest] = part.trim().split("=");
    if (key === name) return rest.join("=");
  }
  return null;
}

function sessionCookie(value, maxAge) {
  return `session=${value}; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=${maxAge}`;
}

async function currentUser(request, env) {
  const token = readCookie(request, "session");
  if (!token) return null;
  return env.DB.prepare(
    `SELECT u.id, u.school_id, u.role, u.name, u.username, u.must_change_password, s.acting_owner_id
     FROM sessions s JOIN users u ON u.id = s.user_id
     WHERE s.token_hash = ? AND s.expires_at > ?`,
  ).bind(await sha256(token), now()).first();
}

async function requireUser(request, env, ...roles) {
  const user = await currentUser(request, env);
  if (!user) throw new HttpError(401, "login_required");
  // The owner doesn't know a headmaster's temporary password, and it stays theirs to change.
  if (user.must_change_password && !user.acting_owner_id) throw new HttpError(403, "password_change_required");
  if (roles.length && !roles.includes(user.role)) throw forbidden();
  return user;
}

async function createUser(env, { schoolId, role, name, login, pass, mustChange = 1 }) {
  const salt = randomHex(16);
  const hash = await hashPassword(pass, salt);
  try {
    const result = await env.DB.prepare(
      `INSERT INTO users (school_id, role, name, username, pass_hash, pass_salt, must_change_password, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
    ).bind(schoolId, role, name, login, hash, salt, mustChange, now()).run();
    return result.meta.last_row_id;
  } catch (error) {
    if (String(error).includes("UNIQUE")) throw new HttpError(409, "username_taken");
    throw error;
  }
}

async function setPassword(env, userId, pass, mustChange) {
  const salt = randomHex(16);
  await env.DB.batch([
    env.DB.prepare("UPDATE users SET pass_hash = ?, pass_salt = ?, must_change_password = ?, failed_logins = 0, locked_until = 0 WHERE id = ?")
      .bind(await hashPassword(pass, salt), salt, mustChange, userId),
    // Anyone logged in with the old password is logged out.
    env.DB.prepare("DELETE FROM sessions WHERE user_id = ?").bind(userId),
  ]);
}

async function startSession(env, userId, { actingOwnerId = null, ms = SESSION_DAYS * DAY_MS } = {}) {
  const token = randomHex(32);
  await env.DB.prepare("INSERT INTO sessions (token_hash, user_id, expires_at, acting_owner_id) VALUES (?, ?, ?, ?)")
    .bind(await sha256(token), userId, now() + ms, actingOwnerId).run();
  return sessionCookie(token, Math.floor(ms / 1000));
}

async function endSession(request, env) {
  const token = readCookie(request, "session");
  if (token) await env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(await sha256(token)).run();
}

// ---------- Access checks: every student or class read goes through these ----------

async function classForUser(env, user, classId) {
  const row = await env.DB.prepare("SELECT * FROM classes WHERE id = ? AND school_id = ?").bind(classId, user.school_id).first();
  if (!row) throw notFound();
  if (user.role === "teacher") {
    const mapped = await env.DB.prepare("SELECT 1 FROM teacher_classes WHERE teacher_id = ? AND class_id = ?").bind(user.id, classId).first();
    if (!mapped) throw notFound();
  }
  return row;
}

async function studentForUser(env, user, studentId) {
  const row = await env.DB.prepare("SELECT * FROM students WHERE id = ? AND school_id = ?").bind(studentId, user.school_id).first();
  if (!row) throw notFound();
  if (user.role === "teacher") await classForUser(env, user, row.class_id);
  return row;
}

async function ownStaff(env, user, id, role) {
  const row = await env.DB.prepare("SELECT * FROM users WHERE id = ? AND school_id = ? AND role = ?").bind(id, user.school_id, role).first();
  if (!row) throw notFound();
  return row;
}

// ---------- Reports ----------

function weakSkillCounts(skills, studentCount) {
  const weak = new Map();
  for (const row of skills) {
    if (row.attempts >= 3 && row.mastery < 0.5) weak.set(row.skill, (weak.get(row.skill) ?? 0) + 1);
  }
  return [...weak.entries()]
    .map(([skill, students]) => ({ skill, students, total: studentCount }))
    .sort((a, b) => b.students - a.students);
}

async function classReport(env, user, classRow) {
  const allowed = VISIBILITY[user.role];
  const fromDay = today() - 6;
  const [students, days, skills] = await Promise.all([
    env.DB.prepare(
      `SELECT s.id, s.name, s.device_token_hash IS NOT NULL AS linked, m.lessons_passed, m.stars, m.level, m.updated_at,
         m.reading_level, m.words_per_minute, m.power_words_learned
       FROM students s LEFT JOIN student_summary m ON m.student_id = s.id
       WHERE s.class_id = ? ORDER BY s.name COLLATE NOCASE`,
    ).bind(classRow.id).all(),
    env.DB.prepare(
      `SELECT d.student_id, SUM(d.minutes) AS minutes, MAX(d.day) AS last_day FROM student_days d
       JOIN students s ON s.id = d.student_id
       WHERE s.class_id = ? AND d.day >= ? AND d.minutes > 0 GROUP BY d.student_id`,
    ).bind(classRow.id, fromDay).all(),
    allowed.skills
      ? env.DB.prepare(
        `SELECT k.skill, k.mastery, k.attempts FROM student_skills k JOIN students s ON s.id = k.student_id WHERE s.class_id = ?`,
      ).bind(classRow.id).all()
      : { results: [] },
  ]);
  const byStudent = new Map(days.results.map((d) => [d.student_id, d]));
  return {
    class: { id: classRow.id, grade: classRow.grade, section: classRow.section },
    students: students.results.map((s) => ({
      id: s.id,
      name: s.name,
      linked: Boolean(s.linked),
      ...(allowed.progress ? {
        lessonsPassed: s.lessons_passed ?? 0, stars: s.stars ?? 0, level: s.level, lastSync: s.updated_at,
        readingLevel: s.reading_level, wordsPerMinute: s.words_per_minute, powerWords: s.power_words_learned,
      } : {}),
      ...(allowed.minutes ? { weekMinutes: byStudent.get(s.id)?.minutes ?? 0 } : {}),
    })),
    // Out of connected children: the others have no data, so they can't count as "doing fine".
    weakSkills: weakSkillCounts(skills.results, students.results.filter((s) => s.linked).length),
  };
}

async function studentReport(env, user, student) {
  const allowed = VISIBILITY[user.role];
  const classRow = await env.DB.prepare("SELECT grade, section FROM classes WHERE id = ?").bind(student.class_id).first();
  const fromDay = today() - 6;
  const [summary, days, skills, parents] = await Promise.all([
    env.DB.prepare("SELECT * FROM student_summary WHERE student_id = ?").bind(student.id).first(),
    env.DB.prepare("SELECT day, minutes FROM student_days WHERE student_id = ? AND day >= ?").bind(student.id, fromDay).all(),
    env.DB.prepare("SELECT skill, mastery, attempts FROM student_skills WHERE student_id = ?").bind(student.id).all(),
    allowed.parentContact
      ? env.DB.prepare("SELECT p.name, p.phone FROM parents p JOIN student_parents sp ON sp.parent_id = p.id WHERE sp.student_id = ?").bind(student.id).all()
      : { results: [] },
  ]);
  const minutesByDay = new Map(days.results.map((d) => [d.day, d.minutes]));
  return {
    student: { id: student.id, name: student.name, grade: classRow.grade, section: classRow.section, linked: student.device_token_hash !== null },
    ...(allowed.progress ? {
      progress: summary ? {
        lessonsPassed: summary.lessons_passed,
        stars: summary.stars,
        wordsLearned: summary.words_learned,
        sentencesSpoken: summary.sentences_spoken,
        streak: summary.streak,
        level: summary.level,
        lastSync: summary.updated_at,
        reading: {
          level: summary.reading_level,
          passagesRead: summary.passages_read,
          accuracy: summary.reading_accuracy,
          wordsPerMinute: summary.words_per_minute,
          expressBestWpm: summary.express_best_wpm,
        },
        wordPower: {
          learned: summary.power_words_learned,
          mastered: summary.power_words_mastered,
          monthlyTestStars: summary.monthly_test_stars,
        },
      } : null,
    } : {}),
    ...(allowed.minutes ? {
      days: Array.from({ length: 7 }, (_, i) => fromDay + i).map((day) => ({ day, minutes: minutesByDay.get(day) ?? 0 })),
    } : {}),
    ...(allowed.skills ? {
      weakSkills: skills.results.filter((k) => k.attempts >= 3 && k.mastery < 0.5).sort((a, b) => a.mastery - b.mastery).map((k) => k.skill),
    } : {}),
    ...(allowed.parentContact ? { parents: parents.results } : {}),
  };
}

// ---------- Routes ----------

const routes = [];
const route = (method, pattern, handler) => {
  const keys = [];
  const regex = new RegExp(`^${pattern.replace(/:(\w+)/g, (_, key) => { keys.push(key); return "(\\d+)"; })}$`);
  routes.push({ method, regex, keys, handler });
};

// First-run: creates the software owner's account. Works once, and needs the SETUP_KEY secret.
route("POST", "/api/setup", async ({ request, env }) => {
  const body = await readBody(request);
  if (!env.SETUP_KEY || typeof body.key !== "string" || !sameSecret(await sha256(body.key), await sha256(env.SETUP_KEY))) throw forbidden();
  const existing = await env.DB.prepare("SELECT 1 FROM users WHERE role = 'owner'").first();
  if (existing) throw new HttpError(409, "already_set_up");
  await createUser(env, { schoolId: null, role: "owner", name: text(body.name), login: username(body.username), pass: password(body.password), mustChange: 0 });
  return json({ ok: true }, 201);
});

route("POST", "/api/login", async ({ request, env }) => {
  const body = await readBody(request);
  const fail = () => { throw new HttpError(401, "bad_login"); };
  if (typeof body.username !== "string" || typeof body.password !== "string") fail();
  const user = await env.DB.prepare("SELECT * FROM users WHERE username = ?").bind(body.username.trim()).first();
  if (!user) fail();
  if (user.locked_until > now()) throw new HttpError(429, "locked");
  const hash = await hashPassword(body.password, user.pass_salt);
  if (!sameSecret(hash, user.pass_hash)) {
    const failures = user.failed_logins + 1;
    const locked = failures >= MAX_FAILED_LOGINS;
    await env.DB.prepare("UPDATE users SET failed_logins = ?, locked_until = ? WHERE id = ?")
      .bind(locked ? 0 : failures, locked ? now() + LOCK_MINUTES * 60_000 : 0, user.id).run();
    fail();
  }
  if (user.failed_logins > 0) await env.DB.prepare("UPDATE users SET failed_logins = 0 WHERE id = ?").bind(user.id).run();
  const cookie = await startSession(env, user.id);
  return json({ role: user.role, name: user.name, mustChangePassword: Boolean(user.must_change_password) }, 200, { "set-cookie": cookie });
});

route("POST", "/api/logout", async ({ request, env }) => {
  await endSession(request, env);
  return json({ ok: true }, 200, { "set-cookie": sessionCookie("", 0) });
});

route("GET", "/api/me", async ({ request, env }) => {
  const user = await currentUser(request, env);
  if (!user) throw new HttpError(401, "login_required");
  const school = user.school_id ? await env.DB.prepare("SELECT name FROM schools WHERE id = ?").bind(user.school_id).first() : null;
  const actingOwner = Boolean(user.acting_owner_id);
  return json({ role: user.role, name: user.name, school: school?.name ?? null, actingOwner, mustChangePassword: Boolean(user.must_change_password) && !actingOwner });
});

route("POST", "/api/password", async ({ request, env }) => {
  const user = await currentUser(request, env);
  if (!user) throw new HttpError(401, "login_required");
  if (user.acting_owner_id) throw forbidden();
  const body = await readBody(request);
  const row = await env.DB.prepare("SELECT pass_hash, pass_salt FROM users WHERE id = ?").bind(user.id).first();
  if (typeof body.current !== "string" || !sameSecret(await hashPassword(body.current, row.pass_salt), row.pass_hash)) throw new HttpError(403, "wrong_password");
  await setPassword(env, user.id, password(body.password), 0);
  return json({ ok: true }, 200, { "set-cookie": await startSession(env, user.id) });
});

// ---------- Owner: creates schools and can open a school's headmaster account ----------

route("POST", "/api/owner/schools/:id/login-as", async ({ request, env, params }) => {
  const owner = await requireUser(request, env, "owner");
  const head = await env.DB.prepare("SELECT id FROM users WHERE school_id = ? AND role = 'headmaster' LIMIT 1").bind(params.id).first();
  if (!head) throw notFound();
  await env.DB.prepare("INSERT INTO owner_access_log (owner_id, school_id, headmaster_id, started_at) VALUES (?, ?, ?, ?)")
    .bind(owner.id, params.id, head.id, now()).run();
  await endSession(request, env);
  return json({ ok: true }, 200, { "set-cookie": await startSession(env, head.id, { actingOwnerId: owner.id, ms: LOGIN_AS_MS }) });
});

route("POST", "/api/owner/return", async ({ request, env }) => {
  const user = await currentUser(request, env);
  if (!user?.acting_owner_id) throw forbidden();
  await endSession(request, env);
  return json({ ok: true }, 200, { "set-cookie": await startSession(env, user.acting_owner_id) });
});

route("GET", "/api/owner/schools", async ({ request, env }) => {
  await requireUser(request, env, "owner");
  const { results } = await env.DB.prepare(
    `SELECT sc.id, sc.name,
       (SELECT COUNT(*) FROM students st WHERE st.school_id = sc.id) AS students,
       (SELECT COUNT(*) FROM students st WHERE st.school_id = sc.id AND st.device_token_hash IS NOT NULL) AS linked,
       (SELECT COUNT(*) FROM users u WHERE u.school_id = sc.id AND u.role = 'teacher') AS teachers,
       (SELECT u.id FROM users u WHERE u.school_id = sc.id AND u.role = 'headmaster' LIMIT 1) AS headmaster_id,
       (SELECT u.username FROM users u WHERE u.school_id = sc.id AND u.role = 'headmaster' LIMIT 1) AS headmaster_username
     FROM schools sc ORDER BY sc.name COLLATE NOCASE`,
  ).all();
  return json({ schools: results });
});

route("POST", "/api/owner/schools", async ({ request, env }) => {
  await requireUser(request, env, "owner");
  const body = await readBody(request);
  const fields = { name: text(body.headmasterName), login: username(body.username), pass: password(body.password) };
  const school = await env.DB.prepare("INSERT INTO schools (name, created_at) VALUES (?, ?)").bind(text(body.schoolName, 120), now()).run();
  try {
    await createUser(env, { schoolId: school.meta.last_row_id, role: "headmaster", ...fields });
  } catch (error) {
    await env.DB.prepare("DELETE FROM schools WHERE id = ?").bind(school.meta.last_row_id).run();
    throw error;
  }
  return json({ id: school.meta.last_row_id }, 201);
});

route("POST", "/api/owner/users/:id/password", async ({ request, env, params }) => {
  await requireUser(request, env, "owner");
  const body = await readBody(request);
  const target = await env.DB.prepare("SELECT id FROM users WHERE id = ? AND role = 'headmaster'").bind(params.id).first();
  if (!target) throw notFound();
  await setPassword(env, target.id, password(body.password), 1);
  return json({ ok: true });
});

// ---------- Classes ----------

route("GET", "/api/classes", async ({ request, env }) => {
  const user = await requireUser(request, env, "headmaster", "teacher");
  const sql = user.role === "teacher"
    ? `SELECT c.id, c.grade, c.section, (SELECT COUNT(*) FROM students s WHERE s.class_id = c.id) AS students
       FROM classes c JOIN teacher_classes t ON t.class_id = c.id WHERE t.teacher_id = ? ORDER BY c.grade, c.section`
    : `SELECT c.id, c.grade, c.section, (SELECT COUNT(*) FROM students s WHERE s.class_id = c.id) AS students
       FROM classes c WHERE c.school_id = ? ORDER BY c.grade, c.section`;
  const { results } = await env.DB.prepare(sql).bind(user.role === "teacher" ? user.id : user.school_id).all();
  return json({ classes: results });
});

route("POST", "/api/classes", async ({ request, env }) => {
  const user = await requireUser(request, env, "headmaster");
  const body = await readBody(request);
  try {
    const result = await env.DB.prepare("INSERT INTO classes (school_id, grade, section) VALUES (?, ?, ?)")
      .bind(user.school_id, int(body.grade, 1, 12), text(body.section, 10).toUpperCase()).run();
    return json({ id: result.meta.last_row_id }, 201);
  } catch (error) {
    if (String(error).includes("UNIQUE")) throw new HttpError(409, "class_exists");
    throw error;
  }
});

route("DELETE", "/api/classes/:id", async ({ request, env, params }) => {
  const user = await requireUser(request, env, "headmaster");
  const row = await classForUser(env, user, params.id);
  const used = await env.DB.prepare("SELECT 1 FROM students WHERE class_id = ?").bind(row.id).first();
  if (used) throw new HttpError(409, "class_not_empty");
  await env.DB.batch([
    env.DB.prepare("DELETE FROM teacher_classes WHERE class_id = ?").bind(row.id),
    env.DB.prepare("DELETE FROM classes WHERE id = ?").bind(row.id),
  ]);
  return json({ ok: true });
});

route("GET", "/api/classes/:id/report", async ({ request, env, params }) => {
  const user = await requireUser(request, env, "headmaster", "teacher");
  return json(await classReport(env, user, await classForUser(env, user, params.id)));
});

// ---------- Teachers ----------

route("GET", "/api/teachers", async ({ request, env }) => {
  const user = await requireUser(request, env, "headmaster");
  const [teachers, links] = await Promise.all([
    env.DB.prepare("SELECT id, name, username FROM users WHERE school_id = ? AND role = 'teacher' ORDER BY name COLLATE NOCASE").bind(user.school_id).all(),
    env.DB.prepare("SELECT t.teacher_id, t.class_id FROM teacher_classes t JOIN classes c ON c.id = t.class_id WHERE c.school_id = ?").bind(user.school_id).all(),
  ]);
  return json({
    teachers: teachers.results.map((t) => ({ ...t, classIds: links.results.filter((l) => l.teacher_id === t.id).map((l) => l.class_id) })),
  });
});

route("POST", "/api/teachers", async ({ request, env }) => {
  const user = await requireUser(request, env, "headmaster");
  const body = await readBody(request);
  const id = await createUser(env, { schoolId: user.school_id, role: "teacher", name: text(body.name), login: username(body.username), pass: password(body.password) });
  return json({ id }, 201);
});

route("PUT", "/api/teachers/:id/classes", async ({ request, env, params }) => {
  const user = await requireUser(request, env, "headmaster");
  const teacher = await ownStaff(env, user, params.id, "teacher");
  const body = await readBody(request);
  if (!Array.isArray(body.classIds) || body.classIds.length > 100) throw invalid();
  const classIds = [...new Set(body.classIds.map((id) => int(id, 1, Number.MAX_SAFE_INTEGER)))];
  for (const classId of classIds) await classForUser(env, user, classId);
  await env.DB.batch([
    env.DB.prepare("DELETE FROM teacher_classes WHERE teacher_id = ?").bind(teacher.id),
    ...classIds.map((classId) => env.DB.prepare("INSERT INTO teacher_classes (teacher_id, class_id) VALUES (?, ?)").bind(teacher.id, classId)),
  ]);
  return json({ ok: true });
});

route("POST", "/api/teachers/:id/password", async ({ request, env, params }) => {
  const user = await requireUser(request, env, "headmaster");
  const teacher = await ownStaff(env, user, params.id, "teacher");
  const body = await readBody(request);
  await setPassword(env, teacher.id, password(body.password), 1);
  return json({ ok: true });
});

route("DELETE", "/api/teachers/:id", async ({ request, env, params }) => {
  const user = await requireUser(request, env, "headmaster");
  const teacher = await ownStaff(env, user, params.id, "teacher");
  await env.DB.batch([
    env.DB.prepare("DELETE FROM sessions WHERE user_id = ?").bind(teacher.id),
    env.DB.prepare("DELETE FROM teacher_classes WHERE teacher_id = ?").bind(teacher.id),
    env.DB.prepare("DELETE FROM users WHERE id = ?").bind(teacher.id),
  ]);
  return json({ ok: true });
});

// ---------- Students and parents ----------

route("GET", "/api/parents", async ({ request, env }) => {
  const user = await requireUser(request, env, "headmaster");
  const [parents, links] = await Promise.all([
    env.DB.prepare("SELECT id, name, phone FROM parents WHERE school_id = ? ORDER BY name COLLATE NOCASE").bind(user.school_id).all(),
    env.DB.prepare(
      `SELECT sp.parent_id, s.id, s.name, c.grade, c.section FROM student_parents sp
       JOIN students s ON s.id = sp.student_id JOIN classes c ON c.id = s.class_id WHERE s.school_id = ?`,
    ).bind(user.school_id).all(),
  ]);
  return json({
    parents: parents.results.map((p) => ({
      ...p,
      children: links.results.filter((l) => l.parent_id === p.id).map(({ id, name, grade, section }) => ({ id, name, grade, section })),
    })),
  });
});

function parentInput(raw) {
  const phone = text(raw.phone, 16).replace(/[\s-]/g, "");
  if (!/^\+?\d{10,14}$/.test(phone)) throw invalid("bad_phone");
  return { name: text(raw.name), phone };
}

/** Finds the school's parent with this phone number, or adds one. This is how siblings end up linked. */
async function parentId(env, schoolId, { name, phone }) {
  const found = await env.DB.prepare("SELECT id FROM parents WHERE school_id = ? AND phone = ?").bind(schoolId, phone).first();
  if (found) return found.id;
  const result = await env.DB.prepare("INSERT INTO parents (school_id, name, phone) VALUES (?, ?, ?)").bind(schoolId, name, phone).run();
  return result.meta.last_row_id;
}

route("GET", "/api/students", async ({ request, env }) => {
  const user = await requireUser(request, env, "headmaster");
  const [students, parents] = await Promise.all([
    env.DB.prepare(
      `SELECT s.id, s.name, s.class_id, s.join_code, s.device_token_hash IS NOT NULL AS linked, c.grade, c.section
       FROM students s JOIN classes c ON c.id = s.class_id WHERE s.school_id = ? ORDER BY c.grade, c.section, s.name COLLATE NOCASE`,
    ).bind(user.school_id).all(),
    env.DB.prepare(
      `SELECT sp.student_id, p.name, p.phone FROM student_parents sp JOIN parents p ON p.id = sp.parent_id WHERE p.school_id = ?`,
    ).bind(user.school_id).all(),
  ]);
  return json({
    students: students.results.map((s) => ({
      id: s.id,
      name: s.name,
      classId: s.class_id,
      grade: s.grade,
      section: s.section,
      linked: Boolean(s.linked),
      joinCode: s.join_code,
      parents: parents.results.filter((p) => p.student_id === s.id).map(({ name, phone }) => ({ name, phone })),
    })),
  });
});

route("POST", "/api/students", async ({ request, env }) => {
  const user = await requireUser(request, env, "headmaster");
  const body = await readBody(request);
  const classRow = await classForUser(env, user, int(body.classId, 1, Number.MAX_SAFE_INTEGER));
  if (!Array.isArray(body.parents) || body.parents.length < 1 || body.parents.length > 4) throw invalid("parent_required");
  const parents = body.parents.map(parentInput);
  const name = text(body.name);
  const code = newJoinCode();
  const student = await env.DB.prepare("INSERT INTO students (school_id, class_id, name, join_code, created_at) VALUES (?, ?, ?, ?, ?)")
    .bind(user.school_id, classRow.id, name, code, now()).run();
  const studentId = student.meta.last_row_id;
  for (const parent of parents) {
    await env.DB.prepare("INSERT OR IGNORE INTO student_parents (student_id, parent_id) VALUES (?, ?)")
      .bind(studentId, await parentId(env, user.school_id, parent)).run();
  }
  return json({ id: studentId, joinCode: code }, 201);
});

route("PUT", "/api/students/:id", async ({ request, env, params }) => {
  const user = await requireUser(request, env, "headmaster");
  const student = await studentForUser(env, user, params.id);
  const body = await readBody(request);
  const classRow = await classForUser(env, user, int(body.classId, 1, Number.MAX_SAFE_INTEGER));
  await env.DB.prepare("UPDATE students SET class_id = ? WHERE id = ?").bind(classRow.id, student.id).run();
  return json({ ok: true });
});

const clearProgress = (env, studentId) => [
  env.DB.prepare("DELETE FROM student_summary WHERE student_id = ?").bind(studentId),
  env.DB.prepare("DELETE FROM student_days WHERE student_id = ?").bind(studentId),
  env.DB.prepare("DELETE FROM student_skills WHERE student_id = ?").bind(studentId),
];

// A new code disconnects any linked phone and removes what it shared.
route("POST", "/api/students/:id/code", async ({ request, env, params }) => {
  const user = await requireUser(request, env, "headmaster");
  const student = await studentForUser(env, user, params.id);
  const code = newJoinCode();
  await env.DB.batch([
    env.DB.prepare("UPDATE students SET join_code = ?, device_token_hash = NULL, consent_at = NULL, consent_version = NULL WHERE id = ?").bind(code, student.id),
    ...clearProgress(env, student.id),
  ]);
  return json({ joinCode: code });
});

route("DELETE", "/api/students/:id", async ({ request, env, params }) => {
  const user = await requireUser(request, env, "headmaster");
  const student = await studentForUser(env, user, params.id);
  await env.DB.batch([
    ...clearProgress(env, student.id),
    env.DB.prepare("DELETE FROM student_parents WHERE student_id = ?").bind(student.id),
    env.DB.prepare("DELETE FROM students WHERE id = ?").bind(student.id),
    // A parent with no children left at the school is no longer needed.
    env.DB.prepare("DELETE FROM parents WHERE school_id = ? AND id NOT IN (SELECT parent_id FROM student_parents)").bind(user.school_id),
  ]);
  return json({ ok: true });
});

route("GET", "/api/students/:id/report", async ({ request, env, params }) => {
  const user = await requireUser(request, env, "headmaster", "teacher");
  return json(await studentReport(env, user, await studentForUser(env, user, params.id)));
});

route("GET", "/api/overview", async ({ request, env }) => {
  const user = await requireUser(request, env, "headmaster");
  const classes = await env.DB.prepare("SELECT * FROM classes WHERE school_id = ? ORDER BY grade, section").bind(user.school_id).all();
  const fromDay = today() - 6;
  const [perClass, active, skills] = await Promise.all([
    env.DB.prepare(
      `SELECT s.class_id, COUNT(*) AS students, SUM(s.device_token_hash IS NOT NULL) AS linked, COALESCE(SUM(m.lessons_passed), 0) AS lessons,
         COALESCE(SUM(m.passages_read), 0) AS passages, COALESCE(SUM(m.power_words_learned), 0) AS power_words
       FROM students s LEFT JOIN student_summary m ON m.student_id = s.id WHERE s.school_id = ? GROUP BY s.class_id`,
    ).bind(user.school_id).all(),
    env.DB.prepare(
      `SELECT s.class_id, COUNT(DISTINCT d.student_id) AS active, SUM(d.minutes) AS minutes
       FROM student_days d JOIN students s ON s.id = d.student_id WHERE s.school_id = ? AND d.day >= ? AND d.minutes > 0 GROUP BY s.class_id`,
    ).bind(user.school_id, fromDay).all(),
    env.DB.prepare(`SELECT k.skill, k.mastery, k.attempts FROM student_skills k JOIN students s ON s.id = k.student_id WHERE s.school_id = ?`).bind(user.school_id).all(),
  ]);
  const rows = classes.results.map((c) => {
    const counts = perClass.results.find((p) => p.class_id === c.id);
    const week = active.results.find((a) => a.class_id === c.id);
    return {
      id: c.id,
      grade: c.grade,
      section: c.section,
      students: counts?.students ?? 0,
      linked: counts?.linked ?? 0,
      lessons: counts?.lessons ?? 0,
      passagesRead: counts?.passages ?? 0,
      powerWords: counts?.power_words ?? 0,
      activeThisWeek: week?.active ?? 0,
      weekMinutes: week?.minutes ?? 0,
    };
  });
  const connected = rows.reduce((sum, r) => sum + r.linked, 0);
  return json({ classes: rows, weakSkills: weakSkillCounts(skills.results, connected) });
});

// ---------- The app on a parent's phone ----------

route("POST", "/api/device/join", async ({ request, env }) => {
  const body = await readBody(request);
  const ip = request.headers.get("cf-connecting-ip") ?? "local";
  const record = await env.DB.prepare("SELECT failures, window_start FROM join_failures WHERE ip = ?").bind(ip).first();
  const inWindow = record && now() - record.window_start < JOIN_WINDOW_MS;
  if (inWindow && record.failures >= MAX_JOIN_FAILURES) throw new HttpError(429, "too_many_tries");
  if (body.consent !== true || !CONSENT_VERSIONS.has(body.consentVersion)) throw invalid("consent_required");
  const code = text(body.code, 12).toUpperCase().replace(/[\s-]/g, "");
  const student = await env.DB.prepare(
    `SELECT s.id, s.name, c.grade, c.section, sc.name AS school FROM students s
     JOIN classes c ON c.id = s.class_id JOIN schools sc ON sc.id = s.school_id WHERE s.join_code = ?`,
  ).bind(code).first();
  if (!student) {
    await env.DB.prepare(
      `INSERT INTO join_failures (ip, failures, window_start) VALUES (?1, 1, ?2)
       ON CONFLICT (ip) DO UPDATE SET failures = CASE WHEN ?2 - window_start < ?3 THEN failures + 1 ELSE 1 END,
         window_start = CASE WHEN ?2 - window_start < ?3 THEN window_start ELSE ?2 END`,
    ).bind(ip, now(), JOIN_WINDOW_MS).run();
    throw new HttpError(404, "bad_code");
  }
  const token = randomHex(32);
  // The code works once. A parent changing phones asks the school for a new one.
  await env.DB.prepare("UPDATE students SET join_code = NULL, device_token_hash = ?, consent_at = ?, consent_version = ? WHERE id = ?")
    .bind(await sha256(token), now(), body.consentVersion, student.id).run();
  return json({ token, studentName: student.name, schoolName: student.school, grade: student.grade, section: student.section }, 201);
});

async function deviceStudent(request, env) {
  const header = request.headers.get("authorization") ?? "";
  if (!header.startsWith("Bearer ")) throw new HttpError(401, "device_required");
  const student = await env.DB.prepare("SELECT id FROM students WHERE device_token_hash = ?").bind(await sha256(header.slice(7))).first();
  if (!student) throw new HttpError(401, "device_not_linked");
  return student;
}

route("POST", "/api/device/sync", async ({ request, env }) => {
  const student = await deviceStudent(request, env);
  const body = await readBody(request);
  const s = body.summary;
  if (!s || typeof s !== "object" || !Array.isArray(body.days) || !Array.isArray(body.skills) || body.days.length > 14 || body.skills.length > 300) throw invalid();
  const big = 1_000_000;
  // Reading and Word Power numbers come from newer phones only; an older phone sends none of them.
  const optional = (value, max) => (value == null ? null : int(value, 0, max));
  const statements = [
    env.DB.prepare(
      `INSERT INTO student_summary (student_id, lessons_passed, stars, words_learned, sentences_spoken, streak, level, lesson_id, updated_at,
         reading_level, passages_read, reading_accuracy, words_per_minute, express_best_wpm, power_words_learned, power_words_mastered, monthly_test_stars)
       VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17)
       ON CONFLICT (student_id) DO UPDATE SET lessons_passed = ?2, stars = ?3, words_learned = ?4, sentences_spoken = ?5,
         streak = ?6, level = ?7, lesson_id = ?8, updated_at = ?9, reading_level = ?10, passages_read = ?11, reading_accuracy = ?12,
         words_per_minute = ?13, express_best_wpm = ?14, power_words_learned = ?15, power_words_mastered = ?16, monthly_test_stars = ?17`,
    ).bind(
      student.id, int(s.lessonsPassed, 0, big), int(s.stars, 0, big), int(s.wordsLearned, 0, big), int(s.sentencesSpoken, 0, big), int(s.streak, 0, big),
      s.level == null ? null : int(s.level, 0, 100), s.lessonId == null ? null : text(s.lessonId, 20), now(),
      optional(s.readingLevel, 100), optional(s.passagesRead, big), optional(s.readingAccuracy, 100), optional(s.wordsPerMinute, 1000),
      optional(s.expressBestWpm, 1000), optional(s.powerWordsLearned, big), optional(s.powerWordsMastered, big), optional(s.monthlyTestStars, 3),
    ),
  ];
  const latest = today() + 1;
  for (const d of body.days) {
    statements.push(
      env.DB.prepare("INSERT INTO student_days (student_id, day, minutes) VALUES (?1, ?2, ?3) ON CONFLICT (student_id, day) DO UPDATE SET minutes = ?3")
        .bind(student.id, int(d.day, latest - 30, latest), int(d.minutes, 0, 1440)),
    );
  }
  for (const k of body.skills) {
    if (typeof k.mastery !== "number" || k.mastery < 0 || k.mastery > 1) throw invalid();
    statements.push(
      env.DB.prepare("INSERT INTO student_skills (student_id, skill, mastery, attempts) VALUES (?1, ?2, ?3, ?4) ON CONFLICT (student_id, skill) DO UPDATE SET mastery = ?3, attempts = ?4")
        .bind(student.id, text(k.skill, 60), k.mastery, int(k.attempts, 0, big)),
    );
  }
  await env.DB.batch(statements);
  return json({ ok: true });
});

// A parent can disconnect at any time; what was shared is deleted.
route("POST", "/api/device/unlink", async ({ request, env }) => {
  const student = await deviceStudent(request, env);
  await env.DB.batch([
    env.DB.prepare("UPDATE students SET device_token_hash = NULL, consent_at = NULL, consent_version = NULL WHERE id = ?").bind(student.id),
    ...clearProgress(env, student.id),
  ]);
  return json({ ok: true });
});

export async function handle(request, env) {
  const url = new URL(request.url);
  try {
    for (const r of routes) {
      if (r.method !== request.method) continue;
      const match = url.pathname.match(r.regex);
      if (!match) continue;
      const params = Object.fromEntries(r.keys.map((key, i) => [key, Number(match[i + 1])]));
      return await r.handler({ request, env, params });
    }
    throw notFound();
  } catch (error) {
    if (error instanceof HttpError) return json({ error: error.code }, error.status);
    console.error(error);
    return json({ error: "server_error" }, 500);
  }
}
