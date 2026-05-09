# 生产启动骨架说明

本轮先补齐可交付的生产骨架，不扩散到 JWT/RBAC、真实医院接入或完整合规体系。

## 已补齐能力

- `MySQL + Flyway`：Java 后端已支持正式环境走 MySQL，并使用 Flyway 执行基线迁移。
- `Docker Compose`：根目录提供 `docker-compose.yml`，可一键拉起 `mysql + backend-ai + backend-java`。
- `OpenAPI/Swagger`：项目统一文档出口为 `/api/openapi`，交互入口为 `/swagger-ui`。
- `traceId + 错误响应`：统一错误响应已包含 `traceId` 与 `path`，响应头同步返回 `X-Trace-Id`。
- `健康检查`：Java 使用 `/actuator/health`，AI 使用 `/health`。

## 本地一键启动

在仓库根目录执行：

```powershell
docker compose up --build
```

默认端口：

- MySQL：`3306`
- Java 后端：`8080`
- AI 服务：`8001`

## 关键访问地址

- Java 健康检查：`http://localhost:8080/actuator/health`
- AI 健康检查：`http://localhost:8001/health`
- OpenAPI JSON：`http://localhost:8080/api/openapi`
- Swagger UI：`http://localhost:8080/swagger-ui`

## 运行时环境变量

`backend-java` 关键变量：

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_DRIVER`
- `AI_BASE_URL`
- `APP_STORAGE_ROOT`
- `REDIS_ENABLED=false`
- `WEATHER_LIVE_ENABLED=false`

`backend-ai` 当前通过容器内 `uvicorn` 启动，默认监听 `8001`。

## 数据库说明

- 开发/测试默认仍可使用 H2。
- 生产 profile 位于 [application-prod.properties](/H:/ProjectTongfeng/backend-java/src/main/resources/application-prod.properties)。
- Flyway 基线脚本位于 [V1__init_schema.sql](/H:/ProjectTongfeng/backend-java/src/main/resources/db/migration/V1__init_schema.sql)。

## 接口与错误追踪

- 成功/失败响应统一由 [ApiResponse.java](/H:/ProjectTongfeng/backend-java/src/main/java/com/tongfeng/backend/app/ApiResponse.java) 输出。
- 全局异常处理位于 [GlobalExceptionHandler.java](/H:/ProjectTongfeng/backend-java/src/main/java/com/tongfeng/backend/app/GlobalExceptionHandler.java)。
- traceId 生成与透传位于 [TraceIdFilter.java](/H:/ProjectTongfeng/backend-java/src/main/java/com/tongfeng/backend/app/TraceIdFilter.java)。

## 当前边界

这份骨架解决的是“可部署、可观测、可联调”的基础设施问题，暂未覆盖：

- JWT / RBAC 正式权限体系
- 多因子认证与可信设备
- 完整隐私政策页、导出/删除、审计后台
- 容器级端到端联调测试
