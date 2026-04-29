import { riskLabel, riskTone } from "../utils/format";

export default function RiskBadge({ level }) {
  return <span className={`risk-badge risk-${riskTone(level)}`}>{riskLabel(level)}</span>;
}
