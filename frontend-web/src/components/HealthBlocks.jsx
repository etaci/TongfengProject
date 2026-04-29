import Card from "./Card";
import EmptyState from "./EmptyState";
import RiskBadge from "./RiskBadge";
import { formatDateTime } from "../utils/format";

export function BulletList({ title, items }) {
  if (!items?.length) {
    return null;
  }

  return (
    <div>
      <strong className="subtle-title">{title}</strong>
      <ul className="bullet-list">
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </div>
  );
}

export function SummaryList({ items }) {
  if (!items?.length) {
    return <EmptyState message="暂无每日汇总。" />;
  }

  return items.map((item) => (
    <article className="list-card" key={item.summaryDate}>
      <div className="result-header">
        <strong>{item.summaryDate}</strong>
        <RiskBadge level={item.overallRiskLevel} />
      </div>
      <p>{item.summaryText || "暂无总结。"}</p>
      <div className="list-card__meta">
        <span>
          尿酸：
          {item.latestUricAcidValue != null ? `${item.latestUricAcidValue} ${item.latestUricAcidUnit || ""}` : "暂无"}
        </span>
        <span>饮水：{item.totalWaterIntakeMl != null ? `${item.totalWaterIntakeMl} ml` : "暂无"}</span>
      </div>
    </article>
  ));
}

export function RecentRecordPanel({ title, items, renderMeta, emptyMessage = "暂无记录。" }) {
  return (
    <Card>
      <div className="card-head">
        <div>
          <p className="eyebrow">Recent records</p>
          <h3>{title}</h3>
        </div>
      </div>
      <div className="stack-list">
        {items?.length ? (
          items.slice(0, 5).map((item) => {
            const meta = renderMeta(item);

            return (
              <article className="list-card" key={item.recordId}>
                <div className="result-header">
                  <strong>{meta.title}</strong>
                  <RiskBadge level={item.riskLevel} />
                </div>
                <p>{meta.summary}</p>
                <div className="list-card__meta">
                  <span>{meta.metaLeft}</span>
                  <span>{meta.metaRight}</span>
                </div>
              </article>
            );
          })
        ) : (
          <EmptyState message={emptyMessage} />
        )}
      </div>
    </Card>
  );
}

export function ArraySummary({ title, items, emptyMessage = "暂无数据。" }) {
  return (
    <div className="stack-list">
      <strong className="subtle-title">{title}</strong>
      {items?.length ? items.map((item) => <div className="token" key={item}>{item}</div>) : <EmptyState message={emptyMessage} />}
    </div>
  );
}

export function MemberList({ title, items, onViewSummary, onRemove, busyMap }) {
  return (
    <div className="stack-list">
      <strong className="subtle-title">{title}</strong>
      {items?.length ? (
        items.map((item) => (
          <article className="list-card" key={item.bindingCode}>
            <div className="result-header">
              <strong>{item.patientNickname || item.caregiverNickname}</strong>
              <span className="inline-tag">{item.relationType}</span>
            </div>
            <p>
              {item.patientNickname && item.caregiverNickname
                ? `${item.patientNickname} / ${item.caregiverNickname}`
                : "家庭绑定已建立"}
            </p>
            <div className="list-card__meta">
              <span>{item.status}</span>
              <span>{formatDateTime(item.createdAt)}</span>
            </div>
            <div className="action-row">
              {item.patientUserId && onViewSummary ? (
                <button className="ghost-button action-button" type="button" onClick={() => onViewSummary(item.patientUserId)}>
                  查看患者摘要
                </button>
              ) : null}
              <button
                className="ghost-button action-button"
                type="button"
                disabled={busyMap[`family-binding-${item.bindingCode}`]}
                onClick={() => onRemove(item.bindingCode)}
              >
                {busyMap[`family-binding-${item.bindingCode}`] ? "处理中..." : "解除绑定"}
              </button>
            </div>
          </article>
        ))
      ) : (
        <EmptyState message="暂无家庭绑定关系。" />
      )}
    </div>
  );
}
