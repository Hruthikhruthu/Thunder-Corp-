import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const AuthContext = createContext(null);
const TOKEN_STORAGE_KEY = 'token';
const USER_STORAGE_KEY = 'user';

const clearStoredSession = () => {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(USER_STORAGE_KEY);
  delete axios.defaults.headers.common['Authorization'];
};

const parseStoredUser = (storedUser) => {
  if (!storedUser) return null;

  try {
    const parsedUser = JSON.parse(storedUser);
    if (!parsedUser || typeof parsedUser !== 'object' || !parsedUser.role) {
      throw new Error('Stored user session is missing required fields');
    }
    return parsedUser;
  } catch (error) {
    console.warn('Ignoring invalid stored auth session:', error.message);
    clearStoredSession();
    return null;
  }
};

/**
 * useAuth exposes the authenticated user, JWT, and session actions.
 *
 * <p>The guard catches accidental usage outside AuthProvider early, which keeps
 * protected pages and API wiring predictable.</p>
 */
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
};

/**
 * AuthProvider is the frontend session boundary.
 *
 * <p>It restores JWT state from localStorage on refresh, configures Axios'
 * default Authorization header, and provides login/logout actions to routed
 * pages and the application shell.</p>
 */
const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
    const parsedUser = parseStoredUser(localStorage.getItem(USER_STORAGE_KEY));

    if (storedToken && parsedUser) {
      setToken(storedToken);
      setUser(parsedUser);
      axios.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`;
    } else if (storedToken || parsedUser) {
      clearStoredSession();
    }

    setLoading(false);
  }, []);

  /**
   * Persists a successful backend login response into React and browser state.
   *
   * @param tokenValue signed JWT returned by /api/auth/login
   * @param userData user profile required by navigation and notifications
   */
  const login = (tokenValue, userData) => {
    setToken(tokenValue);
    setUser(userData);
    localStorage.setItem(TOKEN_STORAGE_KEY, tokenValue);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(userData));
    axios.defaults.headers.common['Authorization'] = `Bearer ${tokenValue}`;
  };

  /**
   * Clears the browser session and removes Authorization headers.
   *
   * <p>AppLayout also closes the active STOMP client before calling this method
   * so realtime connections do not continue after sign-out.</p>
   */
  const logout = () => {
    setToken(null);
    setUser(null);
    clearStoredSession();
  };

  const isAuthenticated = !!token;
  const hasRole = (...roles) => roles.includes(user?.role);
  const canManage = hasRole('SUPER_ADMIN', 'MANAGER');

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated, loading, hasRole, canManage }}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthProvider;
