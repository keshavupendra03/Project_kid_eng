"""
Builds the school textbook chapters (MP Board English, Classes 6-8) from tools/content/book/*.json.

Each source file is one chapter: its sections in the book's order, each with its steps written out.
The generator only does the bookkeeping:
  - numbers the lessons (B7.04.1 ...; the test is B7.04.T) and their steps (B7.04.1.S01 ...),
  - turns short translations into objects: ["hi", "hinglish", "en"] -> L, ["hi", "hinglish"] -> Meaning,
    and a "word" written as ["english", "emoji", "hindi", "hinglish"] into a Word,
  - gives a step its section's skills when it has none,
  - collects steps marked "book": true (questions copied exactly from the textbook),
  - writes app/src/main/assets/content/book.json and one lesson file per section,
  - writes tools/content/book/review/<chapter>.md, an answer sheet for a person to check.

Run: python tools/content/generate_book.py
"""
import json
from pathlib import Path

HERE = Path(__file__).parent
SOURCES = HERE / "book"
REVIEW = SOURCES / "review"
ASSETS = HERE.parent.parent / "app" / "src" / "main" / "assets" / "content"
LESSONS = ASSETS / "lessons"

# Keys whose short lists are translations.
TRANSLATED = {"text", "prompt", "title", "instruction", "meaning", "questionMeaning", "modelMeaning", "exampleMeaning",
              "lineMeanings"}


def expand(value, key=None):
    if isinstance(value, dict):
        return {k: expand(v, k) for k, v in value.items()}
    if isinstance(value, list):
        if key in TRANSLATED and all(isinstance(x, str) for x in value):
            if len(value) == 3:
                return {"hi": value[0], "hinglish": value[1], "en": value[2]}
            if len(value) == 2:
                return {"hi": value[0], "hinglish": value[1]}
            raise ValueError(f"translation {key} needs 2 or 3 parts: {value}")
        if key == "word" and len(value) == 4 and all(isinstance(x, str) for x in value):
            english, emoji, hindi, hinglish = value
            return {"english": english, "emoji": emoji, "hindi": hindi, "hinglish": hinglish}
        return [expand(v, key) for v in value]
    return value


def auto_crossword(step):
    """Places crossword words that have no row/col: each new word must cross a placed one, with no letter
    clashes and no word touching another side by side. Keeps the book's across/down when it can."""
    entries = step["entries"]
    if all("row" in e for e in entries):
        return step

    def cells(word, r, c, across):
        return [((r, c + i) if across else (r + i, c), ch) for i, ch in enumerate(word)]

    def attempt(order, rng):
        grid = {}  # (r, c) -> letter
        placed = []

        def fits(word, r, c, across):
            crossings = 0
            for (rr, cc), ch in cells(word, r, c, across):
                have = grid.get((rr, cc))
                if have is not None:
                    if have != ch:
                        return False
                    crossings += 1
                    continue
                # A new letter may not sit right next to another word's letter.
                sides = [(rr - 1, cc), (rr + 1, cc)] if across else [(rr, cc - 1), (rr, cc + 1)]
                if any(s in grid for s in sides):
                    return False
            before = (r, c - 1) if across else (r - 1, c)
            after = (r, c + len(word)) if across else (r + len(word), c)
            return crossings > 0 and before not in grid and after not in grid

        def place(e, word, r, c, across):
            for cell, ch in cells(word, r, c, across):
                grid[cell] = ch
            placed.append({**e, "row": r, "col": c, "across": across})

        first = order[0]
        place(first, first["answer"].lower(), 0, 0, first.get("across", True))
        pending = order[1:]
        for _ in range(len(order)):  # a word that can't cross anything yet may fit once others are placed
            left = []
            for e in pending:
                word = e["answer"].lower()
                wanted = e.get("across")
                spots = []
                for across in ([wanted, not wanted] if wanted is not None else [True, False]):
                    for (r, c), ch in sorted(grid.items()):
                        for i, letter in enumerate(word):
                            if letter == ch:
                                sr, sc = (r, c - i) if across else (r - i, c)
                                if fits(word, sr, sc, across):
                                    spots.append((sr, sc, across))
                    if spots:
                        break  # the book's direction wins whenever it fits
                if spots:
                    place(e, word, *(spots[0] if rng is None else rng.choice(spots)))
                else:
                    left.append(e)
            pending = left
        return None if pending else (grid, placed)

    # Longest words first; if that can't fit every word, try other orders and keep the most compact grid.
    import random
    rng = random.Random(len(entries))
    best = None
    tries = [(sorted(entries, key=lambda e: -len(e["answer"])), None)] + [
        (rng.sample(entries, len(entries)), random.Random(n)) for n in range(400)]
    for order, pick in tries:
        result = attempt(order, pick)
        if result is None:
            continue
        grid, placed = result
        rows = max(r for r, _ in grid) - min(r for r, _ in grid) + 1
        cols = max(c for _, c in grid) - min(c for _, c in grid) + 1
        if cols <= 13 and (best is None or rows * cols < best[0]):
            best = (rows * cols, grid, placed)
        if best is not None and pick is None:
            break  # the simple layout fits: keep it
    if best is None:
        raise ValueError(f"crossword: couldn't place all of {[e['answer'] for e in entries]}")
    _, grid, placed = best
    top = min(r for r, _ in grid)
    leftmost = min(c for _, c in grid)
    for e in placed:
        e["row"] -= top
        e["col"] -= leftmost
    rows = max(r for r, _ in grid) - top + 1
    cols = max(c for _, c in grid) - leftmost + 1
    return {**step, "rows": rows, "cols": cols, "entries": sorted(placed, key=lambda e: e["number"])}


def shorthand(step):
    """Short forms for the common steps, so a chapter file stays readable. A step with "type" is left as it is.

    card:    [hi, hinglish, en]                                         -> learn_card
    word:    [english, hindi, hinglish, meaning, example, exHi, exHinglish] -> learn_word
    sayw:    [english, hindi, hinglish]                                 -> say_word
    says:    [sentence, hi, hinglish]                                   -> say_sentence
    q:       [question, qHi, qHinglish, [options], answer, model?, modelHi?, modelHinglish?, [emojis]?] -> choice
    tf:      [statement, qHi, qHinglish, true|false, correction?, corrHi?, corrHinglish?] -> True/False choice
    fb:      [sentence with ___, answer, [options], hi, hinglish]       -> fill_blank
    fix:     [sentence, wrong word index, fix, hi, hinglish]            -> spot_mistake
    pairs:   [instrHi, instrHinglish, instrEn, [[left, right, hi, hinglish], ...]] -> match_pairs
    build:   [promptHi, promptHinglish, promptEn, answer, hi, hinglish, [extra]?] -> build_sentence
    """
    s = dict(step)
    extra = {k: s.pop(k) for k in ("book", "skills") if k in s}
    if s.get("type") == "crossword":
        return {**auto_crossword(s), **extra}
    if "type" in s:
        return {**s, **extra}
    (key, v), = s.items()
    if key == "card":
        out = {"type": "learn_card", "text": v}
    elif key == "word":
        out = {"type": "learn_word", "word": [v[0], "", v[1], v[2]], "meaning": v[3], "example": v[4], "exampleMeaning": [v[5], v[6]]}
    elif key == "sayw":
        out = {"type": "say_word", "word": [v[0], "", v[1], v[2]]}
    elif key == "says":
        out = {"type": "say_sentence", "sentence": v[0], "meaning": [v[1], v[2], v[0]]}
    elif key == "q":
        out = {"type": "choice", "question": v[0], "questionMeaning": [v[1], v[2]], "options": v[3], "answer": v[4]}
        if len(v) > 5 and v[5]:
            out.update(modelAnswer=v[5], modelMeaning=[v[6], v[7]])
        if len(v) > 8:
            out["emojis"] = v[8]
    elif key == "tf":
        out = {"type": "choice", "question": f"True or false? {v[0]}", "questionMeaning": [v[1], v[2]],
               "options": ["True", "False"], "answer": "True" if v[3] else "False"}
        if len(v) > 4:
            out.update(modelAnswer=v[4], modelMeaning=[v[5], v[6]])
    elif key == "fb":
        out = {"type": "fill_blank", "sentence": v[0], "answer": v[1], "options": v[2], "meaning": [v[3], v[4]]}
    elif key == "fix":
        out = {"type": "spot_mistake", "words": v[0].split(" "), "wrong": v[1], "fix": v[2], "meaning": [v[3], v[4]]}
    elif key == "pairs":
        out = {"type": "match_pairs", "instruction": [v[0], v[1], v[2]],
               "pairs": [{"left": p[0], "right": p[1], "meaning": [p[2], p[3]]} for p in v[3]]}
    elif key == "build":
        out = {"type": "build_sentence", "prompt": [v[0], v[1], v[2]], "answer": v[3], "meaning": [v[4], v[5]]}
        if len(v) > 6 and v[6]:
            out["extra"] = v[6]
    else:
        raise ValueError(f"unknown step: {step}")
    return {**out, **extra}


def build_chapter(src):
    grade, number = src["grade"], src["number"]
    chapter_id = f"B{grade}.{number:02d}"
    sections, lessons = [], []
    for index, section in enumerate(src["sections"], start=1):
        lesson_id = f"{chapter_id}.T" if section.get("test") else f"{chapter_id}.{index}"
        steps, book_steps = [], []
        for n, raw in enumerate(section["steps"], start=1):
            step = shorthand(raw)
            is_book = step.pop("book", False)
            step_type = step.pop("type")
            step_id = f"{lesson_id}.S{n:02d}"
            skills = step.pop("skills", section.get("skills", []))
            steps.append({"type": step_type, "id": step_id, "skills": skills, **expand(step)})
            if is_book:
                book_steps.append(step_id)
        lessons.append({"id": lesson_id, "level": grade, "bookSteps": book_steps, "steps": steps})
        sections.append({
            "id": lesson_id,
            "emoji": section["emoji"],
            "title": expand(section["title"], "title"),
            "book": section.get("book", False),
            **({"test": True} if section.get("test") else {}),
        })
    chapter = {
        "id": chapter_id, "number": number, "emoji": src["emoji"], "title": src["title"], "pages": src["pages"],
        "sections": sections,
    }
    return grade, chapter, lessons


# ---------- Answer sheet for review ----------

def tr(value):
    """A translation as one line: Hindi, then the Hinglish in brackets."""
    if isinstance(value, dict):
        return f"{value['hi']}  ({value['hinglish']})"
    return str(value)


def review_lines(step, book):
    t = step["type"]
    tag = "📕 BOOK" if book else "⭐ ours"
    out = [f"**{step['id']}** · {t} · {tag}"]
    if t == "learn_card":
        out.append(f"- Tinku (Hindi): {step['text']['hi']}")
        out.append(f"- Tinku (English): {step['text']['en']}")
    elif t == "learn_word":
        w = step["word"]
        out.append(f"- **{w['english']}** = {w['hindi']} ({w['hinglish']}) — {step['meaning']}")
        out.append(f"- Example: {step['example']} = {tr(step['exampleMeaning'])}")
    elif t == "say_word":
        out.append(f"- Say: **{step['word']['english']}**")
    elif t == "say_sentence":
        out.append(f"- Say: **{step['sentence']}** = {step['meaning']['hi']}")
    elif t == "choice":
        out.append(f"- Q: {step['question']}" + (f" = {tr(step['questionMeaning'])}" if step.get("questionMeaning") else ""))
        out.append(f"- Options: {' / '.join(step['options'])}")
        out.append(f"- ✅ Answer: **{step['answer']}**")
        if step.get("modelAnswer"):
            out.append(f"- ✍️ Exam answer: {step['modelAnswer']} = {tr(step['modelMeaning'])}")
    elif t == "fill_blank":
        out.append(f"- {step['sentence']}  →  options: {' / '.join(step['options'])}")
        out.append(f"- ✅ Answer: **{step['answer']}**" + (f" = {tr(step['meaning'])}" if step.get("meaning") else ""))
    elif t == "spot_mistake":
        out.append(f"- Sentence: {' '.join(step['words'])}")
        out.append(f"- ✅ Wrong word: **{step['words'][step['wrong']]}** → **{step['fix']}** = {tr(step.get('meaning', ''))}")
    elif t == "match_pairs":
        out.append(f"- {step['instruction']['en']}")
        for p in step["pairs"]:
            out.append(f"  - **{p['left']}** ↔ {p['right']}" + (f" = {tr(p['meaning'])}" if p.get("meaning") else ""))
    elif t == "crossword":
        for e in sorted(step["entries"], key=lambda e: e["number"]):
            way = "across" if e["across"] else "down"
            given = " (given)" if e.get("given") else ""
            out.append(f"  - {e['number']} {way}: {e['clue']} → **{e['answer']}**{given}")
    elif t == "sort_groups":
        names = {g["id"]: g["name"] for g in step["groups"]}
        for item in step["items"]:
            out.append(f"  - {item['emoji']} {item['text']} → {' or '.join(names[g] for g in item['groups'])}")
    elif t == "build_sentence":
        out.append(f"- Task (Hindi): {step['prompt']['hi']}")
        out.append(f"- ✅ Sentence: **{step['answer']}** = {tr(step['meaning'])}")
        if step.get("extra"):
            out.append(f"- Wrong tiles mixed in: {', '.join(step['extra'])}")
    elif t == "read_along":
        out.append(f"- Poem: {step['title']['en']}")
        for n, line in enumerate(step["sentences"]):
            meaning = step.get("lineMeanings", [])
            if line.strip():
                out.append(f"  - {line}" + (f"  →  {tr(meaning[n])}" if n < len(meaning) else ""))
    elif t == "roleplay":
        for turn in step["turns"]:
            out.append(f"  - Tinku: {turn['tinku']} = {turn['meaning']['hi']}")
            for r in turn["replies"]:
                out.append(f"    - Child: {r['say']} = {r['meaning']['hi']}")
    return out


def write_review(chapter, lessons, src):
    lines = [
        f"# Class {src['grade']} · Lesson {chapter['number']}: {chapter['title']} — answer sheet",
        "",
        f"Book pages {chapter['pages']}. 📕 BOOK = copied exactly from the textbook; ⭐ ours = extra practice.",
        "Please check every ✅ answer, every exam answer and every Hindi line.",
        "",
    ]
    for section, lesson in zip(chapter["sections"], lessons):
        lines += [f"## {section['emoji']} {section['title']['en']} ({lesson['id']})", ""]
        for step in lesson["steps"]:
            lines += review_lines(step, step["id"] in lesson["bookSteps"]) + [""]
    REVIEW.mkdir(parents=True, exist_ok=True)
    (REVIEW / f"{chapter['id']}.md").write_text("\n".join(lines), encoding="utf-8")


def main():
    classes = {}
    count = 0
    for path in sorted(SOURCES.glob("*.json")):
        src = json.loads(path.read_text(encoding="utf-8"))
        grade, chapter, lessons = build_chapter(src)
        classes.setdefault(grade, []).append(chapter)
        for lesson in lessons:
            (LESSONS / f"{lesson['id']}.json").write_text(json.dumps(lesson, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            count += 1
        write_review(chapter, lessons, src)
    book = {"classes": [{"grade": g, "chapters": sorted(chs, key=lambda c: c["number"])} for g, chs in sorted(classes.items())]}
    (ASSETS / "book.json").write_text(json.dumps(book, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    chapters = sum(len(c) for c in classes.values())
    print(f"Book: {chapters} chapter(s), {count} section lessons. Answer sheets in {REVIEW.relative_to(HERE.parent.parent)}")


if __name__ == "__main__":
    main()
