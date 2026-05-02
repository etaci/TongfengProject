# V20 账户安全中心

## 本轮目标

V20 在 V19 的“真实登录与隐私授权骨架”之上，继续补正式账号最缺的一层安全闭环。

这一版不去碰短信验证码、实名校验或医院统一身份，而是先把本地账号体系做成更像真实产品可用的状态。

本轮新增能力：

- 查看当前账号的活跃会话列表
- 区分“当前设备”和“其他设备”
- 手动移除旧设备会话
- 支持修改正式账号密码
- 修改密码后可选择自动退出其他设备
- 前端助手页新增“账户安全”工作区

## 新增接口

### 查看活跃会话

- `GET /api/v1/auth/sessions`

返回内容包括：

- `sessionCode`
- `authMode`
- `accountType`
- `accountIdentifier`
- `currentSession`
- `createdAt`
- `lastSeenAt`
- `expiresAt`

前端可以直接用这组数据渲染“当前设备 / 其他设备”列表。

### 修改密码

- `PUT /api/v1/auth/password`

请求体包含：

- `currentPassword`
- `newPassword`
- `confirmPassword`
- `logoutOtherSessions`

当前规则：

- 仅 `PASSWORD` 登录方式支持修改密码
- 新密码至少 8 位
- 新密码不能与当前密码相同
- 需要校验当前密码

返回内容包括：

- 修改时间
- 本次被退出的其他设备会话数量
- 结果消息

### 移除指定会话

- `DELETE /api/v1/auth/sessions/{sessionCode}`

用于移除其他设备的登录态。

当前版本明确限制：

- 不能通过这个接口移除“当前设备”
- 当前设备请继续走 `POST /api/v1/auth/logout`

## 数据模型变化

本轮扩展 `auth_session`：

- `sessionCode`
- `lastSeenAt`

这样会话列表不再只有 token 存在感，而是能作为“可管理对象”被前端直接展示和操作。

本轮同时扩展：

- `UserSession`
- `AuthTokenResponse`
- `AuthSessionInfoResponse`

这样前端刷新后依然能拿到当前会话标识和最近活跃时间。

## 前端变化

助手页新增两块内容：

- `账户安全`
  - 修改密码
  - 控制是否自动退出其他设备
- `活跃会话`
  - 展示当前设备和其他设备
  - 一键移除旧设备

刷新扩展数据时会自动带回：

- `authSession`
- `authActiveSessions`

## 验证覆盖

本轮自动化测试新增覆盖：

- 一个正式账号同时存在多个会话
- 查看活跃会话列表
- 修改密码并退出其他设备
- 旧密码登录失败
- 新密码登录成功
- 使用新会话移除旧会话
- 被移除的旧 token 立即失效

## 安全边界

当前版本仍然是“本地账号安全中心”，不是完整的互联网医疗认证体系。

当前边界如下：

- 未接入短信验证码、找回密码验证码或风控拦截
- 未区分设备指纹、登录地点、IP 风险等级
- `lastSeenAt` 当前仅记录服务端最近一次有效请求时间
- `mock-login` 仍保留用于本地开发联调，不适合作为生产入口
