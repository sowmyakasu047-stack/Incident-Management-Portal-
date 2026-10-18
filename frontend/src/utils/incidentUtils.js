import { ROLES, STATUSES } from './constants';

/** The only status an incident may move to next (OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED). */
export function nextStatus(status) {
  const index = STATUSES.indexOf(status);
  return index >= 0 && index < STATUSES.length - 1 ? STATUSES[index + 1] : null;
}

export function isAdmin(user) {
  return user?.role === ROLES.ADMIN;
}

/** Mirrors the backend rule: admin or assignee; the creator may only close a RESOLVED incident. */
export function canChangeStatus(incident, user) {
  if (!user || !nextStatus(incident.status)) return false;
  if (isAdmin(user) || incident.assignedTo === user.userId) return true;
  return incident.createdBy === user.userId && incident.status === 'RESOLVED';
}

/** Closed incidents are read-only; otherwise admin, creator and assignee may edit. */
export function canEdit(incident, user) {
  if (!user || incident.status === 'CLOSED') return false;
  return (
    isAdmin(user) ||
    incident.createdBy === user.userId ||
    incident.assignedTo === user.userId
  );
}

export function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString();
}

/** Turns an Axios error into a readable message, including field-level validation errors. */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data;
  if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    return Object.values(data.fieldErrors).join(' • ');
  }
  if (data?.message) return data.message;
  if (error?.code === 'ERR_NETWORK') return 'Cannot reach the server. Is the backend running?';
  return fallback;
}

export function countBy(items, key) {
  return items.reduce((acc, item) => {
    acc[item[key]] = (acc[item[key]] || 0) + 1;
    return acc;
  }, {});
}
