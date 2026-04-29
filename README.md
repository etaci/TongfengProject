# ProjectTongfeng 后端持续开发说明

本仓库当前已经完成两层后端能力：

1. 首版核心闭环
2. 第二版核心闭环

这一轮的重点是把两条增强线一起落完：

- Java 主服务从“内存态”升级为“可持久化 + 可调度”的主动管理后端
- Python AI 服务从“纯 mock”升级为“真实 OCR + 本地知识库检索”的可运行服务

当前默认可用 H2 本地运行，也支持切换到 MySQL；Redis 作为可选会话缓存预留。

## 目录结构

- `backend-java`
  - Spring Boot 主业务服务，对前端提供统一 API
- `backend-ai`
  - FastAPI AI 子服务，供 Java 主服务调用
- `backend-java/sql/schema-v1.sql`
  - MySQL 建表草案
- `docs/api-contract.md`
  - 前后端接口契约
- `docs/frontend-api.http`
  - 联调请求示例

## 当前已经完成的后端能力

### 首版核心闭环

- 模拟登录与 Bearer Token 鉴权
- 用户档案管理
- 文件上传与访问
- 餐盘识别闭环
- 尿酸记录
- 体重记录
- 发作记录
- 饮水/尿液打卡
- 首页总览
- 趋势图数据
- 提醒列表
- 时间线记录

### 第二版核心能力

- 化验单 OCR 解析
- 痛风知识问答
- 用户画像总结
- 用药管理

### 本轮新增增强

- Java 主服务
  - JPA 持久化存储，默认 H2，可切 MySQL
  - 规则引擎驱动的提醒列表
  - `daily_health_summary` 每日健康汇总
  - 定时任务自动刷新提醒与日汇总
  - `GET /api/v1/dashboard/daily-summaries` 前端聚合接口
- Python AI 服务
  - 化验单 OCR 真实管道：`RapidOCR -> pytesseract -> 文本解码 -> 保底估算`
  - 本地 Markdown 知识库检索问答
  - 保留餐盘识别接口，继续供 Java 主服务调用

### 当前已进入的下一版本能力

- V3 半主动管理
  - 主动管理城市配置
  - 天气快照拉取与天气联合风险评估
  - 主动风险简报接口
  - 发作复盘报告接口
  - 定时刷新提醒前自动尝试更新当日天气快照

### 当前继续推进的下一版本能力

- V4 家属协同与亲友监督
  - 患者发起家属邀请
  - 家属接受邀请形成授权绑定
  - 家属查看已绑定患者列表
  - 家属查看患者风险摘要与告警列表
  - 任一方可解除绑定结束授权

### 当前继续推进的平台生态第一段

- 第三方设备接入
  - 设备绑定与解绑
  - 设备同步事件去重
  - 设备尿酸数据自动转正式尿酸记录
  - 设备补水数据自动转正式饮水记录
  - 设备同步历史可查询

### 当前继续推进的平台生态第二段

- V5 积分成长体系
  - 健康行为自动奖励积分
  - 等级成长与连续活跃天数
  - 今日任务清单
  - 勋章徽章解锁
  - 积分流水与成长总览接口

### 当前继续推进的平台生态第三段

- V6 成长运营增强
  - 周目标与成长挑战
  - 风险导向任务
  - 家属协作挑战
  - 奖励兑换与领取历史
  - 可兑换积分余额闭环

### 当前继续推进的平台生态第四段

- V7 多指标设备接入
  - 新增血压正式记录闭环
  - 设备同步扩展到体重与血压
  - 血压记录进入时间线与提醒系统
  - 血压接口已预留给前端直接联调

## 当前技术形态

### Java 主服务

- 框架：Spring Boot
- 数据层：Spring Data JPA
- 默认本地数据库：H2
- 目标数据库：MySQL
- 会话缓存：Redis 可选
- 调度能力：Spring Scheduling

### AI 服务

- 框架：FastAPI
- OCR：RapidOCR，`pytesseract` 作为可选回退
- 检索：本地 Markdown 知识库 + `jieba`
- 知识库文件：
  - `backend-ai/knowledge/gout_diet_guide.md`
  - `backend-ai/knowledge/gout_followup_guide.md`
  - `backend-ai/knowledge/gout_medication_notice.md`
  - 当 AI 子服务暂不可用时，知识问答接口会自动回退到本地兜底答复，保证前端链路不中断

### V3 天气能力说明

- Java 主服务默认按 `Open-Meteo` 官方接口进行城市解析与天气拉取
- 若外部天气接口不可用，后端会自动回退到本地保底天气模型，保证主动管理接口不因第三方失败而中断

## 启动方式

### 1. 启动 AI 服务

```powershell
cd H:\ProjectTongfeng\backend-ai
python -m venv .venv
.venv\Scripts\pip install -r requirements.txt
.venv\Scripts\uvicorn app.main:app --reload --port 8001
```

说明：

- 如本机安装了 `tesseract`，AI 服务会把它作为 OCR 回退引擎之一。
- 即使 `RapidOCR` 或 `tesseract` 不可用，服务仍会回退到文本解码或保底规则，不会直接中断接口。

### 2. 启动 Java 服务

默认直接使用本地 H2：

```powershell
cd H:\ProjectTongfeng\backend-java
.\mvnw.cmd spring-boot:run
```

如果要切 MySQL，可以在启动前设置环境变量：

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/tongfeng?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='123456'
$env:DB_DRIVER='com.mysql.cj.jdbc.Driver'
.\mvnw.cmd spring-boot:run
```

如果要启用 Redis 会话缓存，再额外设置：

```powershell
$env:REDIS_ENABLED='true'
$env:REDIS_HOST='127.0.0.1'
$env:REDIS_PORT='6379'
```

如果要调整主动管理调度频率，可额外设置：

```powershell
$env:SCHEDULER_ENABLED='true'
$env:REMINDER_REFRESH_CRON='0 0/30 * * * *'
$env:SUMMARY_REFRESH_CRON='0 5 0 * * * *'
```

如需关闭实时天气拉取，仅保留本地保底模型，可额外设置：

```powershell
$env:WEATHER_LIVE_ENABLED='false'
```

## 验证方式

Java 主服务测试：

```powershell
cd H:\ProjectTongfeng\backend-java
.\mvnw.cmd test -q
```

Python AI 服务语法检查：

```powershell
python -m compileall H:\ProjectTongfeng\backend-ai
```

本轮代码已通过上述两项验证。

## 联调顺序建议

1. 先调用 `POST /api/v1/auth/mock-login` 获取 token。
2. 再联调用户档案页。
3. 然后接饮食拍照识别和尿酸/体重/发作/饮水记录接口。
4. 首页接 `overview + trends + reminders + daily-summaries`。
5. 再接化验单、问答、画像、用药等第二版能力。
6. 下一版可继续接 `proactive-care/settings + proactive-care/brief + flares/reports/latest`。
7. 家属协同页可继续接 `family/invitations + family/members + family/alerts + family/patients/{patientUserId}/summary`。
8. 设备接入页可继续接 `devices + devices/{deviceCode}/sync + devices/{deviceCode}/sync-events`。
9. 成长激励页可继续接 `growth/overview + growth/tasks + growth/points + growth/badges`。
10. 成长运营页可继续接 `growth/weekly-plan + growth/rewards + growth/rewards/{rewardKey}/claim + growth/reward-claims`。
11. 多指标设备页可继续接 `records/blood-pressure + devices/{deviceCode}/sync`，同步支持 `URIC_ACID / HYDRATION / WEIGHT / BLOOD_PRESSURE`。
