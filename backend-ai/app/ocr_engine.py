from __future__ import annotations

import io
import re
from dataclasses import dataclass
from functools import lru_cache

try:
    import numpy as np
except Exception:  # pragma: no cover
    np = None

try:
    from PIL import Image, ImageFilter, ImageOps
except Exception:  # pragma: no cover
    Image = None
    ImageFilter = None
    ImageOps = None

try:
    from rapidocr_onnxruntime import RapidOCR
except Exception:  # pragma: no cover
    RapidOCR = None

try:
    import pytesseract
except Exception:  # pragma: no cover
    pytesseract = None


@dataclass
class OCRTextResult:
    text: str
    engine: str
    used_fallback: bool


def extract_text(file_bytes: bytes, filename: str) -> OCRTextResult:
    image_result = _extract_from_image(file_bytes)
    if image_result and image_result.text.strip():
        return image_result

    decoded = _decode_text(file_bytes)
    if decoded.strip():
        return OCRTextResult(text=decoded, engine="plain-text", used_fallback=False)

    return OCRTextResult(text="", engine="unavailable", used_fallback=True)


def extract_lab_values(text: str) -> dict[str, float]:
    normalized = _normalize_text(text)
    values: dict[str, float] = {}
    for code, patterns in LAB_PATTERNS.items():
        for pattern in patterns:
            match = re.search(pattern, normalized, flags=re.IGNORECASE)
            if match:
                try:
                    values[code] = float(match.group(1))
                    break
                except ValueError:
                    continue
    return values


def _extract_from_image(file_bytes: bytes) -> OCRTextResult | None:
    if Image is None:
        return None

    try:
        image = Image.open(io.BytesIO(file_bytes))
        image.load()
    except Exception:
        return None

    processed = _preprocess_image(image)

    rapid_result = _extract_with_rapidocr(processed)
    if rapid_result and rapid_result.text.strip():
        return rapid_result

    tesseract_result = _extract_with_tesseract(processed)
    if tesseract_result and tesseract_result.text.strip():
        return tesseract_result

    return OCRTextResult(text="", engine="image-no-result", used_fallback=True)


def _preprocess_image(image):
    processed = image.convert("L")
    if ImageOps is not None:
        processed = ImageOps.autocontrast(processed)
    if ImageFilter is not None:
        processed = processed.filter(ImageFilter.SHARPEN)
    return processed


@lru_cache(maxsize=1)
def _rapidocr_instance():
    if RapidOCR is None:
        return None
    try:
        return RapidOCR()
    except Exception:
        return None


def _extract_with_rapidocr(image) -> OCRTextResult | None:
    if np is None:
        return None
    engine = _rapidocr_instance()
    if engine is None:
        return None
    try:
        result, _ = engine(np.array(image))
    except Exception:
        return None

    if not result:
        return OCRTextResult(text="", engine="rapidocr", used_fallback=True)

    lines = [item[1] for item in result if len(item) >= 2 and item[1]]
    return OCRTextResult(text="\n".join(lines), engine="rapidocr", used_fallback=False)


def _extract_with_tesseract(image) -> OCRTextResult | None:
    if pytesseract is None:
        return None
    try:
        text = pytesseract.image_to_string(image, lang="chi_sim+eng")
    except Exception:
        return None
    return OCRTextResult(text=text, engine="pytesseract", used_fallback=not bool(text.strip()))


def _decode_text(file_bytes: bytes) -> str:
    for encoding in ("utf-8", "gb18030", "latin1"):
        try:
            text = file_bytes.decode(encoding)
        except Exception:
            continue
        if _looks_like_text(text):
            return text
    return ""


def _looks_like_text(text: str) -> bool:
    if not text:
        return False
    printable = sum(1 for ch in text if ch.isprintable() or ch in "\n\r\t")
    return printable / max(len(text), 1) > 0.8


def _normalize_text(text: str) -> str:
    normalized = text.replace("μ", "u").replace("µ", "u")
    normalized = normalized.replace("：", ":").replace("／", "/")
    normalized = normalized.replace("（", "(").replace("）", ")")
    normalized = re.sub(r"[ \t]+", " ", normalized)
    normalized = re.sub(r"\n+", "\n", normalized)
    return normalized


LAB_PATTERNS: dict[str, list[str]] = {
    "UA": [
        r"(?:尿酸|UA|Uric Acid)\s*[:：]?\s*([0-9]+(?:\.[0-9]+)?)\s*(?:u?mol/?L)?",
        r"([0-9]+(?:\.[0-9]+)?)\s*(?:u?mol/?L)\s*(?:尿酸|UA|Uric Acid)",
    ],
    "CR": [
        r"(?:肌酐|Cr|CREA|Creatinine)\s*[:：]?\s*([0-9]+(?:\.[0-9]+)?)\s*(?:u?mol/?L)?",
        r"([0-9]+(?:\.[0-9]+)?)\s*(?:u?mol/?L)\s*(?:肌酐|Cr|CREA|Creatinine)",
    ],
    "CRP": [
        r"(?:C反应蛋白|CRP)\s*[:：]?\s*([0-9]+(?:\.[0-9]+)?)\s*(?:mg/?L)?",
        r"([0-9]+(?:\.[0-9]+)?)\s*(?:mg/?L)\s*(?:C反应蛋白|CRP)",
    ],
    "ESR": [
        r"(?:血沉|ESR)\s*[:：]?\s*([0-9]+(?:\.[0-9]+)?)\s*(?:mm/?h)?",
        r"([0-9]+(?:\.[0-9]+)?)\s*(?:mm/?h)\s*(?:血沉|ESR)",
    ],
}
