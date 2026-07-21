# 后端接口契约

本文档面向前端联调，当前只覆盖对外主链路能力：

- 化验单复盘
- 日常记录闭环
- 家属轻协同
- 账号安全与访问审计

以下能力不在当前前端交付范围：

- 设备接入：`legacy / disabled / internal only`
- 成长体系：`legacy / disabled / internal only`

## 1. 基础约定

- Java 主服务默认地址：`http://localhost:8080`
- AI 子服务默认地址：`http://localhost:8001`
- 前端只对接 Java 主服务
- 除注册、登录、验证码申请、密码重置、开发用 mock-login 外，其余接口均需要：

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
  "timestamp": "2026-06-20T10:00:00Z"
}
```

## 2. 主链路范围说明

### 2.1 当前版本对外承诺

- 账号注册、登录、会话、安全中心
- 用户档案
- 文件上传
- 饮食识别与日常记录
- 记录治理、总览、趋势、提醒、今日行动
- 化验单上传、待确认、正式复盘、医生摘要
- 家属邀请、绑定、摘要、代办、审计
- 用药管理与服药打卡

### 2.2 当前版本不承诺

- 前端设备接入流程
- 前端成长体系流程
- 医院系统直连
- 医疗来源签名
- 医疗机构自动验真

## 3. 正式联调入口

默认联调顺序：

1. `POST /api/v1/auth/register`
2. `POST /api/v1/auth/login`
3. `GET /api/v1/auth/session`
4. `GET /api/v1/app/capabilities`
5. `GET /api/v1/profile`

`POST /api/v1/auth/mock-login` 仅用于开发环境快速起链，不应作为默认产品入口。

## 4. 能力开关

### `GET /api/v1/app/capabilities`

用途：

- 前端启动后判断当前环境开放了哪些模块

前置条件：

- 已登录

是否进入正式结论链路：

- 否

失败后的回退策略：

- 如果接口失败，前端应默认仅展示基础记录与档案入口，不应猜测设备或成长能力开放

返回字段：

- `features`

单个 `feature` 字段：

- `featureKey`
- `displayName`
- `enabled`
- `note`
- `capabilityVersion`
- `generatedAt`

说明：

- `capabilityVersion` 用于前端识别能力契约版本
- `generatedAt` 在同一 Java 服务实例生命周期内保持稳定
- 能力关闭必须通过 `enabled=false` 表达，不能通过字段缺失表达

当前对外约定：

- `daily-records`: `enabled=true`
- `lab-report-review`: `enabled=true`
- `family-care`: 由配置决定
- `device-integration`: `enabled=false`
- `growth-system`: `enabled=false`

## 5. 认证与安全

### `POST /api/v1/auth/register`

用途：

- 注册正式账号并创建登录态

前置条件：

- 无

是否进入正式结论链路：

- 否

失败后的回退策略：

- 保留表单输入，提示用户修正账号信息或验证码状态

### `POST /api/v1/auth/login`

用途：

- 使用邮箱或手机号 + 密码登录

前置条件：

- 已注册

是否进入正式结论链路：

- 否

失败后的回退策略：

- 保持在登录页，提示密码错误、账号状态或安全风险信息

### `GET /api/v1/auth/session`

用途：

- 获取当前会话、设备标签、风险提示

前置条件：

- 已登录

是否进入正式结论链路：

- 否

失败后的回退策略：

- 视为 token 不可用，前端应跳回登录流程

关键返回字段：

- `userId`
- `accountKey`
- `displayName`
- `verified`
- `sessionCode`
- `expiresAt`
- `deviceLabel`
- `currentSession`
- `riskLevel`
- `securityNotices`

### 其他安全接口

- `POST /api/v1/auth/verification-codes/request`
- `POST /api/v1/auth/password-reset/confirm`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/sessions`
- `DELETE /api/v1/auth/sessions/{sessionCode}`
- `PUT /api/v1/auth/password`
- `GET /api/v1/auth/account-verification/status`
- `POST /api/v1/auth/account-verification/request`
- `POST /api/v1/auth/account-verification/confirm`
- `GET /api/v1/privacy/consents/current`
- `GET /api/v1/privacy/consents/history`
- `PUT /api/v1/privacy/consents/current`

## 6. 用户档案

### `GET /api/v1/profile`

用途：

- 获取当前用户档案

前置条件：

- 已登录

是否进入正式结论链路：

- 间接进入
  - `targetUricAcid` 会影响化验单正式复盘和趋势解释

失败后的回退策略：

- 前端可以展示未完善档案态，但不应自行补默认目标值

### `PUT /api/v1/profile`

用途：

- 更新用户档案

关键请求字段：

- `name`
- `gender`
- `birthday`
- `heightCm`
- `targetUricAcid`
- `allergies`
- `comorbidities`
- `emergencyContact`

## 7. 文件

### `POST /api/v1/files/upload`

用途：

- 上传原始文件，供化验单和其他场景引用

前置条件：

- 已登录

是否进入正式结论链路：

- 否

失败后的回退策略：

- 保留本地文件选择状态，允许重新上传

请求方式：

- `multipart/form-data`
- 字段：`file`

### `GET /api/v1/files?limit=20`

用途：

- 获取当前登录用户最近上传的文件列表

前置条件：

- 已登录

是否进入正式结论链路：

- 否

失败后的回退策略：

- 前端保留当前内存态列表，但应提示“最近上传可能未完成服务端回填”

返回字段：

- `fileId`
- `fileName`
- `accessUrl`
- `size`
- `contentType`

### `GET /api/v1/files/{fileId}`

用途：

- 获取当前用户所属原始文件

## 8. 日常记录主链路

### 8.1 饮食识别

#### `POST /api/v1/meals/analyze`

用途：

- 上传餐盘图片并生成饮食记录

前置条件：

- 已登录

是否进入正式结论链路：

- 否

失败后的回退策略：

- 前端应提示识别失败并允许重新拍照，不应伪造识别结果

请求字段：

- `file`
- `mealType`
- `takenAt` 可选，ISO-8601
- `note` 可选

返回字段：

- `recordId`
- `imageUrl`
- `mealType`
- `takenAt`
- `riskLevel`
- `purineEstimateMg`
- `items`
- `suggestions`
- `summary`

### 8.2 核心记录接口

#### 写入

- `POST /api/v1/records/uric-acid`
- `POST /api/v1/records/weight`
- `POST /api/v1/records/flares`
- `POST /api/v1/records/hydration`

用途：

- 写入四类核心记录

前置条件：

- 已登录

是否进入正式结论链路：

- 是
  - 会影响总览、趋势、今日行动、提醒、复盘解释

失败后的回退策略：

- 提示保存失败，不刷新首页意义层

统一说明：

- 时间为空时后端会使用当前时间
- 成功后返回 `recordId + createdAt + message`

#### 查询

- `GET /api/v1/records/uric-acid`
- `GET /api/v1/records/weight`
- `GET /api/v1/records/flares`
- `GET /api/v1/records/hydration`

### 8.3 记录治理

#### `GET /api/v1/records/center`

用途：

- 获取统一记录工作台列表

查询参数：

- `types` 可选，多值
- `cursor` 可选
- `limit` 默认 `20`，范围 `1-100`

返回字段：

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

#### `GET /api/v1/records/detail`

查询参数：

- `type`
- `recordId`

#### `PUT /api/v1/records/detail`

用途：

- 编辑既有记录

是否进入正式结论链路：

- 是

失败后的回退策略：

- 保留当前编辑表单

说明：

- `changeReason` 必填，用于审计
- 不同记录类型只消费对应字段

#### `DELETE /api/v1/records/detail`

用途：

- 软删除记录

#### `GET /api/v1/records/audits`

用途：

- 查询记录审计轨迹

#### `POST /api/v1/records/restore`

用途：

- 从历史审计版本恢复记录

## 9. 总览、趋势、今日行动

### `GET /api/v1/dashboard/overview`

用途：

- 首页总览

### `GET /api/v1/home/today`

用途：

- 今日行动计划

关键返回字段：

- `overallRiskLevel`
- `triageCode`
- `triageTitle`
- `triageSummary`
- `nextStep`
- `reasons`
- `actions`
- `trustNotes`

### `GET /api/v1/dashboard/trends?days=7`

用途：

- 趋势数据

查询参数：

- `days` 范围 `1-90`

### `GET /api/v1/dashboard/daily-summaries?days=7`

用途：

- 每日聚合摘要

### `GET /api/v1/reminders`

用途：

- 当前活跃提醒

### `GET /api/v1/analysis/uric-acid-causes?lookbackDays=7`

用途：

- 解释近期尿酸波动原因

## 10. 化验单可信工作流

### 10.1 接口列表

- `POST /api/v1/lab-reports/analyze`
- `GET /api/v1/lab-reports`
- `GET /api/v1/lab-reports/{reportId}/review`
- `PUT /api/v1/lab-reports/{reportId}/manual-confirmation`

### 10.2 `POST /api/v1/lab-reports/analyze`

用途：

- 上传化验单并生成解析结果

前置条件：

- 已登录

是否进入正式结论链路：

- 不一定
  - 只有满足最低可信条件后，后续 `review` 才进入正式结论链路

失败后的回退策略：

- 保留已上传文件，允许重传
- 如果 AI 不可用，后端应回退到人工确认链路，不输出估算结论

请求方式：

- `multipart/form-data`
- `file`
- `reportDate` 可选，格式 `yyyy-MM-dd`

返回字段：

- `reportId`
- `reportDate`
- `indicators`
- `overallRiskLevel`
- `manualConfirmationRequired`
- `reviewReady`
- `extractionStatus`
- `suggestions`
- `trustNotes`
- `summary`
- `analysisMode`

### 10.3 `GET /api/v1/lab-reports`

用途：

- 获取报告列表，用于“上传结果页 / 历史列表页”

前置条件：

- 已登录

是否进入正式结论链路：

- 否

### 10.4 `GET /api/v1/lab-reports/{reportId}/review`

用途：

- 获取正式复盘页或待确认页数据

前置条件：

- 已登录
- 报告属于当前用户

是否进入正式结论链路：

- 取决于 `reviewReady`

失败后的回退策略：

- 报告不存在或无权限时，前端应返回列表页
- 未满足可信条件时，前端应展示待确认态，不得自行拼装正式结论

返回字段：

- `reportId`
- `reportDate`
- `overallRiskLevel`
- `manualConfirmationRequired`
- `reviewReady`
- `reviewStatus`
- `reviewSummary`
- `workflowTitle`
- `comparedReportId`
- `comparedReportDate`
- `daysBetweenReports`
- `targetUricAcidValue`
- `currentUricAcidValue`
- `currentUricAcidUnit`
- `uricAcidWithinTarget`
- `targetConclusion`
- `comparisons`
- `keyChanges`
- `followUpRecommendation`
- `manualConfirmationTasks`
- `blockedOutputs`
- `nextActions`
- `trustNotes`
- `trustMeta`
- `doctorSummary`
- `generatedAt`

### 10.5 `PUT /api/v1/lab-reports/{reportId}/manual-confirmation`

用途：

- 人工确认关键指标，解锁正式复盘

前置条件：

- 已登录
- 报告属于当前用户
- 当前报告处于需要人工确认状态，或已经手工复核过

是否进入正式结论链路：

- 是

失败后的回退策略：

- 表单不清空
- 保留原始文件和当前输入，允许继续修改

请求字段：

- `indicators`
- `summaryNote`

单个 `indicator` 字段：

- `code`
- `name`
- `value`
- `unit`
- `referenceRange`
- `riskLevel`

## 11. 化验单状态字典

### 11.1 `LabReportAnalyzeResponse`

#### `manualConfirmationRequired`

- `true`：当前不能直接进入正式复盘
- `false`：当前已满足最低可复盘条件

#### `reviewReady`

- `true`：可以进入正式复盘页
- `false`：只能进入待确认页

#### `extractionStatus`

- `MANUAL_CONFIRMATION_REQUIRED`
- `OCR_EXTRACTED`
- `MANUAL_CONFIRMED`

#### `analysisMode`

- `MANUAL_CONFIRMATION_REQUIRED`
- `AI_OCR`
- `SAFE_FALLBACK`
- `MANUAL_CONFIRMED`

### 11.2 `LabReportReviewResponse`

#### `reviewStatus`

- `MANUAL_CONFIRMATION_REQUIRED`
  - 当前只允许展示可信边界、待办、阻断项
- `READY`
  - 当前允许展示正式复盘与医生摘要

### 11.3 `trustMeta.verificationStage`

- `MANUAL_CONFIRMATION_REQUIRED`
- `OCR_EXTRACTED`
- `MANUAL_CONFIRMED`

### 11.4 `doctorSummary.readyToShare`

- `true`
  - 可以作为“可分享给医生的摘要”使用
- `false`
  - 只能作为中间确认态说明，不能当正式医疗摘要

### 11.5 `trustMeta.lockedSections`

表示当前由于可信条件不足，被锁定的正式输出区域。

前端应原样展示，不应自行推断缺失区域。

## 12. 知识问答

### `POST /api/v1/knowledge/ask`

用途：

- 健康问答

前置条件：

- 已登录

是否进入正式结论链路：

- 否

失败后的回退策略：

- 使用后端保守兜底答复

## 13. 用药管理

- `GET /api/v1/medications`
- `PUT /api/v1/medications`
- `POST /api/v1/medications/check-ins`
- `GET /api/v1/medications/adherence?days=7`
- `GET /api/v1/medications/weekly-report?days=7`

用途：

- 管理当前用药计划、服药打卡与周报

是否进入正式结论链路：

- 是
  - 会影响今日行动与管理解释

## 14. 家属轻协同

### 14.1 接口列表

- `POST /api/v1/family/invitations`
- `GET /api/v1/family/invitations`
- `POST /api/v1/family/invitations/{inviteCode}/accept`
- `POST /api/v1/family/invitations/{inviteCode}/cancel`
- `GET /api/v1/family/members`
- `DELETE /api/v1/family/members/{bindingCode}`
- `PUT /api/v1/family/members/{bindingCode}/permissions`
- `GET /api/v1/family/alerts`
- `GET /api/v1/family/tasks`
- `POST /api/v1/family/members/{bindingCode}/tasks`
- `POST /api/v1/family/tasks/{taskCode}/complete`
- `GET /api/v1/family/patients/{patientUserId}/summary`
- `GET /api/v1/family/patients/{patientUserId}/weekly-report?days=7`

### 14.2 家属权限字典

当前前后端统一使用：

- `caregiverPermission`

典型值由后端控制，前端只负责展示与回传，不应自行发明权限语义。

与权限相关的显式布尔值：

- `weeklyReportEnabled`
- `notifyOnHighRisk`

### 14.3 联调约束

- 家属模块只在 `family-care.enabled=true` 时展示
- 家属视角默认展示摘要，不默认暴露患者全部原始数据
- 所有家属访问都应可追踪到访问审计

## 15. 访问策略与审计

- `GET /api/v1/access/policy`
- `GET /api/v1/access/audits`
- `GET /api/v1/access/patient-audits`

用途：

- 获取角色、资源边界、访问规则和访问审计

## 16. 当前不建议前端接入的接口族

以下能力不在当前版本对外交付范围，即使后端残留实现，也不应写入前端主链路：

- 设备接入相关接口
- 成长体系相关接口

## 17. 推荐联调顺序

1. 注册 / 登录 / 当前会话
2. capabilities
3. profile
4. records 写入与列表
5. record center / detail / audits / restore
6. overview / today / trends / reminders
7. lab-reports
8. medications
9. family

## 18. HTTP 错误映射

前端应同时读取 HTTP 状态码和响应体 `code`：

| 场景 | HTTP | code |
|---|---:|---|
| 路由或资源不存在 | 404 | `RESOURCE_NOT_FOUND` |
| 文件不存在 | 404 | `FILE_NOT_FOUND` |
| 请求方法不支持 | 405 | `METHOD_NOT_ALLOWED` |
| 媒体类型不支持 | 415 | `UNSUPPORTED_MEDIA_TYPE` |
| JSON 请求体无法解析 | 400 | `INVALID_REQUEST_BODY` |
| 上传文件超过限制 | 413 | `FILE_TOO_LARGE` |
| 文件不属于当前用户 | 403 | `FORBIDDEN` |
| 未知服务端异常 | 500 | `INTERNAL_ERROR` |

所有错误响应继续保留 `traceId` 和 `path`，便于联调排查。
