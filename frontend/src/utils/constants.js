export const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const ROLES = { ADMIN: 'ADMIN', USER: 'USER' };

export const STATUS_LABELS = {
  OPEN: 'Open',
  IN_PROGRESS: 'In Progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
};

// Bootstrap colour names used by the badges.
export const STATUS_COLORS = {
  OPEN: 'primary',
  IN_PROGRESS: 'warning',
  RESOLVED: 'success',
  CLOSED: 'secondary',
};

export const PRIORITY_COLORS = {
  LOW: 'info',
  MEDIUM: 'primary',
  HIGH: 'warning',
  CRITICAL: 'danger',
};
