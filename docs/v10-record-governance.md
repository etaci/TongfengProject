# V10 记录中心治理增强

本版本在 V9 统一健康记录中心基础上，继续补齐三项能力：

1. 记录中心游标分页
2. 单条记录详情
3. 单条记录删除

目标是让前端可以直接完成：

- 记录中心列表页
- 列表翻页
- 详情抽屉 / 详情页
- 删除记录后的即时刷新

## 1. 记录中心分页

接口：

`GET /api/v1/records/center`

支持参数：

- `types`：可重复传参
- `limit`：默认 `20`，范围 `1-100`
- `cursor`：上一页返回的 `nextCursor`

示例：

`GET /api/v1/records/center?types=WEIGHT&types=BLOOD_PRESSURE&types=URIC_ACID&limit=3`

返回新增字段：

- `nextCursor`
- `hasMore`

说明：

- 当前游标使用记录时间 `occurredAt` 的 ISO-8601 字符串
- 翻页规则为“取严格早于当前游标时间的记录”
- 当前仍按时间倒序返回

## 2. 单条记录详情

接口：

`GET /api/v1/records/detail?type={type}&recordId={recordId}`

当前支持的 `type`：

- `URIC_ACID`
- `WEIGHT`
- `BLOOD_PRESSURE`
- `HYDRATION`
- `FLARE`

返回字段：

- `recordId`
- `type`
- `title`
- `summary`
- `occurredAt`
- `riskLevel`
- `source`
- `note`
- `tags`
- `fields`

其中 `fields` 用于前端通用渲染详情项，字段包括：

- `key`
- `label`
- `value`

## 3. 删除记录

接口：

`DELETE /api/v1/records/detail?type={type}&recordId={recordId}`

返回字段：

- `recordId`
- `type`
- `status`
- `deletedAt`
- `message`

当前实现说明：

- 采用真实删除
- 删除后会同步刷新提醒与当日日汇总
- 删除后再次查询详情会返回 `RECORD_NOT_FOUND`

## 4. 异常码

新增 / 继续使用的关键异常码：

- `RECORD_TYPE_UNSUPPORTED`
- `RECORD_CURSOR_INVALID`
- `RECORD_NOT_FOUND`
- `FORBIDDEN`

## 5. 前端接入建议

推荐交互顺序：

1. 首屏请求 `GET /api/v1/records/center`
2. 点击“加载更多”时带上上一页 `nextCursor`
3. 点击某条记录时调用 `GET /api/v1/records/detail`
4. 删除确认后调用 `DELETE /api/v1/records/detail`
5. 删除成功后重新拉当前筛选条件下的 `records/center`

## 6. 本轮验证

验证命令：

```powershell
cd H:\ProjectTongfeng\backend-java
.\mvnw.cmd test -q
```

覆盖内容：

- 记录中心第一页
- 使用 `nextCursor` 拉取第二页
- 体重记录详情
- 删除体重记录
- 删除后详情不可再查
- 删除后记录中心数量变化
