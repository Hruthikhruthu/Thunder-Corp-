import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import AuthProvider, { useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './layouts/AppLayout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Inventory from './pages/Inventory';
import HR from './pages/HR';
import Finance from './pages/Finance';
import Sales from './pages/Sales';
import Reports from './pages/Reports';
import Unauthorized from './pages/Unauthorized';
import Forbidden from './pages/Forbidden';

/**
 * ProtectedPage composes route authorization with the authenticated shell.
 */
const ProtectedPage = ({ children, roles }) => (
  <ProtectedRoute roles={roles}>
    <AppLayout>
      {children}
    </AppLayout>
  </ProtectedRoute>
);

/**
 * DashboardRedirect dynamically routes logged-in users to their role-specific dashboard.
 */
const DashboardRedirect = () => {
  const { user } = useAuth();
  if (!user) return <Navigate to="/unauthorized" replace />;
  if (user.role === 'SUPER_ADMIN') return <Navigate to="/admin/dashboard" replace />;
  if (user.role === 'MANAGER') return <Navigate to="/manager/dashboard" replace />;
  if (user.role === 'STAFF') return <Navigate to="/staff/dashboard" replace />;
  return <Navigate to="/unauthorized" replace />;
};

/**
 * App defines the top-level React route map for ThunderCore ERP.
 */
function App() {
  return (
    <AuthProvider>
      <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes>
          {/* Public Portal Logins */}
          <Route path="/login" element={<Login />} />
          <Route path="/login/admin" element={<Login requiredRole="SUPER_ADMIN" />} />
          <Route path="/login/manager" element={<Login requiredRole="MANAGER" />} />
          <Route path="/login/staff" element={<Login requiredRole="STAFF" />} />

          {/* Authentication & Authorization Error Pages */}
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="/forbidden" element={<Forbidden />} />

          {/* Root Redirector */}
          <Route path="/" element={<ProtectedRoute><DashboardRedirect /></ProtectedRoute>} />

          {/* Role-Specific Dashboards */}
          <Route path="/admin/dashboard" element={<ProtectedPage roles={['SUPER_ADMIN']}><Dashboard role="SUPER_ADMIN" /></ProtectedPage>} />
          <Route path="/manager/dashboard" element={<ProtectedPage roles={['MANAGER']}><Dashboard role="MANAGER" /></ProtectedPage>} />
          <Route path="/staff/dashboard" element={<ProtectedPage roles={['STAFF']}><Dashboard role="STAFF" /></ProtectedPage>} />

          {/* Modules with Access Controls */}
          <Route path="/inventory" element={<ProtectedPage roles={['SUPER_ADMIN', 'MANAGER', 'STAFF']}><Inventory /></ProtectedPage>} />
          <Route path="/hr" element={<ProtectedPage roles={['SUPER_ADMIN', 'MANAGER']}><HR /></ProtectedPage>} />
          <Route path="/finance" element={<ProtectedPage roles={['SUPER_ADMIN', 'MANAGER']}><Finance /></ProtectedPage>} />
          <Route path="/sales" element={<ProtectedPage roles={['SUPER_ADMIN', 'MANAGER', 'STAFF']}><Sales /></ProtectedPage>} />
          <Route path="/reports" element={<ProtectedPage roles={['SUPER_ADMIN', 'MANAGER']}><Reports /></ProtectedPage>} />

          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
