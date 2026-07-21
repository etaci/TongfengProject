# 前端开发联调说明

本文档给前端开发同学使用，目标是统一四件事：

- 当前版本到底做什么
- 哪些接口应该接，哪些不要接
- 化验单状态怎么解释
- 家属权限怎么解释

## 1. 当前版本范围

当前只做三条主链路：

1. 化验单复盘
2. 日常记录闭环
3. 家属轻协同

不要再把以下能力接进当前正式前端：

- 设备接入
- 成长体系

它们当前统一视为：

- `legacy / disabled / internal only`

## 2. 推荐联调顺序

1. `register`
2. `login`
3. `auth/session`
4. `app/capabilities`
5. `profile`
6. `records`
7. `dashboard + home/today + reminders`
8. `lab-reports`
9. `medications`
10. `family`

开发环境如果只想快速起链，可以用 `mock-login`，但不要把它写成产品默认入口。

## 3. 能力开关解释

启动后优先调用：

- `GET /api/v1/app/capabilities`

当前重点看：

- `daily-records`
- `lab-report-review`
- `family-care`

当前应忽略：

- `device-integration`
- `growth-system`

如果 `enabled=false`，前端不应展示入口，也不应继续请求对应接口。

## 4. 化验单页面模型

化验单前端至少要拆成两个明确页面语义：

1. 上传结果页
2. 正式复盘页

不要把“解析到了”和“可以正式判断了”混成一个页面语义。

### 上传结果页

只看 `LabReportAnalyzeResponse`：

- `manualConfirmationRequired`
- `reviewReady`
- `extractionStatus`
- `analysisMode`
- `trustNotes`

### 正式复盘页 / 待确认页

统一看 `LabReportReviewResponse`：

- `manualConfirmationRequired`
- `reviewReady`
- `reviewStatus`
- `manualConfirmationTasks`
- `blockedOutputs`
- `trustMeta`
- `doctorSummary`

## 5. 化验单状态字典

### 5.1 首层判断

#### `manualConfirmationRequired=true`

- 当前报告不能进入正式复盘
- 应展示待确认说明、阻断项、人工确认任务

#### `reviewReady=true`

- 当前报告可以进入正式复盘
- 才允许展示目标判断、对比结论、医生摘要

### 5.2 `extractionStatus`

- `MANUAL_CONFIRMATION_REQUIRED`
  - OCR 结果不足，当前必须人工确认
- `OCR_EXTRACTED`
  - OCR 已提取到最低可复盘数据
- `MANUAL_CONFIRMED`
  - 已由用户人工确认

### 5.3 `reviewStatus`

- `MANUAL_CONFIRMATION_REQUIRED`
  - 当前是待确认态
- `READY`
  - 当前是正式复盘态

### 5.4 `trustMeta.verificationStage`

- `MANUAL_CONFIRMATION_REQUIRED`
- `OCR_EXTRACTED`
- `MANUAL_CONFIRMED`

这个字段用于展示可信流程阶段，不是按钮文案字段。

### 5.5 `doctorSummary.readyToShare`

- `true`
  - 可以作为“可分享给医生”的摘要
- `false`
  - 只能当中间态说明，不能包装成正式可分享结论

### 5.6 `trustMeta.lockedSections`

这是后端明确告诉前端“当前哪些正式输出被锁住了”。

前端应：

- 原样展示
- 不要自行脑补正式结论
- 不要在锁定状态下继续展示医生摘要下载 / 分享按钮

## 6. 化验单页面固定信息块建议

前端渲染建议固定四块：

1. 原始报告信息
2. 指标提取结果
3. 可信状态
4. 下一步动作

如果 `reviewReady=false`，则：

- 重点展示 `manualConfirmationTasks`
- 重点展示 `blockedOutputs`
- 重点展示 `trustNotes`

如果 `reviewReady=true`，则：

- 可以展示 `targetConclusion`
- 可以展示 `comparisons`
- 可以展示 `doctorSummary`

## 7. 为什么要人工确认

所有需要人工确认的场景，前端都应向用户说明两句话：

1. 为什么需要确认
2. 确认后会解锁什么

推荐直接使用后端返回的：

- `reviewSummary`
- `trustNotes`
- `blockedOutputs`
- `manualConfirmationTasks`

不要前端自己创造新的医疗解释。

## 8. 家属权限字典

家属绑定当前重点看三个字段：

- `caregiverPermission`
- `weeklyReportEnabled`
- `notifyOnHighRisk`

处理原则：

- `caregiverPermission` 由后端定义语义，前端只做展示和透传
- `weeklyReportEnabled` 控制是否展示周报相关能力
- `notifyOnHighRisk` 控制是否展示高风险通知相关设置

如果 `family-care.enabled=false`：

- 家属模块不展示
- 家属接口不请求

## 9. 错误处理建议

### 化验单

- 上传失败：保留文件选择状态
- 进入人工确认：不要误报成系统异常
- 复盘未就绪：展示待确认态，不要跳白页

### 家属

- 权限不足：返回绑定页或摘要页，不要展示空白
- 模块未开放：直接隐藏模块入口

### 记录

- 保存失败：不要刷新首页意义层
- 编辑失败：保留表单和变更原因

### 安全中心

- session 失效：跳回登录页
- 验证码失败：保留输入内容和倒计时状态

## 10. 前端不要做的事

- 不要把 `mock-login` 当正式入口
- 不要再请求设备接口
- 不要再请求成长接口
- 不要在 `reviewReady=false` 时渲染正式医疗结论
- 不要自己拼接家属权限语义
- 不要自己推断化验单可信状态

## 11. 开发环境 mock-login 约定

- 同一昵称忽略首尾空格和大小写后，稳定映射到同一 `userId`
- 不同昵称映射到不同用户
- 再次登录不会重置已有档案、家庭绑定和历史记录
- 生产环境必须保持 `app.mock-login-enabled=false`

## 12. 最近上传回填约定

“最近上传”不要只依赖前端内存态。

推荐做法：

1. 页面初始化或全局 hydrate 时调用 `GET /api/v1/files?limit=20`
2. 上传成功后先本地 prepend
3. 再用服务端列表结果对齐，保证刷新后仍可见

如果列表接口失败：

- 保留本地上传成功态
- 同时提示“最近上传列表暂未完成服务端回填”

## 13. 对应文档

- [README](/H:/ProjectTongfeng/README.md)
- [接口契约](/H:/ProjectTongfeng/docs/api-contract.md)
- [联调请求示例](/H:/ProjectTongfeng/docs/frontend-api.http)
- [化验单可信边界说明](/H:/ProjectTongfeng/docs/v21-lab-trust-boundary.md)
