import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import { BulletList } from "../components/HealthBlocks";
import { countryOptions } from "../constants/options";
import { formatDateTime } from "../utils/format";

export default function ProactivePage({
  data,
  busyMap,
  session,
  proactiveDraft,
  setProactiveDraft,
  handleProactiveSubmit,
}) {
  return (
    <section className="content-section" id="proactive">
      <SectionHeader
        kicker="04 / 主动关怀"
        title="把天气、发作复盘和风险因素串成主动关怀页面，方便后端继续扩展预警和建议能力。"
      />

      <div className="overview-grid">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Settings</p>
              <h3>关怀设置</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleProactiveSubmit}>
            <label>
              <span>监测城市</span>
              <input
                value={proactiveDraft.monitoringCity}
                onChange={(event) => setProactiveDraft((current) => ({ ...current, monitoringCity: event.target.value }))}
                placeholder="例如：Shanghai"
              />
            </label>
            <label>
              <span>国家或地区</span>
              <select
                value={proactiveDraft.countryCode}
                onChange={(event) => setProactiveDraft((current) => ({ ...current, countryCode: event.target.value }))}
              >
                {countryOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={proactiveDraft.weatherAlertsEnabled}
                onChange={(event) => setProactiveDraft((current) => ({ ...current, weatherAlertsEnabled: event.target.checked }))}
              />
              <span>开启天气相关主动提醒</span>
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.proactive}>
              {busyMap.proactive ? "保存中..." : "保存设置"}
            </button>
          </form>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Brief</p>
              <h3>主动关怀概览</h3>
            </div>
          </div>
          <div className={`result-panel ${data.proactiveBrief ? "" : "empty-panel"}`}>
            {data.proactiveBrief ? (
              <>
                <div className="result-header">
                  <div>
                    <h4>综合风险分：{data.proactiveBrief.riskScore}</h4>
                    <p>{data.proactiveBrief.summary}</p>
                  </div>
                  <RiskBadge level={data.proactiveBrief.overallRiskLevel} />
                </div>
                <div className="split-block">
                  <div>
                    <h4>天气情况</h4>
                    {data.proactiveBrief.weather ? (
                      <>
                        <p className="narrative-text">
                          {data.proactiveBrief.weather.city} / {data.proactiveBrief.weather.summaryDate}
                        </p>
                        <p className="narrative-text">{data.proactiveBrief.weather.summary}</p>
                        <p className="narrative-text">
                          体感温度 {data.proactiveBrief.weather.apparentTemperatureC ?? "-"}℃ / 湿度 {data.proactiveBrief.weather.relativeHumidity ?? "-"}%
                        </p>
                      </>
                    ) : (
                      <EmptyState message="暂无天气数据。" />
                    )}
                  </div>
                  <div>
                    <h4>风险因素</h4>
                    <div className="stack-list">
                      {data.proactiveBrief.factors?.length ? (
                        data.proactiveBrief.factors.map((item) => (
                          <article className="list-card" key={item.code}>
                            <div className="result-header">
                              <strong>{item.title}</strong>
                              <RiskBadge level={item.riskLevel} />
                            </div>
                            <p>{item.detail}</p>
                            <div className="list-card__meta">
                              <span>{item.code}</span>
                              <span>{item.evidence || "暂无证据说明"}</span>
                            </div>
                          </article>
                        ))
                      ) : (
                        <EmptyState message="暂无风险因素。" />
                      )}
                    </div>
                  </div>
                </div>
                <BulletList title="建议动作" items={data.proactiveBrief.suggestions} />
              </>
            ) : (
              "配置城市并同步后，这里会展示主动关怀摘要、天气信息和建议动作。"
            )}
          </div>
        </Card>
      </div>

      <Card>
        <div className="card-head">
          <div>
            <p className="eyebrow">Flare review</p>
            <h3>最近一次发作复盘</h3>
          </div>
        </div>
        <div className={`result-panel ${data.flareReview ? "" : "empty-panel"}`}>
          {data.flareReview ? (
            <>
              <div className="result-header">
                <div>
                  <h4>{data.flareReview.joint} / 疼痛等级 {data.flareReview.painLevel}</h4>
                  <p>发作时间：{formatDateTime(data.flareReview.flareStartedAt)}</p>
                </div>
                <RiskBadge level={data.flareReview.overallRiskLevel} />
              </div>
              <p>{data.flareReview.summary}</p>
              <div className="split-block">
                <div>
                  <h4>疑似诱因</h4>
                  <BulletList title="可能触发项" items={data.flareReview.suspectedTriggers} />
                </div>
                <div>
                  <h4>应对建议</h4>
                  <BulletList title="建议动作" items={data.flareReview.actionSuggestions} />
                </div>
              </div>
              <div className="stack-list">
                <strong className="subtle-title">相关事件</strong>
                {data.flareReview.relatedEvents?.length ? (
                  data.flareReview.relatedEvents.map((item) => (
                    <article className="list-card" key={item.eventId}>
                      <div className="result-header">
                        <strong>{item.title || item.type}</strong>
                        <RiskBadge level={item.riskLevel} />
                      </div>
                      <p>{item.detail}</p>
                      <div className="list-card__meta">
                        <span>{item.type}</span>
                        <span>{formatDateTime(item.occurredAt)}</span>
                      </div>
                    </article>
                  ))
                ) : (
                  <EmptyState message="暂无相关事件。" />
                )}
              </div>
            </>
          ) : (
            "当后端返回发作复盘结果后，这里会展示诱因分析、建议动作和相关事件。"
          )}
        </div>
      </Card>
    </section>
  );
}
