import io
import os
from typing import List, Optional

from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from tika import parser
import pytesseract
from PIL import Image


TIKA_MAX_FILE_SIZE = int(os.getenv("TIKA_MAX_FILE_SIZE", 75 * 1024 * 1024))
DEFAULT_LANGUAGES = os.getenv("OCR_LANGUAGES", "kat+eng+rus")

# -------------------------------------------------------
# 🔧 Настройка путей Apache Tika
# -------------------------------------------------------
os.environ.setdefault("TIKA_SERVER_JAR", "/opt/tika/tika-app.jar")
os.environ.setdefault("TIKA_PATH", "/opt/tika/tika-app.jar")
os.environ.setdefault("TIKA_SERVER_ENDPOINT", "http://127.0.0.1:9998")
os.environ.setdefault("TIKA_LOG_PATH", "/tmp/tika.log")


class OcrBlock(BaseModel):
    text: str
    left: int
    top: int
    width: int
    height: int
    confidence: float


class OcrResponse(BaseModel):
    tika_text: str
    ocr_text: str
    combined_text: str
    language_hint: str
    blocks: List[OcrBlock]


app = FastAPI(title="Anubis OCR Gateway", version="1.0.0")


def _ensure_tika_ready() -> None:
    try:
        parser.from_buffer(b"Anubis OCR health-check", xmlContent=False)
    except Exception as exc:  # pragma: no cover - defensive logging
        raise RuntimeError(f"Unable to start embedded Tika server: {exc}") from exc


@app.on_event("startup")
async def startup_event() -> None:
    _ensure_tika_ready()


@app.get("/healthz")
async def health() -> JSONResponse:
    try:
        _ensure_tika_ready()
        return JSONResponse({"status": "ok"})
    except Exception as exc:
        raise HTTPException(status_code=503, detail=str(exc))


def _read_upload(upload: UploadFile) -> bytes:
    file_name = upload.filename or "<unnamed>"
    data = upload.file.read()
    if not data:
        raise HTTPException(status_code=400, detail="Файл пустой")

    file_size_mb = len(data) / (1024 * 1024)
    max_size_mb = TIKA_MAX_FILE_SIZE / (1024 * 1024)

    if len(data) > TIKA_MAX_FILE_SIZE:
        print(f"⚠️ Skipping OCR for {file_name}: {file_size_mb:.1f} MB > {max_size_mb:.1f} MB limit")
        raise HTTPException(status_code=413, detail=f"Файл {file_name} слишком большой для OCR ({file_size_mb:.1f} MB)")

    print(f"📄 Received file for OCR: {file_name} ({file_size_mb:.1f} MB)")
    return data


def _run_tika(data: bytes) -> str:
    try:
        parsed = parser.from_buffer(data, xmlContent=False)
        content = parsed.get("content") or ""
        return _sanitize_text(content)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Tika parse error: {exc}")


def _run_ocr(data: bytes, languages: str) -> tuple[str, List[OcrBlock]]:
    try:
        image = Image.open(io.BytesIO(data))
        image.load()
    except Exception:
        return "", []

    langs = "+".join(
        [lang.strip() for lang in languages.replace(",", "+").replace(";", "+").replace(" ", "+").split("+") if lang.strip()]
    )
    print(f"🧠 Using OCR languages: {langs}")

    try:
        tess_output = pytesseract.image_to_data(
            image,
            lang=langs,
            output_type=pytesseract.Output.DICT,
            config="--psm 3"
        )
    except pytesseract.TesseractError as e:
        raise HTTPException(status_code=500, detail=f"Tesseract OCR error: {e}")

    text_lines: List[str] = []
    blocks: List[OcrBlock] = []
    n = len(tess_output["text"])
    for i in range(n):
        text = tess_output["text"][i]
        if not text.strip():
            continue
        conf_str = tess_output["conf"][i]
        try:
            conf = float(conf_str)
        except (TypeError, ValueError):
            conf = 0.0
        block = OcrBlock(
            text=text.strip(),
            left=int(float(tess_output["left"][i])),
            top=int(float(tess_output["top"][i])),
            width=int(float(tess_output["width"][i])),
            height=int(float(tess_output["height"][i])),
            confidence=conf,
        )
        blocks.append(block)
        text_lines.append(block.text)

    return _sanitize_text(" ".join(text_lines)), blocks


def _sanitize_text(text: Optional[str]) -> str:
    if not text:
        return ""
    sanitized = text.replace("\u0000", " ")
    sanitized = sanitized.replace("\r\n", "\n").replace("\r", "\n")
    return sanitized.strip()


def _merge_texts(primary: str, secondary: str) -> str:
    if not primary:
        return secondary
    if not secondary:
        return primary
    if primary.lower() == secondary.lower() or primary in secondary:
        return secondary
    if secondary in primary:
        return primary
    return f"{primary}\n\n{secondary}".strip()


@app.post("/ocr", response_model=OcrResponse)
async def ocr_endpoint(file: UploadFile = File(...), languages: Optional[str] = None) -> OcrResponse:
    file_name = file.filename or "<unnamed>"
    langs = languages or DEFAULT_LANGUAGES
    print(f"📥 OCR request received: {file_name} ({langs})")

    data = _read_upload(file)
    tika_text = _run_tika(data)
    ocr_text, blocks = _run_ocr(data, langs)
    combined = _merge_texts(tika_text, ocr_text)

    print(f"✅ OCR completed for: {file_name} | Tika={len(tika_text)} chars, OCR={len(ocr_text)} chars")

    return OcrResponse(
        tika_text=tika_text,
        ocr_text=ocr_text,
        combined_text=combined,
        language_hint=langs,
        blocks=blocks,
    )
