import EmptyState from "./EmptyState";

function buildPath(points, width, height, padding) {
  const numbers = points.map((point) => Number(point.value)).filter(Number.isFinite);
  if (!numbers.length) {
    return "";
  }

  const min = Math.min(...numbers);
  const max = Math.max(...numbers);
  const step = points.length > 1 ? (width - padding * 2) / (points.length - 1) : 0;

  return points
    .map((point, index) => {
      const yValue = Number(point.value);
      const ratio = max === min ? 0.5 : (yValue - min) / (max - min);
      const x = padding + step * index;
      const y = height - padding - ratio * (height - padding * 2);
      return `${x},${y}`;
    })
    .join(" ");
}

export default function TrendCard({ title, points, emptyText }) {
  if (!points?.length) {
    return (
      <article className="trend-card">
        <h4>{title}</h4>
        <EmptyState message={emptyText} />
      </article>
    );
  }

  const polyline = buildPath(points, 280, 96, 12);
  const latest = points[points.length - 1];

  return (
    <article className="trend-card">
      <h4>{title}</h4>
      <svg className="sparkline" viewBox="0 0 280 96" preserveAspectRatio="none" aria-hidden="true">
        <polyline points={polyline} fill="none" stroke="rgba(197, 192, 177, 0.55)" strokeWidth="12" />
        <polyline points={polyline} fill="none" stroke="#ff4f00" strokeWidth="3" />
      </svg>
      <div className="trend-card__meta">
        <span>{latest?.date || "-"}</span>
        <strong>
          {latest?.value ?? "暂无"} {latest?.unit || ""}
        </strong>
      </div>
    </article>
  );
}
