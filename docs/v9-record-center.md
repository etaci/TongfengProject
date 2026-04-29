# V9 统一健康记录中心

本版本目标：

- 补齐各类健康记录的查询接口
- 建立统一健康记录中心接口
- 让手工录入与设备同步记录进入同一套查询视图
- 给体重记录补齐来源字段，统一展示“手工/设备来源”

## 1. 新增接口

### 1.1 尿酸记录列表

`GET /api/v1/records/uric-acid`

返回字段：

- `recordId`
- `value`
- `unit`
- `measuredAt`
- `source`
- `note`
- `riskLevel`

### 1.2 体重记录列表

`GET /api/v1/records/weight`

返回字段：

- `recordId`
- `value`
- `unit`
- `measuredAt`
- `source`
- `note`
- `riskLevel`

说明：

- 本版本已为体重记录补齐 `source`
- 设备同步产生的体重记录会自动写入设备来源

### 1.3 补水记录列表

`GET /api/v1/records/hydration`

返回字段：

- `recordId`
- `waterIntakeMl`
- `urineColorLevel`
- `checkedAt`
- `note`
- `riskLevel`

### 1.4 发作记录列表

`GET /api/v1/records/flares`

返回字段：

- `recordId`
- `joint`
- `painLevel`
- `startedAt`
- `durationNote`
- `note`
- `riskLevel`

### 1.5 统一健康记录中心

`GET /api/v1/records/center`

支持参数：

- `types`：可重复传参
- `limit`：默认 `20`，范围 `1-100`

示例：

`GET /api/v1/records/center?types=WEIGHT&types=BLOOD_PRESSURE&types=URIC_ACID&limit=5`

支持的 `types`：

- `URIC_ACID`
- `WEIGHT`
- `BLOOD_PRESSURE`
- `HYDRATION`
- `FLARE`

返回字段：

- `types`
- `totalCount`
- `returnedCount`
- `limit`
- `items`

单条 `items` 字段：

- `recordId`
- `type`
- `title`
- `summary`
- `occurredAt`
- `riskLevel`
- `source`
- `tags`

## 2. 排序规则

- 所有记录列表均按时间倒序返回
- 统一记录中心也按 `occurredAt` 倒序返回

这意味着：

- 手工录入时间晚于设备同步时，会排在设备同步记录之前
- 前端不应假设“风险更高的一条”一定排在第一位

## 3. 风险规则

- 尿酸：
  - `>= 500 -> RED`
  - `> 420 -> YELLOW`
- 血压：
  - `>= 160/100 -> RED`
  - `>= 140/90 -> YELLOW`
- 补水：
  - `urineColorLevel >= 4 -> YELLOW`
- 发作：
  - `painLevel >= 8 -> RED`
  - 其余记录默认 `YELLOW`
- 体重：
  - 当前统一返回 `GREEN`

## 4. 异常场景

当 `types` 传入未支持的记录类型时：

- `code = RECORD_TYPE_UNSUPPORTED`

## 5. 前端接入建议

推荐页面分层：

1. 记录首页优先请求 `GET /api/v1/records/center`
2. 点击筛选标签后带 `types` 参数重新请求
3. 点击“查看更多”可调单项接口：
   - `records/uric-acid`
   - `records/weight`
   - `records/blood-pressure`
   - `records/hydration`
   - `records/flares`

## 6. 本轮验证

后端验证命令：

```powershell
cd H:\ProjectTongfeng\backend-java
.\mvnw.cmd test -q
```

验证覆盖：

- 尿酸记录列表
- 体重记录列表
- 补水记录列表
- 发作记录列表
- 统一健康记录中心筛选
- 设备来源写入体重记录
