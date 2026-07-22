# V26 P0：患者旅程、医生就诊包与数据权利

## 1. V25 接入今日行动和时间线

提交 `POST /api/v1/triage/gout-flare` 后，后端会同时完成：

1. 保存临床决策审计及完整可信载荷。
2. 对黄色或红色分流生成 `GOUT_TRIAGE` 提醒。
3. `GET /api/v1/home/today` 使用最近一次 V25 分流增强今日行动。
4. `GET /api/v1/records/timeline` 增加 `GOUT_TRIAGE` 时间线事件。
5. 已授权且允许高风险通知的家属可通过既有家属告警接口看到该提醒。

前端不得只显示颜色。今日行动必须保留：

- `triageCode`
- `triageDecisionCode`
- `triageRuleVersion`
- `triageVerificationStatus`
- `triageRedFlags`
- `reasons`
- `actions`

时间线的 V25 事件必须保留：

- `triageCode`
- `ruleVersion`
- `decisionCode`
- `verificationStatus`
- `redFlags`
- `nextActions`

## 2. 医生就诊包

接口：

- `GET /api/v1/doctor-visit-packages?days=30`
- `GET /api/v1/doctor-visit-packages/print?days=30`
- `GET /api/v1/doctor-visit-packages/pdf?days=30`
- `POST /api/v1/doctor-visit-shares`
- `DELETE /api/v1/doctor-visit-shares/{shareCode}`
- `GET /api/public/doctor-visit-shares/{shareToken}`

统计周期只允许 `30` 或 `90` 天。就诊包包括尿酸趋势、发作记录、用药依从、化验可信状态、最近分诊、待问医生的问题、来源和可信边界。

分享令牌只在创建时返回明文，数据库只保存 SHA-256 摘要。有效期为 1-168 小时；撤销或过期后公开接口返回 `410 DOCTOR_SHARE_EXPIRED`。

公开分享不包含原始上传文件、密码、会话、设备指纹或 IP 指纹。

## 3. 数据权利

接口：

- `GET /api/v1/privacy/data-export`：下载 ZIP 数据包，包含 `data.json` 和可读取的原始上传文件。
- `POST /api/v1/privacy/consents/withdraw`：撤回可选授权并保存原因。
- `DELETE /api/v1/privacy/account`：永久删除账号。
- `GET /api/public/privacy-notice`：获取隐私说明。

可撤回授权字典：

- `MEDICAL_DATA`
- `FAMILY_COLLABORATION`
- `NOTIFICATION`

撤回授权会新增历史记录，不会覆盖旧记录。导出文件不包含密码摘要、密码盐、会话令牌、设备指纹或 IP 指纹。

永久删除必须提交：

```json
{
  "confirmation": "DELETE_MY_ACCOUNT",
  "reason": "不再使用"
}
```

删除完成后账号、健康记录、临床决策、家属关系、分享记录和上传文件均被清理，全部会话失效。该操作不可恢复。

## 4. 可信边界

- V25 是就医分流，不是诊断。
- 医生就诊包是就诊准备材料，不是处方或医疗证明。
- 化验单必须展示人工确认和可信状态，不能把待确认数据包装成正式结论。
- 当前没有药物相互作用知识源，不得由前端或规则服务自行推断药物冲突。
