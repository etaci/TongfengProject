# V23 真实投递通道与登录安全风控

## 本轮目标

这一轮继续沿着“真实登录与安全信任层”推进，优先完成两件事：

- 为验证码链路补上可切换的真实投递通道骨架
- 为登录链路补上频控、设备识别和异常登录提醒

当前版本仍然保留联调友好的模拟模式，但已经不再是单纯的本地验证码演示。

## 已完成能力

### 1. 验证码投递通道升级

后端新增 `VerificationDeliveryService`，按照账号类型选择投递通道：

- 邮箱账号：优先走 SMTP 邮件投递
- 手机号账号：优先走短信 Webhook 投递
- 未配置真实通道时：
  - 如果允许暴露验证码，则自动回退到联调模式
  - 如果不允许暴露验证码，则直接阻断并提示先配置真实通道

当前接口响应里会带出：

- `deliveryChannel`
- `deliveryProvider`
- `deliveryStatus`

这样前端和测试都能明确知道当前是邮件、短信，还是联调模拟模式。

### 2. 验证码频控

本轮在验证码请求链路新增两层控制：

- 重发冷却时间
- 窗口期最大请求次数

默认配置：

- 冷却时间：60 秒
- 窗口期：30 分钟
- 窗口内最大请求次数：5 次

当用户过于频繁请求时，会返回：

- `VERIFICATION_CODE_COOLDOWN`
- `VERIFICATION_CODE_RATE_LIMITED`

### 3. 设备风险识别

前端现在会为每个浏览器实例生成并持久化一个设备指纹，并在请求头中携带：

- `X-Device-Fingerprint`
- `X-Device-Label`

后端在登录时会结合：

- 设备指纹
- 设备标签
- 客户端 IP

对近一段时间的已知设备进行比对，输出：

- `deviceLabel`
- `clientIpMasked`
- `loginRiskLevel`
- `securityNotices`

风险策略当前是轻量规则版本：

- 首次登录：`GREEN`
- 新设备或新网络：`YELLOW`
- 新设备且新网络同时变化：`RED`

### 4. 异常登录提醒

当正式账号登录被识别为新设备 + 新网络时，系统会自动写入一条 `ACCOUNT_SECURITY` 类型提醒，进入现有提醒中心。

这意味着后续前端不需要再单独做一套安全通知系统，就能复用现有提醒展示链路。

## 新增配置

### 验证码频控

- `AUTH_VERIFICATION_RESEND_COOLDOWN_SECONDS`
- `AUTH_VERIFICATION_RATE_LIMIT_WINDOW_MINUTES`
- `AUTH_VERIFICATION_MAX_REQUESTS_PER_WINDOW`
- `AUTH_VERIFICATION_MAX_ATTEMPTS_PER_CODE`

### 已知设备窗口

- `AUTH_KNOWN_DEVICE_DAYS`

### 邮件投递

- `AUTH_EMAIL_ENABLED`
- `AUTH_EMAIL_FROM`
- `AUTH_EMAIL_SUBJECT_PREFIX`
- `AUTH_EMAIL_SMTP_HOST`
- `AUTH_EMAIL_SMTP_PORT`
- `AUTH_EMAIL_SMTP_USERNAME`
- `AUTH_EMAIL_SMTP_PASSWORD`
- `AUTH_EMAIL_STARTTLS_ENABLED`

### 短信投递

- `AUTH_SMS_ENABLED`
- `AUTH_SMS_WEBHOOK_URL`
- `AUTH_SMS_BEARER_TOKEN`

## 前端变化

### 登录与安全中心

前端现在会展示更多安全上下文：

- 当前设备标签
- 登录风险等级
- 网络标识掩码
- 活跃会话中的设备信息与风险等级

### 浏览器设备标识

Web 端新增本地设备档案存储，用于在同一浏览器长期识别“已知设备”。

## 验证结果

本轮已通过：

- 后端：`backend-java\\mvnw.cmd test -q`
- 前端：`frontend-web\\npm.cmd run build`

新增覆盖：

- 找回密码与账号验证主链路继续可用
- 验证码冷却时间频控生效
- 后续新设备 + 新网络登录会打上高风险并生成安全提醒

## 当前边界

这轮仍然是“可上线前一层”的骨架，不是最终版风控系统。当前还没做：

- 短信服务商专用签名与模板管理
- 邮件投递状态回执
- 登录失败次数累计封禁
- 异常地理位置识别
- 同设备会话盗用检测
- 更安全的前端会话存储替换
