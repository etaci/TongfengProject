import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import SectionHeader from "../components/SectionHeader";
import { deviceTypeOptions, metricTypeOptions } from "../constants/options";
import { formatDateTime } from "../utils/format";

export default function DevicesPage({
  app,
  data,
  busyMap,
  session,
  deviceDraft,
  setDeviceDraft,
  handleDeviceBindingSubmit,
  deviceSyncDraft,
  setDeviceSyncDraft,
  handleDeviceSyncSubmit,
  deviceSyncQueue,
  handleRemoveSyncQueueItem,
  handleSubmitSyncQueue,
  setDeviceSyncQueue,
  withErrorHandling,
}) {
  return (
    <section className="content-section" id="devices">
      <SectionHeader
        kicker="06 / 设备"
        title="把设备目录、绑定、同步和事件回传整合为动态设备工作台，便于后续继续扩展更多硬件能力。"
      />

      <div className="overview-grid">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Catalog</p>
              <h3>设备目录</h3>
            </div>
          </div>
          <div className="stack-list">
            {data.deviceCatalog?.length ? (
              data.deviceCatalog.map((item) => (
                <article className="list-card" key={item.profileCode}>
                  <div className="result-header">
                    <strong>{item.deviceTypeName}</strong>
                    <span className="inline-tag">{item.vendorName}</span>
                  </div>
                  <p>{item.deviceModel || "暂无型号信息"}</p>
                  <div className="list-card__meta">
                    <span>{(item.supportedMetricTypes || []).join(" / ")}</span>
                    <span>{item.profileCode}</span>
                  </div>
                  <p className="narrative-text">{item.bindingHint}</p>
                </article>
              ))
            ) : (
              <EmptyState message="暂无设备目录。" />
            )}
          </div>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Overview</p>
              <h3>设备概览</h3>
            </div>
          </div>
          {data.deviceOverview ? (
            <>
              <div className="stats-grid">
                {[
                  ["设备总数", data.deviceOverview.totalDevices],
                  ["活跃设备", data.deviceOverview.activeDevices],
                  ["近期同步", data.deviceOverview.recentlySyncedDevices],
                  ["需关注设备", data.deviceOverview.attentionDevices],
                ].map(([label, value]) => (
                  <div className="stat-line" key={label}>
                    <span>{label}</span>
                    <strong>{value}</strong>
                  </div>
                ))}
              </div>
              <div className="masonry-list">
                {(data.deviceOverview.devices || []).map((item) => (
                  <article className="list-card" key={item.deviceCode}>
                    <div className="result-header">
                      <strong>{item.aliasName || item.deviceTypeName}</strong>
                      <span className={`inline-tag ${item.syncHealthStatus === "ATTENTION" ? "risk-yellow" : ""}`}>{item.syncHealthStatus}</span>
                    </div>
                    <p>{item.latestSummary || "暂无同步摘要。"}</p>
                    <div className="list-card__meta">
                      <span>{item.totalSyncCount} 次同步</span>
                      <span>{formatDateTime(item.lastSyncedAt)}</span>
                    </div>
                  </article>
                ))}
              </div>
            </>
          ) : (
            <EmptyState message="暂无设备概览。" />
          )}
        </Card>
      </div>

      <div className="overview-grid">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Bind device</p>
              <h3>绑定设备</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleDeviceBindingSubmit}>
            <label>
              <span>设备类型</span>
              <select value={deviceDraft.deviceType} onChange={(event) => setDeviceDraft((current) => ({ ...current, deviceType: event.target.value }))}>
                {deviceTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>厂商</span>
              <input value={deviceDraft.vendorName} onChange={(event) => setDeviceDraft((current) => ({ ...current, vendorName: event.target.value }))} placeholder="例如：Tongfeng" />
            </label>
            <label>
              <span>型号</span>
              <input value={deviceDraft.deviceModel} onChange={(event) => setDeviceDraft((current) => ({ ...current, deviceModel: event.target.value }))} placeholder="例如：UA-1" />
            </label>
            <label>
              <span>序列号</span>
              <input value={deviceDraft.serialNumber} onChange={(event) => setDeviceDraft((current) => ({ ...current, serialNumber: event.target.value }))} placeholder="例如：TF-SN-001" />
            </label>
            <label>
              <span>别名</span>
              <input value={deviceDraft.aliasName} onChange={(event) => setDeviceDraft((current) => ({ ...current, aliasName: event.target.value }))} placeholder="例如：家用尿酸仪" />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.deviceBind}>
              {busyMap.deviceBind ? "绑定中..." : "绑定设备"}
            </button>
          </form>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Device list</p>
              <h3>已绑定设备</h3>
            </div>
          </div>
          <div className="masonry-list">
            {data.devices?.length ? (
              data.devices.map((item) => (
                <article className="list-card" key={item.deviceCode}>
                  <div className="result-header">
                    <strong>{item.aliasName || item.vendorName}</strong>
                    <span className="inline-tag">{item.status}</span>
                  </div>
                  <p>{item.deviceType} / {item.deviceModel || "暂无型号"}</p>
                  <div className="list-card__meta">
                    <span>设备码：{item.deviceCode}</span>
                    <span>最近同步：{formatDateTime(item.lastSyncedAt)}</span>
                  </div>
                  <div className="action-row">
                    <button className="ghost-button action-button" type="button" onClick={() => setDeviceSyncDraft((current) => ({ ...current, deviceCode: item.deviceCode }))}>
                      设为同步目标
                    </button>
                    <button className="ghost-button action-button" type="button" onClick={() => withErrorHandling(() => app.loadDeviceSyncHistory(item.deviceCode))}>
                      查看同步历史
                    </button>
                    <button
                      className="ghost-button action-button"
                      type="button"
                      disabled={busyMap[`device-unbind-${item.deviceCode}`]}
                      onClick={() => withErrorHandling(() => app.removeDeviceBinding(item.deviceCode))}
                    >
                      {busyMap[`device-unbind-${item.deviceCode}`] ? "解绑中..." : "解绑设备"}
                    </button>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无已绑定设备。" />
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Sync data</p>
              <h3>新增同步事件</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleDeviceSyncSubmit}>
            <label>
              <span>目标设备</span>
              <select value={deviceSyncDraft.deviceCode} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, deviceCode: event.target.value }))}>
                <option value="">请选择设备</option>
                {data.devices?.map((item) => (
                  <option key={item.deviceCode} value={item.deviceCode}>
                    {item.aliasName || item.deviceCode}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>指标类型</span>
              <select value={deviceSyncDraft.metricType} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, metricType: event.target.value }))}>
                {metricTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>外部事件 ID</span>
              <input
                value={deviceSyncDraft.externalEventId}
                onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, externalEventId: event.target.value }))}
                placeholder="例如：evt-ua-1"
              />
            </label>
            <label>
              <span>测量时间</span>
              <input type="datetime-local" value={deviceSyncDraft.measuredAt} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, measuredAt: event.target.value }))} />
            </label>
            {deviceSyncDraft.metricType === "HYDRATION" ? (
              <>
                <label>
                  <span>饮水量（ml）</span>
                  <input type="number" value={deviceSyncDraft.waterIntakeMl} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, waterIntakeMl: event.target.value }))} />
                </label>
                <label>
                  <span>尿液颜色等级</span>
                  <input type="number" min="1" max="5" value={deviceSyncDraft.urineColorLevel} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, urineColorLevel: event.target.value }))} />
                </label>
              </>
            ) : deviceSyncDraft.metricType === "BLOOD_PRESSURE" ? (
              <>
                <label>
                  <span>收缩压</span>
                  <input type="number" value={deviceSyncDraft.value} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, value: event.target.value }))} />
                </label>
                <label>
                  <span>舒张压</span>
                  <input type="number" value={deviceSyncDraft.diastolicPressure || ""} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, diastolicPressure: event.target.value }))} />
                </label>
                <label>
                  <span>脉搏</span>
                  <input type="number" value={deviceSyncDraft.pulseRate || ""} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, pulseRate: event.target.value }))} />
                </label>
                <label>
                  <span>单位</span>
                  <input value={deviceSyncDraft.unit} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, unit: event.target.value }))} />
                </label>
              </>
            ) : (
              <>
                <label>
                  <span>数值</span>
                  <input type="number" step="0.1" value={deviceSyncDraft.value} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, value: event.target.value }))} />
                </label>
                <label>
                  <span>单位</span>
                  <input value={deviceSyncDraft.unit} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, unit: event.target.value }))} />
                </label>
              </>
            )}
            <label>
              <span>备注</span>
              <textarea rows="3" value={deviceSyncDraft.note} onChange={(event) => setDeviceSyncDraft((current) => ({ ...current, note: event.target.value }))} placeholder="补充说明本次同步背景" />
            </label>
            <button className="primary-button" type="submit" disabled={!session}>
              {busyMap.deviceSync ? "处理中..." : "加入同步队列"}
            </button>
          </form>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Sync queue</p>
              <h3>同步队列</h3>
            </div>
          </div>
          <div className="stack-list">
            {deviceSyncQueue.length ? (
              deviceSyncQueue.map((item, index) => (
                <article className="list-card" key={`${item.deviceCode}-${item.externalEventId}-${index}`}>
                  <div className="result-header">
                    <strong>{item.metricType}</strong>
                    <span className="inline-tag">{item.deviceCode}</span>
                  </div>
                  <p>
                    {item.metricType === "HYDRATION"
                      ? `饮水 ${item.waterIntakeMl} ml / 颜色等级 ${item.urineColorLevel}`
                      : item.metricType === "BLOOD_PRESSURE"
                        ? `血压 ${item.systolicPressure}/${item.diastolicPressure} ${item.unit || "mmHg"}`
                        : `数值 ${item.value} ${item.unit || ""}`}
                  </p>
                  <div className="list-card__meta">
                    <span>{item.externalEventId}</span>
                    <span>{formatDateTime(item.measuredAt)}</span>
                  </div>
                  <div className="action-row">
                    <button className="ghost-button action-button" type="button" onClick={() => handleRemoveSyncQueueItem(index)}>
                      删除
                    </button>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="同步队列为空，新增数据后会先进入这里。" />
            )}
          </div>
          <div className="action-row">
            <button className="primary-button action-button" type="button" disabled={!deviceSyncQueue.length || busyMap.deviceSync} onClick={handleSubmitSyncQueue}>
              {busyMap.deviceSync ? "提交中..." : "批量提交队列"}
            </button>
            {deviceSyncQueue.length ? (
              <button className="ghost-button action-button" type="button" onClick={() => setDeviceSyncQueue([])}>
                清空队列
              </button>
            ) : null}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Sync events</p>
              <h3>同步事件</h3>
            </div>
          </div>
          <div className="stack-list">
            {data.deviceSyncEvents?.length ? (
              data.deviceSyncEvents.map((item) => (
                <article className="list-card" key={item.syncCode}>
                  <div className="result-header">
                    <strong>{item.metricType}</strong>
                    <span className="inline-tag">{item.syncStatus}</span>
                  </div>
                  <p>{item.summary}</p>
                  <div className="list-card__meta">
                    <span>生成记录：{item.resultRecordId || "暂无"}</span>
                    <span>{formatDateTime(item.measuredAt)}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无同步事件。" />
            )}
          </div>
        </Card>
      </div>
    </section>
  );
}
