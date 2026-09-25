import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { AlertTriangle, LogIn, Zap, UserCheck, ShieldAlert } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';

/**
 * Login page supporting role-restricted portals.
 *
 * Accessible at:
 *   /login          — generic portal with role tabs
 *   /login/admin    — SUPER_ADMIN only
 *   /login/manager  — MANAGER only
 *   /login/staff    — STAFF only
 */
const Login = ({ requiredRole: initialRequiredRole }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated, user: authUser } = useAuth();

  const [requiredRole, setRequiredRole] = useState(initialRequiredRole || null);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Sync role from route path when accessed generically
  useEffect(() => {
    if (initialRequiredRole) {
      setRequiredRole(initialRequiredRole);
    } else {
      if (location.pathname.includes('/admin')) setRequiredRole('SUPER_ADMIN');
      else if (location.pathname.includes('/manager')) setRequiredRole('MANAGER');
      else if (location.pathname.includes('/staff')) setRequiredRole('STAFF');
      else setRequiredRole(null);
    }
  }, [initialRequiredRole, location.pathname]);

  // Pre-fill demo credentials based on portal
  useEffect(() => {
    if (requiredRole === 'SUPER_ADMIN') {
      setEmail('admin@thundercore.com');
      setPassword('Admin@123');
    } else if (requiredRole === 'MANAGER') {
      setEmail('manager@thundercore.com');
      setPassword('Manager@123');
    } else if (requiredRole === 'STAFF') {
      setEmail('staff@thundercore.com');
      setPassword('Staff@123');
    } else {
      setEmail('');
      setPassword('');
    }
    setError('');
  }, [requiredRole]);

  // Redirect already-authenticated users to their dashboard
  useEffect(() => {
    if (isAuthenticated && authUser) {
      redirectByRole(authUser.role);
    }
  }, [isAuthenticated, authUser]);

  const redirectByRole = (role) => {
    if (role === 'SUPER_ADMIN') navigate('/admin/dashboard', { replace: true });
    else if (role === 'MANAGER') navigate('/manager/dashboard', { replace: true });
    else if (role === 'STAFF') navigate('/staff/dashboard', { replace: true });
    else navigate('/', { replace: true });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');

    try {
      const res = await api.post('/auth/login', { email, password });
      const { token, id, role, firstName, lastName, email: responseEmail } = res.data;

      // Enforce portal-specific role restriction
      if (requiredRole && role !== requiredRole) {
        const portalName = requiredRole.replace('_', ' ');
        throw new Error(`This portal is for ${portalName} accounts only. Your role is ${role}.`);
      }

      login(token, { id, email: responseEmail, role, firstName, lastName });
      toast.success(`Welcome back, ${firstName}!`);
      redirectByRole(role);
    } catch (err) {
      const msg =
        err.message ||
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Invalid email or password';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  // Portal-specific styling
  const getPortalDetails = () => {
    switch (requiredRole) {
      case 'SUPER_ADMIN':
        return {
          title: 'Super Admin Portal',
          accentColor: 'bg-primary-500',
          borderColor: 'border-primary-500/30',
          btnClass: 'btn-primary',
          icon: ShieldAlert,
        };
      case 'MANAGER':
        return {
          title: 'Manager Portal',
          accentColor: 'bg-cyan-500',
          borderColor: 'border-cyan-500/30',
          btnClass: 'bg-cyan-600 hover:bg-cyan-500 text-white font-semibold py-2.5 px-5 rounded-lg transition-all duration-200',
          icon: UserCheck,
        };
      case 'STAFF':
        return {
          title: 'Staff Portal',
          accentColor: 'bg-emerald-500',
          borderColor: 'border-emerald-500/30',
          btnClass: 'bg-emerald-600 hover:bg-emerald-500 text-white font-semibold py-2.5 px-5 rounded-lg transition-all duration-200',
          icon: LogIn,
        };
      default:
        return {
          title: 'Sign In',
          accentColor: 'bg-primary-500',
          borderColor: 'border-white/10',
          btnClass: 'btn-primary',
          icon: Zap,
        };
    }
  };

  const portal = getPortalDetails();
  const PortalIcon = portal.icon;

  return (
    <div className="min-h-screen bg-dark-950 flex items-center justify-center p-4">
      <div className="w-full max-w-md animate-fade-in">
        {/* Header */}
        <div className="text-center mb-8">
          <div className={`inline-flex items-center justify-center w-14 h-14 ${portal.accentColor} rounded-lg mb-4 text-white shadow-lg shadow-black/20`}>
            <PortalIcon size={26} />
          </div>
          <h1 className="text-3xl font-bold text-white">ThunderCore ERP</h1>
          <p className="text-white/40 mt-1 text-sm">{portal.title}</p>
        </div>

        <div className={`card border ${portal.borderColor} bg-dark-900 shadow-2xl relative overflow-hidden`}>
          {/* Accent top bar */}
          <div className={`absolute top-0 left-0 right-0 h-[2px] ${portal.accentColor}`} />

          {/* Role tabs — only shown on generic /login */}
          {!initialRequiredRole && (
            <div className="flex border-b border-white/5 -mx-6 -mt-6 mb-6">
              {[
                { id: 'SUPER_ADMIN', label: 'Admin' },
                { id: 'MANAGER', label: 'Manager' },
                { id: 'STAFF', label: 'Staff' },
              ].map((tab) => (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setRequiredRole(tab.id)}
                  className={`flex-1 py-3 text-center text-xs font-semibold tracking-wider uppercase border-b-2 transition-all ${
                    requiredRole === tab.id
                      ? 'border-primary-500 text-white bg-white/5'
                      : 'border-transparent text-white/40 hover:text-white/70'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          )}

          <h2 className="text-xl font-semibold text-white mb-6">Credential Sign In</h2>

          {error && (
            <div className="mb-4 px-4 py-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-sm flex items-center gap-2">
              <AlertTriangle size={16} className="shrink-0" />
              <span className="font-medium">{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-white/60 text-sm font-medium mb-1.5">
                Email Address
              </label>
              <input
                id="email-input"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="email@thundercore.com"
                className="input-field"
                required
                autoComplete="email"
              />
            </div>
            <div>
              <label className="block text-white/60 text-sm font-medium mb-1.5">
                Password
              </label>
              <input
                id="password-input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Password"
                className="input-field"
                required
                autoComplete="current-password"
              />
            </div>

            <button
              id="login-btn"
              type="submit"
              disabled={loading}
              className={`${portal.btnClass} w-full mt-4 flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed`}
            >
              {loading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Authenticating...
                </>
              ) : (
                <>
                  <LogIn size={16} />
                  Login to ERP
                </>
              )}
            </button>
          </form>

          {/* Demo credentials hint */}
          {requiredRole && (
            <p className="text-white/30 text-[10px] text-center mt-6 tracking-wide font-mono">
              DEMO: {email} / {password}
            </p>
          )}
        </div>

        {/* Portal links */}
        {!initialRequiredRole && (
          <div className="mt-4 flex justify-center gap-4 text-xs text-white/30">
            <a href="/login/admin" className="hover:text-white/60 transition-colors">Admin Portal</a>
            <span>·</span>
            <a href="/login/manager" className="hover:text-white/60 transition-colors">Manager Portal</a>
            <span>·</span>
            <a href="/login/staff" className="hover:text-white/60 transition-colors">Staff Portal</a>
          </div>
        )}

        <p className="text-center text-white/20 text-xs mt-6">
          © 2026 ThunderCore ERP. All rights reserved.
        </p>
      </div>
    </div>
  );
};

export default Login;
