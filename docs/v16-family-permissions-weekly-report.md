# V16 家属权限分层与周报共享

## 本轮目标

V16 把家属协同从“能绑定、能看摘要”推进到“有边界地共同照护”。

本轮新增能力：

- 家属权限分层：
  - `READ_ONLY`：只读查看患者摘要
  - `REMINDER`：可接收高风险提醒
  - `TASK`：适合共同照护，保留提醒并支持更多协同入口
- 周报共享开关：患者可决定是否向家属开放用药周报
- 高风险提醒开关：患者可决定是否把高风险提醒推送给该家属
- 家属共享周报接口：家属在获得授权后可查看患者近 7 天用药依从周报

## 新增接口

### 1. 更新家属权限

- `PUT /api/v1/family/members/{bindingCode}/permissions`

请求体：

```json
{
  "caregiverPermission": "READ_ONLY",
  "weeklyReportEnabled": false,
  "notifyOnHighRisk": false
}
```

说明：

- 只有患者本人可以调整权限
- 解绑或失效绑定不可调整

### 2. 查看家属共享周报

- `GET /api/v1/family/patients/{patientUserId}/weekly-report?days=7`

返回内容包括：

- 家属当前权限
- 周报是否已开放
- 近 7 天用药周报数据

若患者关闭周报共享，会返回：

- `WEEKLY_REPORT_DISABLED`

## 邀请扩展字段

发起家属邀请时，现在可直接携带：

- `caregiverPermission`
- `weeklyReportEnabled`
- `notifyOnHighRisk`

这样患者在创建邀请时就能先设好授权边界。

## 前端变化

- 家庭协同页支持在邀请时选择权限层级
- 患者视角下可直接修改每个家属绑定的权限、周报共享和提醒开关
- 家属视角下可查看患者摘要，也可在已授权时加载共享周报

## 安全边界

- 家属权限调整必须由患者本人发起
- 没有共享授权的周报不会暴露给家属
- 关闭高风险提醒后，家属不再收到该患者的风险提醒流
