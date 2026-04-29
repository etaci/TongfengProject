import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import { ArraySummary, MemberList } from "../components/HealthBlocks";
import { familyRelationOptions } from "../constants/options";
import { formatDateTime } from "../utils/format";

export default function FamilyPage({
  app,
  data,
  busyMap,
  session,
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
    <section className="content-section" id="family">
      <SectionHeader
        kicker="05 / 家庭协同"
        title="把邀请、绑定、提醒和患者摘要整理为家庭协同工作区，方便继续承接更多家属能力。"
      />

      <div className="overview-grid">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Create invite</p>
              <h3>发起邀请</h3>
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
                value={inviteDraft.inviteMessage}
                onChange={(event) => setInviteDraft((current) => ({ ...current, inviteMessage: event.target.value }))}
                rows="4"
                placeholder="补充说明协同目的、照护方式或使用说明"
              />
            </label>
            <label>
              <span>有效天数</span>
              <input
                value={inviteDraft.expiresInDays}
                onChange={(event) => setInviteDraft((current) => ({ ...current, expiresInDays: event.target.value }))}
                type="number"
                min="1"
                max="30"
              />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.familyInvite}>
              {busyMap.familyInvite ? "创建中..." : "创建邀请"}
            </button>
          </form>
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Accept invite</p>
              <h3>接受邀请</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleAcceptInvite}>
            <label>
              <span>邀请码</span>
              <input value={acceptInviteCode} onChange={(event) => setAcceptInviteCode(event.target.value)} placeholder="输入收到的邀请码" />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.acceptInvite}>
              {busyMap.acceptInvite ? "处理中..." : "接受邀请"}
            </button>
          </form>
          <div className="session-card">
            <p>患者可以向家属发起邀请，家属接受后即可接收提醒、查看摘要，并在授权范围内协同管理。</p>
          </div>
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Alerts</p>
              <h3>家庭提醒</h3>
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
              <EmptyState message="暂无家庭提醒。" />
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Invite archive</p>
              <h3>邀请记录</h3>
            </div>
          </div>
          <div className="masonry-list">
            {data.familyInvites?.length ? (
              data.familyInvites.map((item) => (
                <article className="list-card" key={item.inviteCode}>
                  <div className="result-header">
                    <strong>{item.patientNickname || "当前用户"}</strong>
                    <span className="inline-tag">{item.status}</span>
                  </div>
                  <p>{item.inviteMessage || "暂无邀请留言。"}</p>
                  <div className="list-card__meta">
                    <span>邀请码：{item.inviteCode}</span>
                    <span>过期时间：{formatDateTime(item.expiresAt)}</span>
                  </div>
                  <div className="action-row">
                    <span className="token">{item.relationType}</span>
                    {item.status === "PENDING" ? (
                      <button
                        className="ghost-button action-button"
                        type="button"
                        disabled={busyMap[`cancel-${item.inviteCode}`]}
                        onClick={() => withErrorHandling(() => app.cancelInvite(item.inviteCode))}
                      >
                        {busyMap[`cancel-${item.inviteCode}`] ? "取消中..." : "取消邀请"}
                      </button>
                    ) : null}
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无邀请记录。" />
            )}
          </div>
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Family summary</p>
              <h3>患者摘要</h3>
            </div>
          </div>
          <div className={`result-panel ${data.familyPatientSummary ? "" : "empty-panel"}`}>
            {data.familyPatientSummary ? (
              <>
                <div className="result-header">
                  <div>
                    <h4>{familySummaryTargetName || "患者摘要"}</h4>
                    <p>{data.familyPatientSummary.latestRiskSummary}</p>
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
              "当家属身份加载患者摘要后，这里会展示风险总结、今日关注和提醒。"
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Members</p>
              <h3>家庭成员关系</h3>
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
    </section>
  );
}
