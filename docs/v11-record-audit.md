# V11 记录更正与审计

本版本为统一记录中心补齐两个核心能力：

1. 记录更正
2. 审计留痕与查询

## 新增接口

### 1. 更正记录

- 方法：`PUT /api/v1/records/detail`
- Query：
  - `type`：记录类型，支持 `URIC_ACID`、`WEIGHT`、`BLOOD_PRESSURE`、`HYDRATION`、`FLARE`
  - `recordId`：记录 ID

请求体为通用结构，按不同记录类型传对应字段：

```json
{
  "decimalValue": 70.6,
  "source": "manual-adjustment",
  "note": "after calibration",
  "changeReason": "sync source normalized"
}
```

说明：

- `changeReason` 必填
- 体重更正使用 `decimalValue`
- 尿酸更正使用 `value`
- 血压更正支持 `systolicPressure`、`diastolicPressure`、`pulseRate`
- 补水更正支持 `waterIntakeMl`、`urineColorLevel`
- 发作更正支持 `joint`、`painLevel`、`durationNote`

### 2. 查询记录审计

- 方法：`GET /api/v1/records/audits`
- Query：
  - `type`
  - `recordId`
  - `limit`，默认 `20`，最大 `50`

返回字段说明：

- `action`：`UPDATE` 或 `DELETE`
- `changeReason`：本次操作原因
- `summary`：摘要文案
- `operatedAt`：操作时间
- `fields`：变更字段明细

字段明细结构：

```json
{
  "key": "value",
  "label": "体重",
  "beforeValue": "71.8",
  "afterValue": "70.6"
}
```

## 当前闭环

当前统一记录中心已支持：

1. 聚合分页查询
2. 详情查询
3. 记录删除
4. 记录更正
5. 审计查询
6. 删除审计保留
