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

function getLabExtractionStatusLabel(status) {
  switch (status) {
    case "MANUAL_CONFIRMED":
      return "已人工补录";
    case "MANUAL_CONFIRMATION_REQUIRED":
      return "待人工确认";
    case "OCR_EXTRACTED":
    case "EXTRACTED":
      return "已提取";
    case "PROCESSING":
      return "解析中";
    default:
      return status || "待处理";
  }
}

function getLabReviewStatusLabel(status, reviewReady) {
  if (reviewReady) {
    return "可复盘";
  }

  switch (status) {
    case "MANUAL_CONFIRMATION_REQUIRED":
      return "待人工确认";
    case "PENDING_BASELINE":
      return "待建立基线";
    case "PENDING_REVIEW":
      return "待复盘";
    default:
      return status || "待复盘";
  }
}

function getLabWorkflowTitleLabel(title) {
  switch (title) {
    case "LAB_MANUAL_CONFIRMATION_PENDING":
      return "人工确认待处理";
    case "LAB_REVIEW_READY":
      return "正式复盘已就绪";
    default:
      return title || "复盘工作流";
  }
}

function getLabManualTaskStatusLabel(status) {
  switch (status) {
    case "DO_NOW":
      return "现在先做";
    case "NEXT":
      return "然后处理";
    case "FOLLOW_UP":
      return "确认后再做";
    case "DONE":
      return "已完成";
    default:
      return status || "待处理";
  }
}

function getLabVerificationStageLabel(stage) {
  switch (stage) {
    case "MANUAL_CONFIRMATION_REQUIRED":
      return "待人工确认";
    case "MANUAL_CONFIRMED":
      return "已人工确认";
    case "OCR_EXTRACTED":
      return "已 OCR 提取";
    default:
      return stage || "待核验";
  }
}

function getTrustTimelineStatusLabel(status) {
  switch (status) {
    case "DONE":
      return "已完成";
    case "IN_PROGRESS":
      return "进行中";
    case "PENDING":
      return "待处理";
    default:
      return status || "待处理";
  }
}

function getLabFieldSourceLabel(sourceType) {
  switch (sourceType) {
    case "MANUAL_CONFIRMATION":
      return "人工确认";
    case "OCR_EXTRACTED":
      return "OCR 提取";
    default:
      return sourceType || "待核验";
  }
}

function getLabFieldVerificationStatusLabel(status) {
  switch (status) {
    case "VERIFIED":
      return "已核验";
    case "OCR_READY":
      return "可直接复盘";
    case "REVIEW_RECOMMENDED":
      return "建议复核";
    default:
      return status || "待处理";
  }
}

function createLabManualIndicatorDraft() {
  return {
    code: "",
    name: "",
    value: "",
    unit: "umol/L",
    referenceRange: "",
    riskLevel: "YELLOW",
  };
}

function getAccountSecurityStateLabel(authUiState) {
  if (authUiState?.loginLockedMessage) {
    return "登录已锁定";
  }

  if (authUiState?.passwordResetCooldownMessage || authUiState?.accountVerificationCooldownMessage) {
    return "存在限流保护";
  }

  return "当前正常";
}

function getAccountSecurityStateTone(authUiState) {
  if (authUiState?.loginLockedMessage) {
    return "risk-red";
  }

  if (authUiState?.passwordResetCooldownMessage || authUiState?.accountVerificationCooldownMessage) {
    return "risk-yellow";
  }

  return "risk-green";
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
            <div className="list-card__meta">
              <span>{getLabExtractionStatusLabel(item.extractionStatus)}</span>
              <span>{item.reviewReady ? "复盘已就绪" : "复盘未就绪"}</span>
            </div>
            <div className="action-row">
              {activeReportId === item.reportId ? <span className="inline-tag">当前查看</span> : null}
              {item.manualConfirmationRequired ? <span className="inline-tag risk-yellow">待人工确认</span> : null}
              <button
                className="ghost-button action-button"
                type="button"
                disabled={busyMap.labReview}
                onClick={() => withErrorHandling(() => app.loadLabReportReview(item.reportId))}
              >
                {busyMap.labReview
                  ? "加载中..."
                  : item.manualConfirmationRequired
                    ? "查看确认说明"
                    : activeReportId === item.reportId
                      ? "刷新复盘"
                      : "查看复盘"}
              </button>
            </div>
          </article>
        ))}
      </div>
      {reports.length > 4 ? <p className="meta-text">当前仅展示最近 4 份报告，可继续通过刷新数据同步最新化验单。</p> : null}
    </div>
  );
}

function LabManualTaskGuide({ tasks, blockedOutputs }) {
  const taskItems = tasks || [];
  const blockedItems = blockedOutputs || [];

  if (!taskItems.length && !blockedItems.length) {
    return null;
  }

  return (
    <div className="stack-list lab-manual-guide">
      {taskItems.length ? (
        <div className="stack-list">
          <strong className="subtle-title">先按这个顺序处理</strong>
          {taskItems.map((item, index) => (
            <article className="list-card lab-task-card" key={item.actionKey || `${item.title}-${index}`}>
              <div className="result-header">
                <div>
                  <strong>{index + 1}. {item.title}</strong>
                  <p>{item.description}</p>
                </div>
                <span className={`inline-tag ${item.priority === "HIGH" ? "risk-yellow" : ""}`}>
                  {getLabManualTaskStatusLabel(item.status)}
                </span>
              </div>
            </article>
          ))}
        </div>
      ) : null}
      {blockedItems.length ? <BulletList title="确认前系统暂不输出" items={blockedItems} /> : null}
    </div>
  );
}

function LabTrustMetaPanel({ trustMeta }) {
  if (!trustMeta) {
    return null;
  }

  return (
    <div className="stack-list lab-trust-panel">
      <div className="result-header">
        <div>
          <strong>可信链路</strong>
          <p>把来源、核验阶段和人工确认历史放在同一处，避免未核验结果混入正式复盘。</p>
        </div>
        <span className={`inline-tag ${trustMeta.verificationStage === "MANUAL_CONFIRMATION_REQUIRED" ? "risk-yellow" : "risk-green"}`}>
          {getLabVerificationStageLabel(trustMeta.verificationStage)}
        </span>
      </div>
      <div className="stats-grid stats-grid--compact">
        <div className="stat-line">
          <span>报告来源</span>
          <strong>{trustMeta.documentSourceLabel || "未标记"}</strong>
        </div>
        <div className="stat-line">
          <span>原始文件</span>
          <strong>{trustMeta.originalFileAttached ? "已保留" : "未保留"}</strong>
        </div>
        <div className="stat-line">
          <span>人工确认时间</span>
          <strong>{trustMeta.manualConfirmedAt ? formatDateTime(trustMeta.manualConfirmedAt) : "尚未确认"}</strong>
        </div>
      </div>
      <div className="list-card__meta">
        <span>文件名：{trustMeta.originalFileName || "未记录"}</span>
        <span>机构来源：{trustMeta.institutionSourceLabel || "待补充"}</span>
      </div>
      {trustMeta.lockedSections?.length ? (
        <BulletList title="当前锁定输出" items={trustMeta.lockedSections} />
      ) : null}
      {trustMeta.fieldConfidenceItems?.length ? (
        <div className="stack-list">
          <strong className="subtle-title">字段级置信度</strong>
          <div className="indicator-grid">
            {trustMeta.fieldConfidenceItems.map((item) => (
              <article className="indicator-chip" key={`${item.code}-${item.name}-${item.sourceType}`}>
                <span>{item.name || item.code}</span>
                <strong>{item.confidenceScore != null ? `${item.confidenceScore} 分` : "待评估"}</strong>
                <small>{getLabFieldSourceLabel(item.sourceType)} / {getLabFieldVerificationStatusLabel(item.verificationStatus)}</small>
                <small>{item.note}</small>
              </article>
            ))}
          </div>
        </div>
      ) : null}
      {trustMeta.confirmationHistory?.length ? (
        <div className="stack-list">
          <strong className="subtle-title">确认历史</strong>
          {trustMeta.confirmationHistory.map((item) => (
            <article className="list-card trust-timeline-card" key={`${item.eventKey}-${item.occurredAt || item.title}`}>
              <div className="result-header">
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.detail}</p>
                </div>
                <span className={`inline-tag ${item.status === "DONE" ? "risk-green" : "risk-yellow"}`}>
                  {getTrustTimelineStatusLabel(item.status)}
                </span>
              </div>
              {item.occurredAt ? <p className="meta-text">{formatDateTime(item.occurredAt)}</p> : null}
            </article>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function LabDoctorSummaryPanel({ doctorSummary }) {
  if (!doctorSummary) {
    return null;
  }

  return (
    <div className="stack-list lab-doctor-summary">
      <div className="result-header">
        <div>
          <strong>给医生的结构化摘要</strong>
          <p>先把本次结果、关键变化和复诊诉求整理好，再决定是否直接分享给医生。</p>
        </div>
        <span className={`inline-tag ${doctorSummary.readyToShare ? "risk-green" : "risk-yellow"}`}>
          {doctorSummary.readyToShare ? "可分享" : "暂不建议直接分享"}
        </span>
      </div>
      <p className="narrative-text">{doctorSummary.shareSummary || "当前暂无可分享摘要。"}</p>
      <ArraySummary title="关键发现" items={doctorSummary.keyFindings} emptyMessage="当前暂无关键发现。" />
      <ArraySummary title="建议向医生确认" items={doctorSummary.careRequests} emptyMessage="当前暂无需额外确认的问题。" />
      <BulletList title="分享前提醒" items={doctorSummary.trustNotes} />
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
  handleRequestAccountVerificationCode,
  handleConfirmAccountVerification,
  authUiState,
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
  const [accountVerificationCode, setAccountVerificationCode] = useState("");
  const [labManualDraft, setLabManualDraft] = useState({
    indicators: [
      {
        ...createLabManualIndicatorDraft(),
        code: "UA",
        name: "尿酸",
        referenceRange: "208-428",
      },
    ],
    summaryNote: "",
  });
  const medicationItems = data.medication?.currentMedications || [];
  const selectedMedication = medicationItems.find((item) => item.name === medicationCheckinDraft.medicationName) || medicationItems[0] || null;
  const medicationPeriodOptions = selectedMedication ? getMedicationPeriods(selectedMedication.frequency) : ["MORNING"];
  const accountVerificationStatus = data.accountVerificationStatus || {
    accountType: data.authSession?.accountType || session?.accountType || "",
    accountIdentifier: data.authSession?.accountIdentifier || session?.accountIdentifier || "",
    verified: Boolean(data.authSession?.accountVerified ?? session?.accountVerified),
    verifiedAt: null,
    message: "",
  };

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

  async function handleLabManualConfirm(event) {
    event.preventDefault();

    if (!data.labResult?.reportId) {
      app.setBanner({ tone: "warning", message: "当前没有可补录的化验单，请先选择一份待人工确认的报告。" });
      return;
    }

    const sanitizedIndicators = (labManualDraft.indicators || [])
      .map((item) => ({
        code: item.code.trim(),
        name: item.name.trim(),
        value: item.value === "" ? "" : Number(item.value),
        unit: item.unit.trim(),
        referenceRange: item.referenceRange.trim(),
        riskLevel: item.riskLevel,
      }))
      .filter((item) => item.code || item.name || item.value !== "" || item.unit || item.referenceRange);

    if (!sanitizedIndicators.length) {
      app.setBanner({ tone: "warning", message: "请至少补录一项关键指标。" });
      return;
    }

    if (sanitizedIndicators.some((item) => !item.name || !item.code || !item.unit || item.value === "" || Number.isNaN(item.value))) {
      app.setBanner({ tone: "warning", message: "请先补全每一项指标的名称、编码、数值和单位。" });
      return;
    }

    await withErrorHandling(async () => {
      await app.submitLabManualConfirmation(data.labResult.reportId, {
        indicators: sanitizedIndicators.map((item) => ({
          code: item.code,
          name: item.name,
          value: item.value,
          unit: item.unit,
          referenceRange: item.referenceRange || null,
          riskLevel: item.riskLevel,
        })),
        summaryNote: labManualDraft.summaryNote.trim() || null,
      });
      setLabManualDraft({
        indicators: [
          {
            ...createLabManualIndicatorDraft(),
            code: "UA",
            name: "尿酸",
            referenceRange: "208-428",
          },
        ],
        summaryNote: "",
      });
    });
  }

  function updateLabManualIndicator(index, key, value) {
    setLabManualDraft((current) => ({
      ...current,
      indicators: current.indicators.map((item, itemIndex) => (
        itemIndex === index ? { ...item, [key]: value } : item
      )),
    }));
  }

  function addLabManualIndicator() {
    setLabManualDraft((current) => ({
      ...current,
      indicators: [...current.indicators, createLabManualIndicatorDraft()],
    }));
  }

  function removeLabManualIndicator(index) {
    setLabManualDraft((current) => ({
      ...current,
      indicators: current.indicators.length === 1
        ? [createLabManualIndicatorDraft()]
        : current.indicators.filter((_, itemIndex) => itemIndex !== index),
    }));
  }

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
                <div className="stat-line">
                  <span>当前设备</span>
                  <strong>{data.authSession?.deviceLabel || session.deviceLabel || "-"}</strong>
                </div>
              </div>
              <div className="action-row">
                <span className={`inline-tag ${accountVerificationStatus.verified ? "risk-green" : "risk-yellow"}`}>
                  {accountVerificationStatus.verified ? "账号已验证" : "账号待验证"}
                </span>
                <span className={`inline-tag ${(data.authSession?.privacyConsentCompleted ?? session.privacyConsentCompleted) ? "risk-green" : "risk-yellow"}`}>
                  {(data.authSession?.privacyConsentCompleted ?? session.privacyConsentCompleted) ? "授权已完成" : "授权待完善"}
                </span>
                <span className="inline-tag">风险 {data.authSession?.loginRiskLevel || session.loginRiskLevel || "GREEN"}</span>
              </div>
              <div className="session-card">
                <div className="result-header">
                  <div>
                    <strong>安全状态摘要</strong>
                    <p>
                      {authUiState?.loginLockedMessage
                        || authUiState?.accountVerificationCooldownMessage
                        || authUiState?.passwordResetCooldownMessage
                        || "当前未触发登录锁定或验证码限流，账户安全链路处于正常状态。"}
                    </p>
                  </div>
                  <span className={`inline-tag ${getAccountSecurityStateTone(authUiState)}`}>
                    {getAccountSecurityStateLabel(authUiState)}
                  </span>
                </div>
                <div className="stats-grid stats-grid--compact">
                  <div className="stat-line">
                    <span>登录锁定</span>
                    <strong>{authUiState?.loginLockedMessage ? "已触发" : "未触发"}</strong>
                  </div>
                  <div className="stat-line">
                    <span>找回密码限流</span>
                    <strong>{authUiState?.passwordResetCooldownMessage ? "冷却中" : "正常"}</strong>
                  </div>
                  <div className="stat-line">
                    <span>账号验证限流</span>
                    <strong>{authUiState?.accountVerificationCooldownMessage ? "冷却中" : "正常"}</strong>
                  </div>
                </div>
                {authUiState?.loginLockedAt ? <p className="meta-text">最近锁定：{formatDateTime(authUiState.loginLockedAt)}</p> : null}
                {authUiState?.passwordResetCooldownAt ? <p className="meta-text">找回密码限流触发：{formatDateTime(authUiState.passwordResetCooldownAt)}</p> : null}
                {authUiState?.accountVerificationCooldownAt ? <p className="meta-text">账号验证限流触发：{formatDateTime(authUiState.accountVerificationCooldownAt)}</p> : null}
                {authUiState?.lastErrorCode ? <p className="meta-text">最近后端状态码：{authUiState.lastErrorCode}</p> : null}
              </div>
              <div className="session-card">
                <div className="result-header">
                  <div>
                    <strong>账号验证状态</strong>
                    <p>{accountVerificationStatus.accountIdentifier || data.authSession?.accountIdentifier || "当前正式账号"}</p>
                  </div>
                  <span className={`inline-tag ${accountVerificationStatus.verified ? "risk-green" : "risk-yellow"}`}>
                    {accountVerificationStatus.verified ? "已验证" : "待验证"}
                  </span>
                </div>
                <p className="narrative-text">
                  {accountVerificationStatus.verified
                    ? accountVerificationStatus.message || "当前账号已完成验证，可作为后续安全提醒、找回密码和风险通知的可信触点。"
                    : accountVerificationStatus.message || "建议先完成一次账号验证，后续再逐步接入短信或邮件真实投递能力。"}
                </p>
                {accountVerificationStatus.verifiedAt ? (
                  <p className="meta-text">完成时间：{formatDateTime(accountVerificationStatus.verifiedAt)}</p>
                ) : null}
                {!accountVerificationStatus.verified ? (
                  <div className="stack-form compact-form">
                    <div className="action-row">
                      <button
                        className="ghost-button"
                        type="button"
                        disabled={!session || busyMap.accountVerificationRequest}
                        onClick={handleRequestAccountVerificationCode}
                      >
                        {busyMap.accountVerificationRequest ? "发送中..." : "发送验证验证码"}
                      </button>
                      <span className="meta-text">
                        {authUiState?.accountVerificationCooldownMessage || "当前为安全骨架，联调验证码会在页面提示条中展示。"}
                      </span>
                    </div>
                    {authUiState?.accountVerificationCooldownAt ? (
                      <p className="meta-text">最近限流：{formatDateTime(authUiState.accountVerificationCooldownAt)}</p>
                    ) : null}
                    <label>
                      <span>验证码</span>
                      <input
                        value={accountVerificationCode}
                        onChange={(event) => setAccountVerificationCode(event.target.value)}
                        placeholder="输入收到的 6 位验证码"
                      />
                    </label>
                    <button
                      className="primary-button"
                      type="button"
                      disabled={!session || busyMap.accountVerificationConfirm}
                      onClick={async () => {
                        const verified = await handleConfirmAccountVerification(accountVerificationCode);
                        if (verified) {
                          setAccountVerificationCode("");
                        }
                      }}
                    >
                      {busyMap.accountVerificationConfirm ? "验证中..." : "确认完成账号验证"}
                    </button>
                  </div>
                ) : null}
                <BulletList title="安全提示" items={data.authSession?.securityNotices} />
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
                  <div className="list-card__meta">
                    <span>{item.deviceLabel || "未识别设备"}</span>
                    <span>{item.clientIpMasked || "-"}</span>
                  </div>
                  <div className="list-card__meta">
                    <span>风险等级：{item.loginRiskLevel || "GREEN"}</span>
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
                <div className="stats-grid stats-grid--compact">
                  <div className="stat-line">
                    <span>提取状态</span>
                    <strong>{getLabExtractionStatusLabel(data.labResult.extractionStatus)}</strong>
                  </div>
                  <div className="stat-line">
                    <span>复盘状态</span>
                    <strong>{getLabReviewStatusLabel(data.labReview?.reviewStatus, data.labReview?.reviewReady)}</strong>
                  </div>
                  <div className="stat-line">
                    <span>人工确认</span>
                    <strong>{data.labResult.manualConfirmationRequired ? "需要" : "不需要"}</strong>
                  </div>
                </div>
                {data.labReview?.workflowTitle ? (
                  <div className="action-row">
                    <span className={`inline-tag ${data.labReview.reviewReady ? "risk-green" : "risk-yellow"}`}>
                      {getLabWorkflowTitleLabel(data.labReview.workflowTitle)}
                    </span>
                  </div>
                ) : null}
                {data.labResult.manualConfirmationRequired ? (
                  <div className="result-panel">
                    <p className="narrative-text">
                      当前报告已进入人工确认模式。系统不会再使用估算值补全指标，也不会把这份报告直接纳入目标值判断和趋势复盘。
                    </p>
                    <p className="meta-text">
                      当前提取状态：{getLabExtractionStatusLabel(data.labResult.extractionStatus)} / 复盘状态：{getLabReviewStatusLabel(data.labReview?.reviewStatus, data.labReview?.reviewReady)}
                    </p>
                  </div>
                ) : (
                  <div className="indicator-grid">
                    {(data.labResult.indicators || []).map((item) => (
                      <div className="indicator-chip" key={`${item.code}-${item.name}`}>
                        <span>{item.name || item.code}</span>
                        <strong>{item.value != null ? `${item.value} ${item.unit || ""}` : "暂无"}</strong>
                        <small>{item.referenceRange || "无参考范围"} / {item.riskLevel}</small>
                      </div>
                    ))}
                  </div>
                )}
                <BulletList title="建议动作" items={data.labResult.suggestions} />
                <BulletList title="可信边界" items={data.labResult.trustNotes} />
                {data.labReview ? (
                  <>
                    {data.labReview.reviewReady ? (
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
                        <div className="list-card__meta">
                          <span>复盘状态：{getLabReviewStatusLabel(data.labReview.reviewStatus, data.labReview.reviewReady)}</span>
                          <span>工作流：{getLabWorkflowTitleLabel(data.labReview.workflowTitle)}</span>
                        </div>
                        <div className="list-card__meta">
                          <span>
                            对比报告：
                            {data.labReview.comparedReportDate ? formatDate(data.labReview.comparedReportDate) : "暂无"}
                          </span>
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
                        <LabTrustMetaPanel trustMeta={data.labReview.trustMeta} />
                        <LabDoctorSummaryPanel doctorSummary={data.labReview.doctorSummary} />
                      </>
                    ) : (
                      <>
                        <div className="action-row">
                          <span className="inline-tag">{getLabWorkflowTitleLabel(data.labReview.workflowTitle)}</span>
                          <span className="inline-tag risk-yellow">{getLabReviewStatusLabel(data.labReview.reviewStatus, data.labReview.reviewReady)}</span>
                          {data.labReview.manualConfirmationRequired ? <span className="inline-tag">等待补充清晰报告</span> : null}
                        </div>
                        <p className="narrative-text">{data.labReview.reviewSummary}</p>
                        <p className="narrative-text">{data.labReview.targetConclusion}</p>
                        <LabManualTaskGuide
                          tasks={data.labReview.manualConfirmationTasks}
                          blockedOutputs={data.labReview.blockedOutputs}
                        />
                        <form className="stack-form compact-form" onSubmit={handleLabManualConfirm}>
                          <div className="result-header">
                            <div>
                              <strong>手动补录关键指标</strong>
                              <p>支持一次补录多项关键指标。先把尿酸和本次异常项补齐，再切回正式复盘链路。</p>
                            </div>
                            <span className="inline-tag risk-yellow">优先补录</span>
                          </div>
                          <div className="stack-list">
                            {labManualDraft.indicators.map((indicator, index) => (
                              <article className="list-card lab-manual-indicator-row" key={`lab-manual-${index}`}>
                                <div className="result-header">
                                  <div>
                                    <strong>指标 {index + 1}</strong>
                                    <p>按原始化验单逐项补录，至少保证名称、编码、数值和单位完整。</p>
                                  </div>
                                  <div className="action-row">
                                    {labManualDraft.indicators.length > 1 ? (
                                      <button
                                        className="ghost-button action-button"
                                        type="button"
                                        onClick={() => removeLabManualIndicator(index)}
                                      >
                                        删除这一项
                                      </button>
                                    ) : null}
                                  </div>
                                </div>
                                <label>
                                  <span>指标名称</span>
                                  <input
                                    value={indicator.name}
                                    onChange={(event) => updateLabManualIndicator(index, "name", event.target.value)}
                                    placeholder="例如：尿酸"
                                  />
                                </label>
                                <label>
                                  <span>指标编码</span>
                                  <input
                                    value={indicator.code}
                                    onChange={(event) => updateLabManualIndicator(index, "code", event.target.value)}
                                    placeholder="例如：UA"
                                  />
                                </label>
                                <label>
                                  <span>数值</span>
                                  <input
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    value={indicator.value}
                                    onChange={(event) => updateLabManualIndicator(index, "value", event.target.value)}
                                    placeholder="例如：428"
                                  />
                                </label>
                                <label>
                                  <span>单位</span>
                                  <input
                                    value={indicator.unit}
                                    onChange={(event) => updateLabManualIndicator(index, "unit", event.target.value)}
                                    placeholder="例如：umol/L"
                                  />
                                </label>
                                <label>
                                  <span>参考范围</span>
                                  <input
                                    value={indicator.referenceRange}
                                    onChange={(event) => updateLabManualIndicator(index, "referenceRange", event.target.value)}
                                    placeholder="例如：208-428"
                                  />
                                </label>
                                <label>
                                  <span>风险等级</span>
                                  <select
                                    value={indicator.riskLevel}
                                    onChange={(event) => updateLabManualIndicator(index, "riskLevel", event.target.value)}
                                  >
                                    <option value="GREEN">低风险 / 正常</option>
                                    <option value="YELLOW">中风险 / 偏高</option>
                                    <option value="RED">高风险 / 明显异常</option>
                                  </select>
                                </label>
                              </article>
                            ))}
                          </div>
                          <div className="action-row">
                            <button className="ghost-button action-button" type="button" onClick={addLabManualIndicator}>
                              新增一项指标
                            </button>
                            <span className="meta-text">建议至少补录尿酸和本次异常指标，减少后续医生复核成本。</span>
                          </div>
                          <label>
                            <span>补录备注</span>
                            <textarea
                              rows="3"
                              value={labManualDraft.summaryNote}
                              onChange={(event) => setLabManualDraft((current) => ({ ...current, summaryNote: event.target.value }))}
                              placeholder="例如：依据原始化验单手动核对，OCR 未识别出尿酸这一项。"
                            />
                          </label>
                          <div className="action-row">
                            <button
                              className="primary-button"
                              type="submit"
                              disabled={!session || busyMap.labManualConfirm || !data.labResult?.reportId}
                            >
                              {busyMap.labManualConfirm ? "提交中..." : "确认补录并生成复盘"}
                            </button>
                            <span className="meta-text">补录成功后，这份报告会自动切换到正式复盘结果。</span>
                          </div>
                        </form>
                        <ArraySummary title="当前状态" items={data.labReview.keyChanges} emptyMessage="当前暂无可复盘指标。" />
                        <BulletList title="下一步三件事" items={data.labReview.nextActions} />
                        <BulletList title="可信边界" items={data.labReview.trustNotes} />
                        <LabTrustMetaPanel trustMeta={data.labReview.trustMeta} />
                        <LabDoctorSummaryPanel doctorSummary={data.labReview.doctorSummary} />
                      </>
                    )}
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
