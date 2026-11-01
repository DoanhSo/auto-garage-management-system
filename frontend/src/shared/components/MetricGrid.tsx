import type { ReactNode } from 'react';

export type MetricTone = 'info' | 'success' | 'warning' | 'danger';

export interface MetricItem {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  icon?: ReactNode;
  tone?: MetricTone;
}

export function MetricGrid({ items, label }: { items: MetricItem[]; label: string }) {
  return (
    <section
      className="metric-grid snapshot-metrics metric-row"
      style={{ ["--metric-cols" as string]: items.length }}
      aria-label={label}
    >
      {items.map((item) => (
        <article key={item.label} className={`metric-card metric-tone-${item.tone ?? 'info'}`}>
          <div className="metric-label">
            <span>{item.label}</span>
            {item.icon && <span className="metric-icon">{item.icon}</span>}
          </div>
          <strong className="metric-value">{item.value}</strong>
          {item.hint && <p>{item.hint}</p>}
        </article>
      ))}
    </section>
  );
}
