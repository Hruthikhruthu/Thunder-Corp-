import axios from 'axios';

/**
 * Shared Axios client for every REST call made by the React application.
 *
 * <p>The base URL is intentionally relative so local Vite proxy and Docker
 * Nginx proxy can route /api requests to the backend without environment-specific
 * frontend rebuilds.</p>
 */
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

api.interceptors.request.use(
  (config) => {
    // Attach the persisted JWT so browser refreshes keep API authorization.
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Expired or invalid tokens should return the user to a clean login state.
    const isLoginRequest = error.config?.url?.includes('/auth/login');
    if (error.response?.status === 401 && !isLoginRequest) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
