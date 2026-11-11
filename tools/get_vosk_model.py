"""Downloads the small Indian English Vosk model into the test build's assets.

The model (about 36 MB, Apache 2.0) is not kept in git. Run once before building the test app:
    python tools/get_vosk_model.py
"""
import io
import pathlib
import shutil
import urllib.request
import uuid
import zipfile

URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip"
TARGET = pathlib.Path(__file__).resolve().parents[1] / "app/src/debug/assets/model-en-in"


def main() -> None:
    if (TARGET / "am" / "final.mdl").exists():
        print(f"Already there: {TARGET}")
        return
    print(f"Downloading {URL} ...")
    data = urllib.request.urlopen(URL).read()
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        root = archive.namelist()[0].split("/")[0]
        if TARGET.exists():
            shutil.rmtree(TARGET)
        TARGET.mkdir(parents=True)
        for item in archive.infolist():
            name = item.filename[len(root) + 1:]
            if not name or item.is_dir():
                continue
            out = TARGET / name
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(archive.read(item))
    # Vosk copies the model out of the app on first run, and again whenever this id changes.
    (TARGET / "uuid").write_text(uuid.uuid4().hex)
    print(f"Done: {TARGET}")


if __name__ == "__main__":
    main()
