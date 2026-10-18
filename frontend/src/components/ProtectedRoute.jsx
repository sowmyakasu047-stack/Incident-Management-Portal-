import { Navigate, useLocation } from 'react-router';
import { useAuth } from '../context/AuthContext';

/** Redirects to /login when signed out, and to / when the user's role is not allowed. */
export default function ProtectedRoute({ roles, children }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return children;
}
