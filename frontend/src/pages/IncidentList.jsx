import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import IncidentTable from '../components/IncidentTable';
import { useAuth } from '../context/AuthContext';
import IncidentService from '../services/IncidentService';
import UserService from '../services/UserService';
import { PRIORITIES, STATUSES, STATUS_LABELS } from '../utils/constants';
import { getErrorMessage, isAdmin } from '../utils/incidentUtils';

export default function IncidentList() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Filters live in the URL so they survive refresh and can be linked from the dashboard.
  const status = searchParams.get('status') || '';
  const priority = searchParams.get('priority') || '';
  const application = searchParams.get('application') || '';

  const [applicationInput, setApplicationInput] = useState(application);
  const [incidents, setIncidents] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setIncidents(await IncidentService.getAll({ status, priority, application }));
      setError('');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [status, priority, application]);

  useEffect(() => { load(); }, [load]);

  // Only admins can assign, so only admins need the user list.
  useEffect(() => {
    if (isAdmin(user)) {
      UserService.getAll().then(setUsers).catch(() => setUsers([]));
    }
  }, [user]);

  const updateFilter = (key, value) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value); else next.delete(key);
    setSearchParams(next);
  };

  const clearFilters = () => {
    setApplicationInput('');
    setSearchParams({});
  };

  const runAction = async (action, successMessage) => {
    setMessage('');
    setError('');
    try {
      await action();
      setMessage(successMessage);
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const handleStatusChange = (incident, next) =>
    runAction(() => IncidentService.changeStatus(incident.id, next),
      `Incident #${incident.id} moved to ${STATUS_LABELS[next]}.`);

  const handleAssign = (incident, userId) =>
    runAction(() => IncidentService.assign(incident.id, userId), `Incident #${incident.id} assigned.`);

  const handleDelete = (incident) => {
    if (!window.confirm(`Delete incident #${incident.id} "${incident.title}"?`)) return;
    runAction(() => IncidentService.remove(incident.id), `Incident #${incident.id} deleted.`);
  };

  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h3 mb-0">Incidents</h1>
        <Link to="/incidents/new" className="btn btn-primary">+ New Incident</Link>
      </div>

      <form
        className="row g-2 mb-3"
        onSubmit={(e) => { e.preventDefault(); updateFilter('application', applicationInput.trim()); }}
      >
        <div className="col-md-3">
          <select className="form-select" aria-label="Filter by status" value={status}
                  onChange={(e) => updateFilter('status', e.target.value)}>
            <option value="">All statuses</option>
            {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
          </select>
        </div>
        <div className="col-md-3">
          <select className="form-select" aria-label="Filter by priority" value={priority}
                  onChange={(e) => updateFilter('priority', e.target.value)}>
            <option value="">All priorities</option>
            {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
          </select>
        </div>
        <div className="col-md-4">
          <input className="form-control" placeholder="Application (exact name, press Enter)"
                 aria-label="Filter by application" value={applicationInput}
                 onChange={(e) => setApplicationInput(e.target.value)} />
        </div>
        <div className="col-md-2">
          <button type="button" className="btn btn-outline-secondary w-100" onClick={clearFilters}>
            Clear
          </button>
        </div>
      </form>

      {message && <div className="alert alert-success py-2">{message}</div>}
      {error && <div className="alert alert-danger py-2">{error}</div>}

      {loading ? (
        <p className="text-muted">Loading…</p>
      ) : (
        <IncidentTable
          incidents={incidents}
          user={user}
          users={users}
          onStatusChange={handleStatusChange}
          onAssign={handleAssign}
          onDelete={handleDelete}
        />
      )}
    </>
  );
}
