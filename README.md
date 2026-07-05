# ProjectTongfeng

面向痛风 / 高尿酸管理场景的全栈项目，当前版本只聚焦三条主链路：

- 化验单复盘
- 日常记录闭环
- 家属轻协同

当前版本不承诺前端接入以下能力：

- 设备接入：`legacy / disabled / internal only`
- 成长体系：`legacy / disabled / internal only`

## 运行时约束

- 推荐 JDK：`21 LTS`
- 当前 `backend-java/pom.xml` 的目标版本也是 `21`
- JDK 25 下已知可能出现 Netty / Mockito dynamic agent 兼容性警告
- 短期建议前后端联调、CI、IDE 本地测试统一到 JDK 21

## 项目结构

- `backend-java`
  - Spring Boot 主服务，对前端提供统一 API
- `backend-ai`
  - FastAPI AI 子服务，提供餐盘识别、化验单 OCR、知识问答能力
- `docs/api-contract.md`
  - 对前端的接口契约
- `docs/frontend-api.http`
  - 联调请求示例
- `docs/frontend-dev-guide.md`
  - 前端开发说明、状态字典、联调顺序与可信边界

## 当前产品范围

### 主链路 1：化验单复盘

- 上传化验单
- OCR 提取或进入人工确认
- 可信状态展示
- 正式复盘
- 医生摘要

### 主链路 2：日常记录闭环

- 饮食识别与记录
- 尿酸、体重、发作、饮水记录
- 记录中心、审计、恢复
- 今日行动、总览、趋势、提醒
- 用药计划、服药打卡、周报

### 主链路 3：家属轻协同

- 家属邀请与绑定
- 家属摘要
- 家属提醒
- 家属代办
- 访问策略与审计

## 启动方式

### 1. 启动 AI 服务

```powershell
cd H:\ProjectTongfeng\backend-ai
python -m venv .venv
.venv\Scripts\pip install -r requirements.txt
.venv\Scripts\uvicorn app.main:app --reload --port 8001
```

说明：

- AI 服务不可用时，Java 主服务会对部分能力做保守回退
- 化验单场景回退后会进入人工确认链路，不会输出估算化验结论

### 2. 启动 Java 服务

```powershell
cd H:\ProjectTongfeng\backend-java
.\mvnw.cmd spring-boot:run
```

默认使用本地 H2。

如需切换 MySQL，可在启动前设置：

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/tongfeng?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='123456'
$env:DB_DRIVER='com.mysql.cj.jdbc.Driver'
.\mvnw.cmd spring-boot:run
```

如需启用 Redis 会话缓存：

```powershell
$env:REDIS_ENABLED='true'
$env:REDIS_HOST='127.0.0.1'
$env:REDIS_PORT='6379'
```

如需调整调度器：

```powershell
$env:SCHEDULER_ENABLED='true'
$env:REMINDER_REFRESH_CRON='0 0/30 * * * *'
$env:SUMMARY_REFRESH_CRON='0 5 0 * * * *'
```

如需关闭实时天气拉取：

```powershell
$env:WEATHER_LIVE_ENABLED='false'
```

## 正式联调入口

前后端联调默认使用正式账号链路：

1. `POST /api/v1/auth/register`
2. `POST /api/v1/auth/login`
3. `GET /api/v1/auth/session`
4. `GET /api/v1/app/capabilities`

`POST /api/v1/auth/mock-login` 仅用于开发环境和临时调试，不应作为正式产品入口，也不应作为默认联调路径。

## 化验单可信边界

化验单当前有两种输出层级：

- 可上传、可解析：表示系统收到了报告，并生成了提取结果或待确认状态
- 可进入正式复盘：表示已经满足最低可信条件，可以生成目标判断、对比结论、医生摘要

当前前端必须使用以下字段判断状态：

- `manualConfirmationRequired`
- `reviewReady`
- `reviewStatus`
- `trustMeta.verificationStage`
- `doctorSummary.readyToShare`

详细定义见：

- [docs/api-contract.md](/H:/ProjectTongfeng/docs/api-contract.md)
- [docs/frontend-dev-guide.md](/H:/ProjectTongfeng/docs/frontend-dev-guide.md)
- [docs/v21-lab-trust-boundary.md](/H:/ProjectTongfeng/docs/v21-lab-trust-boundary.md)

## 能力开关约定

`GET /api/v1/app/capabilities` 当前会对外返回：

- `daily-records`
- `lab-report-review`
- `family-care`
- `device-integration`
- `growth-system`

其中：

- `device-integration.enabled=false`
- `growth-system.enabled=false`

前端不应再主动请求设备 / 成长相关接口。

## 验证方式

Java 编译：

```powershell
cd H:\ProjectTongfeng\backend-java
.\mvnw.cmd -q -DskipTests compile
```

Java 测试：

```powershell
cd H:\ProjectTongfeng\backend-java
.\mvnw.cmd test -q
```

Python 语法检查：

```powershell
python -m compileall H:\ProjectTongfeng\backend-ai
```

## 文档索引

- [接口契约](/H:/ProjectTongfeng/docs/api-contract.md)
- [前端联调示例](/H:/ProjectTongfeng/docs/frontend-api.http)
- [前端开发说明](/H:/ProjectTongfeng/docs/frontend-dev-guide.md)
- [化验单可信边界说明](/H:/ProjectTongfeng/docs/v21-lab-trust-boundary.md)
