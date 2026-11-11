"""Builds the reading ladder from tools/content/reading.json:
  app/src/main/assets/content/reading.json   (levels of passages, same shape as levels.json)
  app/src/main/assets/content/lessons/R*.json (one lesson per passage)

Each passage lesson: an intro card, one card per key word, the read-along, then two story questions.
    python tools/content/generate_reading.py
"""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCE = ROOT / "tools/content/reading.json"
CONTENT = ROOT / "app/src/main/assets/content"


def L(hi, hl, en):
    return {"hi": hi, "hinglish": hl, "en": en}


def lang(d):
    return L(d["hi"], d["hl"], d["en"])


def sentences(text):
    """Sentences in reading order; "" marks a paragraph break. Splits after . ! ? (and a closing quote)."""
    out = []
    for i, paragraph in enumerate(text.split("\n\n")):
        if i:
            out.append("")
        parts = re.split(r'(?<=[.!?])\s+|(?<=[.!?]["”])\s+', paragraph.strip())
        out += [p for p in parts if p]
    return out


def lesson_for(level, passage):
    pid = passage["id"]
    title = lang(passage["title"])
    steps = [{
        "type": "learn_card", "id": f"{pid}.S01", "skills": [],
        "text": L(
            f"आज की कहानी: ‘{title['en']}’ ({title['hi']})। पहले कुछ शब्द सीखते हैं।",
            f"Aaj ki kahani: ‘{title['en']}’ ({title['hinglish']}). Pehle kuch shabd seekhte hain.",
            f"Today's story: ‘{title['en']}’. First, a few words.",
        ),
    }]
    for k, word in enumerate(passage["words"], start=2):
        steps.append({
            "type": "learn_card", "id": f"{pid}.S{k:02d}", "skills": [],
            "text": L(
                f"‘{word['en']}’ मतलब {word['hi']}। छूकर सुनो, फिर बोलो।",
                f"‘{word['en']}’ matlab {word['hl']}. Chhookar suno, phir bolo.",
                f"‘{word['en']}’ means {word['hi']}. Tap to listen, then say it.",
            ),
            "word": {"english": word["en"], "emoji": word["emoji"], "hindi": word["hi"], "hinglish": word["hl"]},
        })
    n = len(passage["words"]) + 2
    steps.append({
        "type": "read_along", "id": f"{pid}.S{n:02d}", "skills": ["read.passage"],
        "title": title,
        "sentences": sentences(passage["text"]),
        "words": [{"english": w["en"], "emoji": w["emoji"], "hindi": w["hi"], "hinglish": w["hl"]} for w in passage["words"]],
    })
    for q in passage["questions"]:
        n += 1
        steps.append({
            "type": "story_question", "id": f"{pid}.S{n:02d}", "skills": ["read.understand"],
            "question": lang(q["q"]), "options": q["options"], "answer": q["answer"],
        })
    return {"id": pid, "level": 100 + level["number"], "steps": steps}


def main():
    source = json.loads(SOURCE.read_text(encoding="utf-8"))
    levels = []
    for level in source["levels"]:
        for passage in level["passages"]:
            lesson = lesson_for(level, passage)
            (CONTENT / "lessons" / f"{passage['id']}.json").write_text(
                json.dumps(lesson, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        levels.append({
            "number": level["number"], "emoji": level["emoji"], "title": lang(level["title"]),
            "lessons": [{"id": p["id"], "emoji": p["emoji"], "title": lang(p["title"])} for p in level["passages"]],
        })
    (CONTENT / "reading.json").write_text(json.dumps({"levels": levels}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    words = sum(len(" ".join(sentences(p["text"])).split()) for lv in source["levels"] for p in lv["passages"])
    print(f"{sum(len(lv['passages']) for lv in source['levels'])} passages, {words} words")


if __name__ == "__main__":
    main()
