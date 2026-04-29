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
- `POST /api/v1/records/weight`
- `POST /api/v1/records/blood-pressure`
- `GET /api/v1/records/blood-pressure`
- `POST /api/v1/records/flares`
- `POST /api/v1/records/hydration`
- `GET /api/v1/records/timeline`

说明：

- 上述五个记录写入接口都会返回 `recordId + createdAt + message`
- 时间字段为空时，后端会自动使用当前时间

`POST /api/v1/records/blood-pressure` 请求体字段：

- `systolicPressure`
- `diastolicPressure`
- `pulseRate`
- `unit`
- `measuredAt`
- `source`
- `note`

`GET /api/v1/records/blood-pressure` 返回字段：

- `recordId`
- `systolicPressure`
- `diastolicPressure`
- `pulseRate`
- `unit`
- `measuredAt`
- `source`
- `note`
- `riskLevel`

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

## 5. 设备接入与同步接口

### 设备绑定

- `POST /api/v1/devices`
- `GET /api/v1/devices`
- `DELETE /api/v1/devices/{deviceCode}`

创建设备请求体示例：

```json
{
  "deviceType": "URIC_ACID_METER",
  "vendorName": "Tongfeng",
  "deviceModel": "UA-1",
  "serialNumber": "UA-SN-001",
  "aliasName": "家用尿酸仪"
}
```

当前推荐设备类型：

- `URIC_ACID_METER`
- `SMART_WATER_CUP`

### 设备同步

- `POST /api/v1/devices/{deviceCode}/sync`
- `GET /api/v1/devices/{deviceCode}/sync-events`

批量同步请求体示例：

```json
{
  "items": [
    {
      "metricType": "URIC_ACID",
      "externalEventId": "evt-ua-1",
      "measuredAt": "2026-04-29T08:00:00Z",
      "value": 502,
      "unit": "μmol/L",
      "note": "device sync"
    },
    {
      "metricType": "HYDRATION",
      "externalEventId": "evt-hyd-1",
      "measuredAt": "2026-04-29T09:00:00Z",
      "waterIntakeMl": 900,
      "urineColorLevel": 4,
      "note": "smart cup sync"
    }
  ]
}
```

当前支持的同步指标：

- `URIC_ACID`
- `HYDRATION`
- `WEIGHT`
- `BLOOD_PRESSURE`

说明：

- `externalEventId` 在同一设备下会做去重
- 同步成功后会自动写入正式健康记录
- 同步完成后会刷新当日日汇总和提醒规则

血压同步字段补充：

- `systolicPressure`
- `diastolicPressure`
- `pulseRate`
- `unit`

## 6. 积分成长接口

### 成长总览

- `GET /api/v1/growth/overview`

返回数据字段：

- `userId`
- `level`
- `levelTitle`
- `totalPoints`
- `redeemablePoints`
- `currentLevelMinPoints`
- `nextLevelPoints`
- `currentStreakDays`
- `longestStreakDays`
- `todayPoints`
- `badgesCount`
- `highlights`

### 今日任务

- `GET /api/v1/growth/tasks`

单个任务字段：

- `taskCode`
- `title`
- `description`
- `rewardPoints`
- `completedCount`
- `targetCount`
- `completed`

### 周目标与成长挑战

- `GET /api/v1/growth/weekly-plan`

返回数据字段：

- `weekStartDate`
- `weekEndDate`
- `weeklyEarnedPoints`
- `targetPoints`
- `progressPercent`
- `challenges`

单个 `challenge` 字段：

- `challengeCode`
- `category`
- `title`
- `description`
- `rewardPoints`
- `completedCount`
- `targetCount`
- `priority`
- `completed`
- `hints`

说明：

- 当前挑战会混合返回 `WEEKLY / RISK / FAMILY` 三类任务
- `RISK` 类任务会根据当前提醒自动生成
- `FAMILY` 类任务会根据家属绑定状态自动变化

### 积分流水

- `GET /api/v1/growth/points?limit=20`

说明：

- `limit` 取值范围：`1-50`
- 当前会返回最近积分流水，默认按创建时间倒序

单条流水字段：

- `pointId`
- `actionType`
- `points`
- `summary`
- `awardedDate`
- `createdAt`

### 徽章列表

- `GET /api/v1/growth/badges`

单个徽章字段：

- `badgeKey`
- `badgeName`
- `badgeDescription`
- `awardedAt`

说明：

- 当前积分会自动挂接到尿酸、体重、发作、补水、设备接入、家属绑定、知识问答、用药计划、主动管理设置等关键行为
- 当前内置徽章覆盖首个健康动作、首条尿酸记录、设备接入、家属协同、补水坚持与连续活跃等场景

### 奖励中心

- `GET /api/v1/growth/rewards`
- `POST /api/v1/growth/rewards/{rewardKey}/claim`
- `GET /api/v1/growth/reward-claims`

奖励列表字段：

- `rewardKey`
- `rewardName`
- `rewardDescription`
- `rewardType`
- `pointsCost`
- `remainingClaims`
- `claimable`
- `claimHint`

领取结果字段：

- `claimCode`
- `rewardKey`
- `rewardName`
- `rewardType`
- `pointsCost`
- `remainingPoints`
- `status`
- `claimNote`
- `claimedAt`

说明：

- 奖励兑换消耗的是 `redeemablePoints`，不会回退用户历史总积分与等级
- 当前奖励采用按用户限次领取策略
- `reward-claims` 返回当前用户的奖励领取历史

## 7. 调度与规则说明

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

## 8. AI 子服务内部接口

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

## 9. 前端建议接入顺序

1. 登录拿 token。
2. 完成用户档案页。
3. 接饮食拍照识别、尿酸/体重/发作/饮水打卡。
4. 首页接 `overview + trends + reminders + daily-summaries`。
5. 再接化验单、问答、画像、用药。
6. 如进入 V3 页面，再接 `proactive-care/settings`、`proactive-care/brief` 和 `flares/reports/latest`。
7. 如进入家属协同页，再接 `family/invitations`、`family/members`、`family/alerts` 和 `family/patients/{patientUserId}/summary`。
8. 如进入设备接入页，再接 `devices`、`devices/{deviceCode}/sync` 和 `devices/{deviceCode}/sync-events`。
9. 如进入成长激励页，再接 `growth/overview`、`growth/tasks`、`growth/points` 和 `growth/badges`。
10. 如进入成长运营页，再接 `growth/weekly-plan`、`growth/rewards`、`growth/rewards/{rewardKey}/claim` 和 `growth/reward-claims`。
