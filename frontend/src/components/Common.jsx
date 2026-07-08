import {
  CheckCircle2,
  LoaderCircle,
  Search,
  XCircle,
} from "lucide-react";
import { ROLE_LABELS } from "../utils.js";

export function SectionHeader({ icon, title, subtitle, action }) {
  return (
    <div className="section-header">
      <div className="section-title">
        <span className="section-icon">{icon}</span>
        <div>
          <h1>{title}</h1>
          {subtitle && <p>{subtitle}</p>}
        </div>
      </div>
      {action && <div className="section-action">{action}</div>}
    </div>
  );
}

export function Metric({ icon, label, value }) {
  return (
    <div className="metric">
      <span>{icon}</span>
      <div>
        <strong>{value}</strong>
        <small>{label}</small>
      </div>
    </div>
  );
}

export function StatusBadge({ status }) {
  return <span className={`status-badge status-${status?.toLowerCase()}`}>{status}</span>;
}

export function RoleBadge({ role }) {
  return <span className={`role-badge role-${role?.toLowerCase()}`}>{ROLE_LABELS[role] || role}</span>;
}

export function Message({ type = "info", text }) {
  const Icon = type === "error" ? XCircle : type === "success" ? CheckCircle2 : Search;
  return (
    <div className={`message ${type}`}>
      <Icon size={17} />
      <span>{text}</span>
    </div>
  );
}

export function InlineLoader({ text }) {
  return (
    <div className="inline-loader">
      <LoaderCircle className="spin" size={18} />
      <span>{text}</span>
    </div>
  );
}

export function LoadingScreen() {
  return (
    <section className="panel loading-panel">
      <InlineLoader text="Apertura SmartMall" />
    </section>
  );
}

export function EmptyState({ icon, title }) {
  return (
    <div className="empty-state">
      <span>{icon}</span>
      <strong>{title}</strong>
    </div>
  );
}
