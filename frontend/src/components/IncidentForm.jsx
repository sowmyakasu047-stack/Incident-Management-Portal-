import { useState } from 'react';
import { PRIORITIES } from '../utils/constants';
import AiAssistPanel from './AiAssistPanel';

const EMPTY = { title: '', description: '', application: '', priority: 'MEDIUM', category: '' };

/** Shared by Create and Edit. `application` cannot be changed after creation (backend rule). */
export default function IncidentForm({
  initialValues = EMPTY,
  onSubmit,
  submitLabel,
  disabled = false,
  lockApplication = false,
  enableAi = false,
}) {
  const [values, setValues] = useState({ ...EMPTY, ...initialValues });
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => setValues((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await onSubmit(values);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <fieldset disabled={disabled || submitting}>
        <div className="mb-3">
          <label htmlFor="title" className="form-label">Title</label>
          <input id="title" name="title" className="form-control" maxLength={100} required
                 value={values.title} onChange={handleChange} />
        </div>

        <div className="mb-3">
          <label htmlFor="description" className="form-label">Description</label>
          <textarea id="description" name="description" className="form-control" rows={5}
                    maxLength={1000} required value={values.description} onChange={handleChange} />
        </div>

        <div className="row">
          <div className="col-md-4 mb-3">
            <label htmlFor="application" className="form-label">Application</label>
            <input id="application" name="application" className="form-control" maxLength={100} required
                   readOnly={lockApplication} value={values.application} onChange={handleChange} />
          </div>
          <div className="col-md-4 mb-3">
            <label htmlFor="category" className="form-label">Category</label>
            <input id="category" name="category" className="form-control" maxLength={50} required
                   placeholder="e.g. Outage, Performance" value={values.category} onChange={handleChange} />
          </div>
          <div className="col-md-4 mb-3">
            <label htmlFor="priority" className="form-label">Priority</label>
            <select id="priority" name="priority" className="form-select"
                    value={values.priority} onChange={handleChange}>
              {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
            </select>
          </div>
        </div>

        {enableAi && (
          <AiAssistPanel
            values={values}
            onApply={(priority, category) => setValues((prev) => ({ ...prev, priority, category }))}
          />
        )}

        <button type="submit" className="btn btn-primary">
          {submitting ? 'Saving…' : submitLabel}
        </button>
      </fieldset>
    </form>
  );
}
