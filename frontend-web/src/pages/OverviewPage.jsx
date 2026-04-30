import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import TrendCard from "../components/TrendCard";
import { SummaryList } from "../components/HealthBlocks";
import { formatDateTime } from "../utils/format";

export default function OverviewPage({ app, data, trendOptions }) {
  const metricHighlights = [
    ["活跃用户", data.mvpMetricsSummary?.activeUsers ?? "-"],
    ["总事件数", data.mvpMetricsSummary?.totalEvents ?? "-"],
    ["尿酸记录用户", data.mvpMetricsSummary?.uricAcidRecordUsers ?? "-"],
    ["家庭协同用户", (data.mvpMetricsSummary?.familyInviteUsers || 0) + (data.mvpMetricsSummary?.familySummaryUsers || 0)],
  ];
  const metricBreakdown = (data.mvpMetricsSummary?.eventBreakdown || []).filter((item) => item.totalEvents > 0);

  return (
    <section className="content-section" id="overview">
      <SectionHeader
        kicker="01 / 总览"
        title="集中查看今日重点、提醒流、趋势监测和每日汇总，作为整个动态前端的首页工作台。"
      />

      <div className="overview-grid">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Dashboard overview</p>
              <h3>概览数据</h3>
            </div>
            <span className="inline-tag">{data.overview?.stage || "未评估"}</span>
          </div>
          <div className="stats-grid">
            {[
              ["饮食记录", data.overview?.mealsCount ?? "-"],
              ["高风险餐", data.overview?.highRiskMealsCount ?? "-"],
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
              <p className="eyebrow">Trend monitor</p>
              <h3>趋势监测</h3>
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
            <TrendCard title="饮水趋势" points={data.trends?.hydration || []} emptyText="暂无饮水趋势数据" />
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

      <Card>
        <div className="card-head">
          <div>
            <p className="eyebrow">MVP validation</p>
            <h3>近 {data.mvpMetricsSummary?.days || 7} 天功能使用概览</h3>
          </div>
          <span className="inline-tag">
            {data.mvpMetricsSummary?.generatedAt ? `更新于 ${formatDateTime(data.mvpMetricsSummary.generatedAt)}` : "等待同步"}
          </span>
        </div>
        {data.mvpMetricsSummary ? (
          <>
            <div className="stats-grid">
              {metricHighlights.map(([label, value]) => (
                <div className="stat-line" key={label}>
                  <span>{label}</span>
                  <strong>{value}</strong>
                </div>
              ))}
            </div>
            <div className="stack-list">
              {metricBreakdown.length ? (
                metricBreakdown.map((item) => (
                  <article className="list-card" key={item.eventType}>
                    <div className="result-header">
                      <strong>{item.label}</strong>
                      <span>{item.totalEvents} 次事件 / {item.uniqueUsers} 位用户</span>
                    </div>
                    <div className="list-card__meta">
                      <span>{item.eventType}</span>
                      <span>{item.latestEventAt ? formatDateTime(item.latestEventAt) : "暂无时间"}</span>
                    </div>
                  </article>
                ))
              ) : (
                <EmptyState message="近 7 天还没有形成可统计的 MVP 使用事件。" />
              )}
            </div>
          </>
        ) : (
          <EmptyState message="指标汇总正在加载，登录并同步后会展示近 7 天使用情况。" />
        )}
      </Card>
    </section>
  );
}
