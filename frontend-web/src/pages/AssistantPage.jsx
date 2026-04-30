import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import { ArraySummary, BulletList, MemberList } from "../components/HealthBlocks";
import { familyRelationOptions, genderOptions } from "../constants/options";
import { formatDate, formatDateTime } from "../utils/format";

export default function AssistantPage({
  app,
  data,
  busyMap,
  session,
  handleKnowledgeSubmit,
  profileDraft,
  setProfileDraft,
  handleProfileSubmit,
  medicationDraft,
  setMedicationDraft,
  handleMedicationSubmit,
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
                placeholder="每行一条，例如：别嘌醇 | 100mg | 每日一次 | 晚饭后"
              />
            </label>
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
