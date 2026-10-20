import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import UserService from '../services/UserService';
import { ROLES } from '../utils/constants';
import { formatDate, getErrorMessage } from '../utils/incidentUtils';

const EMPTY = { username: '', email: '', password: '', role: ROLES.USER };

/** Admin-only user management: list, create, change role. */
export default function Users() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    try {
      setUsers(await UserService.getAll());
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleChange = (e) => setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const handleCreate = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    try {
      await UserService.create(form);
      setMessage(`User "${form.username}" created.`);
      setForm(EMPTY);
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const handleRoleChange = async (target, role) => {
    setError('');
    setMessage('');
    try {
      await UserService.updateRole(target.id, role);
      setMessage(`${target.username} is now ${role}.`);
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  return (
    <>
      <h1 className="h3 mb-3">User Management</h1>

      {message && <div className="alert alert-success py-2">{message}</div>}
      {error && <div className="alert alert-danger py-2">{error}</div>}

      <div className="row g-4">
        <div className="col-lg-4">
          <div className="card">
            <div className="card-header">Create user</div>
            <div className="card-body">
              <form onSubmit={handleCreate}>
                <div className="mb-3">
                  <label htmlFor="new-username" className="form-label">Username</label>
                  <input id="new-username" name="username" className="form-control" minLength={3}
                         maxLength={50} required value={form.username} onChange={handleChange} />
                </div>
                <div className="mb-3">
                  <label htmlFor="new-email" className="form-label">Email</label>
                  <input id="new-email" name="email" type="email" className="form-control" maxLength={100}
                         required placeholder="name@yourcompany.com" value={form.email} onChange={handleChange} />
                  <div className="form-text">Must be a company email address.</div>
                </div>
                <div className="mb-3">
                  <label htmlFor="new-password" className="form-label">Password</label>
                  <input id="new-password" name="password" type="password" className="form-control"
                         minLength={8} maxLength={100} required autoComplete="new-password"
                         value={form.password} onChange={handleChange} />
                  <div className="form-text">At least 8 characters.</div>
                </div>
                <div className="mb-3">
                  <label htmlFor="new-role" className="form-label">Role</label>
                  <select id="new-role" name="role" className="form-select" value={form.role} onChange={handleChange}>
                    <option value={ROLES.USER}>USER</option>
                    <option value={ROLES.ADMIN}>ADMIN</option>
                  </select>
                </div>
                <button type="submit" className="btn btn-primary w-100">Create user</button>
              </form>
            </div>
          </div>
        </div>

        <div className="col-lg-8">
          <div className="table-responsive">
            <table className="table align-middle">
              <thead className="table-light">
                <tr><th>#</th><th>Username</th><th>Email</th><th>Role</th><th>Created</th></tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.username}{u.id === currentUser.userId && <span className="text-muted"> (you)</span>}</td>
                    <td>{u.email || '-'}</td>
                    <td style={{ width: 160 }}>
                      <select className="form-select form-select-sm" aria-label={`Role for ${u.username}`}
                              value={u.role} disabled={u.id === currentUser.userId}
                              onChange={(e) => handleRoleChange(u, e.target.value)}>
                        <option value={ROLES.USER}>USER</option>
                        <option value={ROLES.ADMIN}>ADMIN</option>
                      </select>
                    </td>
                    <td>{formatDate(u.createdDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </>
  );
}
