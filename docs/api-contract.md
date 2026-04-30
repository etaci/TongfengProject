# 后端接口契约

## 1. 基础约定

- Java 主服务默认地址：`http://localhost:8080`
- AI 子服务默认地址：`http://localhost:8001`
- 前端默认只对接 Java 主服务，AI 子服务由 Java 内部调用
- 除 `POST /api/v1/auth/mock-login` 外，其余接口都需要：

```http
Authorization: Bearer {token}
```

- 统一响应结构：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {},
  "timestamp": "2026-04-26T18:00:00Z"
}
```

## 2. Java 主服务接口

### 登录

- `POST /api/v1/auth/mock-login`

请求体：

```json
{
  "nickname": "测试用户"
}
```

返回数据字段：

- `userId`
- `nickname`
- `token`
- `tokenType`
- `expiresAt`

### 应用能力

- `GET /api/v1/app/capabilities`

返回数据字段：

- `features`

单个 `feature` 字段：

- `featureKey`
- `displayName`
- `enabled`
- `note`

说明：

- 当前仅保留 `family-care` 能力状态，用于前端决定是否展示家属协同入口

### 用户档案

- `GET /api/v1/profile`
- `PUT /api/v1/profile`

`PUT /api/v1/profile` 请求体字段：

- `name`
- `gender`
- `birthday`
- `heightCm`
- `targetUricAcid`
- `allergies`
- `comorbidities`
- `emergencyContact`

### 文件

- `POST /api/v1/files/upload`
  - `multipart/form-data`
  - 字段：`file`
- `GET /api/v1/files/{fileId}`

### 餐盘识别与饮食记录

- `POST /api/v1/meals/analyze`
  - `multipart/form-data`
  - 字段：`file`
  - 字段：`mealType`
  - 字段：`takenAt`，可选，ISO-8601
  - 字段：`note`，可选
- `GET /api/v1/meals`

`POST /api/v1/meals/analyze` 返回数据字段：

- `recordId`
- `imageUrl`
- `mealType`
- `takenAt`
- `riskLevel`
- `purineEstimateMg`
- `items`
- `suggestions`
- `summary`

### 指标记录

- `POST /api/v1/records/uric-acid`
- `GET /api/v1/records/uric-acid`
- `POST /api/v1/records/weight`
- `GET /api/v1/records/weight`
- `POST /api/v1/records/flares`
- `GET /api/v1/records/flares`
- `POST /api/v1/records/hydration`
- `GET /api/v1/records/hydration`
- `GET /api/v1/records/timeline`
- `GET /api/v1/records/center`
- `GET /api/v1/records/detail`
- `PUT /api/v1/records/detail`
- `DELETE /api/v1/records/detail`
- `GET /api/v1/records/audits`
- `POST /api/v1/records/restore`

说明：

- 上述四个记录写入接口都会返回 `recordId + createdAt + message`
- 时间字段为空时，后端会自动使用当前时间
- 记录中心当前只支持 `URIC_ACID / WEIGHT / HYDRATION / FLARE`

`GET /api/v1/records/center` 查询参数：

- `types`，可选，多值筛选
- `cursor`，可选，基于发生时间的游标
- `limit`，可选，默认 `20`，范围 `1-100`

`GET /api/v1/records/center` 返回数据字段：

- `types`
- `totalCount`
- `returnedCount`
- `limit`
- `items`
- `nextCursor`
- `hasMore`

单个 `item` 字段：

- `recordId`
- `type`
- `title`
- `summary`
- `occurredAt`
- `riskLevel`
- `source`
- `tags`

`GET /api/v1/records/detail` 查询参数：

- `type`
- `recordId`

`GET /api/v1/records/detail` 返回数据字段：

- `recordId`
- `type`
- `title`
- `summary`
- `occurredAt`
- `riskLevel`
- `source`
- `note`
- `tags`
- `fields`

单个 `field` 字段：

- `key`
- `label`
- `value`

`PUT /api/v1/records/detail` 查询参数：

- `type`
- `recordId`

`PUT /api/v1/records/detail` 请求体字段：

- `value`
- `decimalValue`
- `unit`
- `measuredAt`
- `source`
- `waterIntakeMl`
- `urineColorLevel`
- `checkedAt`
- `joint`
- `painLevel`
- `startedAt`
- `durationNote`
- `note`
- `changeReason`

说明：

- 不同记录类型只会消费对应字段，例如 `URIC_ACID` 使用 `value + unit + measuredAt`
- `changeReason` 必填，用于审计留痕

`DELETE /api/v1/records/detail` 查询参数：

- `type`
- `recordId`

返回数据字段：

- `recordId`
- `type`
- `status`
- `deletedAt`
- `message`

`GET /api/v1/records/audits` 查询参数：

- `type`
- `recordId`
- `limit`，可选，默认 `20`，范围 `1-50`

返回数据字段：

- `auditId`
- `recordId`
- `type`
- `action`
- `changeReason`
- `summary`
- `operatedAt`
- `fields`

单个审计 `field` 字段：

- `key`
- `label`
- `beforeValue`
- `afterValue`

`POST /api/v1/records/restore` 查询参数：

- `type`
- `recordId`
- `auditId`

请求体字段：

- `changeReason`

返回数据字段：

- `recordId`
- `type`
- `restoredFromAuditId`
- `status`
- `restoredAt`
- `message`
- `detail`

### 首页主动管理

- `GET /api/v1/dashboard/overview`
- `GET /api/v1/dashboard/trends?days=7`
- `GET /api/v1/dashboard/daily-summaries?days=7`
- `GET /api/v1/reminders`

说明：

- `days` 取值范围：`1-90`
- 写入饮食、尿酸、体重、发作、饮水、化验单后，会即时刷新提醒和当日日汇总
- `overview` 会优先读取当日日汇总结果

`GET /api/v1/dashboard/daily-summaries` 返回示例：

```json
[
  {
    "summaryDate": "2026-04-26",
    "latestUricAcidValue": 468,
    "latestUricAcidUnit": "μmol/L",
    "latestWeightValue": 72.5,
    "totalWaterIntakeMl": 1600,
    "highRiskMealCount": 1,
    "flareCount": 0,
    "overallRiskLevel": "YELLOW",
    "summaryText": "2026-04-26 汇总：尿酸 468 μmol/L，高风险饮食 1 次，饮水 1600ml，发作 0 次，整体风险 YELLOW。"
  }
]
```

`GET /api/v1/reminders` 返回数据字段：

- `reminderId`
- `type`
- `title`
- `content`
- `riskLevel`
- `triggerAt`

### 化验单 OCR

- `POST /api/v1/lab-reports/analyze`
  - `multipart/form-data`
  - 字段：`file`
  - 字段：`reportDate`，可选，格式 `yyyy-MM-dd`
- `GET /api/v1/lab-reports`

`POST /api/v1/lab-reports/analyze` 返回数据字段：

- `reportId`
- `reportDate`
- `indicators`
- `overallRiskLevel`
- `suggestions`
- `summary`

单个 `indicator` 字段：

- `code`
- `name`
- `value`
- `unit`
- `referenceRange`
- `riskLevel`

### 知识问答

- `POST /api/v1/knowledge/ask`

请求体：

```json
{
  "question": "尿酸高的时候能不能喝啤酒？",
  "scene": "聚餐前咨询"
}
```

返回数据示例：

```json
{
  "answer": "根据当前知识库检索结果，优先可参考以下信息：\n饮酒风险：...",
  "references": [
    "gout_diet_guide.md#饮酒风险"
  ],
  "escalateToDoctor": false,
  "disclaimer": "本回答仅用于健康管理参考，不替代医生面诊、诊断和处方建议。"
}
```

### 用户画像

- `GET /api/v1/persona/summary`

返回数据字段：

- `tags`
- `triggers`
- `narrative`

### 用药管理

- `GET /api/v1/medications`
- `PUT /api/v1/medications`

`PUT /api/v1/medications` 请求体示例：

```json
{
  "currentMedications": [
    {
      "name": "别嘌醇",
      "dosage": "100mg",
      "frequency": "每日一次",
      "remark": "晚饭后"
    }
  ],
  "followUpNote": "两周后复查尿酸"
}
```

## 3. V3 半主动管理接口

### 主动管理设置

- `GET /api/v1/proactive-care/settings`
- `PUT /api/v1/proactive-care/settings`

`PUT /api/v1/proactive-care/settings` 请求体示例：

```json
{
  "monitoringCity": "Shanghai",
  "countryCode": "CN",
  "weatherAlertsEnabled": true
}
```

返回数据字段：

- `monitoringCity`
- `countryCode`
- `latitude`
- `longitude`
- `timezoneId`
- `weatherAlertsEnabled`
- `updatedAt`
- `lastWeatherSyncAt`

### 主动风险简报

- `GET /api/v1/proactive-care/brief`

返回数据字段：

- `overallRiskLevel`
- `riskScore`
- `summary`
- `weather`
- `factors`
- `suggestions`
- `generatedAt`

其中：

- `weather` 为当日天气快照，包含温度、体感温度、湿度、降水概率、天气风险等级
- `factors` 为多因素风险列表，当前覆盖天气、尿酸、补水、近期饮食、近期发作、化验单信号

### 发作复盘报告

- `GET /api/v1/flares/reports/latest?lookbackDays=7`

说明：

- `lookbackDays` 取值范围：`1-30`
- 当前返回最近一次发作的复盘结果

返回数据字段：

- `reportId`
- `flareRecordId`
- `flareStartedAt`
- `joint`
- `painLevel`
- `overallRiskLevel`
- `suspectedTriggers`
- `relatedEvents`
- `actionSuggestions`
- `summary`
- `generatedAt`

## 4. V4 家属协同与亲友监督接口

### 家属邀请

- `POST /api/v1/family/invitations`
- `GET /api/v1/family/invitations`
- `POST /api/v1/family/invitations/{inviteCode}/accept`
- `POST /api/v1/family/invitations/{inviteCode}/cancel`

创建邀请请求体示例：

```json
{
  "relationType": "SPOUSE",
  "inviteMessage": "一起关注近期风险",
  "expiresInDays": 7
}
```

邀请返回数据字段：

- `inviteCode`
- `patientUserId`
- `patientNickname`
- `creatorUserId`
- `relationType`
- `inviteMessage`
- `status`
- `acceptedByUserId`
- `acceptedByNickname`
- `expiresAt`
- `createdAt`

### 家属绑定列表

- `GET /api/v1/family/members`
- `DELETE /api/v1/family/members/{bindingCode}`

`GET /api/v1/family/members` 返回：

- `asPatient`
  - 当前用户作为患者时，谁已被授权为家属
- `asCaregiver`
  - 当前用户作为家属时，正在关注哪些患者

### 家属告警列表

- `GET /api/v1/family/alerts`

说明：

- 当前返回按风险等级和时间倒序排列的监督告警
- 告警来源包括患者提醒列表和主动管理风险简报

### 家属查看患者摘要

- `GET /api/v1/family/patients/{patientUserId}/summary`

返回数据字段：

- `patientUserId`
- `patientNickname`
- `relationType`
- `overallRiskLevel`
- `latestRiskSummary`
- `todayFocus`
- `reminders`
- `weather`
- `lastFlareAt`
- `lastUricAcidValue`
- `lastUricAcidUnit`
- `nextActions`
- `generatedAt`

说明：

- 只有已建立有效授权绑定的家属才能查看指定患者摘要
- 当前摘要默认聚合患者的提醒、主动管理结果、最近一次尿酸和最近一次发作信息

## 5. 调度与规则说明

- 默认开启主动管理调度：`app.scheduler-enabled=true`
- 提醒刷新 cron：`app.reminder-refresh-cron=0 0/30 * * * *`
- 日汇总刷新 cron：`app.summary-refresh-cron=0 5 0 * * * *`
- 天气实时拉取开关：`app.weather-live-enabled=true`
- 相关规则当前覆盖：
  - 尿酸缺失、偏高、30 天未复查、趋势上升
  - 饮水量不足、尿液颜色偏深
  - 近期高风险饮食连续出现
  - 近期高疼痛等级发作
  - 化验单高风险结果跟进
  - 已配置监测城市时的天气联合风险提醒

说明：

- 天气能力默认按 Open-Meteo 官方接口拉取
- 若天气接口不可用，后端会回退到保底天气模型，不影响主动管理接口返回

## 6. AI 子服务内部接口

- `POST /api/v1/vision/meal-analyze`
  - Java 内部调用的餐盘识别接口
- `POST /api/v1/ocr/lab-report-analyze`
  - 先尝试真实 OCR 抽取，抽不到关键指标时再走保底估算
- `POST /api/v1/knowledge/ask`
  - 基于本地 Markdown 知识库检索返回答案与引用来源

说明：

- 前端默认不需要直连以上 AI 接口
- Java 主服务已经把这些能力封装成统一业务接口
- 若知识问答 AI 子服务暂不可用，Java 主服务会自动返回本地兜底建议，避免接口直接报错

## 7. 前端建议接入顺序

1. 登录拿 token。
2. 登录后先拉 `app/capabilities`，确定家属协同入口是否开放。
3. 完成用户档案页。
4. 接饮食拍照识别、尿酸/体重/发作/饮水打卡。
5. 如需记录治理能力，再接 `records/center`、`records/detail`、`records/audits` 和 `records/restore`。
6. 首页接 `overview + trends + reminders + daily-summaries`。
7. 再接化验单、问答、画像、用药。
8. 如进入 V3 页面，再接 `proactive-care/settings`、`proactive-care/brief` 和 `flares/reports/latest`。
9. 如进入家属协同页，再接 `family/invitations`、`family/members`、`family/alerts` 和 `family/patients/{patientUserId}/summary`。
