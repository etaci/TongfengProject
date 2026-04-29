export default function Card({ children, className = "" }) {
  return <article className={`content-card ${className}`.trim()}>{children}</article>;
}
