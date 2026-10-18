import { Link } from 'react-router';
import { PriorityBadge, StatusBadge } from './Badges';
import { STATUS_LABELS } from '../utils/constants';
import { canChangeStatus, formatDate, isAdmin, nextStatus } from '../utils/incidentUtils';

/**
 * Presentational table. Parents pass callbacks; admins additionally get assign + delete controls.
 * `users` is only needed (and only loaded) for admins.
 */
export default function IncidentTable({ incidents, user, users = [], onStatusChange, onAssign, onDelete }) {
  if (incidents.length === 0) {
    return <div className="alert alert-light border">No incidents found.</div>;
  }

  const admin = isAdmin(user);

  return (
    <div className="table-responsive">
      <table className="table table-hover align-middle">
        <thead className="table-light">
          <tr>
            <th>#</th>
            <th>Title</th>
            <th>Application</th>
            <th>Priority</th>
            <th>Status</th>
            <th>Created by</th>
            <th>Assigned to</th>
            <th>Created</th>
            <th className="text-end">Actions</th>
          </tr>
        </thead>
        <tbody>
          {incidents.map((incident) => {
            const next = nextStatus(incident.status);
            return (
              <tr key={incident.id}>
                <td>{incident.id}</td>
                <td>
                  <Link to={`/incidents/${incident.id}`}>{incident.title}</Link>
                </td>
                <td>{incident.application}</td>
                <td><PriorityBadge priority={incident.priority} /></td>
                <td><StatusBadge status={incident.status} /></td>
                <td>{incident.createdByUsername}</td>
                <td>
                  {admin && incident.status !== 'CLOSED' ? (
                    <select
                      className="form-select form-select-sm"
                      aria-label={`Assign incident ${incident.id}`}
                      value={incident.assignedTo ?? ''}
                      onChange={(e) => e.target.value && onAssign(incident, Number(e.target.value))}
                    >
                      <option value="">Unassigned</option>
                      {users.map((u) => (
                        <option key={u.id} value={u.id}>{u.username}</option>
                      ))}
                    </select>
                  ) : (
                    incident.assignedToUsername || '-'
                  )}
                </td>
                <td className="text-nowrap">{formatDate(incident.createdDate)}</td>
                <td className="text-end text-nowrap">
                  <Link className="btn btn-sm btn-outline-secondary me-1" to={`/incidents/${incident.id}`}>
                    View
                  </Link>
                  {canChangeStatus(incident, user) && (
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-primary me-1"
                      onClick={() => onStatusChange(incident, next)}
                    >
                      → {STATUS_LABELS[next]}
                    </button>
                  )}
                  {admin && (
                    <button
                      type="button"
                      className="btn btn-sm btn-outline-danger"
                      onClick={() => onDelete(incident)}
                    >
                      Delete
                    </button>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
