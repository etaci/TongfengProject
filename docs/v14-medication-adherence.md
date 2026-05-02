# V14 用药依从闭环

## 本轮目标

V14 聚焦“高频、可留存、可复盘”的用药链路，把上一轮的今日行动页进一步落到患者每天都会操作的服药任务上。

本轮新增能力：

- 用药依从摘要接口：返回今日计划剂次、已服用、漏服、跳过、连续完成天数、待确认项和最近打卡。
- 用药打卡接口：支持按药物名 + 时段提交 `TAKEN / MISSED / SKIPPED`，同一天同药同一时段会覆盖更新。
- 今日行动联动：今日行动页中的“用药”动作会根据待确认项和打卡状态动态变化。
- 前端助理页闭环：支持查看依从摘要、提交打卡、查看最近记录。

## 后端接口

### 1. 获取用药依从摘要

- `GET /api/v1/medications/adherence?days=7`

返回重点：

- `plannedDoseCount`
- `takenDoseCount`
- `missedDoseCount`
- `skippedDoseCount`
- `adherenceRate`
- `currentStreakDays`
- `overdueItems`
- `nextActions`
- `recentCheckins`

### 2. 提交用药打卡

- `POST /api/v1/medications/check-ins`

请求体：

```json
{
  "medicationName": "allopurinol",
  "scheduledPeriod": "MORNING",
  "status": "TAKEN",
  "note": "after breakfast"
}
```

状态说明：

- `TAKEN`：已服用
- `MISSED`：漏服
- `SKIPPED`：跳过

时段说明：

- `MORNING`
- `NOON`
- `EVENING`
- `BEDTIME`

## 规则说明

- `once-daily / 每日一次` 默认映射到 `MORNING`
- `twice-daily / 每日两次` 映射到 `MORNING + EVENING`
- `three-times-daily / 每日三次` 映射到 `MORNING + NOON + EVENING`
- `bedtime / 睡前` 映射到 `BEDTIME`

## 医疗安全边界

- 漏服后仅给出“不要自行加倍补服，必要时联系医生或药师”的安全提示。
- 当前版本不做个体化补服算法，不替代医生处方调整。
- 如果用户提交的药名或时段不在当前计划中，后端直接拒绝，避免脏数据进入依从分析。
