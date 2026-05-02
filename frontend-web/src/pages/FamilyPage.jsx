import { useEffect, useState } from "react";
import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import { ArraySummary, MemberList } from "../components/HealthBlocks";
import { familyPermissionOptions, familyRelationOptions } from "../constants/options";
import { formatDateTime } from "../utils/format";

function FamilyOverviewPanel({ data }) {
  const pendingInvites = (data.familyInvites || []).filter((item) => item.status === "PENDING").length;
  const asPatientCount = data.familyMembers?.asPatient?.length || 0;
  const asCaregiverCount = data.familyMembers?.asCaregiver?.length || 0;
  const familyTaskCount = (data.familyTasks?.asPatient?.length || 0) + (data.familyTasks?.asCaregiver?.length || 0);

  return (
    <Card className="span-2">
      <div className="card-head">
        <div>
          <p className="eyebrow">Overview</p>
          <h3>协同总览</h3>
        </div>
      </div>
      <div className="stats-grid stats-grid--compact">
        <div className="stat-line">
          <span>待处理邀请</span>
          <strong>{pendingInvites}</strong>
        </div>
        <div className="stat-line">
          <span>我作为患者</span>
          <strong>{asPatientCount}</strong>
        </div>
        <div className="stat-line">
          <span>我作为家属</span>
          <strong>{asCaregiverCount}</strong>
        </div>
      </div>
      <div className="split-block">
        <div>
          <h4>协同价值</h4>
          <ul className="bullet-list">
            <li>患者可向家属发出邀请，让提醒和摘要被看见。</li>
            <li>家属可查看患者摘要，及时发现风险上升。</li>
            <li>双方都能在需要时解除绑定，保持协同边界清晰。</li>
          </ul>
        </div>
        <div>
          <h4>当前状态</h4>
          <ul className="bullet-list">
            <li>家庭提醒 {data.familyAlerts?.length || 0} 条。</li>
            <li>邀请记录 {(data.familyInvites || []).length} 条。</li>
            <li>协同代办 {familyTaskCount} 条。</li>
            <li>已加载患者摘要 {data.familyPatientSummary ? "是" : "否"}。</li>
          </ul>
        </div>
      </div>
    </Card>
  );
}

function CaregiverQuickPanel({ data, app, withErrorHandling, busyMap }) {
  const caregiverMembers = data.familyMembers?.asCaregiver || [];

  return (
    <Card>
      <div className="card-head">
        <div>
          <p className="eyebrow">Quick access</p>
          <h3>快速查看患者</h3>
        </div>
      </div>
      <div className="stack-list">
        {caregiverMembers.length ? (
          caregiverMembers.map((item) => (
            <article className="list-card" key={item.bindingCode}>
              <div className="result-header">
                <strong>{item.patientNickname}</strong>
                <span className="inline-tag">{item.relationType}</span>
              </div>
              <p>绑定时间：{formatDateTime(item.createdAt)}</p>
              <div className="action-row">
                <button
                  className="primary-button action-button"
                  type="button"
                  disabled={busyMap.familySummary}
                  onClick={() => withErrorHandling(() => app.loadFamilySummary(item.patientUserId))}
                >
                  {busyMap.familySummary ? "加载中..." : "查看患者摘要"}
                </button>
              </div>
            </article>
          ))
        ) : (
          <EmptyState message="当前没有可直接查看摘要的患者绑定关系。" />
        )}
      </div>
    </Card>
  );
}

function FamilyTaskPanel({
  app,
  data,
  busyMap,
  familyTaskDraft,
  setFamilyTaskDraft,
  handleFamilyTaskSubmit,
  taskCompletionDrafts,
  setTaskCompletionDrafts,
  withErrorHandling,
}) {
  const patientTaskBindings = (data.familyMembers?.asPatient || []).filter((item) => item.caregiverPermission === "TASK" && item.status === "ACTIVE");
  const patientTasks = data.familyTasks?.asPatient || [];
  const caregiverTasks = data.familyTasks?.asCaregiver || [];

  return (
    <div className="overview-grid overview-grid--secondary">
      <Card>
        <div className="card-head">
          <div>
            <p className="eyebrow">Task dispatch</p>
            <h3>发起家属代办</h3>
          </div>
        </div>
        {patientTaskBindings.length ? (
          <form className="stack-form" onSubmit={handleFamilyTaskSubmit}>
            <label>
              <span>协同家属</span>
              <select
                value={familyTaskDraft.bindingCode}
                onChange={(event) => setFamilyTaskDraft((current) => ({ ...current, bindingCode: event.target.value }))}
              >
                {patientTaskBindings.map((item) => (
                  <option key={item.bindingCode} value={item.bindingCode}>
                    {item.caregiverNickname} / {item.relationType}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>代办标题</span>
              <input
                value={familyTaskDraft.title}
                onChange={(event) => setFamilyTaskDraft((current) => ({ ...current, title: event.target.value }))}
                placeholder="例如：周一上午帮我确认补药"
              />
            </label>
            <label>
              <span>说明</span>
              <textarea
                value={familyTaskDraft.description}
                onChange={(event) => setFamilyTaskDraft((current) => ({ ...current, description: event.target.value }))}
                rows="4"
                placeholder="补充需要协助的背景、截止条件或确认标准"
              />
            </label>
            <label>
              <span>期望完成时间</span>
              <input
                type="datetime-local"
                value={familyTaskDraft.dueAt}
                onChange={(event) => setFamilyTaskDraft((current) => ({ ...current, dueAt: event.target.value }))}
              />
            </label>
            <button className="primary-button" type="submit" disabled={busyMap["family-task-create"]}>
              {busyMap["family-task-create"] ? "创建中..." : "创建代办"}
            </button>
          </form>
        ) : (
          <EmptyState message="当前没有具备共同照护权限的家属成员，请先把权限升级到 TASK。" />
        )}
      </Card>

      <Card className="span-2">
        <div className="card-head">
          <div>
            <p className="eyebrow">Task board</p>
            <h3>家属代办看板</h3>
          </div>
        </div>
        <div className="split-block">
          <div>
            <strong className="subtle-title">我发起的代办</strong>
            <div className="stack-list">
              {patientTasks.length ? (
                patientTasks.map((item) => (
                  <article className="list-card" key={item.taskCode}>
                    <div className="result-header">
                      <strong>{item.title}</strong>
                      <span className="inline-tag">{item.status}</span>
                    </div>
                    <p>{item.description || "暂无补充说明。"}</p>
                    <div className="list-card__meta">
                      <span>协同家属：{item.caregiverNickname} / {item.relationType}</span>
                      <span>创建时间：{formatDateTime(item.createdAt)}</span>
                    </div>
                    <div className="list-card__meta">
                      <span>截止时间：{item.dueAt ? formatDateTime(item.dueAt) : "未设置"}</span>
                      <span>完成时间：{item.completedAt ? formatDateTime(item.completedAt) : "待确认"}</span>
                    </div>
                    {item.completionNote ? <p>处理反馈：{item.completionNote}</p> : null}
                  </article>
                ))
              ) : (
                <EmptyState message="你发给家属的代办会显示在这里。" />
              )}
            </div>
          </div>
          <div>
            <strong className="subtle-title">我待确认的代办</strong>
            <div className="stack-list">
              {caregiverTasks.length ? (
                caregiverTasks.map((item) => (
                  <article className="list-card" key={item.taskCode}>
                    <div className="result-header">
                      <strong>{item.title}</strong>
                      <span className="inline-tag">{item.status}</span>
                    </div>
                    <p>{item.description || "暂无补充说明。"}</p>
                    <div className="list-card__meta">
                      <span>患者：{item.patientNickname}</span>
                      <span>截止时间：{item.dueAt ? formatDateTime(item.dueAt) : "未设置"}</span>
                    </div>
                    {item.status === "OPEN" ? (
                      <div className="stack-form compact-form">
                        <label>
                          <span>处理反馈</span>
                          <textarea
                            rows="3"
                            value={taskCompletionDrafts[item.taskCode] || ""}
                            onChange={(event) => setTaskCompletionDrafts((current) => ({ ...current, [item.taskCode]: event.target.value }))}
                            placeholder="补充你已经完成了什么，方便患者回看"
                          />
                        </label>
                        <button
                          className="ghost-button action-button"
                          type="button"
                          disabled={busyMap[`family-task-${item.taskCode}`]}
                          onClick={() => withErrorHandling(async () => {
                            await app.submitFamilyTaskCompletion(item.taskCode, {
                              completionNote: (taskCompletionDrafts[item.taskCode] || "").trim() || null,
                            });
                            setTaskCompletionDrafts((current) => ({ ...current, [item.taskCode]: "" }));
                          })}
                        >
                          {busyMap[`family-task-${item.taskCode}`] ? "提交中..." : "确认已处理"}
                        </button>
                      </div>
                    ) : (
                      <p>处理反馈：{item.completionNote || "已完成，未填写补充说明。"}</p>
                    )}
                  </article>
                ))
              ) : (
                <EmptyState message="当前没有等待你处理的家属代办。" />
              )}
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}

export default function FamilyPage({
  app,
  data,
  busyMap,
  session,
  inviteDraft,
  setInviteDraft,
  handleInviteSubmit,
  familyTaskDraft,
  setFamilyTaskDraft,
  handleFamilyTaskSubmit,
  acceptInviteCode,
  setAcceptInviteCode,
  handleAcceptInvite,
  familySummaryTargetName,
  familyWeeklyReportTargetName,
  withErrorHandling,
}) {
  const [permissionDrafts, setPermissionDrafts] = useState({});
  const [taskCompletionDrafts, setTaskCompletionDrafts] = useState({});

  useEffect(() => {
    const nextDrafts = {};
    (data.familyMembers?.asPatient || []).forEach((item) => {
      nextDrafts[item.bindingCode] = {
        caregiverPermission: item.caregiverPermission || familyPermissionOptions[1].value,
        weeklyReportEnabled: Boolean(item.weeklyReportEnabled),
        notifyOnHighRisk: Boolean(item.notifyOnHighRisk),
      };
    });
    setPermissionDrafts(nextDrafts);
  }, [data.familyMembers]);

  return (
    <section className="content-section" id="family">
      <SectionHeader
        kicker="05 / 家庭协同"
        title="把邀请、绑定、提醒和患者摘要整理为家庭协同工作区，方便继续承接更多家属能力。"
      />

      <div className="overview-grid">
        <FamilyOverviewPanel data={data} />
        <CaregiverQuickPanel data={data} app={app} withErrorHandling={withErrorHandling} busyMap={busyMap} />
      </div>

      <div className="overview-grid overview-grid--secondary">
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
            <label>
              <span>家属权限</span>
              <select
                value={inviteDraft.caregiverPermission}
                onChange={(event) => setInviteDraft((current) => ({ ...current, caregiverPermission: event.target.value }))}
              >
                {familyPermissionOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={inviteDraft.weeklyReportEnabled}
                onChange={(event) => setInviteDraft((current) => ({ ...current, weeklyReportEnabled: event.target.checked }))}
              />
              <span>允许家属查看用药周报</span>
            </label>
            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={inviteDraft.notifyOnHighRisk}
                onChange={(event) => setInviteDraft((current) => ({ ...current, notifyOnHighRisk: event.target.checked }))}
              />
              <span>高风险时向家属发送提醒</span>
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

      <FamilyTaskPanel
        app={app}
        data={data}
        busyMap={busyMap}
        familyTaskDraft={familyTaskDraft}
        setFamilyTaskDraft={setFamilyTaskDraft}
        handleFamilyTaskSubmit={handleFamilyTaskSubmit}
        taskCompletionDrafts={taskCompletionDrafts}
        setTaskCompletionDrafts={setTaskCompletionDrafts}
        withErrorHandling={withErrorHandling}
      />

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
                  <div className="list-card__meta">
                    <span>权限：{item.caregiverPermission}</span>
                    <span>周报：{item.weeklyReportEnabled ? "已开放" : "未开放"} / 提醒：{item.notifyOnHighRisk ? "开启" : "关闭"}</span>
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
              <p className="eyebrow">Shared weekly report</p>
              <h3>家属共享周报</h3>
            </div>
            <span className="inline-tag">{familyWeeklyReportTargetName || "待加载"}</span>
          </div>
          <div className={`result-panel ${data.familyWeeklyReport ? "" : "empty-panel"}`}>
            {data.familyWeeklyReport ? (
              <>
                <div className="result-header">
                  <div>
                    <h4>{data.familyWeeklyReport.patientNickname}</h4>
                    <p>权限：{data.familyWeeklyReport.caregiverPermission} / 周报共享：{data.familyWeeklyReport.weeklyReportEnabled ? "已开启" : "已关闭"}</p>
                  </div>
                  <span className="inline-tag">{data.familyWeeklyReport.weeklyReport?.adherenceRate || 0}%</span>
                </div>
                <div className="stats-grid stats-grid--compact">
                  <div className="stat-line">
                    <span>本周依从率</span>
                    <strong>{data.familyWeeklyReport.weeklyReport?.adherenceRate || 0}%</strong>
                  </div>
                  <div className="stat-line">
                    <span>已服用 / 计划</span>
                    <strong>{data.familyWeeklyReport.weeklyReport?.takenDoseCount || 0} / {data.familyWeeklyReport.weeklyReport?.plannedDoseCount || 0}</strong>
                  </div>
                  <div className="stat-line">
                    <span>当前连续天数</span>
                    <strong>{data.familyWeeklyReport.weeklyReport?.currentStreakDays || 0}</strong>
                  </div>
                </div>
                <ArraySummary
                  title="本周亮点"
                  items={data.familyWeeklyReport.weeklyReport?.highlights}
                  emptyMessage="暂无本周亮点。"
                />
                <ArraySummary
                  title="下一步建议"
                  items={data.familyWeeklyReport.weeklyReport?.nextActions}
                  emptyMessage="暂无下一步建议。"
                />
              </>
            ) : (
              "当患者向你开放周报共享后，这里会展示近 7 天的依从复盘。"
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
                onViewWeeklyReport={null}
                onManagePermission={(bindingCode, payload) => withErrorHandling(() => app.submitFamilyBindingPermissions(bindingCode, payload))}
                permissionDrafts={permissionDrafts}
                setPermissionDrafts={setPermissionDrafts}
                onRemove={(bindingCode) => withErrorHandling(() => app.unbindFamilyMember(bindingCode))}
              />
            </div>
            <div>
              <MemberList
                title="我作为家属"
                items={data.familyMembers?.asCaregiver || []}
                busyMap={busyMap}
                onViewSummary={(patientUserId) => withErrorHandling(() => app.loadFamilySummary(patientUserId))}
                onViewWeeklyReport={(patientUserId) => withErrorHandling(() => app.loadFamilyWeeklyReport(patientUserId))}
                onRemove={(bindingCode) => withErrorHandling(() => app.unbindFamilyMember(bindingCode))}
              />
            </div>
          </div>
        </Card>
      </div>
    </section>
  );
}
