import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * ProtectedRoute guards authenticated ERP pages.
 *
 * <p>It waits for AuthProvider to restore localStorage session state, then
 * redirects anonymous users to login before business pages are rendered.</p>
 */
const ProtectedRoute = ({ children, roles }) => {
  const { isAuthenticated, loading, hasRole } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-dark-950 flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-white/60 text-sm">Loading ThunderCore ERP...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (roles?.length && !hasRole(...roles)) return <Navigate to="/forbidden" replace />;
  return children;
};

export default ProtectedRoute;
