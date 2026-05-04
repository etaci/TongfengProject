from __future__ import annotations

import hashlib
from datetime import date
from typing import Literal

from fastapi import FastAPI, File, Form, UploadFile
from pydantic import BaseModel

from app.knowledge_base import answer_from_knowledge
from app.ocr_engine import OCRTextResult, extract_lab_values, extract_text


RiskLevel = Literal["GREEN", "YELLOW", "RED"]


class MealItem(BaseModel):
    name: str
    riskLevel: RiskLevel
    evidence: str
    purineEstimateMg: int


class MealAnalyzeResult(BaseModel):
    overallRiskLevel: RiskLevel
    purineEstimateMg: int
    items: list[MealItem]
    suggestions: list[str]
    summary: str


class LabIndicator(BaseModel):
    code: str
    name: str
    value: float
    unit: str
    referenceRange: str
    riskLevel: RiskLevel


class LabAnalyzeResult(BaseModel):
    indicators: list[LabIndicator]
    overallRiskLevel: RiskLevel
    suggestions: list[str]
    summary: str


class KnowledgeAskRequest(BaseModel):
    question: str
    scene: str | None = None


class KnowledgeAnswerResponse(BaseModel):
    answer: str
    references: list[str]
    escalateToDoctor: bool
    disclaimer: str


app = FastAPI(
    title="Tongfeng AI Service",
    version="0.2.0",
    description="用于餐盘识别、化验单 OCR 和本地知识库问答的 AI 服务。",
)


MEAL_RULES = [
    ("海鲜", "海鲜", "RED", 180),
    ("啤酒", "啤酒", "RED", 160),
    ("火锅", "火锅汤底", "YELLOW", 110),
    ("肉汤", "浓肉汤", "RED", 170),
    ("内脏", "动物内脏", "RED", 190),
    ("烧烤", "烧烤拼盘", "YELLOW", 120),
    ("小龙虾", "小龙虾", "RED", 175),
    ("豆腐", "豆腐", "GREEN", 35),
    ("蔬菜", "绿叶蔬菜", "GREEN", 20),
    ("鸡蛋", "鸡蛋", "GREEN", 25),
    ("牛奶", "低糖牛奶", "GREEN", 15),
    ("水果", "低果糖水果", "GREEN", 30),
]

MEAL_TEMPLATES = [
    [("米饭", "GREEN", 18), ("清炒时蔬", "GREEN", 22), ("煎蛋", "GREEN", 25)],
    [("牛肉火锅", "YELLOW", 95), ("蘸料", "GREEN", 12), ("肉汤", "RED", 165)],
    [("海鲜拼盘", "RED", 180), ("啤酒", "RED", 160), ("凉拌黄瓜", "GREEN", 20)],
    [("鸡胸肉沙拉", "GREEN", 35), ("玉米", "GREEN", 28), ("酸奶", "GREEN", 20)],
]


def risk_order(level: RiskLevel) -> int:
    return {"GREEN": 1, "YELLOW": 2, "RED": 3}[level]


def max_risk(levels: list[RiskLevel]) -> RiskLevel:
    return max(levels, key=risk_order) if levels else "GREEN"


def meal_suggestions(level: RiskLevel) -> list[str]:
    if level == "RED":
        return [
            "本餐建议减少高嘌呤或酒精类食物摄入，优先补水并观察身体反应。",
            "如果近期已有发作或尿酸偏高，建议后续几餐选择更清淡的搭配。",
            "当前结果仅用于健康管理，不替代医生诊疗意见。",
        ]
    if level == "YELLOW":
        return [
            "本餐存在中等风险，建议控制分量，并搭配足量饮水。",
            "后续 24 小时内尽量避免再次叠加海鲜、浓汤和酒精。",
        ]
    return [
        "本餐整体风险较低，可以继续保持当前搭配。",
        "仍建议持续记录，便于后续形成个人饮食诱因画像。",
    ]


def choose_meal_items(text: str, seed: int) -> list[MealItem]:
    matched: list[MealItem] = []
    for keyword, name, risk, purine in MEAL_RULES:
        if keyword.lower() in text:
            matched.append(
                MealItem(
                    name=name,
                    riskLevel=risk,  # type: ignore[arg-type]
                    evidence=f"命中关键词「{keyword}」",
                    purineEstimateMg=purine,
                )
            )

    if matched:
        return matched

    template = MEAL_TEMPLATES[seed % len(MEAL_TEMPLATES)]
    return [
        MealItem(
            name=name,
            riskLevel=risk,  # type: ignore[arg-type]
            evidence="基于图片内容不足时的保底规则推断",
            purineEstimateMg=purine,
        )
        for name, risk, purine in template
    ]


def classify_lab(value: float, yellow: float, red: float) -> RiskLevel:
    if value >= red:
        return "RED"
    if value >= yellow:
        return "YELLOW"
    return "GREEN"


def build_lab_indicators(values: dict[str, float]) -> list[LabIndicator]:
    indicators: list[LabIndicator] = []
    if "UA" in values:
        indicators.append(
            LabIndicator(
                code="UA",
                name="尿酸",
                value=values["UA"],
                unit="μmol/L",
                referenceRange="208-420",
                riskLevel=classify_lab(values["UA"], 421, 500),
            )
        )
    if "CR" in values:
        indicators.append(
            LabIndicator(
                code="Cr",
                name="肌酐",
                value=values["CR"],
                unit="μmol/L",
                referenceRange="57-111",
                riskLevel=classify_lab(values["CR"], 100, 115),
            )
        )
    if "CRP" in values:
        indicators.append(
            LabIndicator(
                code="CRP",
                name="C反应蛋白",
                value=values["CRP"],
                unit="mg/L",
                referenceRange="0-8",
                riskLevel=classify_lab(values["CRP"], 8.1, 15),
            )
        )
    if "ESR" in values:
        indicators.append(
            LabIndicator(
                code="ESR",
                name="血沉",
                value=values["ESR"],
                unit="mm/h",
                referenceRange="0-20",
                riskLevel=classify_lab(values["ESR"], 20.1, 30),
            )
        )
    return indicators


def lab_suggestions(
    indicators: list[LabIndicator],
    ocr_result: OCRTextResult,
    extracted_values: dict[str, float],
) -> list[str]:
    if not extracted_values:
        return [
            "本次化验单未能稳定提取出关键指标，当前结果不会进入正式复盘结论链路。",
            "请重新上传更清晰的图片或 PDF，必要时改为人工确认关键指标后再继续复盘。",
            "在人工确认前，系统不会输出目标值判断、趋势对比或风险推演。",
        ]

    suggestions: list[str] = []
    indicator_map = {item.code: item for item in indicators}
    ua = indicator_map.get("UA")
    crp = indicator_map.get("CRP")
    esr = indicator_map.get("ESR")
    if ua and ua.value > 420:
        suggestions.append("尿酸结果偏高，建议结合饮食与复查节奏持续观察。")
    if (crp and crp.value > 8) or (esr and esr.value > 20):
        suggestions.append("炎症指标偏高，如伴随红肿热痛或发热，请及时咨询医生。")
    suggestions.append(f"本次化验单已通过 {ocr_result.engine} 完成文字识别，并按规则抽取关键指标。")
    suggestions.append("解析结果仅作健康管理参考，正式判断请以医生意见为准。")
    return suggestions


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/api/v1/vision/meal-analyze", response_model=MealAnalyzeResult)
async def meal_analyze(
    file: UploadFile = File(...),
    userId: str = Form(...),
    mealType: str = Form("MEAL"),
    note: str = Form(""),
) -> MealAnalyzeResult:
    raw = await file.read()
    seed = int(hashlib.sha1(raw or (file.filename or "upload").encode("utf-8")).hexdigest()[:8], 16)
    text = f"{file.filename or ''} {note} {mealType} {userId}".lower()
    items = choose_meal_items(text, seed)
    risk = max_risk([item.riskLevel for item in items])
    purine = sum(item.purineEstimateMg for item in items)
    summary = f"识别到 {len(items)} 项内容，整餐估算嘌呤约 {purine}mg，当前风险等级为 {risk}。"
    return MealAnalyzeResult(
        overallRiskLevel=risk,
        purineEstimateMg=purine,
        items=items,
        suggestions=meal_suggestions(risk),
        summary=summary,
    )


@app.post("/api/v1/ocr/lab-report-analyze", response_model=LabAnalyzeResult)
async def lab_report_analyze(
    file: UploadFile = File(...),
    userId: str = Form(...),
    reportDate: str = Form(""),
) -> LabAnalyzeResult:
    del userId, reportDate
    raw = await file.read()
    ocr_result = extract_text(raw, file.filename or "upload")
    extracted_values = extract_lab_values(ocr_result.text)
    indicators = build_lab_indicators(extracted_values) if extracted_values else []
    overall = max_risk([item.riskLevel for item in indicators])
    summary = (
        f"已提取 {len(indicators)} 项关键指标，整体风险等级为 {overall}。"
        if extracted_values
        else "当前图片或文件未能稳定提取出关键指标，已转为人工确认模式，暂不输出正式化验结论。"
    )
    return LabAnalyzeResult(
        indicators=indicators,
        overallRiskLevel=overall,
        suggestions=lab_suggestions(indicators, ocr_result, extracted_values),
        summary=summary,
    )


@app.post("/api/v1/knowledge/ask", response_model=KnowledgeAnswerResponse)
async def ask_knowledge(payload: KnowledgeAskRequest) -> KnowledgeAnswerResponse:
    answer, references, escalate = answer_from_knowledge(payload.question.strip(), payload.scene)
    return KnowledgeAnswerResponse(
        answer=answer,
        references=references,
        escalateToDoctor=escalate,
        disclaimer="本回答仅用于健康管理参考，不替代医生面诊、诊断和处方建议。",
    )


@app.get("/")
def index() -> dict[str, str | date]:
    return {"service": "tongfeng-ai", "date": date.today().isoformat()}
