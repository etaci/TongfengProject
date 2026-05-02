import { Link } from "react-router-dom";
import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import TrendCard from "../components/TrendCard";
import { SummaryList } from "../components/HealthBlocks";
import { formatDate, formatDateTime } from "../utils/format";

function CapabilityPanel({ capabilities }) {
  const features = capabilities?.features || [];

  return (
    <Card>
      <div className="card-head">
        <div>
          <p className="eyebrow">Capabilities</p>
          <h3>当前已开放能力</h3>
        </div>
      </div>
      <div className="token-list">
        {features.length ? (
          features.map((item) => (
            <div className="token" key={item.featureKey}>
              <strong>{item.featureName || item.featureKey}</strong>
              <p className="narrative-text">{item.enabled ? "已启用" : "未启用"}</p>
            </div>
          ))
        ) : (
          <EmptyState message="当前没有可展示的能力开关。" />
        )}
      </div>
    </Card>
  );
}

function LoopMetricsPanel({ summary }) {
  if (!summary) {
    return (
      <Card className="span-2">
        <EmptyState message="登录后这里会展示当前闭环链路的使用汇总。" />
      </Card>
    );
  }

  const statItems = [
    ["近 7 天事件数", summary.totalEvents ?? 0],
    ["尿酸记录用户", summary.uricAcidRecordUsers ?? 0],
    ["家庭邀请用户", summary.familyInviteUsers ?? 0],
    ["家庭接受用户", summary.familyAcceptUsers ?? 0],
    ["摘要查看用户", summary.familySummaryUsers ?? 0],
  ];

  return (
    <Card className="span-2">
      <div className="card-head">
        <div>
          <p className="eyebrow">Closed-loop metrics</p>
          <h3>闭环链路覆盖</h3>
        </div>
      </div>
      <div className="stats-grid medication-stats-grid">
        {statItems.map(([label, value]) => (
          <div className="stat-line" key={label}>
            <span>{label}</span>
            <strong>{value}</strong>
          </div>
        ))}
      </div>
      <p className="narrative-text">
        这组数据用于确认“记录、提醒、家庭协同、用药打卡”这些关键链路是否已经被用户真实走通。
      </p>
    </Card>
  );
}

function TriagePanel({ todayPlan }) {
  if (!todayPlan) {
    return (
      <Card className="span-2">
        <EmptyState message="登录后这里会直接告诉你今天最重要的风险、行动项和就医边界。" />
      </Card>
    );
  }

  return (
    <Card className="span-2 today-triage-card">
      <div className="card-head">
        <div>
          <p className="eyebrow">Today care plan</p>
          <h3>今天先做什么</h3>
        </div>
        <RiskBadge level={todayPlan.overallRiskLevel} />
      </div>

      <div className="today-triage-hero">
        <div>
          <span className="inline-tag">{todayPlan.triageCode}</span>
          <h4>{todayPlan.triageTitle}</h4>
          <p className="narrative-text">{todayPlan.triageSummary}</p>
        </div>
        <div className="today-triage-next">
          <span>下一步</span>
          <strong>{todayPlan.nextStep}</strong>
          <small>生成时间：{formatDateTime(todayPlan.generatedAt)}</small>
        </div>
      </div>

      <div className="split-block">
        <div>
          <h4>为什么这样判断</h4>
          <ul className="bullet-list">
            {(todayPlan.reasons || []).map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
        <div>
          <h4>AI 使用边界</h4>
          <ul className="bullet-list">
            {(todayPlan.trustNotes || []).map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      </div>
    </Card>
  );
}

function ActionList({ todayPlan }) {
  return (
    <Card>
      <div className="card-head">
        <div>
          <p className="eyebrow">Action checklist</p>
          <h3>今天必须完成的事</h3>
        </div>
      </div>

      <div className="stack-list">
        {todayPlan?.actions?.length ? (
          todayPlan.actions.map((item) => (
            <article className="list-card today-action-card" key={item.actionKey}>
              <div className="result-header">
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.description}</p>
                </div>
                <div className="today-action-badges">
                  <span className="inline-tag">{item.priority}</span>
                  <span className="inline-tag">{item.status}</span>
                </div>
              </div>
              <div className="list-card__meta">
                <span>{item.category}</span>
              </div>
            </article>
          ))
        ) : (
          <EmptyState message="今天的行动项会在登录并加载记录后自动生成。" />
        )}
      </div>
    </Card>
  );
}

function MedicationWeeklyPanel({ weeklyReport }) {
  if (!weeklyReport) {
    return (
      <Card className="span-2">
        <EmptyState message="登录后这里会展示近 7 天用药依从周报和补药提醒。" />
      </Card>
    );
  }

  return (
    <Card className="span-2">
      <div className="card-head">
        <div>
          <p className="eyebrow">Medication weekly report</p>
          <h3>近 7 天用药复盘</h3>
        </div>
        <span className="inline-tag">{weeklyReport.startDate} 至 {weeklyReport.endDate}</span>
      </div>
      <div className="stats-grid medication-stats-grid">
        <div className="stat-line">
          <span>周依从率</span>
          <strong>{weeklyReport.adherenceRate || 0}%</strong>
        </div>
        <div className="stat-line">
          <span>已服用 / 计划</span>
          <strong>{weeklyReport.takenDoseCount || 0} / {weeklyReport.plannedDoseCount || 0}</strong>
        </div>
        <div className="stat-line">
          <span>漏服 / 跳过</span>
          <strong>{weeklyReport.missedDoseCount || 0} / {weeklyReport.skippedDoseCount || 0}</strong>
        </div>
        <div className="stat-line">
          <span>当前 / 最长连续</span>
          <strong>{weeklyReport.currentStreakDays || 0} / {weeklyReport.longestStreakDays || 0} 天</strong>
        </div>
        <div className="stat-line">
          <span>待补说明剂次</span>
          <strong>{weeklyReport.overdueDoseCount || 0}</strong>
        </div>
      </div>
      <div className="split-block">
        <div>
          <h4>本周亮点</h4>
          <ul className="bullet-list">
            {(weeklyReport.highlights || []).map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
        <div>
          <h4>本周下一步</h4>
          <ul className="bullet-list">
            {(weeklyReport.nextActions || []).map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      </div>
      <div className="split-block">
        <div>
          <h4>重点药物</h4>
          <div className="token-list">
            {weeklyReport.focusMedications?.length ? (
              weeklyReport.focusMedications.map((item) => <div className="token" key={item}>{item}</div>)
            ) : (
              <EmptyState message="本周没有明显的重点问题药物。" />
            )}
          </div>
        </div>
        <div>
          <h4>每日拆分</h4>
          <div className="stack-list">
            {weeklyReport.dailyBreakdown?.length ? (
              weeklyReport.dailyBreakdown.map((item) => (
                <article className="list-card" key={item.summaryDate}>
                  <div className="result-header">
                    <strong>{item.summaryDate}</strong>
                    <span className="inline-tag">{item.adherenceRate}%</span>
                  </div>
                  <div className="list-card__meta">
                    <span>已服用 {item.takenDoseCount} / 计划 {item.plannedDoseCount}</span>
                    <span>漏服 {item.missedDoseCount} / 跳过 {item.skippedDoseCount}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="本周还没有可复盘的用药数据。" />
            )}
          </div>
        </div>
      </div>
    </Card>
  );
}

function MedicationRefillPanel({ weeklyReport }) {
  return (
    <Card>
      <div className="card-head">
        <div>
          <p className="eyebrow">Refill alerts</p>
          <h3>补药提醒</h3>
        </div>
      </div>
      <div className="stack-list">
        {weeklyReport?.refillAlerts?.length ? (
          weeklyReport.refillAlerts.map((item) => (
            <article className="list-card" key={`${item.medicationName}-${item.remainingDays}`}>
              <div className="result-header">
                <div>
                  <strong>{item.medicationName}</strong>
                  <p>{item.dosage || "剂量未填写"}</p>
                </div>
                <RiskBadge level={item.riskLevel} />
              </div>
              <p>{item.suggestion}</p>
              <div className="list-card__meta">
                <span>剩余 {item.remainingDays} 天</span>
                <span>提醒阈值 {item.refillThresholdDays} 天</span>
              </div>
            </article>
          ))
        ) : (
          <EmptyState message="当前没有临近补药的药物。" />
        )}
      </div>
    </Card>
  );
}

function LabReviewEntryPanel({ data }) {
  if (!data.labResult) {
    return (
      <Card>
        <div className="card-head">
          <div>
            <p className="eyebrow">Lab review</p>
            <h3>化验单复盘</h3>
          </div>
          <Link className="ghost-button action-button" to="/assistant">进入页面</Link>
        </div>
        <EmptyState message="上传或同步化验单后，这里会先给出目标达成情况、关键变化和复查建议。" />
      </Card>
    );
  }

  const review = data.labReview;

  return (
    <Card>
      <div className="card-head">
        <div>
          <p className="eyebrow">Lab review</p>
          <h3>化验单复盘</h3>
        </div>
        <Link className="ghost-button action-button" to="/assistant">查看详情</Link>
      </div>
      <div className="result-header">
        <div>
          <strong>{formatDate(data.labResult.reportDate)}</strong>
          <p>{review?.targetConclusion || review?.reviewSummary || data.labResult.summary || "暂无化验单摘要。"}</p>
        </div>
        <RiskBadge level={review?.overallRiskLevel || data.labResult.overallRiskLevel} />
      </div>
      <div className="stats-grid stats-grid--compact">
        <div className="stat-line">
          <span>本次尿酸</span>
          <strong>
            {review?.currentUricAcidValue != null
              ? `${review.currentUricAcidValue} ${review.currentUricAcidUnit || ""}`
              : "未识别"}
          </strong>
        </div>
        <div className="stat-line">
          <span>目标尿酸</span>
          <strong>{review?.targetUricAcidValue != null ? `${review.targetUricAcidValue} ${review.currentUricAcidUnit || ""}` : "未设置"}</strong>
        </div>
        <div className="stat-line">
          <span>与上次间隔</span>
          <strong>{review?.daysBetweenReports != null ? `${review.daysBetweenReports} 天` : "暂无基线"}</strong>
        </div>
      </div>
      <p className="narrative-text">
        {review?.followUpRecommendation || data.labResult.suggestions?.[0] || "进入问答与档案页后，可继续查看完整复盘和下一步建议。"}
      </p>
      <div className="action-row">
        {(review?.nextActions || review?.keyChanges || []).slice(0, 3).map((item) => (
          <span className="token token--tiny" key={item}>{item}</span>
        ))}
      </div>
    </Card>
  );
}

function ProactiveEntryPanel({ proactiveBrief, flareReview }) {
  return (
    <Card>
      <div className="card-head">
        <div>
          <p className="eyebrow">Proactive care</p>
          <h3>主动关怀入口</h3>
        </div>
        <Link className="ghost-button action-button" to="/proactive">进入页面</Link>
      </div>
      {proactiveBrief ? (
        <>
          <div className="result-header">
            <div>
              <strong>综合风险分 {proactiveBrief.riskScore}</strong>
              <p>{proactiveBrief.summary || "暂无主动关怀摘要。"}</p>
            </div>
            <RiskBadge level={proactiveBrief.overallRiskLevel} />
          </div>
          <div className="list-card__meta">
            <span>风险因素 {proactiveBrief.factors?.length || 0} 项</span>
            <span>建议动作 {proactiveBrief.suggestions?.length || 0} 项</span>
          </div>
          <div className="action-row">
            {(proactiveBrief.suggestions || []).slice(0, 2).map((item) => (
              <span className="token token--tiny" key={item}>{item}</span>
            ))}
          </div>
          {flareReview ? (
            <div className="session-card">
              <strong>最近发作复盘</strong>
              <p>{flareReview.summary || "暂无发作复盘摘要。"}</p>
            </div>
          ) : null}
        </>
      ) : (
        <EmptyState message="配置城市并同步后，这里会出现主动关怀摘要和发作复盘入口。" />
      )}
    </Card>
  );
}

function FamilyTaskEntryPanel({ data, familyFeatureEnabled }) {
  if (!familyFeatureEnabled) {
    return (
      <Card>
        <div className="card-head">
          <div>
            <p className="eyebrow">Family tasks</p>
            <h3>家属代办摘要</h3>
          </div>
        </div>
        <EmptyState message="当前账号未开放家庭协同能力。" />
      </Card>
    );
  }

  const patientTasks = data.familyTasks?.asPatient || [];
  const caregiverTasks = data.familyTasks?.asCaregiver || [];
  const openPatientTasks = patientTasks.filter((item) => item.status === "OPEN");
  const openCaregiverTasks = caregiverTasks.filter((item) => item.status === "OPEN");
  const completedTasks = [...patientTasks, ...caregiverTasks].filter((item) => item.status === "COMPLETED");
  const focusTasks = [
    ...openCaregiverTasks.map((item) => ({ ...item, roleLabel: `来自 ${item.patientNickname || "患者"}` })),
    ...openPatientTasks.map((item) => ({ ...item, roleLabel: `交给 ${item.caregiverNickname || "家属"}` })),
  ].slice(0, 2);

  return (
    <Card>
      <div className="card-head">
        <div>
          <p className="eyebrow">Family tasks</p>
          <h3>家属代办摘要</h3>
        </div>
        <Link className="ghost-button action-button" to="/family">进入页面</Link>
      </div>
      <div className="stats-grid stats-grid--compact">
        <div className="stat-line">
          <span>我发起待完成</span>
          <strong>{openPatientTasks.length}</strong>
        </div>
        <div className="stat-line">
          <span>我待处理</span>
          <strong>{openCaregiverTasks.length}</strong>
        </div>
        <div className="stat-line">
          <span>已完成代办</span>
          <strong>{completedTasks.length}</strong>
        </div>
      </div>
      <div className="stack-list">
        {focusTasks.length ? (
          focusTasks.map((item) => (
            <article className="list-card" key={item.taskCode}>
              <div className="result-header">
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.description || "暂无补充说明。"}</p>
                </div>
                <span className="inline-tag">{item.status}</span>
              </div>
              <div className="list-card__meta">
                <span>{item.roleLabel}</span>
                <span>截止：{item.dueAt ? formatDateTime(item.dueAt) : "未设置"}</span>
              </div>
            </article>
          ))
        ) : (
          <EmptyState message="当前没有待推进的家属代办，可在家庭协同页创建补药、复查或提醒类任务。" />
        )}
      </div>
    </Card>
  );
}

function FamilyEntryPanel({ app, data, familyFeatureEnabled }) {
  if (!familyFeatureEnabled) {
    return (
      <Card>
        <div className="card-head">
          <div>
            <p className="eyebrow">Family care</p>
            <h3>家庭协同入口</h3>
          </div>
        </div>
        <EmptyState message="当前账号未开放家庭协同能力。" />
      </Card>
    );
  }

  const caregiverMembers = data.familyMembers?.asCaregiver || [];
  const patientMembers = data.familyMembers?.asPatient || [];
  const firstCaregiverTarget = caregiverMembers[0];

  return (
    <Card>
      <div className="card-head">
        <div>
          <p className="eyebrow">Family care</p>
          <h3>家庭协同入口</h3>
        </div>
        <Link className="ghost-button action-button" to="/family">进入页面</Link>
      </div>
      <div className="stats-grid stats-grid--compact">
        <div className="stat-line">
          <span>待处理邀请</span>
          <strong>{(data.familyInvites || []).filter((item) => item.status === "PENDING").length}</strong>
        </div>
        <div className="stat-line">
          <span>我作为患者</span>
          <strong>{patientMembers.length}</strong>
        </div>
        <div className="stat-line">
          <span>我作为家属</span>
          <strong>{caregiverMembers.length}</strong>
        </div>
      </div>
      <div className="stack-list">
        {data.familyAlerts?.length ? (
          data.familyAlerts.slice(0, 2).map((item) => (
            <article className="list-card" key={item.alertId}>
              <div className="result-header">
                <strong>{item.patientNickname}</strong>
                <RiskBadge level={item.riskLevel} />
              </div>
              <p>{item.title}</p>
            </article>
          ))
        ) : (
          <EmptyState message="当前没有需要家属立即处理的提醒。" />
        )}
      </div>
      {firstCaregiverTarget ? (
        <div className="action-row">
          <button className="primary-button action-button" type="button" onClick={() => app.loadFamilySummary(firstCaregiverTarget.patientUserId)}>
            快速查看 {firstCaregiverTarget.patientNickname} 摘要
          </button>
        </div>
      ) : null}
    </Card>
  );
}

export default function OverviewPage({ app, data, trendOptions, familyFeatureEnabled }) {
  return (
    <section className="content-section" id="overview">
      <SectionHeader
        kicker="01 / 今日行动"
        title="先看今天风险，再按顺序完成 1 到 3 个关键动作。"
      />

      <div className="overview-grid">
        <TriagePanel todayPlan={data.todayPlan} />
        <ActionList todayPlan={data.todayPlan} />
      </div>

      <div className="overview-grid overview-grid--secondary">
        <MedicationWeeklyPanel weeklyReport={data.medicationWeeklyReport} />
        <MedicationRefillPanel weeklyReport={data.medicationWeeklyReport} />
      </div>

      <div className="overview-grid overview-grid--secondary">
        <LabReviewEntryPanel data={data} />
        <FamilyTaskEntryPanel data={data} familyFeatureEnabled={familyFeatureEnabled} />
        <FamilyEntryPanel app={app} data={data} familyFeatureEnabled={familyFeatureEnabled} />
      </div>

      <div className="overview-grid overview-grid--secondary">
        <ProactiveEntryPanel proactiveBrief={data.proactiveBrief} flareReview={data.flareReview} />
        <CapabilityPanel capabilities={data.capabilities} />
      </div>

      <div className="overview-grid overview-grid--secondary">
        <LoopMetricsPanel summary={data.mvpMetricsSummary} />
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Reminder stream</p>
              <h3>提醒流</h3>
            </div>
          </div>
          <div className="stack-list">
            {data.reminders?.length ? (
              data.reminders.map((item) => (
                <article className="list-card" key={item.reminderId}>
                  <div className="result-header">
                    <strong>{item.title}</strong>
                    <RiskBadge level={item.riskLevel} />
                  </div>
                  <p>{item.content}</p>
                  <div className="list-card__meta">
                    <span>{item.type}</span>
                    <span>{formatDateTime(item.triggerAt)}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无提醒数据。" />
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Core status</p>
              <h3>今天的数据底座</h3>
            </div>
            <span className="inline-tag">{data.overview?.stage || "等待评估"}</span>
          </div>
          <div className="stats-grid">
            {[
              ["饮食记录", data.overview?.mealsCount ?? "-"],
              ["高风险饮食", data.overview?.highRiskMealsCount ?? "-"],
              ["尿酸记录", data.overview?.uricAcidCount ?? "-"],
              ["发作次数", data.overview?.flareCount ?? "-"],
            ].map(([label, value]) => (
              <div className="stat-line" key={label}>
                <span>{label}</span>
                <strong>{value}</strong>
              </div>
            ))}
          </div>
          <div className="split-block">
            <div>
              <h4>今日关注</h4>
              <div className="token-list">
                {data.overview?.todayFocus?.length ? (
                  data.overview.todayFocus.map((item) => <div className="token" key={item}>{item}</div>)
                ) : (
                  <EmptyState message="暂无今日关注项，登录并同步后会自动展示。" />
                )}
              </div>
            </div>
            <div>
              <h4>风险摘要</h4>
              <p className="narrative-text">{data.overview?.latestRiskSummary || "暂无风险摘要。"}</p>
            </div>
          </div>
        </Card>

      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Trend monitor</p>
              <h3>关键趋势</h3>
            </div>
            <label className="inline-control">
              <span>统计周期</span>
              <select value={app.trendDays} onChange={(event) => app.setTrendDays(event.target.value)}>
                {trendOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="trend-grid">
            <TrendCard title="尿酸趋势" points={data.trends?.uricAcid || []} emptyText="暂无尿酸趋势数据" />
            <TrendCard title="体重趋势" points={data.trends?.weight || []} emptyText="暂无体重趋势数据" />
            <TrendCard title="补水趋势" points={data.trends?.hydration || []} emptyText="暂无补水趋势数据" />
          </div>
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Daily summaries</p>
              <h3>每日汇总</h3>
            </div>
          </div>
          <div className="stack-list">
            <SummaryList items={data.summaries} />
          </div>
        </Card>
      </div>
    </section>
  );
}
