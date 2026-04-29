import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import { RecentRecordPanel } from "../components/HealthBlocks";
import { recordTypeOptions } from "../constants/options";
import { formatDateTime } from "../utils/format";
import { emptyToNull, toIsoString } from "../utils/forms";

function RecordInput({ name, label, type = "text", ...props }) {
  return (
    <label>
      <span>{label}</span>
      <input name={name} type={type} {...props} />
    </label>
  );
}

function RecordTextarea({ name, label, rows = 3, ...props }) {
  return (
    <label>
      <span>{label}</span>
      <textarea name={name} rows={rows} {...props} />
    </label>
  );
}

export default function RecordsPage({
  app,
  data,
  busyMap,
  session,
  recordCenterType,
  handleRecordSubmit,
  handleRecordFilterChange,
  withErrorHandling,
  recordEditDraft,
  setRecordEditDraft,
  recordChangeReason,
  setRecordChangeReason,
  handleRecordUpdate,
  recordRestoreReason,
  setRecordRestoreReason,
  handleRecordRestore,
}) {
  return (
    <>
      <section className="content-section" id="records">
        <SectionHeader
          kicker="02 / 记录"
          title="继续承接后端新增的多类型记录能力，把录入、回看、纠错和审计收进统一的动态记录中心。"
        />

        <div className="record-grid">
          <Card>
            <div className="card-head">
              <div>
                <p className="eyebrow">Quick record</p>
                <h3>尿酸记录</h3>
              </div>
            </div>
            <form
              className="stack-form compact-form"
              onSubmit={(event) =>
                handleRecordSubmit(event, "uric", "/api/v1/records/uric-acid", "尿酸记录已提交。", (formData) => ({
                  value: Number(formData.get("value")),
                  unit: String(formData.get("unit")).trim(),
                  measuredAt: toIsoString(emptyToNull(formData.get("measuredAt"))),
                  source: emptyToNull(formData.get("source")),
                  note: emptyToNull(formData.get("note")),
                }))
              }
            >
              <RecordInput name="value" label="数值" type="number" min="1" placeholder="例如：468" required />
              <RecordInput name="unit" label="单位" defaultValue="umol/L" required />
              <RecordInput name="measuredAt" label="测量时间" type="datetime-local" />
              <RecordInput name="source" label="来源" placeholder="例如：家用设备 / 医院化验" />
              <RecordTextarea name="note" label="备注" placeholder="补充说明本次测量背景" />
              <button className="primary-button" type="submit" disabled={!session || busyMap.uric}>
                {busyMap.uric ? "提交中..." : "提交尿酸记录"}
              </button>
            </form>
          </Card>

          <Card>
            <div className="card-head">
              <div>
                <p className="eyebrow">Quick record</p>
                <h3>体重记录</h3>
              </div>
            </div>
            <form
              className="stack-form compact-form"
              onSubmit={(event) =>
                handleRecordSubmit(event, "weight", "/api/v1/records/weight", "体重记录已提交。", (formData) => ({
                  value: Number(formData.get("value")),
                  measuredAt: toIsoString(emptyToNull(formData.get("measuredAt"))),
                  note: emptyToNull(formData.get("note")),
                }))
              }
            >
              <RecordInput name="value" label="体重（kg）" type="number" step="0.1" min="0.1" placeholder="例如：72.5" required />
              <RecordInput name="measuredAt" label="测量时间" type="datetime-local" />
              <RecordTextarea name="note" label="备注" placeholder="例如：晨起空腹" />
              <button className="primary-button" type="submit" disabled={!session || busyMap.weight}>
                {busyMap.weight ? "提交中..." : "提交体重记录"}
              </button>
            </form>
          </Card>

          <Card>
            <div className="card-head">
              <div>
                <p className="eyebrow">Quick record</p>
                <h3>饮水记录</h3>
              </div>
            </div>
            <form
              className="stack-form compact-form"
              onSubmit={(event) =>
                handleRecordSubmit(event, "hydration", "/api/v1/records/hydration", "饮水记录已提交。", (formData) => ({
                  waterIntakeMl: Number(formData.get("waterIntakeMl")),
                  urineColorLevel: Number(formData.get("urineColorLevel")),
                  checkedAt: toIsoString(emptyToNull(formData.get("checkedAt"))),
                  note: emptyToNull(formData.get("note")),
                }))
              }
            >
              <RecordInput name="waterIntakeMl" label="饮水量（ml）" type="number" min="0" placeholder="例如：600" required />
              <RecordInput name="urineColorLevel" label="尿液颜色等级（1-5）" type="number" min="1" max="5" placeholder="例如：3" required />
              <RecordInput name="checkedAt" label="记录时间" type="datetime-local" />
              <RecordTextarea name="note" label="备注" placeholder="例如：运动后补水" />
              <button className="primary-button" type="submit" disabled={!session || busyMap.hydration}>
                {busyMap.hydration ? "提交中..." : "提交饮水记录"}
              </button>
            </form>
          </Card>

          <Card>
            <div className="card-head">
              <div>
                <p className="eyebrow">Quick record</p>
                <h3>发作记录</h3>
              </div>
            </div>
            <form
              className="stack-form compact-form"
              onSubmit={(event) =>
                handleRecordSubmit(event, "flare", "/api/v1/records/flares", "发作记录已提交。", (formData) => ({
                  joint: String(formData.get("joint")).trim(),
                  painLevel: Number(formData.get("painLevel")),
                  startedAt: toIsoString(emptyToNull(formData.get("startedAt"))),
                  durationNote: emptyToNull(formData.get("durationNote")),
                  note: emptyToNull(formData.get("note")),
                }))
              }
            >
              <RecordInput name="joint" label="发作关节" placeholder="例如：左脚大拇趾" required />
              <RecordInput name="painLevel" label="疼痛等级（1-10）" type="number" min="1" max="10" placeholder="例如：8" required />
              <RecordInput name="startedAt" label="开始时间" type="datetime-local" />
              <RecordInput name="durationNote" label="持续时长" placeholder="例如：约 4 小时" />
              <RecordTextarea name="note" label="备注" placeholder="补充诱因、处理方式等" />
              <button className="primary-button" type="submit" disabled={!session || busyMap.flare}>
                {busyMap.flare ? "提交中..." : "提交发作记录"}
              </button>
            </form>
          </Card>

          <Card>
            <div className="card-head">
              <div>
                <p className="eyebrow">Quick record</p>
                <h3>血压记录</h3>
              </div>
            </div>
            <form
              className="stack-form compact-form"
              onSubmit={(event) =>
                handleRecordSubmit(event, "bloodPressure", "/api/v1/records/blood-pressure", "血压记录已提交。", (formData) => ({
                  systolicPressure: Number(formData.get("systolicPressure")),
                  diastolicPressure: Number(formData.get("diastolicPressure")),
                  pulseRate: formData.get("pulseRate") ? Number(formData.get("pulseRate")) : null,
                  unit: emptyToNull(formData.get("unit")) || "mmHg",
                  measuredAt: toIsoString(emptyToNull(formData.get("measuredAt"))),
                  source: emptyToNull(formData.get("source")),
                  note: emptyToNull(formData.get("note")),
                }))
              }
            >
              <RecordInput name="systolicPressure" label="收缩压" type="number" min="60" max="260" placeholder="例如：126" required />
              <RecordInput name="diastolicPressure" label="舒张压" type="number" min="40" max="180" placeholder="例如：82" required />
              <RecordInput name="pulseRate" label="脉搏" type="number" min="30" max="240" placeholder="例如：75" />
              <RecordInput name="unit" label="单位" defaultValue="mmHg" />
              <RecordInput name="measuredAt" label="测量时间" type="datetime-local" />
              <RecordInput name="source" label="来源" placeholder="例如：家庭血压计" />
              <RecordTextarea name="note" label="备注" placeholder="补充症状或测量状态" />
              <button className="primary-button" type="submit" disabled={!session || busyMap.bloodPressure}>
                {busyMap.bloodPressure ? "提交中..." : "提交血压记录"}
              </button>
            </form>
          </Card>
        </div>

        <div className="overview-grid overview-grid--secondary">
          <RecentRecordPanel
            title="最近尿酸"
            items={data.recordSnapshots?.uricAcid}
            renderMeta={(item) => ({
              title: `${item.value} ${item.unit || ""}`,
              summary: item.note || "暂无备注",
              metaLeft: item.source || "无来源说明",
              metaRight: formatDateTime(item.measuredAt),
            })}
          />
          <RecentRecordPanel
            title="最近体重"
            items={data.recordSnapshots?.weight}
            renderMeta={(item) => ({
              title: `${item.value} ${item.unit || "kg"}`,
              summary: item.note || "暂无备注",
              metaLeft: item.source || "无来源说明",
              metaRight: formatDateTime(item.measuredAt),
            })}
          />
          <RecentRecordPanel
            title="最近血压"
            items={data.recordSnapshots?.bloodPressure}
            renderMeta={(item) => ({
              title: `${item.systolicPressure}/${item.diastolicPressure} ${item.unit || "mmHg"}`,
              summary: item.note || "暂无备注",
              metaLeft: item.pulseRate ? `脉搏 ${item.pulseRate}` : "无脉搏数据",
              metaRight: formatDateTime(item.measuredAt),
            })}
          />
        </div>

        <div className="overview-grid overview-grid--secondary">
          <RecentRecordPanel
            title="最近饮水"
            items={data.recordSnapshots?.hydration}
            renderMeta={(item) => ({
              title: `${item.waterIntakeMl} ml`,
              summary: item.note || "暂无备注",
              metaLeft: `颜色等级 ${item.urineColorLevel}`,
              metaRight: formatDateTime(item.checkedAt),
            })}
          />
          <RecentRecordPanel
            title="最近发作"
            items={data.recordSnapshots?.flares}
            renderMeta={(item) => ({
              title: item.joint || "未标注部位",
              summary: item.note || item.durationNote || "暂无补充说明",
              metaLeft: `疼痛等级 ${item.painLevel}`,
              metaRight: formatDateTime(item.startedAt),
            })}
          />
        </div>
      </section>

      <section className="content-section" id="record-center">
        <SectionHeader
          kicker="03 / 记录中心"
          title="统一查看所有记录，支持详情加载、修改、删除、审计追溯和版本恢复。"
        />

        <div className="overview-grid">
          <Card className="span-2">
            <div className="card-head">
              <div>
                <p className="eyebrow">Record center</p>
                <h3>记录列表</h3>
              </div>
              <label className="inline-control">
                <span>筛选类型</span>
                <select value={recordCenterType} onChange={(event) => handleRecordFilterChange(event.target.value)}>
                  {recordTypeOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="masonry-list">
              {data.recordCenter?.items?.length ? (
                data.recordCenter.items.map((item) => (
                  <article className="list-card" key={`${item.type}-${item.recordId}`}>
                    <div className="result-header">
                      <strong>{item.title}</strong>
                      <RiskBadge level={item.riskLevel} />
                    </div>
                    <p>{item.summary}</p>
                    <div className="list-card__meta">
                      <span>{item.type}</span>
                      <span>{formatDateTime(item.occurredAt)}</span>
                    </div>
                    <div className="action-row">
                      <button className="ghost-button action-button" type="button" onClick={() => withErrorHandling(() => app.loadRecordDetailData(item.type, item.recordId))}>
                        查看详情
                      </button>
                      {(item.tags || []).map((tag) => (
                        <span className="token token--tiny" key={tag}>{tag}</span>
                      ))}
                    </div>
                  </article>
                ))
              ) : (
                <EmptyState message="暂无记录中心数据。" />
              )}
            </div>
            {data.recordCenter?.hasMore ? (
              <div className="action-row">
                <button
                  className="ghost-button action-button"
                  type="button"
                  disabled={busyMap.recordCenter}
                  onClick={() => withErrorHandling(() => app.loadRecordCenterData({ type: recordCenterType, cursor: data.recordCenter.nextCursor, append: true }))}
                >
                  {busyMap.recordCenter ? "加载中..." : "加载更多"}
                </button>
              </div>
            ) : null}
          </Card>

          <Card>
            <div className="card-head">
              <div>
                <p className="eyebrow">Detail & audit</p>
                <h3>详情与审计</h3>
              </div>
            </div>
            <div className={`result-panel ${data.recordDetail ? "" : "empty-panel"}`}>
              {data.recordDetail ? (
                <>
                  <div className="result-header">
                    <div>
                      <h4>{data.recordDetail.title}</h4>
                      <p>{data.recordDetail.summary}</p>
                    </div>
                    <RiskBadge level={data.recordDetail.riskLevel} />
                  </div>

                  <form className="stack-list" onSubmit={handleRecordUpdate}>
                    {(data.recordDetail.fields || []).map((field) => (
                      <label key={field.key} className="stack-form__inline">
                        <span>{field.label}</span>
                        <input
                          value={recordEditDraft[field.key] || ""}
                          onChange={(event) => setRecordEditDraft((current) => ({ ...current, [field.key]: event.target.value }))}
                        />
                      </label>
                    ))}
                    <label className="stack-form__inline">
                      <span>来源</span>
                      <input value={recordEditDraft.source || ""} onChange={(event) => setRecordEditDraft((current) => ({ ...current, source: event.target.value }))} />
                    </label>
                    <label className="stack-form__inline">
                      <span>备注</span>
                      <textarea rows="3" value={recordEditDraft.note || ""} onChange={(event) => setRecordEditDraft((current) => ({ ...current, note: event.target.value }))} />
                    </label>
                    <label className="stack-form__inline">
                      <span>更正原因</span>
                      <input value={recordChangeReason} onChange={(event) => setRecordChangeReason(event.target.value)} placeholder="说明为什么要修改这条记录" />
                    </label>
                    <div className="action-row">
                      <button className="primary-button action-button" type="submit" disabled={busyMap.recordUpdate}>
                        {busyMap.recordUpdate ? "保存中..." : "保存更改"}
                      </button>
                      <button
                        className="ghost-button action-button"
                        type="button"
                        disabled={busyMap.recordDelete}
                        onClick={() => withErrorHandling(() => app.removeRecord(data.recordDetail.type, data.recordDetail.recordId))}
                      >
                        {busyMap.recordDelete ? "删除中..." : "删除记录"}
                      </button>
                    </div>
                  </form>

                  <label className="stack-form__inline">
                    <span>恢复原因</span>
                    <input value={recordRestoreReason} onChange={(event) => setRecordRestoreReason(event.target.value)} />
                  </label>

                  <div className="stack-list">
                    <strong className="subtle-title">审计历史</strong>
                    {data.recordAudits?.length ? (
                      data.recordAudits.map((audit) => (
                        <article className="list-card" key={audit.auditId}>
                          <div className="result-header">
                            <strong>{audit.action}</strong>
                            <span className="inline-tag">{formatDateTime(audit.operatedAt)}</span>
                          </div>
                          <p>{audit.summary}</p>
                          <div className="list-card__meta">
                            <span>{audit.changeReason || "无变更原因"}</span>
                            <span>{audit.auditId}</span>
                          </div>
                          <div className="action-row">
                            <button className="ghost-button action-button" type="button" disabled={busyMap.recordRestore} onClick={() => handleRecordRestore(audit.auditId)}>
                              {busyMap.recordRestore ? "恢复中..." : "恢复到此版本"}
                            </button>
                          </div>
                        </article>
                      ))
                    ) : (
                      <EmptyState message="暂无审计历史。" />
                    )}
                  </div>
                </>
              ) : (
                "从左侧选择一条记录后，这里会展示详情、可编辑字段和审计历史。"
              )}
            </div>
          </Card>
        </div>
      </section>
    </>
  );
}
