import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { PriorityBadge, StatusBadge } from '../components/Badges';
import { useAuth } from '../context/AuthContext';
import IncidentService from '../services/IncidentService';
import { PRIORITIES, STATUSES, STATUS_LABELS, STATUS_COLORS } from '../utils/constants';
import { countBy, formatDate, getErrorMessage, isAdmin } from '../utils/incidentUtils';

export default function Dashboard() {
  const { user } = useAuth();
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    IncidentService.getAll()
      .then(setIncidents)
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="text-muted">Loading…</p>;

  const byStatus = countBy(incidents, 'status');
  const byPriority = countBy(incidents, 'priority');
  const recent = incidents.slice(0, 5); // API returns newest first

  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h1 className="h3 mb-0">Dashboard</h1>
          <small className="text-muted">
            {isAdmin(user) ? 'All incidents' : 'Incidents you created or are assigned to'}
          </small>
        </div>
        <Link to="/incidents/new" className="btn btn-primary">+ New Incident</Link>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="row g-3 mb-4">
        {STATUSES.map((status) => (
          <div className="col-6 col-md-3" key={status}>
            <Link to={`/incidents?status=${status}`} className="text-decoration-none">
              <div className={`card border-${STATUS_COLORS[status]} h-100`}>
                <div className="card-body">
                  <div className="text-muted small">{STATUS_LABELS[status]}</div>
                  <div className="display-6">{byStatus[status] || 0}</div>
                </div>
              </div>
            </Link>
          </div>
        ))}
      </div>

      <div className="row g-3">
        <div className="col-md-4">
          <div className="card h-100">
            <div className="card-header">By priority</div>
            <ul className="list-group list-group-flush">
              {PRIORITIES.map((p) => (
                <li key={p} className="list-group-item d-flex justify-content-between align-items-center">
                  <PriorityBadge priority={p} />
                  <span>{byPriority[p] || 0}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="col-md-8">
          <div className="card h-100">
            <div className="card-header d-flex justify-content-between">
              <span>Recent incidents</span>
              <Link to="/incidents">View all</Link>
            </div>
            {recent.length === 0 ? (
              <div className="card-body text-muted">No incidents yet.</div>
            ) : (
              <ul className="list-group list-group-flush">
                {recent.map((incident) => (
                  <li key={incident.id} className="list-group-item d-flex justify-content-between align-items-center">
                    <div>
                      <Link to={`/incidents/${incident.id}`}>{incident.title}</Link>
                      <div className="small text-muted">
                        {incident.application} · {formatDate(incident.createdDate)}
                      </div>
                    </div>
                    <div className="d-flex gap-2">
                      <PriorityBadge priority={incident.priority} />
                      <StatusBadge status={incident.status} />
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
