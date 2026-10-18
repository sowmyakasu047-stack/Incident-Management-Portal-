import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { PriorityBadge, StatusBadge } from '../components/Badges';
import IncidentForm from '../components/IncidentForm';
import { useAuth } from '../context/AuthContext';
import IncidentService from '../services/IncidentService';
import UserService from '../services/UserService';
import { STATUS_LABELS } from '../utils/constants';
import { canChangeStatus, canEdit, formatDate, getErrorMessage, isAdmin, nextStatus } from '../utils/incidentUtils';

/** Incident details page: edit fields, move through the lifecycle, assign and delete (admin). */
export default function EditIncident() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const admin = isAdmin(user);

  const [incident, setIncident] = useState(null);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    try {
      setIncident(await IncidentService.getById(id));
      setError('');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (admin) {
      UserService.getAll().then(setUsers).catch(() => setUsers([]));
    }
  }, [admin]);

  const run = async (action, successMessage) => {
    setMessage('');
    setError('');
    try {
      const result = await action();
      if (successMessage) setMessage(successMessage);
      return result;
    } catch (err) {
      setError(getErrorMessage(err));
      return undefined;
    }
  };

  const handleSave = async (values) => {
    const updated = await run(() => IncidentService.update(id, values), 'Incident updated.');
    if (updated) setIncident(updated);
  };

  const handleStatus = async (status) => {
    const updated = await run(() => IncidentService.changeStatus(id, status),
      `Status changed to ${STATUS_LABELS[status]}.`);
    if (updated) setIncident(updated);
  };

  const handleAssign = async (userId) => {
    if (!userId) return;
    const updated = await run(() => IncidentService.assign(id, Number(userId)), 'Incident assigned.');
    if (updated) setIncident(updated);
  };

  const handleDelete = async () => {
    if (!window.confirm(`Delete incident #${id}?`)) return;
    try {
      await IncidentService.remove(id);
      navigate('/incidents');
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  if (loading) return <p className="text-muted">Loading…</p>;
  if (!incident) {
    return (
      <>
        <div className="alert alert-danger">{error || 'Incident not found.'}</div>
        <Link to="/incidents">Back to incidents</Link>
      </>
    );
  }

  const editable = canEdit(incident, user);
  const next = nextStatus(incident.status);

  return (
    <div style={{ maxWidth: 860 }}>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h3 mb-0">
          Incident #{incident.id} <StatusBadge status={incident.status} />{' '}
          <PriorityBadge priority={incident.priority} />
        </h1>
        <Link to="/incidents" className="btn btn-outline-secondary btn-sm">← Back</Link>
      </div>

      {message && <div className="alert alert-success py-2">{message}</div>}
      {error && <div className="alert alert-danger py-2">{error}</div>}

      <div className="card mb-3">
        <div className="card-body">
          <dl className="row mb-0">
            <dt className="col-sm-3">Created by</dt>
            <dd className="col-sm-9">{incident.createdByUsername} · {formatDate(incident.createdDate)}</dd>
            <dt className="col-sm-3">Assigned to</dt>
            <dd className="col-sm-9">
              {incident.assignedToUsername
                ? `${incident.assignedToUsername} · ${formatDate(incident.assignedDate)}`
                : 'Unassigned'}
            </dd>
            <dt className="col-sm-3">Last updated</dt>
            <dd className="col-sm-9 mb-0">{formatDate(incident.updatedDate)}</dd>
          </dl>
        </div>
      </div>

      <div className="card mb-3">
        <div className="card-body d-flex flex-wrap gap-3 align-items-center">
          {canChangeStatus(incident, user) && (
            <button type="button" className="btn btn-primary" onClick={() => handleStatus(next)}>
              Move to {STATUS_LABELS[next]}
            </button>
          )}

          {admin && incident.status !== 'CLOSED' && (
            <div className="d-flex align-items-center gap-2">
              <label htmlFor="assign" className="mb-0">Assign to</label>
              <select id="assign" className="form-select form-select-sm" style={{ width: 200 }}
                      value={incident.assignedTo ?? ''} onChange={(e) => handleAssign(e.target.value)}>
                <option value="">Select user…</option>
                {users.map((u) => <option key={u.id} value={u.id}>{u.username}</option>)}
              </select>
            </div>
          )}

          {admin && (
            <button type="button" className="btn btn-outline-danger ms-auto" onClick={handleDelete}>
              Delete
            </button>
          )}

          {!canChangeStatus(incident, user) && !admin && (
            <span className="text-muted">No status actions available for you on this incident.</span>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          {editable ? 'Edit details' : 'Details (read-only)'}
          {incident.status === 'CLOSED' && ' – closed incidents cannot be modified'}
        </div>
        <div className="card-body">
          <IncidentForm
            key={incident.updatedDate}
            initialValues={incident}
            onSubmit={handleSave}
            submitLabel="Save changes"
            disabled={!editable}
            lockApplication
          />
        </div>
      </div>
    </div>
  );
}
