# 后端错误码规范

这份文档只说明使用约定，不再手工维护完整错误码明细表。

## 单一事实来源

- 公开字典接口：`GET /api/public/error-codes`
- OpenAPI 文档：`/api/openapi`
- 统一响应结构：`success / code / message / data / timestamp / traceId / path`

错误码明细请以后端接口返回为准，不再以前端常量或零散文档为准。

## 返回约定

错误响应示例：

```json
{
  "success": false,
  "code": "FILE_NOT_FOUND",
  "message": "文件不存在",
  "data": null,
  "timestamp": "2026-05-09T15:00:00Z",
  "traceId": "4d13d1d0-2d8b-4df7-96e6-8e8a7ef8b1f0",
  "path": "/api/v1/files/not-exists"
}
```

## 错误码字典字段

`/api/public/error-codes` 返回的每个条目包含：

- `code`：稳定错误码
- `httpStatus`：HTTP 状态码数值
- `httpStatusText`：HTTP 状态文本
- `category`：错误所属域，例如 `AUTH / FILE / RECORD / VERIFICATION`
- `retryable`：是否建议在满足条件后重试
- `defaultMessage`：默认中文文案

## 使用要求

- 前端优先根据 `code` 做分支，不要依赖中文 `message` 作为逻辑判断。
- 排障时优先记录 `traceId + code + path`，避免只记录展示文案。
- 新增 `BusinessException("XXX", ...)` 前，先把错误码注册到后端统一目录。
- 涉及账号安全、验证码、依赖服务不可用的场景，不要默认映射为 `400`，应显式给出更准确的 HTTP 状态。
