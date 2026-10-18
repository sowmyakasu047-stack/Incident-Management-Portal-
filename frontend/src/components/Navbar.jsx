import { NavLink, useNavigate } from 'react-router';
import { useAuth } from '../context/AuthContext';
import { isAdmin } from '../utils/incidentUtils';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const linkClass = ({ isActive }) => `nav-link${isActive ? ' active' : ''}`;

  return (
    <nav className="navbar navbar-expand-md navbar-dark bg-dark mb-4">
      <div className="container">
        <span className="navbar-brand fw-semibold">Incident Portal</span>
        {user && (
          <>
            <ul className="navbar-nav me-auto">
              <li className="nav-item">
                <NavLink end to="/" className={linkClass}>Dashboard</NavLink>
              </li>
              <li className="nav-item">
                <NavLink end to="/incidents" className={linkClass}>Incidents</NavLink>
              </li>
              <li className="nav-item">
                <NavLink to="/incidents/new" className={linkClass}>New Incident</NavLink>
              </li>
              {isAdmin(user) && (
                <li className="nav-item">
                  <NavLink to="/users" className={linkClass}>Users</NavLink>
                </li>
              )}
            </ul>
            <div className="d-flex align-items-center gap-3 text-light">
              <span>
                {user.username} <span className="badge text-bg-light">{user.role}</span>
              </span>
              <button type="button" className="btn btn-outline-light btn-sm" onClick={handleLogout}>
                Logout
              </button>
            </div>
          </>
        )}
      </div>
    </nav>
  );
}
