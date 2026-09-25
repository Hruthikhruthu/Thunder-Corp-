import React, { useCallback, useEffect, useRef, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {
  Bell,
  FileBarChart,
  Gauge,
  Handshake,
  LogOut,
  Menu,
  Package,
  Users,
  Wallet,
  Zap,
} from 'lucide-react';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';

// NAV_ITEMS will be generated dynamically within the component based on user role

/**
 * AppLayout is the authenticated ERP shell.
 *
 * <p>Responsibilities:
 * - persistent module navigation
 * - authenticated user profile and logout
 * - notification bell state
 * - STOMP subscriptions for dashboard and notification events
 * - live connection status indicator
 * </p>
 */
const AppLayout = ({ children }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [showNotif, setShowNotif] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [wsConnected, setWsConnected] = useState(false);
  const stompClientRef = useRef(null);

  useEffect(() => {
    if (!user?.id) return;

    // Load persisted inbox state before realtime messages begin arriving.
    api.get('/notifications/me')
      .then((res) => {
        const data = res.data?.data || [];
        setNotifications(data);
        setUnreadCount(data.filter((notification) => !notification.isRead).length);
      })
      .catch(() => {
        setNotifications([]);
        setUnreadCount(0);
      });
  }, [user?.id]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return undefined;

    // Connect to the backend STOMP endpoint using the same JWT as REST calls.
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      reconnectDelay: 5000,
      onConnect: () => {
        setWsConnected(true);
        // Private queue for alerts targeted to the authenticated user.
        client.subscribe('/user/queue/notifications', (message) => {
          try {
            const notification = JSON.parse(message.body);
            setNotifications((current) => [notification, ...current]);
            setUnreadCount((current) => current + 1);
          } catch {
            setWsConnected(false);
          }
        });
        // Shared topic that tells dashboard pages to refresh their KPI data.
        client.subscribe('/topic/dashboard', (message) => {
          try {
            window.dispatchEvent(new CustomEvent('dashboard:update', { detail: JSON.parse(message.body) }));
          } catch {
            window.dispatchEvent(new CustomEvent('dashboard:update'));
          }
        });
      },
      onDisconnect: () => setWsConnected(false),
      onWebSocketClose: () => setWsConnected(false),
      onStompError: () => setWsConnected(false),
      onWebSocketError: () => setWsConnected(false),
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  /** Logs out the user and closes any active realtime connection. */
  const handleLogout = useCallback(() => {
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
    }
    logout();
    navigate('/login');
  }, [logout, navigate]);

  /** Optimistically clears unread state and syncs it with the backend. */
  const markAllRead = useCallback(() => {
    setUnreadCount(0);
    setNotifications((current) => current.map((notification) => ({ ...notification, isRead: true })));
    api.patch('/notifications/me/read-all').catch(() => {});
  }, []);

  return (
    <div className="flex h-screen bg-dark-950 overflow-hidden">
      <aside className={`${sidebarOpen ? 'w-64' : 'w-16'} transition-all duration-300 flex flex-col bg-dark-900 border-r border-white/5 shrink-0`}>
        <div className="flex items-center gap-3 px-5 py-5 border-b border-white/5">
          <div className="w-9 h-9 bg-primary-500 rounded-lg flex items-center justify-center text-white shrink-0">
            <Zap size={18} />
          </div>
          {sidebarOpen && (
            <div>
              <p className="text-white font-bold text-sm">ThunderCore</p>
              <p className="text-white/40 text-xs">ERP Platform</p>
            </div>
          )}
        </div>

        <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
          {[
            { to: user?.role === 'SUPER_ADMIN' ? '/admin/dashboard' : user?.role === 'MANAGER' ? '/manager/dashboard' : '/staff/dashboard', label: 'Dashboard', icon: Gauge, roles: ['SUPER_ADMIN', 'MANAGER', 'STAFF'] },
            { to: '/inventory', label: 'Inventory', icon: Package, roles: ['SUPER_ADMIN', 'MANAGER', 'STAFF'] },
            { to: '/hr', label: 'HR & Employees', icon: Users, roles: ['SUPER_ADMIN', 'MANAGER'] },
            { to: '/finance', label: 'Finance', icon: Wallet, roles: ['SUPER_ADMIN', 'MANAGER'] },
            { to: '/sales', label: 'Sales & CRM', icon: Handshake, roles: ['SUPER_ADMIN', 'MANAGER', 'STAFF'] },
            { to: '/reports', label: 'Reports', icon: FileBarChart, roles: ['SUPER_ADMIN', 'MANAGER'] },
          ].filter(item => item.roles.includes(user?.role)).map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to.endsWith('/dashboard')}
              className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
            >
              <Icon size={18} className="shrink-0" />
              {sidebarOpen && <span className="text-sm">{label}</span>}
            </NavLink>
          ))}
        </nav>

        {sidebarOpen && (
          <div className="px-4 py-2">
            <div className="flex items-center gap-2">
              <div className={`w-2 h-2 rounded-full ${wsConnected ? 'bg-emerald-400' : 'bg-red-400'}`} />
              <span className="text-white/30 text-xs">{wsConnected ? 'Live' : 'Connecting...'}</span>
            </div>
          </div>
        )}

        <div className="p-3 border-t border-white/5">
          {sidebarOpen ? (
            <div className="flex items-center gap-3 px-3 py-2 rounded-lg bg-white/5">
              <div className="w-8 h-8 bg-primary-500 rounded-full flex items-center justify-center text-xs font-bold shrink-0">
                {user?.firstName?.[0]?.toUpperCase() || user?.email?.[0]?.toUpperCase() || 'U'}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-white text-xs font-medium truncate">{user?.firstName} {user?.lastName}</p>
                <p className="text-white/40 text-xs">{user?.role}</p>
              </div>
              <button onClick={handleLogout} className="text-white/40 hover:text-red-400 transition-colors" title="Logout">
                <LogOut size={16} />
              </button>
            </div>
          ) : (
            <button onClick={handleLogout} className="w-full sidebar-link justify-center" title="Logout">
              <LogOut size={18} />
            </button>
          )}
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-16 bg-dark-900 border-b border-white/5 flex items-center justify-between px-6 shrink-0">
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="text-white/60 hover:text-white transition-colors"
            title="Toggle navigation"
          >
            <Menu size={22} />
          </button>
          <div className="flex items-center gap-3">
            <div className="relative">
              <button
                onClick={() => setShowNotif((value) => !value)}
                className="relative p-2 rounded-lg bg-white/5 hover:bg-white/10 transition-colors"
                title="Notifications"
              >
                <Bell size={18} />
                {unreadCount > 0 && (
                  <span className="absolute -top-1 -right-1 min-w-5 h-5 px-1 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-bold">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </button>

              {showNotif && (
                <div className="absolute right-0 top-12 w-80 bg-dark-800 border border-white/10 rounded-lg shadow-2xl z-50 overflow-hidden">
                  <div className="flex items-center justify-between px-4 py-3 border-b border-white/5">
                    <h3 className="text-white font-semibold text-sm">Notifications</h3>
                    <button onClick={markAllRead} className="text-primary-500 text-xs hover:underline">Mark all read</button>
                  </div>
                  <div className="max-h-72 overflow-y-auto">
                    {notifications.length === 0 ? (
                      <p className="text-white/40 text-sm text-center py-6">No notifications</p>
                    ) : (
                      notifications.slice(0, 10).map((notification, index) => (
                        <div
                          key={notification.id || index}
                          className={`px-4 py-3 border-b border-white/5 hover:bg-white/5 transition-colors ${!notification.isRead ? 'bg-primary-500/5' : ''}`}
                        >
                          <p className="text-white text-xs font-medium">{notification.title}</p>
                          <p className="text-white/50 text-xs mt-0.5 truncate">{notification.message}</p>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>

            <div className="w-8 h-8 bg-primary-500 rounded-full flex items-center justify-center text-xs font-bold">
              {user?.firstName?.[0]?.toUpperCase() || user?.email?.[0]?.toUpperCase() || 'U'}
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
};

export default AppLayout;
