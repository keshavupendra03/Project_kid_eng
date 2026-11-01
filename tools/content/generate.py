#!/usr/bin/env python3
"""
Builds Samjho English lesson files from the compact source files next to this script.

    python tools/content/generate.py

Sources (all in tools/content/):
  curriculum.json  levels and the lesson list shown in the app (becomes assets/content/levels.json)
  phonics.json     Level 1 sound lessons (template: letter-sound cards, blending, sound pairs)
                   and hand-made lessons ("raw": a list of steps)
  words.json       Level 2 vocabulary lessons (template: picture words + meanings)
  patterns.json    Level 3 sentence-pattern lessons (template: Sentence Train, fill the gap,
                   Mistake Detective, speaking)

Text fields use {"hi": Devanagari, "hl": Hinglish, "en": English}. Lessons not described in a
source file (like L0.U01) are hand-written JSON and are left alone. After generating, run
`gradlew test` — the content checker validates every lesson.
"""
import json
import pathlib
import random

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / "tools" / "content"
OUT = ROOT / "app" / "src" / "main" / "assets" / "content"

# Letters (or letter teams) → recorded sound file in assets/audio/phonics.
SOUNDS = {
    "s": "c13_s", "a": "v01_a_apple", "t": "c14_t", "p": "c11_p", "i": "v03_i_ink", "n": "c10_n",
    "m": "c09_m", "d": "c02_d", "g": "c04_g", "o": "v04_o_pot", "c": "c07_k", "k": "c07_k",
    "ck": "c07_k", "e": "v02_e_egg", "u": "v05_u_cup", "r": "c12_r", "h": "c05_h", "b": "c01_b",
    "f": "c03_f", "l": "c08_l", "j": "c06_j", "y": "c17_y", "z": "c18_z", "w": "c16_w", "v": "c15_v",
    "sh": "c19_sh", "ch": "c20_ch", "ng": "c23_ng", "th": "c21_th_thin",
    "ee": "v08_ee_tree", "oa": "v10_oa_boat", "ai": "v07_ai_cake", "oo": "v11_oo_moon",
}


def text(d):
    """Source text {hi, hl, en} → app text {hi, hinglish, en}."""
    return {"hi": d["hi"], "hinglish": d["hl"], "en": d["en"]}


def word(w):
    """[english, emoji, hindi, hinglish, (accept)] → app Word."""
    out = {"english": w[0]}
    if w[1]:
        out["emoji"] = w[1]
    if len(w) > 2 and w[2]:
        out["hindi"] = w[2]
    if len(w) > 3 and w[3]:
        out["hinglish"] = w[3]
    return out


def accept_of(w):
    return w[4] if len(w) > 4 else []


def meaning(w):
    return {"hi": w[2], "hinglish": w[3]}


class Lesson:
    def __init__(self, lesson_id, level):
        self.id = lesson_id
        self.level = level
        self.steps = []
        self.rng = random.Random(lesson_id)  # same output every run

    def add(self, step_type, skills=(), **fields):
        step = {"type": step_type, "id": f"{self.id}.S{len(self.steps) + 1:02d}"}
        if skills:
            step["skills"] = list(dict.fromkeys(skills))
        step.update(fields)
        self.steps.append(step)

    def write(self):
        path = OUT / "lessons" / f"{self.id}.json"
        data = {"id": self.id, "level": self.level, "steps": self.steps}
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return path


# ---------- Level 1: phonics ----------

def sound_card_text(s, example):
    return {
        "hi": f"यह ‘{s['letters']}’ है। इसकी आवाज़ है ‘{s['hint']}’, जैसे {example} में।",
        "hinglish": f"Yeh ‘{s['letters']}’ hai. Iski awaaz hai ‘{s['hint_hl']}’, jaise {example} mein.",
        "en": f"This is ‘{s['letters']}’. Listen to its sound, like at the start of {example}.",
    }


def phonics_lesson(spec):
    if "raw" in spec:
        return raw_lesson(spec, level=1)
    lesson = Lesson(spec["id"], 1)
    lesson.add("learn_card", text=text(spec["intro"]))

    examples = []
    for i, s in enumerate(spec["sounds"]):
        ex = s["example"]
        examples.append(ex)
        skill = f"phonics.{s['letters']}"
        lesson.add(
            "learn_sound", [skill],
            letters=s["letters"], sound=SOUNDS[s["letters"]], hint=s["hint"],
            text=text(s["text"]) if "text" in s else sound_card_text(s, ex[0]),
            example=word(ex),
        )
        if i % 2 == 1:
            others = [e for e in examples if e is not ex]
            options = [ex] + lesson.rng.sample(others, min(3, len(others)))
            lesson.add("listen_tap", ["listen.word", skill], answer=ex[0], options=[word(o) for o in options])

    for ex in examples[:2]:
        lesson.add("say_word", ["speak.word"], word=word(ex), **({"accept": accept_of(ex)} if accept_of(ex) else {}))

    blends = spec.get("blends", [])
    for j, b in enumerate(blends):
        parts = [{"letters": p, "sound": SOUNDS[p]} for p in b["parts"].split("-")]
        extra = {"accept": b["accept"]} if b.get("accept") else {}
        lesson.add("blend", ["phonics.blend", "speak.word"], word=b["word"], emoji=b["emoji"], parts=parts, **extra)
        if j % 2 == 1 and len(blends) >= 3:
            others = [o["word"] for o in blends if o is not b]
            lesson.add(
                "word_pick", ["read.word", "phonics.blend"],
                emoji=b["emoji"], answer=b["word"], options=[b["word"]] + lesson.rng.sample(others, 2),
            )

    for p in spec.get("pairs", []):
        lesson.add("minimal_pair", ["listen.minimal_pair"] + p.get("skills", []), answer=p["answer"], options=p["options"])

    lesson.add("learn_card", text=text(spec["outro"]))
    return lesson


def raw_lesson(spec, level):
    """Hand-made steps; the generator only numbers them and expands short forms."""
    lesson = Lesson(spec["id"], level)
    for step in spec["raw"]:
        step = dict(step)
        step_type = step.pop("type")
        skills = step.pop("skills", [])
        for key, value in list(step.items()):
            if key in ("text",) and isinstance(value, dict):
                step[key] = text(value)
            elif key in ("word", "example") and isinstance(value, list):
                step[key] = word(value)
            elif key == "meaning" and isinstance(value, dict):
                step[key] = {"hi": value["hi"], "hinglish": value["hl"], **({"en": value["en"]} if "en" in value else {})}
        lesson.add(step_type, skills, **step)
    return lesson


# ---------- Level 2: words ----------

def words_lesson(spec):
    lesson = Lesson(spec["id"], 2)
    rng = lesson.rng
    words = spec["words"]
    skill = spec["skill"]
    with_picture = [w for w in words if w[1]]

    def learn(w):
        lesson.add(
            "learn_card", [skill],
            text={
                "hi": f"‘{w[0]}’ मतलब ‘{w[2]}’।",
                "hinglish": f"‘{w[0]}’ matlab ‘{w[3]}’.",
                "en": f"Say it with me: {w[0]}!",
            },
            word=word(w),
        )

    def match(w):
        others = [o for o in words if o is not w and o[2] != w[2]]
        options = [meaning(w)] + [meaning(o) for o in rng.sample(others, 2)]
        lesson.add("match_meaning", ["vocab.meaning", skill], word=word(w), answer=meaning(w), options=options)

    def ask_listening(w):
        others = [o for o in with_picture if o is not w and o[1] != w[1]]
        if w[1] and len(others) >= 2:
            options = [w] + rng.sample(others, min(3, len(others)))
            lesson.add("listen_tap", ["listen.word", skill], answer=w[0], options=[word(o) for o in options])
        else:
            match(w)

    def ask_reading(w):
        others = [o[0] for o in words if o is not w]
        if w[1]:
            lesson.add("word_pick", ["read.word", skill], emoji=w[1], answer=w[0], options=[w[0]] + rng.sample(others, 2))
        else:
            lesson.add("listen_read", ["read.word", skill], answer=w[0], options=[w[0]] + rng.sample(others, 2))

    lesson.add("learn_card", text=text(spec["intro"]))
    for k in range(0, len(words), 2):
        pair = words[k:k + 2]
        for w in pair:
            learn(w)
        ask_listening(pair[0])
        if len(pair) > 1:
            ask_reading(pair[1])

    # Mixed review: meanings, speaking (short picture words are easiest to say), reading by ear.
    for w in rng.sample(words, 2):
        match(w)
    for w in sorted(with_picture, key=lambda w: len(w[0]))[:2]:
        lesson.add("say_word", ["speak.word", skill], word=word(w), **({"accept": accept_of(w)} if accept_of(w) else {}))
    w = rng.choice(words)
    others = [o[0] for o in words if o is not w]
    lesson.add("listen_read", ["read.word", skill], answer=w[0], options=[w[0]] + rng.sample(others, 2))

    listed = ", ".join(w[0] for w in words)
    lesson.add("learn_card", text={
        "hi": f"शाबाश! आज के नए शब्द: {listed}।",
        "hinglish": f"Shabaash! Aaj ke naye shabd: {listed}.",
        "en": f"Well done! Today's new words: {listed}.",
    })
    return lesson


# ---------- Level 3: sentence patterns ----------

def role_tokens(line):
    """'मैं|s खुश|o हूँ|v' → [(word, role)]. Use _ for a space inside one block (ice_cream)."""
    out = []
    for token in line.split():
        w, role = token.rsplit("|", 1)
        out.append((w.replace("_", " "), role))
    return out


def pattern_lesson(spec):
    lesson = Lesson(spec["id"], 3)
    skill = spec["skill"]

    def train(t):
        hi, hl, en = role_tokens(t["hi"]), role_tokens(t["hl"]), role_tokens(t["en"])
        assert [r for _, r in hi] == [r for _, r in hl], f"{spec['id']}: hi/hl roles differ in {t['hi']}"
        lesson.add(
            "sentence_train", ["grammar.word_order", skill],
            hindi=[{"hi": h, "hinglish": l, "role": r} for (h, r), (l, _) in zip(hi, hl)],
            answer=[{"text": w, "role": r} for w, r in en],
            **({"extra": t["extra"]} if t.get("extra") else {}),
        )

    def blank(b):
        lesson.add(
            "fill_blank", [skill],
            sentence=b["s"], answer=b["a"], options=b["o"],
            meaning={"hi": b["hi"], "hinglish": b["hl"]},
        )

    def mistake(m):
        words = m["s"].split()
        wrong = words[m["w"]]
        # Keep the sentence's punctuation on the fix: "runs." → "run."
        fix = m["f"] + wrong[len(wrong.rstrip(".?!,")):]
        lesson.add(
            "spot_mistake", ["grammar.spot_mistake", skill],
            words=words, wrong=m["w"], fix=fix,
            meaning={"hi": m["hi"], "hinglish": m["hl"]},
        )

    def say(s):
        lesson.add(
            "say_sentence", ["speak.sentence", skill],
            sentence=s["en"], meaning={"hi": s["hi"], "hinglish": s["hl"], "en": s["en"]},
        )

    for card in spec["explain"]:
        lesson.add("learn_card", [skill], text=text(card))
    t, b, m, s = spec["train"], spec["blanks"], spec["mistakes"], spec["say"]
    train(t[0]); blank(b[0]); train(t[1]); mistake(m[0]); blank(b[1]); say(s[0])
    train(t[2]); blank(b[2]); mistake(m[1]); say(s[1])
    lesson.add("learn_card", text={
        "hi": "शाबाश! अब ऐसी लाइनें बोलना तुम्हारे लिए आसान है। रोज़ बोलकर प्रैक्टिस करो!",
        "hinglish": "Shabaash! Ab aisi lines bolna tumhare liye aasaan hai. Roz bolkar practice karo!",
        "en": "Well done! Now these sentences are easy for you. Practise saying them every day!",
    })
    return lesson


# ---------- Main ----------

def load(name):
    return json.loads((SRC / name).read_text(encoding="utf-8"))


def main():
    (OUT / "lessons").mkdir(parents=True, exist_ok=True)
    written = []
    for spec in load("phonics.json")["lessons"]:
        written.append(phonics_lesson(spec).write())
    for spec in load("words.json")["lessons"]:
        written.append(words_lesson(spec).write())
    for spec in load("patterns.json")["lessons"]:
        written.append(pattern_lesson(spec).write())

    curriculum = load("curriculum.json")
    levels = []
    for level in curriculum["levels"]:
        lessons = [{"id": l["id"], "emoji": l["emoji"], "title": text(l["title"])} for l in level.get("lessons", [])]
        for l in lessons:
            if not (OUT / "lessons" / f"{l['id']}.json").exists():
                raise SystemExit(f"curriculum lists {l['id']} but there is no lesson file for it")
        entry = {"number": level["number"], "emoji": level["emoji"], "title": text(level["title"])}
        if lessons:
            entry["lessons"] = lessons
        levels.append(entry)
    (OUT / "levels.json").write_text(json.dumps({"levels": levels}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    total = sum(len(l.get("lessons", [])) for l in levels)
    print(f"Generated {len(written)} lessons; levels.json lists {total} lessons in {len(levels)} levels.")


if __name__ == "__main__":
    main()
