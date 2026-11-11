"""Fetches the read-along listener for the test build: sherpa-onnx's Android libraries and the
small streaming Zipformer English model (both Apache 2.0). None of it is kept in git.

    python tools/get_readalong_engine.py

Needs: pip install sentencepiece   (to turn the model's bpe.model into the bpe.vocab sherpa-onnx reads)
"""
import io
import pathlib
import shutil
import tarfile
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
SHERPA_VERSION = "v2.0.1"
LIBS_URL = f"https://github.com/k2-fsa/sherpa-onnx/releases/download/{SHERPA_VERSION}/sherpa-onnx-{SHERPA_VERSION}-android.tar.bz2"
MODEL = "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17"
MODEL_URL = f"https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/{MODEL}.tar.bz2"
# This bigger model was trained with the same 500-piece BPE vocabulary; the small one ships without the file.
BPE_URL = "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-26/resolve/main/bpe.model"
KOTLIN_API = ["OnlineRecognizer.kt", "OnlineStream.kt", "FeatureConfig.kt", "HomophoneReplacerConfig.kt", "QnnConfig.kt"]
ABIS = ["arm64-v8a", "armeabi-v7a"]


def fetch(url: str) -> bytes:
    print("downloading", url)
    return urllib.request.urlopen(url).read()


def main() -> None:
    libs = MAIN / "jniLibs"
    if not (libs / ABIS[0] / "libsherpa-onnx-jni.so").exists():
        with tarfile.open(fileobj=io.BytesIO(fetch(LIBS_URL)), mode="r:bz2") as archive:
            for member in archive.getmembers():
                parts = pathlib.PurePosixPath(member.name).parts
                if member.isfile() and len(parts) >= 3 and parts[-2] in ABIS and parts[-1] in ("libsherpa-onnx-jni.so", "libonnxruntime.so"):
                    out = libs / parts[-2] / parts[-1]
                    out.parent.mkdir(parents=True, exist_ok=True)
                    out.write_bytes(archive.extractfile(member).read())
    api = MAIN / "java/com/k2fsa/sherpa/onnx"
    api.mkdir(parents=True, exist_ok=True)
    for name in KOTLIN_API:
        if not (api / name).exists():
            (api / name).write_bytes(fetch(f"https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/{SHERPA_VERSION}/sherpa-onnx/kotlin-api/{name}"))

    model_dir = MAIN / "assets/zipformer-en-20m"
    if not (model_dir / "tokens.txt").exists():
        model_dir.mkdir(parents=True, exist_ok=True)
        with tarfile.open(fileobj=io.BytesIO(fetch(MODEL_URL)), mode="r:bz2") as archive:
            for member in archive.getmembers():
                name = pathlib.PurePosixPath(member.name).name
                if member.isfile() and (name.endswith(".int8.onnx") or name == "tokens.txt"):
                    (model_dir / name).write_bytes(archive.extractfile(member).read())
    vocab = model_dir / "bpe.vocab"
    if not vocab.exists():
        import sentencepiece as spm

        bpe = model_dir / "bpe.model"
        bpe.write_bytes(fetch(BPE_URL))
        sp = spm.SentencePieceProcessor(model_file=str(bpe))
        vocab.write_text("".join(f"{sp.id_to_piece(i)} {sp.get_score(i)}\n" for i in range(sp.get_piece_size())), encoding="utf-8")
        bpe.unlink()
    print("ready:", libs, model_dir)
    shutil.rmtree(ROOT / "app/build/intermediates/merged_native_libs", ignore_errors=True)


if __name__ == "__main__":
    main()
