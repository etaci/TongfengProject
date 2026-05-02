import { useEffect, useState } from "react";
import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import { ArraySummary, BulletList, MemberList } from "../components/HealthBlocks";
import { familyRelationOptions, genderOptions } from "../constants/options";
import { formatDate, formatDateTime } from "../utils/format";
import { getMedicationPeriodLabel, getMedicationPeriods } from "../utils/forms";

function getMedicationStatusLabel(status) {
  switch (status) {
    case "TAKEN":
      return "已服用";
    case "MISSED":
      return "漏服";
    case "SKIPPED":
      return "跳过";
    default:
      return status || "未知";
  }
}

function getAuthModeLabel(mode) {
  switch (mode) {
    case "PASSWORD":
      return "正式账号";
    case "MOCK":
      return "开发体验";
    default:
      return mode || "未知";
  }
}

function LabReportSelector({ app, data, busyMap, withErrorHandling }) {
  const reports = data.labs || [];
  const activeReportId = data.labResult?.reportId || data.labReview?.reportId;

  if (!reports.length) {
    return null;
  }

  return (
    <div className="stack-list report-selector">
      <strong className="subtle-title">最近报告</strong>
      <div className="masonry-list">
        {reports.slice(0, 4).map((item) => (
          <article className="list-card" key={item.reportId}>
            <div className="result-header">
              <div>
                <strong>{formatDate(item.reportDate)}</strong>
                <p>报告 ID：{item.reportId}</p>
              </div>
              <RiskBadge level={item.overallRiskLevel} />
            </div>
            <p>{item.summary || "暂无解析摘要。"}</p>
            <div className="action-row">
              {activeReportId === item.reportId ? <span className="inline-tag">当前查看</span> : null}
              <button
                className="ghost-button action-button"
                type="button"
                disabled={busyMap.labReview}
                onClick={() => withErrorHandling(() => app.loadLabReportReview(item.reportId))}
              >
                {busyMap.labReview ? "加载中..." : activeReportId === item.reportId ? "刷新复盘" : "查看复盘"}
              </button>
            </div>
          </article>
        ))}
      </div>
      {reports.length > 4 ? <p className="meta-text">当前仅展示最近 4 份报告，可继续通过刷新数据同步最新化验单。</p> : null}
    </div>
  );
}

export default function AssistantPage({
  app,
  data,
  busyMap,
  session,
  handleKnowledgeSubmit,
  profileDraft,
  setProfileDraft,
  handleProfileSubmit,
  handlePrivacyConsentSubmit,
  handlePasswordChange,
  handleRevokeSession,
  medicationDraft,
  setMedicationDraft,
  handleMedicationSubmit,
  medicationCheckinDraft,
  setMedicationCheckinDraft,
  handleMedicationCheckinSubmit,
  handleFileUpload,
  handleOpenFile,
  handleLabSubmit,
  familyFeatureEnabled,
  inviteDraft,
  setInviteDraft,
  handleInviteSubmit,
  acceptInviteCode,
  setAcceptInviteCode,
  handleAcceptInvite,
  familySummaryTargetName,
  withErrorHandling,
}) {
  const [privacyDraft, setPrivacyDraft] = useState({
    consentVersion: "v1.0",
    privacyPolicyVersion: "privacy-v1.0",
    privacyAccepted: true,
    termsAccepted: true,
    medicalDataAuthorized: true,
    familyCollaborationAuthorized: true,
    notificationAuthorized: true,
  });
  const [passwordDraft, setPasswordDraft] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
    logoutOtherSessions: true,
  });
  const medicationItems = data.medication?.currentMedications || [];
  const selectedMedication = medicationItems.find((item) => item.name === medicationCheckinDraft.medicationName) || medicationItems[0] || null;
  const medicationPeriodOptions = selectedMedication ? getMedicationPeriods(selectedMedication.frequency) : ["MORNING"];

  useEffect(() => {
    if (!data.privacyConsentCurrent) {
      return;
    }
    setPrivacyDraft({
      consentVersion: data.privacyConsentCurrent.consentVersion || "v1.0",
      privacyPolicyVersion: data.privacyConsentCurrent.privacyPolicyVersion || "privacy-v1.0",
      privacyAccepted: Boolean(data.privacyConsentCurrent.privacyAccepted),
      termsAccepted: Boolean(data.privacyConsentCurrent.termsAccepted),
      medicalDataAuthorized: Boolean(data.privacyConsentCurrent.medicalDataAuthorized),
      familyCollaborationAuthorized: Boolean(data.privacyConsentCurrent.familyCollaborationAuthorized),
      notificationAuthorized: Boolean(data.privacyConsentCurrent.notificationAuthorized),
    });
  }, [data.privacyConsentCurrent]);

  return (
    <section className="content-section" id="assistant">
      <SectionHeader
        kicker="04 / 问答与档案"
        title="把知识问答、个人档案、用药、文件和家属轻协同收进同一工作台，减少验证路径里的页面跳转。"
      />

      <div className="overview-grid">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Knowledge assistant</p>
              <h3>知识问答</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleKnowledgeSubmit}>
            <label>
              <span>问题</span>
              <textarea name="question" rows="4" placeholder="例如：尿酸偏高时晚餐应该怎么控制？" />
            </label>
            <label>
              <span>场景</span>
              <input name="scene" placeholder="例如：聚餐后、夜间疼痛、复查前" />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.knowledge}>
              {busyMap.knowledge ? "提交中..." : "发起问答"}
            </button>
          </form>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Answer panel</p>
              <h3>回答结果</h3>
            </div>
          </div>
          <div className={`result-panel ${data.knowledge ? "" : "empty-panel"}`}>
            {data.knowledge ? (
              <>
                <div className="result-header">
                  <div>
                    <h4>知识库回复</h4>
                    <p>
                      {data.knowledge.escalateToDoctor
                        ? "当前问题风险偏高，建议优先联系医生或尽快线下就医。"
                        : "当前回答适合作为日常管理参考。"}
                    </p>
                  </div>
                  <span className={`inline-tag ${data.knowledge.escalateToDoctor ? "risk-red" : "risk-green"}`}>
                    {data.knowledge.escalateToDoctor ? "建议就医" : "可先自我管理"}
                  </span>
                </div>
                <p>{data.knowledge.answer || "暂无回答内容。"}</p>
                <BulletList title="引用来源" items={data.knowledge.references} />
                <p className="narrative-text">{data.knowledge.disclaimer}</p>
              </>
            ) : (
              "登录后即可向知识库提问，这里会展示回答、引用来源和风险提示。"
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Account security</p>
              <h3>账户安全</h3>
            </div>
            <span className="inline-tag">{session?.authMode === "PASSWORD" ? "正式账号" : "开发体验"}</span>
          </div>
          {session?.authMode === "PASSWORD" ? (
            <>
              <div className="stats-grid stats-grid--compact">
                <div className="stat-line">
                  <span>账号类型</span>
                  <strong>{data.authSession?.accountType || session.accountType || "-"}</strong>
                </div>
                <div className="stat-line">
                  <span>账号标识</span>
                  <strong>{data.authSession?.accountIdentifier || session.accountIdentifier || "-"}</strong>
                </div>
                <div className="stat-line">
                  <span>活跃会话</span>
                  <strong>{data.authActiveSessions?.length || 0} 个</strong>
                </div>
              </div>
              <form
                className="stack-form compact-form"
                onSubmit={async (event) => {
                  event.preventDefault();
                  const changed = await handlePasswordChange(passwordDraft);
                  if (changed) {
                    setPasswordDraft((current) => ({
                      ...current,
                      currentPassword: "",
                      newPassword: "",
                      confirmPassword: "",
                    }));
                  }
                }}
              >
                <label>
                  <span>当前密码</span>
                  <input
                    type="password"
                    value={passwordDraft.currentPassword}
                    onChange={(event) => setPasswordDraft((current) => ({ ...current, currentPassword: event.target.value }))}
                  />
                </label>
                <label>
                  <span>新密码</span>
                  <input
                    type="password"
                    value={passwordDraft.newPassword}
                    onChange={(event) => setPasswordDraft((current) => ({ ...current, newPassword: event.target.value }))}
                    placeholder="至少 8 位"
                  />
                </label>
                <label>
                  <span>确认新密码</span>
                  <input
                    type="password"
                    value={passwordDraft.confirmPassword}
                    onChange={(event) => setPasswordDraft((current) => ({ ...current, confirmPassword: event.target.value }))}
                  />
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={passwordDraft.logoutOtherSessions}
                    onChange={(event) => setPasswordDraft((current) => ({ ...current, logoutOtherSessions: event.target.checked }))}
                  />
                  <span>修改密码后自动退出其他设备会话</span>
                </label>
                <button className="primary-button" type="submit" disabled={!session || busyMap.passwordChange}>
                  {busyMap.passwordChange ? "更新中..." : "更新密码"}
                </button>
              </form>
            </>
          ) : (
            <EmptyState message="开发体验账号没有正式密码体系，切换到正式账号登录后可使用这里的安全能力。" />
          )}
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Active sessions</p>
              <h3>活跃会话</h3>
            </div>
          </div>
          <div className="stack-list">
            {data.authActiveSessions?.length ? (
              data.authActiveSessions.map((item) => (
                <article className="list-card" key={item.sessionCode}>
                  <div className="result-header">
                    <div>
                      <strong>{getAuthModeLabel(item.authMode)}</strong>
                      <p>{item.accountIdentifier || "开发体验账号"}</p>
                    </div>
                    <span className="inline-tag">{item.currentSession ? "当前设备" : "其他设备"}</span>
                  </div>
                  <div className="list-card__meta">
                    <span>创建：{formatDateTime(item.createdAt)}</span>
                    <span>最近活跃：{formatDateTime(item.lastSeenAt)}</span>
                  </div>
                  <div className="list-card__meta">
                    <span>过期：{formatDateTime(item.expiresAt)}</span>
                    <span>{item.accountType || "DEMO"}</span>
                  </div>
                  {!item.currentSession ? (
                    <div className="action-row">
                      <button
                        className="ghost-button action-button"
                        type="button"
                        disabled={busyMap[`revoke-session-${item.sessionCode}`]}
                        onClick={() => handleRevokeSession(item.sessionCode)}
                      >
                        {busyMap[`revoke-session-${item.sessionCode}`] ? "移除中..." : "移除此设备"}
                      </button>
                    </div>
                  ) : null}
                </article>
              ))
            ) : (
              <EmptyState message="当前没有可展示的活跃会话。" />
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Activity timeline</p>
              <h3>活动时间线</h3>
            </div>
          </div>
          <div className="timeline-list">
            {data.timeline?.length ? (
              data.timeline.map((item) => (
                <article className="timeline-item" key={item.eventId}>
                  <div className="result-header">
                    <div>
                      <strong>{item.title || item.type}</strong>
                      <p>{item.detail || "暂无详细说明。"}</p>
                    </div>
                    <RiskBadge level={item.riskLevel} />
                  </div>
                  <div className="timeline-item__meta">
                    <span className="event-type">{item.type}</span>
                    <span>{formatDateTime(item.occurredAt)}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无活动时间线数据。" />
            )}
          </div>
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Profile archive</p>
              <h3>个人档案</h3>
            </div>
          </div>
          <form className="stack-form compact-form" onSubmit={handleProfileSubmit}>
            <label>
              <span>姓名</span>
              <input value={profileDraft.name} onChange={(event) => setProfileDraft((current) => ({ ...current, name: event.target.value }))} />
            </label>
            <label>
              <span>性别</span>
              <select value={profileDraft.gender} onChange={(event) => setProfileDraft((current) => ({ ...current, gender: event.target.value }))}>
                {genderOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>生日</span>
              <input type="date" value={profileDraft.birthday} onChange={(event) => setProfileDraft((current) => ({ ...current, birthday: event.target.value }))} />
            </label>
            <label>
              <span>身高（cm）</span>
              <input
                type="number"
                min="100"
                max="240"
                value={profileDraft.heightCm}
                onChange={(event) => setProfileDraft((current) => ({ ...current, heightCm: event.target.value }))}
              />
            </label>
            <label>
              <span>目标尿酸（umol/L）</span>
              <input
                type="number"
                min="180"
                max="600"
                value={profileDraft.targetUricAcid}
                onChange={(event) => setProfileDraft((current) => ({ ...current, targetUricAcid: event.target.value }))}
              />
            </label>
            <label>
              <span>过敏信息</span>
              <input
                value={profileDraft.allergies}
                onChange={(event) => setProfileDraft((current) => ({ ...current, allergies: event.target.value }))}
                placeholder="多个条目用逗号分隔"
              />
            </label>
            <label>
              <span>合并症</span>
              <input
                value={profileDraft.comorbidities}
                onChange={(event) => setProfileDraft((current) => ({ ...current, comorbidities: event.target.value }))}
                placeholder="多个条目用逗号分隔"
              />
            </label>
            <label>
              <span>紧急联系人</span>
              <input
                value={profileDraft.emergencyContact}
                onChange={(event) => setProfileDraft((current) => ({ ...current, emergencyContact: event.target.value }))}
              />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.profile}>
              {busyMap.profile ? "保存中..." : "保存个人档案"}
            </button>
          </form>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Privacy consent</p>
              <h3>隐私与授权</h3>
            </div>
            <span className="inline-tag">{data.privacyConsentCurrent?.consentVersion || "未授权"}</span>
          </div>
          {data.privacyConsentCurrent ? (
            <>
              <div className="stats-grid stats-grid--compact">
                <div className="stat-line">
                  <span>当前版本</span>
                  <strong>{data.privacyConsentCurrent.consentVersion}</strong>
                </div>
                <div className="stat-line">
                  <span>隐私政策</span>
                  <strong>{data.privacyConsentCurrent.privacyPolicyVersion}</strong>
                </div>
                <div className="stat-line">
                  <span>生效时间</span>
                  <strong>{formatDateTime(data.privacyConsentCurrent.effectiveAt)}</strong>
                </div>
              </div>
              <form
                className="stack-form compact-form"
                onSubmit={(event) => {
                  event.preventDefault();
                  handlePrivacyConsentSubmit(privacyDraft);
                }}
              >
                <label className="checkbox-row">
                  <input type="checkbox" checked={privacyDraft.privacyAccepted} readOnly />
                  <span>已同意隐私政策</span>
                </label>
                <label className="checkbox-row">
                  <input type="checkbox" checked={privacyDraft.termsAccepted} readOnly />
                  <span>已同意服务条款</span>
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={privacyDraft.medicalDataAuthorized}
                    onChange={(event) => setPrivacyDraft((current) => ({ ...current, medicalDataAuthorized: event.target.checked }))}
                  />
                  <span>允许平台使用健康数据生成主动管理建议</span>
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={privacyDraft.familyCollaborationAuthorized}
                    onChange={(event) => setPrivacyDraft((current) => ({ ...current, familyCollaborationAuthorized: event.target.checked }))}
                  />
                  <span>允许后续启用家庭协同授权能力</span>
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={privacyDraft.notificationAuthorized}
                    onChange={(event) => setPrivacyDraft((current) => ({ ...current, notificationAuthorized: event.target.checked }))}
                  />
                  <span>允许接收风险提醒和随访通知</span>
                </label>
                <button className="primary-button" type="submit" disabled={!session || busyMap.privacyConsent}>
                  {busyMap.privacyConsent ? "保存中..." : "保存授权设置"}
                </button>
              </form>
            </>
          ) : (
            <EmptyState message="当前账号还没有正式隐私授权记录，mock 体验账号不会生成这部分数据。" />
          )}
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Consent history</p>
              <h3>授权历史</h3>
            </div>
          </div>
          <div className="stack-list">
            {data.privacyConsentHistory?.length ? (
              data.privacyConsentHistory.slice(0, 5).map((item) => (
                <article className="list-card" key={item.consentCode}>
                  <div className="result-header">
                    <strong>{item.consentVersion}</strong>
                    <span className="inline-tag">{item.sourceType}</span>
                  </div>
                  <p>隐私政策：{item.privacyPolicyVersion}</p>
                  <div className="list-card__meta">
                    <span>医疗数据：{item.medicalDataAuthorized ? "允许" : "关闭"}</span>
                    <span>家属协同：{item.familyCollaborationAuthorized ? "允许" : "关闭"}</span>
                  </div>
                  <div className="list-card__meta">
                    <span>通知提醒：{item.notificationAuthorized ? "允许" : "关闭"}</span>
                    <span>{formatDateTime(item.effectiveAt)}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无授权历史。" />
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Medication plan</p>
              <h3>用药计划</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleMedicationSubmit}>
            <label>
              <span>当前用药清单</span>
              <textarea
                rows="8"
                value={medicationDraft.lines}
                onChange={(event) => setMedicationDraft((current) => ({ ...current, lines: event.target.value }))}
                placeholder="每行一条，例如：别嘌醇 | 100mg | 每日一次 | 晚饭后 | 5 | 3"
              />
            </label>
            <p className="meta-text">格式：药名 | 剂量 | 频次 | 备注 | 剩余天数 | 提前提醒阈值。后两个字段可留空。</p>
            <label>
              <span>随访备注</span>
              <textarea
                rows="4"
                value={medicationDraft.followUpNote}
                onChange={(event) => setMedicationDraft((current) => ({ ...current, followUpNote: event.target.value }))}
                placeholder="记录不适反应、复查安排或医生建议"
              />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.medication}>
              {busyMap.medication ? "保存中..." : "保存用药计划"}
            </button>
          </form>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Medication adherence</p>
              <h3>用药依从概览</h3>
            </div>
            <span className="inline-tag">{data.medicationAdherence?.summaryDate || "今日"}</span>
          </div>
          {data.medicationAdherence ? (
            <>
              <div className="stats-grid medication-stats-grid">
                <div className="stat-line">
                  <span>计划剂次</span>
                  <strong>{data.medicationAdherence.plannedDoseCount || 0}</strong>
                </div>
                <div className="stat-line">
                  <span>已服用</span>
                  <strong>{data.medicationAdherence.takenDoseCount || 0}</strong>
                </div>
                <div className="stat-line">
                  <span>漏服</span>
                  <strong>{data.medicationAdherence.missedDoseCount || 0}</strong>
                </div>
                <div className="stat-line">
                  <span>跳过</span>
                  <strong>{data.medicationAdherence.skippedDoseCount || 0}</strong>
                </div>
                <div className="stat-line">
                  <span>依从率 / 连续天数</span>
                  <strong>{data.medicationAdherence.adherenceRate || 0}% / {data.medicationAdherence.currentStreakDays || 0} 天</strong>
                </div>
              </div>
              <div className="split-block">
                <div>
                  <ArraySummary
                    title="今日待确认"
                    items={data.medicationAdherence.overdueItems}
                    emptyMessage="今天的计划剂次已经全部确认。"
                  />
                </div>
                <div>
                  <BulletList title="下一步建议" items={data.medicationAdherence.nextActions} />
                </div>
              </div>
              <div className="stack-list medication-checkin-list">
                <strong className="subtle-title">最近打卡</strong>
                {data.medicationAdherence.recentCheckins?.length ? (
                  data.medicationAdherence.recentCheckins.map((item) => (
                    <article className="list-card" key={`${item.checkinId}-${item.checkinAt}`}>
                      <div className="result-header">
                        <div>
                          <strong>{item.medicationName}</strong>
                          <p>{getMedicationPeriodLabel(item.scheduledPeriod)} / {getMedicationStatusLabel(item.status)}</p>
                        </div>
                        <span className="inline-tag">{item.checkinDate}</span>
                      </div>
                      <p>{item.guidance || item.note || "暂无补充说明。"}</p>
                      <div className="list-card__meta">
                        <span>{item.note || "无备注"}</span>
                        <span>{formatDateTime(item.checkinAt)}</span>
                      </div>
                    </article>
                  ))
                ) : (
                  <EmptyState message="还没有用药打卡记录。" />
                )}
              </div>
            </>
          ) : (
            <EmptyState message="登录后会在这里展示今天的计划剂次、待确认项和最近打卡。" />
          )}
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Medication check-in</p>
              <h3>服药打卡</h3>
            </div>
          </div>
          <form className="stack-form compact-form" onSubmit={handleMedicationCheckinSubmit}>
            <label>
              <span>药物</span>
              <select
                value={medicationCheckinDraft.medicationName}
                onChange={(event) => setMedicationCheckinDraft((current) => ({ ...current, medicationName: event.target.value }))}
                disabled={!medicationItems.length}
              >
                {medicationItems.length ? (
                  medicationItems.map((item) => (
                    <option key={`${item.name}-${item.frequency}`} value={item.name}>
                      {item.name} / {item.dosage}
                    </option>
                  ))
                ) : (
                  <option value="">请先维护用药计划</option>
                )}
              </select>
            </label>
            <label>
              <span>时段</span>
              <select
                value={medicationCheckinDraft.scheduledPeriod}
                onChange={(event) => setMedicationCheckinDraft((current) => ({ ...current, scheduledPeriod: event.target.value }))}
                disabled={!selectedMedication}
              >
                {medicationPeriodOptions.map((period) => (
                  <option key={period} value={period}>
                    {getMedicationPeriodLabel(period)}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>状态</span>
              <select
                value={medicationCheckinDraft.status}
                onChange={(event) => setMedicationCheckinDraft((current) => ({ ...current, status: event.target.value }))}
                disabled={!selectedMedication}
              >
                <option value="TAKEN">已服用</option>
                <option value="MISSED">漏服</option>
                <option value="SKIPPED">跳过</option>
              </select>
            </label>
            <label>
              <span>备注</span>
              <textarea
                rows="4"
                value={medicationCheckinDraft.note}
                onChange={(event) => setMedicationCheckinDraft((current) => ({ ...current, note: event.target.value }))}
                placeholder="例如：早餐后服用、因胃部不适暂缓、忘记携带药物"
              />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.medicationCheckin || !selectedMedication}>
              {busyMap.medicationCheckin ? "提交中..." : "提交本次打卡"}
            </button>
          </form>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Files</p>
              <h3>文件上传</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleFileUpload}>
            <label>
              <span>选择文件</span>
              <input name="file" type="file" />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.fileUpload}>
              {busyMap.fileUpload ? "上传中..." : "上传文件"}
            </button>
          </form>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Recent uploads</p>
              <h3>最近上传</h3>
            </div>
          </div>
          <div className="masonry-list">
            {data.uploadedFiles?.length ? (
              data.uploadedFiles.map((item) => (
                <article className="list-card" key={item.fileId}>
                  <div className="result-header">
                    <strong>{item.fileName}</strong>
                    <span className="inline-tag">{item.contentType || "file"}</span>
                  </div>
                  <p>文件大小：{item.size} bytes</p>
                  <div className="action-row">
                    <button className="ghost-button action-button" type="button" onClick={() => handleOpenFile(item.fileId)}>
                      打开文件
                    </button>
                    <span className="token token--tiny">{item.fileId}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无已上传文件。" />
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Lab OCR</p>
              <h3>化验单解读</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleLabSubmit}>
            <label>
              <span>报告文件（图片或 PDF）</span>
              <input name="file" type="file" accept="image/*,.pdf" />
            </label>
            <label>
              <span>报告日期</span>
              <input name="reportDate" type="date" />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.lab}>
              {busyMap.lab ? "解析中..." : "开始解析"}
            </button>
          </form>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Latest lab report</p>
              <h3>最近一次化验单结果</h3>
            </div>
          </div>
          <LabReportSelector app={app} data={data} busyMap={busyMap} withErrorHandling={withErrorHandling} />
          <div className={`result-panel ${data.labResult ? "" : "empty-panel"}`}>
            {data.labResult ? (
              <>
                <div className="result-header">
                  <div>
                    <h4>{formatDate(data.labResult.reportDate)}</h4>
                    <p>报告 ID：{data.labResult.reportId}</p>
                  </div>
                  <RiskBadge level={data.labResult.overallRiskLevel} />
                </div>
                <p>{data.labResult.summary || "暂无解析摘要。"}</p>
                <div className="indicator-grid">
                  {(data.labResult.indicators || []).map((item) => (
                    <div className="indicator-chip" key={`${item.code}-${item.name}`}>
                      <span>{item.name || item.code}</span>
                      <strong>{item.value != null ? `${item.value} ${item.unit || ""}` : "暂无"}</strong>
                      <small>{item.referenceRange || "无参考范围"} / {item.riskLevel}</small>
                    </div>
                  ))}
                </div>
                <BulletList title="建议动作" items={data.labResult.suggestions} />
                {data.labReview ? (
                  <>
                    <div className="stats-grid stats-grid--compact">
                      <div className="stat-line">
                        <span>目标尿酸</span>
                        <strong>{data.labReview.targetUricAcidValue || "-"} {data.labReview.currentUricAcidUnit || ""}</strong>
                      </div>
                      <div className="stat-line">
                        <span>本次尿酸</span>
                        <strong>{data.labReview.currentUricAcidValue != null ? `${data.labReview.currentUricAcidValue} ${data.labReview.currentUricAcidUnit || ""}` : "未识别"}</strong>
                      </div>
                      <div className="stat-line">
                        <span>与上次间隔</span>
                        <strong>{data.labReview.daysBetweenReports != null ? `${data.labReview.daysBetweenReports} 天` : "暂无基线"}</strong>
                      </div>
                    </div>
                    <p className="narrative-text">{data.labReview.reviewSummary}</p>
                    <p className="narrative-text">{data.labReview.targetConclusion}</p>
                    <ArraySummary title="关键变化" items={data.labReview.keyChanges} emptyMessage="暂无关键变化。" />
                    <div className="indicator-grid">
                      {(data.labReview.comparisons || []).map((item) => (
                        <div className="indicator-chip" key={`${item.code}-${item.name}-review`}>
                          <span>{item.name || item.code}</span>
                          <strong>
                            {item.currentValue != null ? `${item.currentValue} ${item.unit || ""}` : "暂无"}
                            {item.previousValue != null ? ` / 上次 ${item.previousValue}` : ""}
                          </strong>
                          <small>{item.trend} / {item.currentRiskLevel}</small>
                          <small>{item.interpretation}</small>
                        </div>
                      ))}
                    </div>
                    <BulletList title="复查建议" items={data.labReview.followUpRecommendation ? [data.labReview.followUpRecommendation] : []} />
                    <BulletList title="下一步三件事" items={data.labReview.nextActions} />
                    <BulletList title="可信边界" items={data.labReview.trustNotes} />
                  </>
                ) : null}
              </>
            ) : (
              "上传化验单后，这里会展示重点指标、风险判断和建议动作。"
            )}
          </div>
        </Card>
      </div>

      {familyFeatureEnabled ? (
        <>
          <div className="overview-grid overview-grid--secondary">
            <Card>
              <div className="card-head">
                <div>
                  <p className="eyebrow">Family invite</p>
                  <h3>家属协同入口</h3>
                </div>
              </div>
              <form className="stack-form" onSubmit={handleInviteSubmit}>
                <label>
                  <span>关系类型</span>
                  <select value={inviteDraft.relationType} onChange={(event) => setInviteDraft((current) => ({ ...current, relationType: event.target.value }))}>
                    {familyRelationOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  <span>邀请留言</span>
                  <textarea
                    rows="4"
                    value={inviteDraft.inviteMessage}
                    onChange={(event) => setInviteDraft((current) => ({ ...current, inviteMessage: event.target.value }))}
                    placeholder="补充协同目的、照护方式或使用说明"
                  />
                </label>
                <label>
                  <span>有效天数</span>
                  <input
                    type="number"
                    min="1"
                    max="30"
                    value={inviteDraft.expiresInDays}
                    onChange={(event) => setInviteDraft((current) => ({ ...current, expiresInDays: event.target.value }))}
                  />
                </label>
                <button className="primary-button" type="submit" disabled={!session || busyMap.familyInvite}>
                  {busyMap.familyInvite ? "创建中..." : "创建家属邀请"}
                </button>
              </form>
              <form className="stack-form" onSubmit={handleAcceptInvite}>
                <label>
                  <span>接受邀请码</span>
                  <input value={acceptInviteCode} onChange={(event) => setAcceptInviteCode(event.target.value)} placeholder="输入收到的邀请码" />
                </label>
                <button className="ghost-button" type="submit" disabled={!session || busyMap.acceptInvite}>
                  {busyMap.acceptInvite ? "处理中..." : "接受家庭邀请"}
                </button>
              </form>
            </Card>

            <Card className="span-2">
              <div className="card-head">
                <div>
                  <p className="eyebrow">Family patient summary</p>
                  <h3>家属患者摘要</h3>
                </div>
                <span className="inline-tag">
                  {familySummaryTargetName || "待加载"}
                </span>
              </div>
              <div className={`result-panel ${data.familyPatientSummary ? "" : "empty-panel"}`}>
                {data.familyPatientSummary ? (
                  <>
                    <div className="result-header">
                      <div>
                        <h4>{familySummaryTargetName || "患者摘要"}</h4>
                        <p>{data.familyPatientSummary.latestRiskSummary || "暂无风险摘要。"}</p>
                      </div>
                      <RiskBadge level={data.familyPatientSummary.overallRiskLevel} />
                    </div>
                    <ArraySummary title="今日关注" items={data.familyPatientSummary.todayFocus} emptyMessage="暂无今日关注。" />
                    <ArraySummary title="下一步建议" items={data.familyPatientSummary.nextActions} emptyMessage="暂无下一步建议。" />
                    <div className="stack-list">
                      <strong className="subtle-title">提醒</strong>
                      {data.familyPatientSummary.reminders?.length ? (
                        data.familyPatientSummary.reminders.map((item) => (
                          <article className="list-card" key={item.reminderId}>
                            <div className="result-header">
                              <strong>{item.title}</strong>
                              <RiskBadge level={item.riskLevel} />
                            </div>
                            <p>{item.content}</p>
                          </article>
                        ))
                      ) : (
                        <EmptyState message="暂无患者提醒。" />
                      )}
                    </div>
                  </>
                ) : (
                  "当你以家属身份绑定患者后，可以在这里查看风险摘要、今日关注和提醒。"
                )}
              </div>
            </Card>
          </div>

          <div className="overview-grid overview-grid--secondary">
            <Card>
              <div className="card-head">
                <div>
                  <p className="eyebrow">Family alerts</p>
                  <h3>轻量提醒</h3>
                </div>
              </div>
              <div className="stack-list">
                {data.familyAlerts?.length ? (
                  data.familyAlerts.map((item) => (
                    <article className="list-card" key={item.alertId}>
                      <div className="result-header">
                        <strong>{item.patientNickname}</strong>
                        <RiskBadge level={item.riskLevel} />
                      </div>
                      <p>{item.title}</p>
                      <div className="list-card__meta">
                        <span>{item.sourceType}</span>
                        <span>{formatDateTime(item.generatedAt)}</span>
                      </div>
                    </article>
                  ))
                ) : (
                  <EmptyState message="暂无家属提醒。" />
                )}
              </div>
            </Card>

            <Card className="span-2">
              <div className="card-head">
                <div>
                  <p className="eyebrow">Family members</p>
                  <h3>当前家庭关系</h3>
                </div>
              </div>
              <div className="split-block">
                <div>
                  <MemberList
                    title="我作为患者"
                    items={data.familyMembers?.asPatient || []}
                    busyMap={busyMap}
                    onViewSummary={null}
                    onRemove={(bindingCode) => withErrorHandling(() => app.unbindFamilyMember(bindingCode))}
                  />
                </div>
                <div>
                  <MemberList
                    title="我作为家属"
                    items={data.familyMembers?.asCaregiver || []}
                    busyMap={busyMap}
                    onViewSummary={(patientUserId) => withErrorHandling(() => app.loadFamilySummary(patientUserId))}
                    onRemove={(bindingCode) => withErrorHandling(() => app.unbindFamilyMember(bindingCode))}
                  />
                </div>
              </div>
            </Card>
          </div>
        </>
      ) : null}
    </section>
  );
}
