import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import { ArraySummary, BulletList } from "../components/HealthBlocks";
import { mealTypeOptions } from "../constants/options";
import { formatDate, formatDateTime, mealTypeLabel } from "../utils/format";

export default function AnalysisPage({ data, busyMap, session, handleMealSubmit }) {
  return (
    <section className="content-section" id="analysis">
      <SectionHeader
        kicker="03 / 分析"
        title="聚焦饮食识别、尿酸波动原因分析和用户画像，让用户更快理解“为什么会高、接下来怎么做”。"
      />

      <div className="overview-grid">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Meal analyzer</p>
              <h3>饮食识别</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleMealSubmit}>
            <label>
              <span>餐盘图片</span>
              <input name="file" type="file" accept="image/*" />
            </label>
            <label>
              <span>餐次</span>
              <select name="mealType" defaultValue="LUNCH">
                {mealTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>进食时间</span>
              <input name="takenAt" type="datetime-local" />
            </label>
            <label>
              <span>备注</span>
              <textarea name="note" rows="3" placeholder="补充说明本次饮食背景，例如聚餐、夜宵、应酬等" />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.meal}>
              {busyMap.meal ? "识别中..." : "开始识别"}
            </button>
          </form>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Latest meal result</p>
              <h3>最近一次饮食分析</h3>
            </div>
          </div>
          <div className={`result-panel ${data.mealResult ? "" : "empty-panel"}`}>
            {data.mealResult ? (
              <>
                <div className="result-header">
                  <div>
                    <h4>{mealTypeLabel(data.mealResult.mealType)}</h4>
                    <p>记录时间：{formatDateTime(data.mealResult.takenAt)}</p>
                  </div>
                  <RiskBadge level={data.mealResult.riskLevel} />
                </div>
                <p>{data.mealResult.summary || "暂无分析摘要。"}</p>
                <div className="indicator-grid">
                  <div className="indicator-chip">
                    <span>估算嘌呤</span>
                    <strong>{data.mealResult.purineEstimateMg != null ? `${data.mealResult.purineEstimateMg} mg` : "暂无"}</strong>
                    <small>基于识别食材生成</small>
                  </div>
                  {(data.mealResult.items || []).map((item) => (
                    <div className="indicator-chip" key={`${item.name}-${item.evidence}`}>
                      <span>{item.name}</span>
                      <strong>{item.purineEstimateMg != null ? `${item.purineEstimateMg} mg` : "待估算"}</strong>
                      <small>{item.evidence} / {item.riskLevel}</small>
                    </div>
                  ))}
                </div>
                <BulletList title="建议动作" items={data.mealResult.suggestions} />
              </>
            ) : (
              "上传餐盘图片后，这里会展示识别结果、风险等级和后续建议。"
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Cause analysis</p>
              <h3>尿酸波动原因分析</h3>
            </div>
          </div>
          <div className={`result-panel ${data.uricAcidCauseAnalysis ? "" : "empty-panel"}`}>
            {data.uricAcidCauseAnalysis ? (
              <>
                <div className="result-header">
                  <div>
                    <h4>
                      最近一次尿酸
                      {data.uricAcidCauseAnalysis.latestUricAcidValue != null
                        ? ` ${data.uricAcidCauseAnalysis.latestUricAcidValue} ${data.uricAcidCauseAnalysis.latestUricAcidUnit || ""}`
                        : " 暂无结果"}
                    </h4>
                    <p>{data.uricAcidCauseAnalysis.summary || "暂无原因分析摘要。"}</p>
                  </div>
                  <RiskBadge level={data.uricAcidCauseAnalysis.overallRiskLevel} />
                </div>
                <div className="stats-grid stats-grid--compact">
                  <div className="stat-line">
                    <span>回看范围</span>
                    <strong>{data.uricAcidCauseAnalysis.lookbackDays} 天</strong>
                  </div>
                  <div className="stat-line">
                    <span>目标值</span>
                    <strong>{data.uricAcidCauseAnalysis.targetUricAcidValue || "-"} {data.uricAcidCauseAnalysis.latestUricAcidUnit || ""}</strong>
                  </div>
                  <div className="stat-line">
                    <span>分析时间</span>
                    <strong>{formatDate(data.uricAcidCauseAnalysis.generatedAt)}</strong>
                  </div>
                </div>
                <div className="indicator-grid">
                  {(data.uricAcidCauseAnalysis.factors || []).map((factor) => (
                    <div className="indicator-chip" key={factor.code}>
                      <span>{factor.title}</span>
                      <strong>{factor.riskLevel}</strong>
                      <small>{factor.detail}</small>
                      <small>{factor.evidence}</small>
                    </div>
                  ))}
                </div>
                <BulletList title="建议动作" items={data.uricAcidCauseAnalysis.nextActions} />
              </>
            ) : (
              "补充尿酸、饮食、饮水和发作记录后，这里会自动生成最近一次波动的规则归因结果。"
            )}
          </div>
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Persona summary</p>
              <h3>用户画像</h3>
            </div>
          </div>
          <div className={`result-panel ${data.persona ? "" : "empty-panel"}`}>
            {data.persona ? (
              <>
                <ArraySummary title="画像标签" items={data.persona.tags} emptyMessage="暂无画像标签。" />
                <BulletList title="高频触发因素" items={data.persona.triggers} />
                <p className="narrative-text">{data.persona.narrative || "暂无画像描述。"}</p>
              </>
            ) : (
              "这里会展示后端生成的用户画像标签、诱因和总结描述。"
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Meal history</p>
              <h3>饮食识别历史</h3>
            </div>
          </div>
          <div className="masonry-list">
            {data.meals?.length ? (
              data.meals.map((item) => (
                <article className="list-card" key={item.recordId}>
                  <div className="result-header">
                    <strong>{mealTypeLabel(item.mealType)}</strong>
                    <RiskBadge level={item.riskLevel} />
                  </div>
                  <p>{item.summary || "暂无摘要。"}</p>
                  <div className="list-card__meta">
                    <span>估算嘌呤：{item.purineEstimateMg != null ? `${item.purineEstimateMg} mg` : "暂无"}</span>
                    <span>{formatDateTime(item.takenAt)}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无饮食识别历史。" />
            )}
          </div>
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Analysis hint</p>
              <h3>分析建议</h3>
            </div>
          </div>
          <div className="result-panel">
            <p className="narrative-text">
              当前原因分析以规则驱动为主，会优先关联近 7 天的饮食、饮酒、补水、发作和化验单信号。
              如果希望分析更稳定，优先保持连续 3 至 7 天记录，而不是只录入单个高值。
            </p>
            <BulletList
              title="优先补齐的数据"
              items={[
                "最近一次尿酸结果",
                "近几餐的饮食拍照和备注",
                "最近 1 至 3 天的饮水记录",
                "近期发作或化验单结果",
              ]}
            />
          </div>
        </Card>
      </div>
    </section>
  );
}
