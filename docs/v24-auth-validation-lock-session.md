# V24 账号强校验、登录失败锁定与前端会话收敛

## 本轮目标

这一轮只收口剩余 `P1`，聚焦三个安全与可信度细节：

- 账号格式从弱校验升级为正式校验
- 连续登录失败触发短时锁定，避免暴力尝试
- Web 端认证会话从长期 `localStorage` 收敛到会话级 `sessionStorage`

## 已完成能力

### 1. 账号格式强校验

后端 `normalizePrincipal(...)` 已升级为正式规则：

- 邮箱：
  - 统一转小写
  - 使用正式邮箱正则校验
  - 非法输入返回 `EMAIL_INVALID`
- 手机号：
  - 自动去掉空格、短横线等非数字字符
  - 兼容 `+86` / `86` 前缀
  - 最终统一为 11 位中国大陆手机号
  - 非法输入返回 `PHONE_INVALID`

这样做之后，正式登录、注册、验证码请求、找回密码等链路都会共享同一套账号规范化逻辑。

### 2. 登录失败累计锁定

正式账号密码登录现在会累计失败次数：

- 默认连续失败 5 次后锁定账号
- 默认锁定 15 分钟
- 锁定期间返回 `ACCOUNT_LOGIN_LOCKED`
- 成功登录后会自动清空失败次数、失败时间与锁定状态

新增配置：

- `AUTH_LOGIN_FAILURE_THRESHOLD`
- `AUTH_LOGIN_FAILURE_LOCK_MINUTES`

对应数据表 `auth_identity` 已补充：

- `failed_login_count`
- `last_failed_login_at`
- `locked_until`

### 3. 前端会话存储收敛

Web 端现在改为：

- 认证会话优先写入 `sessionStorage`
- 首次读取时会自动把旧版 `localStorage` 会话迁移到 `sessionStorage`
- 登出时同时清理新旧两套会话存储
- 设备指纹档案继续保留在 `localStorage`

这意味着浏览器关闭后，认证 token 默认不会长期留存在本地，风险比之前更低。

## 联调关注点

前端联调时需要注意：

- 手机号输入可以继续接受 `+86 138-0013-8000` 这类格式，但后端会统一回写 `13800138000`
- 连续输错密码达到阈值后，下一次登录会收到 `ACCOUNT_LOGIN_LOCKED`
- 如果浏览器里还残留旧版本地会话，首次打开新版本页面时会自动迁移

## 本轮验证

计划覆盖以下验证：

- 后端：`backend-java\\mvnw.cmd test -q`
- 前端：`frontend-web\\npm.cmd run build`

新增测试重点：

- 非法邮箱 / 非法手机号返回明确错误码
- 合法中国大陆手机号会被统一规范化
- 连续失败登录后触发账号锁定
