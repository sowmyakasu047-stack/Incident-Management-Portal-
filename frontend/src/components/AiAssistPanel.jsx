import { useState } from 'react';
import { Link } from 'react-router';
import AiService from '../services/AiService';
import { getErrorMessage } from '../utils/incidentUtils';

/**
 * AI helpers for the create form: suggest priority/category and find similar incidents.
 * Needs a title and description. All output is plain text; the user stays in control.
 */
export default function AiAssistPanel({ values, onApply }) {
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [suggestion, setSuggestion] = useState(null);
  const [similar, setSimilar] = useState(null);

  const ready = values.title.trim() !== '' && values.description.trim() !== '';

  const run = async (kind) => {
    setBusy(kind);
    setError('');
    try {
      if (kind === 'suggest') {
        const result = await AiService.suggest(values.title.trim(), values.description.trim());
        setSuggestion(result);
        onApply(result.priority, result.category);
      } else {
        setSimilar(await AiService.similar(values.title.trim(), values.description.trim()));
      }
    } catch (err) {
      setError(getErrorMessage(err, 'The AI helper is not available right now.'));
    } finally {
      setBusy('');
    }
  };

  return (
    <div className="border rounded p-3 mb-3 bg-light">
      <div className="d-flex flex-wrap gap-2 align-items-center">
        <strong className="me-2">AI assistant</strong>
        <button type="button" className="btn btn-sm btn-outline-primary" disabled={!ready || busy !== ''}
                onClick={() => run('suggest')}>
          {busy === 'suggest' ? 'Thinking…' : '✨ Suggest priority & category'}
        </button>
        <button type="button" className="btn btn-sm btn-outline-secondary" disabled={!ready || busy !== ''}
                onClick={() => run('similar')}>
          {busy === 'similar' ? 'Searching…' : 'Find similar incidents'}
        </button>
        {!ready && <small className="text-muted">Fill in title and description first.</small>}
      </div>

      {error && <div className="alert alert-warning py-2 mt-2 mb-0">{error}</div>}

      {suggestion && (
        <div className="small mt-2">
          Suggested <strong>{suggestion.priority}</strong> / <strong>{suggestion.category}</strong>
          {suggestion.reason && <> — {suggestion.reason}</>}. Applied to the form; change it if you disagree.
        </div>
      )}

      {similar && (
        <div className="small mt-2">
          {similar.length === 0 ? (
            'No similar incidents found.'
          ) : (
            <>
              Possibly related:
              <ul className="mb-0">
                {similar.map((i) => (
                  <li key={i.id}>
                    <Link to={`/incidents/${i.id}`} target="_blank">#{i.id} {i.title}</Link> ({i.status})
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </div>
  );
}
