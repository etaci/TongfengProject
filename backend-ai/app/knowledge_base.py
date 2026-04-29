from __future__ import annotations

import re
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

try:
    import jieba
except Exception:  # pragma: no cover
    jieba = None


BASE_DIR = Path(__file__).resolve().parent.parent
KNOWLEDGE_DIR = BASE_DIR / "knowledge"


@dataclass
class KnowledgeChunk:
    source: str
    heading: str
    content: str


@dataclass
class RetrievedChunk:
    chunk: KnowledgeChunk
    score: float


def retrieve(question: str, scene: str | None = None, top_k: int = 3) -> list[RetrievedChunk]:
    question_tokens = _tokenize(question + " " + (scene or ""))
    results: list[RetrievedChunk] = []
    for chunk in _load_chunks():
        chunk_tokens = _tokenize(chunk.heading + " " + chunk.content)
        overlap = len(question_tokens & chunk_tokens)
        phrase_bonus = sum(2 for token in question_tokens if len(token) > 1 and token in chunk.content)
        score = overlap + phrase_bonus
        if score > 0:
            results.append(RetrievedChunk(chunk=chunk, score=float(score)))
    results.sort(key=lambda item: item.score, reverse=True)
    return results[:top_k]


def answer_from_knowledge(question: str, scene: str | None = None) -> tuple[str, list[str], bool]:
    hits = retrieve(question, scene)
    if not hits:
        answer = (
            "当前本地知识库没有检索到足够相关的内容。"
            "建议先补充更具体的问题描述，或结合近期尿酸、化验单和发作情况一起判断。"
        )
        return answer, [], _should_escalate(question)

    lines = []
    for item in hits:
        snippet = _trim_text(item.chunk.content, 120)
        lines.append(f"{item.chunk.heading}：{snippet}")

    answer = "根据当前知识库检索结果，优先可参考以下信息：\n" + "\n".join(lines)
    return answer, [f"{item.chunk.source}#{item.chunk.heading}" for item in hits], _should_escalate(question)


def _should_escalate(question: str) -> bool:
    high_risk_keywords = [
        "急性",
        "剧痛",
        "发热",
        "红肿",
        "肾",
        "药物过敏",
        "呼吸困难",
        "胸痛",
        "持续恶化",
    ]
    return any(keyword in question for keyword in high_risk_keywords)


@lru_cache(maxsize=1)
def _load_chunks() -> list[KnowledgeChunk]:
    chunks: list[KnowledgeChunk] = []
    for path in sorted(KNOWLEDGE_DIR.glob("*.md")):
        text = path.read_text(encoding="utf-8")
        heading = path.stem
        sections = re.split(r"^##\s+", text, flags=re.MULTILINE)
        if len(sections) == 1:
            body = _normalize_whitespace(text)
            if body:
                chunks.append(KnowledgeChunk(source=path.name, heading=heading, content=body))
            continue
        for raw_section in sections[1:]:
            lines = raw_section.strip().splitlines()
            if not lines:
                continue
            section_heading = lines[0].strip()
            content = _normalize_whitespace("\n".join(lines[1:]))
            if content:
                chunks.append(KnowledgeChunk(source=path.name, heading=section_heading, content=content))
    return chunks


def _normalize_whitespace(text: str) -> str:
    text = re.sub(r"\s+", " ", text).strip()
    return text


def _trim_text(text: str, limit: int) -> str:
    return text if len(text) <= limit else text[: limit - 1] + "…"


def _tokenize(text: str) -> set[str]:
    tokens = set(re.findall(r"[A-Za-z0-9]+|[\u4e00-\u9fff]", text.lower()))
    if jieba is not None:
        for token in jieba.lcut(text):
            token = token.strip().lower()
            if len(token) >= 2:
                tokens.add(token)
    return {token for token in tokens if token}
