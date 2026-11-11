"""Builds Word Power from tools/content/wordpower.json:
  app/src/main/assets/content/wordpower.json   bands, packs and their words (for the screens)
  app/src/main/assets/content/lessons/W??.P??.json   learn a pack (10 word cards, say, practise)
  app/src/main/assets/content/lessons/W??.P??T.json  the pack test (10 questions, one per word)
  app/src/main/assets/content/lessons/W.PLACE.json   placement test (5 words per band)

    python tools/content/generate_wordpower.py
"""
import json
import pathlib
import random
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCE = ROOT / "tools/content/wordpower.json"
HINDI = ROOT / "tools/content/wordpower_hindi.json"
CONTENT = ROOT / "app/src/main/assets/content"
PLACEMENT = "W.PLACE"
SKILL = "vocab.power"
SPELL = "spell.word"


def L(hi, hl, en):
    return {"hi": hi, "hinglish": hl, "en": en}


def lang(d):
    return L(d["hi"], d["hl"], d["en"])


def word_obj(w):
    return {"english": w["en"], "emoji": w["emoji"], "hindi": w["hi"], "hinglish": w["hl"]}


def blank(example, word):
    pattern = re.compile(r"\b" + re.escape(word) + r"\b", re.IGNORECASE)
    if len(pattern.findall(example)) != 1:
        raise SystemExit(f"'{word}' must appear exactly once in: {example}")
    return pattern.sub("___", example, count=1)


def others(words, w, rng, n):
    pool = [x for x in words if x["en"] != w["en"] and x["hi"] != w["hi"]]
    return rng.sample(pool, n)


def match_meaning(sid, w, words, rng):
    opts = [{"hi": w["hi"], "hinglish": w["hl"]}] + [{"hi": o["hi"], "hinglish": o["hl"]} for o in others(words, w, rng, 2)]
    return {"type": "match_meaning", "id": sid, "skills": [SKILL], "word": word_obj(w),
            "answer": {"hi": w["hi"], "hinglish": w["hl"]}, "options": opts}


def fill_blank(sid, w, words, rng):
    return {"type": "fill_blank", "id": sid, "skills": [SKILL], "sentence": blank(w["example"], w["en"]),
            "answer": w["en"], "options": [w["en"]] + [o["en"] for o in others(words, w, rng, 2)],
            "meaning": {"hi": w["hi"], "hinglish": w["hl"]}}


def listen_read(sid, w, words, rng):
    return {"type": "listen_read", "id": sid, "skills": [SKILL],
            "answer": w["en"], "options": [w["en"]] + [o["en"] for o in others(words, w, rng, 2)]}


def spell(sid, w, typed):
    return {"type": "spell_word", "id": sid, "skills": [SPELL], "word": word_obj(w), "typed": typed}


def learn_lesson(pack_id, title, words, rng):
    steps = [{
        "type": "learn_card", "id": f"{pack_id}.S01", "skills": [],
        "text": L(
            f"आज के 10 नए शब्द: {title['hi']}। हर शब्द सुनो, समझो और बोलो।",
            f"Aaj ke 10 naye shabd: {title['hinglish']}. Har shabd suno, samjho aur bolo.",
            f"Ten new words today: {title['en']}. Listen, understand and say each one.",
        ),
    }]
    n = 1

    def sid():
        nonlocal n
        n += 1
        return f"{pack_id}.S{n:02d}"

    for w in words:
        steps.append({"type": "learn_word", "id": sid(), "skills": [SKILL], "word": word_obj(w),
                      "meaning": w["meaning"], "example": w["example"],
                      "exampleMeaning": {"hi": w["exampleHi"], "hinglish": w["exampleHl"]}})
    practice = [{"type": "say_word", "id": "", "skills": ["speak.word"], "word": word_obj(w)} for w in words[:4]]
    practice += [match_meaning("", words[i], words, rng) for i in (0, 2, 4, 6)]
    practice += [fill_blank("", words[i], words, rng) for i in (1, 3, 5, 7)]
    practice += [listen_read("", words[i], words, rng) for i in (8, 9)]
    rng.shuffle(practice)
    for step in practice:
        step["id"] = sid()
        steps.append(step)
    return steps


def test_lesson(test_id, words, pack_index, typed, rng):
    steps = []
    order = list(words)
    rng.shuffle(order)
    for i, w in enumerate(order):
        sid = f"{test_id}.S{i + 1:02d}"
        kind = (i + pack_index) % 4
        if kind == 0:
            steps.append(match_meaning(sid, w, words, rng))
        elif kind == 1:
            steps.append(fill_blank(sid, w, words, rng))
        elif kind == 2:
            steps.append(listen_read(sid, w, words, rng))
        else:
            steps.append(spell(sid, w, typed))
    return steps


def main():
    source = json.loads(SOURCE.read_text(encoding="utf-8"))
    hindi = {k: v for k, v in json.loads(HINDI.read_text(encoding="utf-8")).items() if not k.startswith("_")}
    bands_out = []
    placement_steps = []
    seen = set()
    for band in source["bands"]:
        packs_out = []
        typed = band["id"] != "W34"
        for p, pack in enumerate(band["packs"]):
            pack_id = f"{band['id']}.P{p + 1:02d}"
            words = [dict(zip(["en", "emoji", "hi", "hl", "meaning", "example"], w)) for w in pack["words"]]
            for w in words:
                if w["en"] not in hindi:
                    raise SystemExit(f"no Hindi meaning for the example of '{w['en']}' in wordpower_hindi.json")
                w["exampleHi"], w["exampleHl"] = hindi[w["en"]]
                if w["en"] in seen:
                    raise SystemExit(f"duplicate word {w['en']}")
                seen.add(w["en"])
                if re.search(r"\d", w["example"] + w["meaning"]):
                    raise SystemExit(f"digits in {w['en']}")
            title = lang(pack["title"])
            rng = random.Random(pack_id)
            lessons = {
                pack_id: learn_lesson(pack_id, title, words, rng),
                pack_id + "T": test_lesson(pack_id + "T", words, p, typed, rng),
            }
            for lesson_id, steps in lessons.items():
                (CONTENT / "lessons" / f"{lesson_id}.json").write_text(
                    json.dumps({"id": lesson_id, "level": 200, "steps": steps}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            if p % 2 == 1:
                placement_steps.append(match_meaning(f"{PLACEMENT}.{band['id']}.{p // 2 + 1}", words[0], words, rng))
            packs_out.append({
                "id": pack_id, "testId": pack_id + "T", "emoji": pack["emoji"], "title": title,
                "words": [{"english": w["en"], "emoji": w["emoji"], "hindi": w["hi"], "hinglish": w["hl"],
                           "meaning": w["meaning"], "example": w["example"],
                           "exampleMeaning": {"hi": w["exampleHi"], "hinglish": w["exampleHl"]}} for w in words],
            })
        bands_out.append({"id": band["id"], "emoji": band["emoji"], "title": lang(band["title"]),
                          "minClass": band["minClass"], "maxClass": band["maxClass"], "packs": packs_out})
    (CONTENT / "lessons" / f"{PLACEMENT}.json").write_text(
        json.dumps({"id": PLACEMENT, "level": 200, "steps": placement_steps}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (CONTENT / "wordpower.json").write_text(
        json.dumps({"placement": PLACEMENT, "bands": bands_out}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    unused = set(hindi) - seen
    if unused:
        raise SystemExit(f"Hindi meanings for words not in any pack: {sorted(unused)}")
    print(f"{len(bands_out)} bands, {sum(len(b['packs']) for b in bands_out)} packs, {len(seen)} words, placement {len(placement_steps)} questions")


if __name__ == "__main__":
    main()
