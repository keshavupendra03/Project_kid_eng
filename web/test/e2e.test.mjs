// End-to-end API test. Needs the local server running on BASE:
//   npm run db:local && npm run dev   (in another terminal), then npm test
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { before, test } from "node:test";

const BASE = process.env.BASE ?? "http://localhost:8788";
const SETUP_KEY = process.env.SETUP_KEY ?? "local-setup-key";
const stamp = Date.now().toString(36);
const CONSENT = "2026-11-v3";
const IST_DAY = Math.floor((Date.now() + 330 * 60_000) / 86_400_000);
const fixture = JSON.parse(readFileSync(new URL("./fixtures/sync-payload.json", import.meta.url)));
fixture.days[0].day = IST_DAY;

async function call(method, path, { cookie, token, body } = {}) {
  const response = await fetch(BASE + path, {
    method,
    headers: {
      ...(body !== undefined ? { "content-type": "application/json" } : {}),
      ...(cookie ? { cookie } : {}),
      ...(token ? { authorization: `Bearer ${token}` } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  const setCookie = response.headers.get("set-cookie");
  return { status: response.status, body: await response.json(), cookie: setCookie?.split(";")[0] };
}

async function login(username, password) {
  const r = await call("POST", "/api/login", { body: { username, password } });
  assert.equal(r.status, 200, `login ${username}: ${JSON.stringify(r.body)}`);
  return r.cookie;
}

/** Logs in with a temporary password and replaces it, as a real first login does. */
async function firstLogin(username, temporary, final = "changed-pass-1") {
  const cookie = await login(username, temporary);
  const r = await call("POST", "/api/password", { cookie, body: { current: temporary, password: final } });
  assert.equal(r.status, 200);
  return r.cookie;
}

let owner, headA, headB, teacherA, teacherOther;
let classFive, classSeven, otherClass, sisterId, brotherId, codeSister, codeBrother, deviceSister, schoolBStudent, teacherOtherId;
const phone = `98${String(Date.now()).slice(-8)}`;

before(async () => {
  const setup = await call("POST", "/api/setup", { body: { key: SETUP_KEY, name: "Owner", username: "owner.test", password: "owner-pass-1" } });
  assert.ok(setup.status === 201 || setup.status === 409);
  owner = await login("owner.test", "owner-pass-1");

  for (const [name, user] of [["School A", `head.a.${stamp}`], ["School B", `head.b.${stamp}`]]) {
    const r = await call("POST", "/api/owner/schools", { cookie: owner, body: { schoolName: `${name} ${stamp}`, headmasterName: "Head", username: user, password: "temporary-1" } });
    assert.equal(r.status, 201);
  }
  headA = await firstLogin(`head.a.${stamp}`, "temporary-1");
  headB = await firstLogin(`head.b.${stamp}`, "temporary-1");

  classFive = (await call("POST", "/api/classes", { cookie: headA, body: { grade: 5, section: "a" } })).body.id;
  classSeven = (await call("POST", "/api/classes", { cookie: headA, body: { grade: 7, section: "B" } })).body.id;
  otherClass = (await call("POST", "/api/classes", { cookie: headB, body: { grade: 5, section: "A" } })).body.id;

  const t1 = await call("POST", "/api/teachers", { cookie: headA, body: { name: "Teacher One", username: `teacher.${stamp}`, password: "temporary-2" } });
  assert.equal(t1.status, 201);
  const t2 = await call("POST", "/api/teachers", { cookie: headA, body: { name: "Teacher Two", username: `teacher2.${stamp}`, password: "temporary-2" } });
  teacherOtherId = t2.body.id;
  await call("PUT", `/api/teachers/${t1.body.id}/classes`, { cookie: headA, body: { classIds: [classFive] } });
  await call("PUT", `/api/teachers/${t2.body.id}/classes`, { cookie: headA, body: { classIds: [classSeven] } });
  teacherA = await firstLogin(`teacher.${stamp}`, "temporary-2");
  teacherOther = await firstLogin(`teacher2.${stamp}`, "temporary-2");

  const parent = { name: "Sunita", phone };
  const sister = await call("POST", "/api/students", { cookie: headA, body: { name: "Riddhi", classId: classFive, parents: [parent] } });
  const brother = await call("POST", "/api/students", { cookie: headA, body: { name: "Aarav", classId: classSeven, parents: [parent] } });
  assert.equal(sister.status, 201);
  [sisterId, codeSister, brotherId, codeBrother] = [sister.body.id, sister.body.joinCode, brother.body.id, brother.body.joinCode];
  schoolBStudent = (await call("POST", "/api/students", { cookie: headB, body: { name: "Other", classId: otherClass, parents: [{ name: "P", phone: "9000000001" }] } })).body.id;
});

test("setup only works once", async () => {
  const r = await call("POST", "/api/setup", { body: { key: SETUP_KEY, name: "X", username: "owner.two", password: "owner-pass-2" } });
  assert.equal(r.status, 409);
});

test("setup needs the secret key", async () => {
  const r = await call("POST", "/api/setup", { body: { key: "wrong", name: "X", username: "owner.two", password: "owner-pass-2" } });
  assert.equal(r.status, 403);
});

test("a temporary password must be changed before anything else", async () => {
  const user = `new.${stamp}`;
  await call("POST", "/api/teachers", { cookie: headA, body: { name: "New", username: user, password: "temporary-3" } });
  const cookie = await login(user, "temporary-3");
  assert.equal((await call("GET", "/api/classes", { cookie })).body.error, "password_change_required");
});

test("no login, no data", async () => {
  assert.equal((await call("GET", `/api/students/${sisterId}/report`)).status, 401);
  assert.equal((await call("GET", "/api/overview")).status, 401);
});

test("account locks after repeated wrong passwords", async () => {
  const user = `lock.${stamp}`;
  await call("POST", "/api/teachers", { cookie: headA, body: { name: "L", username: user, password: "temporary-4" } });
  for (let i = 0; i < 5; i++) await call("POST", "/api/login", { body: { username: user, password: "nope-nope" } });
  assert.equal((await call("POST", "/api/login", { body: { username: user, password: "temporary-4" } })).status, 429);
});

test("siblings in different classes share one parent", async () => {
  const { parents } = (await call("GET", "/api/parents", { cookie: headA })).body;
  const shared = parents.find((p) => p.phone === phone);
  assert.deepEqual(shared.children.map((c) => c.name).sort(), ["Aarav", "Riddhi"]);
  assert.deepEqual(shared.children.map((c) => c.grade).sort(), [5, 7]);
});

test("joining needs parental consent", async () => {
  const r = await call("POST", "/api/device/join", { body: { code: codeSister, consent: false, consentVersion: CONSENT } });
  assert.equal(r.body.error, "consent_required");
});

test("a wrong code is refused", async () => {
  assert.equal((await call("POST", "/api/device/join", { body: { code: "ZZZZZZZZ", consent: true, consentVersion: CONSENT } })).body.error, "bad_code");
});

test("phone joins and syncs, and the mapped teacher sees it", async () => {
  const joined = await call("POST", "/api/device/join", { body: { code: codeSister.toLowerCase(), consent: true, consentVersion: CONSENT } });
  assert.equal(joined.status, 201);
  assert.equal(joined.body.studentName, "Riddhi");
  deviceSister = joined.body.token;

  assert.equal((await call("POST", "/api/device/sync", { token: deviceSister, body: fixture })).status, 200);

  const report = (await call("GET", `/api/students/${sisterId}/report`, { cookie: teacherA })).body;
  assert.equal(report.student.linked, true);
  assert.equal(report.progress.lessonsPassed, 12);
  assert.equal(report.days.at(-1).minutes, 25);
  assert.deepEqual(report.weakSkills, ["sound.th"]);

  const cls = (await call("GET", `/api/classes/${classFive}/report`, { cookie: teacherA })).body;
  assert.equal(cls.students[0].weekMinutes, 25);
  assert.deepEqual(cls.weakSkills, [{ skill: "sound.th", students: 1, total: 1 }]);
});

test("reading, Tinku Express and Word Power reach the school", async () => {
  const report = (await call("GET", `/api/students/${sisterId}/report`, { cookie: teacherA })).body;
  assert.deepEqual(report.progress.reading, { level: 2, passagesRead: 7, accuracy: 86, wordsPerMinute: 41, expressBestWpm: 55 });
  assert.deepEqual(report.progress.wordPower, { learned: 30, mastered: 12, monthlyTestStars: 2 });

  const cls = (await call("GET", `/api/classes/${classFive}/report`, { cookie: teacherA })).body;
  assert.deepEqual([cls.students[0].readingLevel, cls.students[0].wordsPerMinute, cls.students[0].powerWords], [2, 41, 30]);

  const overview = (await call("GET", "/api/overview", { cookie: headA })).body;
  const row = overview.classes.find((c) => c.id === classFive);
  assert.deepEqual([row.passagesRead, row.powerWords], [7, 30]);

  const bad = structuredClone(fixture);
  bad.summary.readingAccuracy = 150;
  assert.equal((await call("POST", "/api/device/sync", { token: deviceSister, body: bad })).status, 400);
});

test("a phone with the older app still syncs, without the new numbers", async () => {
  const { joinCode } = (await call("POST", "/api/students", { cookie: headA, body: { name: "Old Phone", classId: classFive, parents: [{ name: "Q", phone: "9000000003" }] } })).body;
  const joined = await call("POST", "/api/device/join", { body: { code: joinCode, consent: true, consentVersion: "2026-11-v2" } });
  assert.equal(joined.status, 201);
  const old = structuredClone(fixture);
  for (const key of ["readingLevel", "passagesRead", "readingAccuracy", "wordsPerMinute", "expressBestWpm", "powerWordsLearned", "powerWordsMastered", "monthlyTestStars"]) delete old.summary[key];
  assert.equal((await call("POST", "/api/device/sync", { token: joined.body.token, body: old })).status, 200);
  const cls = (await call("GET", `/api/classes/${classFive}/report`, { cookie: teacherA })).body;
  const student = cls.students.find((s) => s.name === "Old Phone");
  assert.deepEqual([student.lessonsPassed, student.readingLevel, student.powerWords], [12, null, null]);
});

test("a join code works only once", async () => {
  assert.equal((await call("POST", "/api/device/join", { body: { code: codeSister, consent: true, consentVersion: CONSENT } })).status, 404);
});

test("syncing again updates instead of duplicating", async () => {
  const next = structuredClone(fixture);
  next.summary.lessonsPassed = 13;
  next.days[0].minutes = 40;
  await call("POST", "/api/device/sync", { token: deviceSister, body: next });
  const report = (await call("GET", `/api/students/${sisterId}/report`, { cookie: teacherA })).body;
  assert.equal(report.progress.lessonsPassed, 13);
  assert.equal(report.days.at(-1).minutes, 40);
});

test("a teacher can't see another class in the same school", async () => {
  assert.equal((await call("GET", `/api/students/${sisterId}/report`, { cookie: teacherOther })).status, 404);
  assert.equal((await call("GET", `/api/classes/${classFive}/report`, { cookie: teacherOther })).status, 404);
  assert.deepEqual((await call("GET", "/api/classes", { cookie: teacherOther })).body.classes.map((c) => c.id), [classSeven]);
});

test("a teacher sees no parent phone numbers; a headmaster does", async () => {
  assert.equal((await call("GET", `/api/students/${sisterId}/report`, { cookie: teacherA })).body.parents, undefined);
  assert.equal((await call("GET", `/api/students/${sisterId}/report`, { cookie: headA })).body.parents[0].phone, phone);
});

test("teachers can't use headmaster actions", async () => {
  for (const [method, path] of [["GET", "/api/overview"], ["GET", "/api/students"], ["GET", "/api/parents"], ["GET", "/api/teachers"]]) {
    assert.equal((await call(method, path, { cookie: teacherA })).status, 403, path);
  }
  assert.equal((await call("DELETE", `/api/students/${sisterId}`, { cookie: teacherA })).status, 403);
});

test("schools are completely separate", async () => {
  assert.equal((await call("GET", `/api/students/${sisterId}/report`, { cookie: headB })).status, 404);
  assert.equal((await call("GET", `/api/students/${schoolBStudent}/report`, { cookie: headA })).status, 404);
  assert.equal((await call("GET", `/api/classes/${classFive}/report`, { cookie: headB })).status, 404);
  assert.equal((await call("POST", `/api/students/${sisterId}/code`, { cookie: headB })).status, 404);
  assert.equal((await call("PUT", `/api/teachers/${teacherOtherId}/classes`, { cookie: headB, body: { classIds: [] } })).status, 404);
  const crossClass = await call("POST", "/api/students", { cookie: headB, body: { name: "X", classId: classFive, parents: [{ name: "P", phone: "9000000002" }] } });
  assert.equal(crossClass.status, 404);
});

test("the owner sees counts, not children", async () => {
  const { schools } = (await call("GET", "/api/owner/schools", { cookie: owner })).body;
  assert.ok(schools.some((s) => s.students >= 2));
  assert.equal(JSON.stringify(schools).includes("Riddhi"), false);
  assert.equal((await call("GET", `/api/students/${sisterId}/report`, { cookie: owner })).status, 403);
});

test("the server ignores anything beyond the agreed summary", async () => {
  const sneaky = { ...structuredClone(fixture), voiceClips: ["a.m4a"], otherApps: [{ pkg: "com.instagram.android", minutes: 90 }] };
  await call("POST", "/api/device/sync", { token: deviceSister, body: sneaky });
  const report = JSON.stringify((await call("GET", `/api/students/${sisterId}/report`, { cookie: headA })).body);
  assert.equal(report.includes("instagram"), false);
  assert.equal(report.includes("voiceClips"), false);
});

test("bad sync data is refused", async () => {
  const bad = structuredClone(fixture);
  bad.skills[0].mastery = 7;
  assert.equal((await call("POST", "/api/device/sync", { token: deviceSister, body: bad })).status, 400);
  assert.equal((await call("POST", "/api/device/sync", { token: "not-a-token", body: fixture })).status, 401);
});

test("disconnecting deletes what was shared", async () => {
  const joined = await call("POST", "/api/device/join", { body: { code: codeBrother, consent: true, consentVersion: CONSENT } });
  await call("POST", "/api/device/sync", { token: joined.body.token, body: fixture });
  assert.equal((await call("POST", "/api/device/unlink", { token: joined.body.token })).status, 200);
  const report = (await call("GET", `/api/students/${brotherId}/report`, { cookie: headA })).body;
  assert.equal(report.progress, null);
  assert.equal(report.student.linked, false);
  assert.equal((await call("POST", "/api/device/sync", { token: joined.body.token, body: fixture })).status, 401);
});

test("a new code disconnects the old phone", async () => {
  const { joinCode } = (await call("POST", `/api/students/${sisterId}/code`, { cookie: headA })).body;
  assert.ok(joinCode);
  assert.equal((await call("POST", "/api/device/sync", { token: deviceSister, body: fixture })).status, 401);
});

test("deleting a student removes the parent once no children remain", async () => {
  assert.equal((await call("DELETE", `/api/students/${sisterId}`, { cookie: headA })).status, 200);
  let parents = (await call("GET", "/api/parents", { cookie: headA })).body.parents;
  assert.equal(parents.find((p) => p.phone === phone).children.length, 1);
  await call("DELETE", `/api/students/${brotherId}`, { cookie: headA });
  parents = (await call("GET", "/api/parents", { cookie: headA })).body.parents;
  assert.equal(parents.find((p) => p.phone === phone), undefined);
});

// Runs last: logging in as a headmaster replaces the owner's session.
test("owner can open a school as its headmaster and come back", async () => {
  const tempSchool = `Login As ${stamp}`;
  const created = await call("POST", "/api/owner/schools", { cookie: owner, body: { schoolName: tempSchool, headmasterName: "New Head", username: `newhead.${stamp}`, password: "temp-pass-1" } });
  assert.equal(created.status, 201);

  // Other roles can't use it.
  assert.equal((await call("POST", `/api/owner/schools/${created.body.id}/login-as`, { cookie: headA })).status, 403);
  assert.equal((await call("POST", `/api/owner/schools/${created.body.id}/login-as`, { cookie: teacherA })).status, 403);
  assert.equal((await call("POST", `/api/owner/schools/${created.body.id}/login-as`)).status, 401);
  assert.equal((await call("POST", "/api/owner/schools/999999/login-as", { cookie: owner })).status, 404);
  assert.equal((await call("POST", "/api/owner/return", { cookie: headA })).status, 403);

  const opened = await call("POST", `/api/owner/schools/${created.body.id}/login-as`, { cookie: owner });
  assert.equal(opened.status, 200);
  const acting = opened.cookie;
  assert.match((await fetchSetCookie(`/api/owner/schools/${created.body.id}/login-as`, acting)) ?? "", /^$/); // an acting session is not an owner

  const me = await call("GET", "/api/me", { cookie: acting });
  assert.deepEqual([me.body.role, me.body.school, me.body.actingOwner, me.body.mustChangePassword], ["headmaster", tempSchool, true, false]);

  // Works even though this headmaster still has a temporary password.
  assert.equal((await call("GET", "/api/overview", { cookie: acting })).status, 200);
  assert.equal((await call("GET", "/api/owner/schools", { cookie: acting })).status, 403);
  assert.equal((await call("POST", "/api/password", { cookie: acting, body: { current: "temp-pass-1", password: "owner-chose-1" } })).status, 403);

  const back = await call("POST", "/api/owner/return", { cookie: acting });
  assert.equal(back.status, 200);
  assert.equal((await call("GET", "/api/me", { cookie: back.cookie })).body.role, "owner");
  assert.equal((await call("GET", "/api/me", { cookie: acting })).status, 401);
  assert.equal((await call("GET", "/api/me", { cookie: owner })).status, 401);

  // The real headmaster is still made to change their temporary password.
  const real = await call("POST", "/api/login", { body: { username: `newhead.${stamp}`, password: "temp-pass-1" } });
  assert.equal(real.status, 200);
  assert.equal(real.body.mustChangePassword, true);
  assert.equal((await call("GET", "/api/overview", { cookie: real.cookie })).status, 403);
});

async function fetchSetCookie(path, cookie) {
  const response = await fetch(BASE + path, { method: "POST", headers: { cookie } });
  assert.equal(response.status, 403);
  return response.headers.get("set-cookie");
}
