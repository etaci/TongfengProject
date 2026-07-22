# 痛风发作结构化分诊

## 目标

`POST /api/v1/triage/gout-flare` 用于把用户当前的发作症状整理成可解释的健康管理分流结果。它不是痛风诊断器，也不能排除关节感染、骨折或其他急症。

## 请求

前置条件：用户已登录，使用 `Authorization: Bearer <session-token>`。

请求字段：

- `onsetAt`：起病时间，ISO-8601 时间。
- `jointLocation`：主要关节位置。
- `painLevel`：0-10 的疼痛等级。
- `rednessOrSwelling`：是否有红肿。
- `fever`：是否发热。
- `canBearWeight`：是否可以负重。
- `recentMedicationChange`：近期是否调整用药。
- `traumaHistory`：近期是否有外伤史。
- `firstEpisode`：是否首次出现类似发作。
- `systemicSymptoms`：是否有全身不适症状。

## 响应

成功响应 `data` 包含：

- `decisionCode`：本次分流决策审计编号。
- `triageCode`：`SELF_MANAGEMENT`、`CONTACT_DOCTOR_SOON` 或 `URGENT_OFFLINE`。
- `triageLevel`：分别对应 `GREEN`、`YELLOW`、`RED`。
- `summary`、`reasons`、`redFlags`、`nextActions`：结果、判断依据、红旗信号和下一步行动。
- `ruleVersion`、`sourceReferences`、`generatedAt`：规则版本、知识来源和生成时间。
- `verificationStatus`：当前为 `RULE_EVALUATED_NOT_CLINICIAN_REVIEWED`，表示规则已计算但没有医生人工复核。
- `disclaimer`：医疗边界说明，前端必须原样展示或完整保留语义。

## 分流规则

- 发热并伴有关节红肿、全身不适，或无法负重且伴外伤/红肿：`URGENT_OFFLINE`。
- 首次发作、疼痛等级不低于 7、近期调整用药、外伤史、发热或无法负重：`CONTACT_DOCTOR_SOON`。
- 其他未命中红旗的轻症输入：`SELF_MANAGEMENT`。

规则是保守分流，不代表确诊。任何症状加重、持续不缓解或用户不确定时，应优先线下就医。

## 是否进入正式结论链路

否。该接口输出的是分流建议，不会自动生成化验单正式复盘结论、医生摘要或处方建议。后续如接入今日行动引擎，应保留 `triageCode`、`reasons`、`ruleVersion` 和 `decisionCode` 的来源关系。

## 审计与隐私

后端会在 `clinical_decision_audit` 保存用户归属、规则版本、来源、结构化输入快照、输出摘要和生成时间。该表与“谁访问了什么数据”的 `access_audit` 不同，前者回答“系统为什么产生建议”。问卷健康正文不得写入普通应用日志，查询审计记录必须经过受控权限。

当前不包含药物相互作用判断。药物安全能力必须在接入经授权的药品知识源并建立 gateway 后再开放，不能由前端或本接口自行推断。

## 失败回退

- 未登录：返回 `401 UNAUTHORIZED`，前端回到登录状态。
- 字段缺失、起病时间晚于当前时间或疼痛等级越界：返回 `400 VALIDATION_ERROR`，保留表单内容并提示用户补全。
- JSON 无法解析：返回 `400 INVALID_REQUEST_BODY`，保留用户已填写内容并允许重新提交。
- 服务异常：返回统一错误响应和 `traceId`，前端不得把失败包装成已完成的医疗判断。
