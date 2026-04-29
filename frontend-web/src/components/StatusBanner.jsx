export default function StatusBanner({ tone = "neutral", message }) {
  return <div className={`status-banner is-${tone}`}>{message}</div>;
}
