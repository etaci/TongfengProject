import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import { ArraySummary, BulletList } from "../components/HealthBlocks";
import { mealTypeOptions } from "../constants/options";
import { formatDate, formatDateTime, mealTypeLabel } from "../utils/format";

export default function AnalysisPage({ data, busyMap, session, handleMealSubmit, handleLabSubmit }) {
  return (
    <section className="content-section" id="analysis">
      <SectionHeader
        kicker="03 / 分析"
        title="承接后端的饮食识别、化验单解析和画像能力，把分析结果整理成可以持续扩展的动态面板。"
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
              <textarea name="note" rows="3" placeholder="补充说明本次饮食背景、是否聚餐等信息" />
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
              "上传餐盘图片后，这里会展示识别结果、嘌呤估算和后续建议。"
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Lab OCR</p>
              <h3>化验单解析</h3>
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
              "上传化验单后，这里会展示解析指标、风险判断和建议。"
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Meal history</p>
              <h3>饮食记录</h3>
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
              <EmptyState message="暂无饮食分析历史。" />
            )}
          </div>
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Lab archive</p>
              <h3>化验单归档</h3>
            </div>
          </div>
          <div className="masonry-list">
            {data.labs?.length ? (
              data.labs.map((item) => (
                <article className="list-card" key={item.reportId}>
                  <div className="result-header">
                    <strong>{formatDate(item.reportDate)}</strong>
                    <RiskBadge level={item.overallRiskLevel} />
                  </div>
                  <p>{item.summary || "暂无摘要。"}</p>
                  <div className="list-card__meta">
                    <span>指标数：{item.indicators?.length || 0}</span>
                    <span>{item.reportId}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无化验单归档记录。" />
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
              "这里会在后端返回画像后展示标签、触发因素和总结性描述。"
            )}
          </div>
        </Card>
      </div>
    </section>
  );
}
