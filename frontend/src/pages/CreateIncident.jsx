import { useState } from 'react';
import { useNavigate } from 'react-router';
import IncidentForm from '../components/IncidentForm';
import IncidentService from '../services/IncidentService';
import { getErrorMessage } from '../utils/incidentUtils';

export default function CreateIncident() {
  const navigate = useNavigate();
  const [error, setError] = useState('');

  const handleSubmit = async (values) => {
    setError('');
    try {
      const created = await IncidentService.create(values);
      navigate(`/incidents/${created.id}`);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  return (
    <div style={{ maxWidth: 760 }}>
      <h1 className="h3 mb-3">Create Incident</h1>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="card">
        <div className="card-body">
          <IncidentForm onSubmit={handleSubmit} submitLabel="Create Incident" />
        </div>
      </div>
    </div>
  );
}
