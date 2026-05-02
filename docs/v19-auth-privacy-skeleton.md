# V19 真实登录与隐私授权骨架

## 本轮目标

V19 把原先偏开发联调的 `mock-login` 主入口，推进成“真实登录可用、隐私授权可追踪、前端可直接接入”的后端骨架版本。

本轮新增能力：

- 支持基于账号密码的正式注册
- 支持邮箱 / 手机号两种账号类型登录
- 保留 `mock-login` 作为开发体验入口
- 新增当前会话查询与主动注销
- 新增隐私授权当前版本查询
- 新增隐私授权历史查询
- 新增隐私授权更新接口
- 为前端补齐真实登录、授权管理、历史回显所需字段

## 新增接口

### 注册

- `POST /api/v1/auth/register`

请求体包含：

- `nickname`
- `accountType`，当前支持 `EMAIL`、`PHONE`
- `account`
- `password`
- `confirmPassword`
- `consent`

其中 `consent` 里必须至少完成：

- 同意隐私政策 `privacyAccepted = true`
- 同意服务条款 `termsAccepted = true`

注册成功后，服务端会直接创建登录态并返回 token。

### 登录

- `POST /api/v1/auth/login`

请求体包含：

- `accountType`
- `account`
- `password`

登录成功后返回：

- 用户 ID
- 昵称
- 登录方式 `authMode`
- 账号类型 `accountType`
- 账号标识 `accountIdentifier`
- 是否已完成必要隐私授权 `privacyConsentCompleted`
- `token`
- `expiresAt`

### 当前会话

- `GET /api/v1/auth/session`

用于前端刷新后恢复“当前是谁、用什么方式登录、授权是否完成、token 何时过期”。

### 注销

- `POST /api/v1/auth/logout`

注销后当前 token 立即失效，旧 token 再访问受保护接口会被拒绝。

### 查询当前隐私授权

- `GET /api/v1/privacy/consents/current`

返回内容包括：

- 授权版本 `consentVersion`
- 隐私政策版本 `privacyPolicyVersion`
- 是否同意隐私政策 / 服务条款
- 是否授权医疗数据分析
- 是否授权家属协同
- 是否授权提醒通知
- 来源类型
- 生效时间

### 查询隐私授权历史

- `GET /api/v1/privacy/consents/history`

用于前端展示授权版本变更轨迹，当前按时间倒序返回。

### 更新当前隐私授权

- `PUT /api/v1/privacy/consents/current`

当前版本允许用户更新以下可选授权项：

- `medicalDataAuthorized`
- `familyCollaborationAuthorized`
- `notificationAuthorized`

必要项：

- `privacyAccepted`
- `termsAccepted`

仍要求保持为 `true`，否则会拒绝保存。

## 数据模型变化

本轮新增持久化对象：

- `auth_identity`：保存正式账号身份、账号类型和密码摘要
- `privacy_consent_record`：保存授权版本与每次授权快照

本轮扩展会话对象：

- `auth_session`
- `UserSession`

新增字段：

- `authMode`
- `accountType`
- `accountIdentifier`
- `privacyConsentCompleted`

这样前端不需要二次拼装，就能直接渲染正式登录态。

## 前端接入点

前端已经补齐以下交互：

- 首页登录区切换为“账号登录 / 新用户注册”
- `mock-login` 下沉为“开发体验入口”
- 助手页新增“隐私与授权”卡片
- 助手页新增“授权历史”卡片
- 刷新扩展数据时自动带回：
  - `authSession`
  - `privacyConsentCurrent`
  - `privacyConsentHistory`

## 安全边界

当前版本是“真实登录与授权骨架”，不是完整的医疗账号体系。

当前边界如下：

- 仍是本地账号密码模型，未接入短信验证码、实名校验或医院统一身份
- 密码已做摘要存储，但暂未接入更完整的风控策略
- 隐私授权已具备版本记录和历史追踪，但尚未接入正式的隐私政策内容页、撤回流程和审计后台
- `mock-login` 仍保留，仅用于本地开发和联调，不建议作为生产入口
