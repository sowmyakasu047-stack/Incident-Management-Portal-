import { PRIORITY_COLORS, STATUS_COLORS, STATUS_LABELS } from '../utils/constants';

export function StatusBadge({ status }) {
  return (
    <span className={`badge text-bg-${STATUS_COLORS[status] || 'secondary'}`}>
      {STATUS_LABELS[status] || status}
    </span>
  );
}

export function PriorityBadge({ priority }) {
  return <span className={`badge text-bg-${PRIORITY_COLORS[priority] || 'secondary'}`}>{priority}</span>;
}
