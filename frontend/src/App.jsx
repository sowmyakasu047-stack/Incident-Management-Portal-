import { Navigate, Route, Routes } from 'react-router';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import { ROLES } from './utils/constants';
import CreateIncident from './pages/CreateIncident';
import Dashboard from './pages/Dashboard';
import EditIncident from './pages/EditIncident';
import IncidentList from './pages/IncidentList';
import Login from './pages/Login';
import Users from './pages/Users';

// Wraps a page with the auth guard and the common layout (navbar + container).
const guarded = (page, roles) => (
  <ProtectedRoute roles={roles}>
    <Layout>{page}</Layout>
  </ProtectedRoute>
);

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={guarded(<Dashboard />)} />
      <Route path="/incidents" element={guarded(<IncidentList />)} />
      <Route path="/incidents/new" element={guarded(<CreateIncident />)} />
      <Route path="/incidents/:id" element={guarded(<EditIncident />)} />
      <Route path="/users" element={guarded(<Users />, [ROLES.ADMIN])} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
