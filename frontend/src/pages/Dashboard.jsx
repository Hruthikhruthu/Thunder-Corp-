import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
import { AlertTriangle, FileText, Package, Plus, Users, Wallet } from 'lucide-react';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler,
);

/**
 * Shared Chart.js presentation defaults for dashboard analytics.
 */
const CHART_DEFAULTS = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { labels: { color: 'rgba(255,255,255,0.6)', font: { size: 12 } } },
  },
  scales: {
    x: { ticks: { color: 'rgba(255,255,255,0.4)' }, grid: { color: 'rgba(255,255,255,0.05)' } },
    y: { ticks: { color: 'rgba(255,255,255,0.4)' }, grid: { color: 'rgba(255,255,255,0.05)' } },
  },
};

/** KPI card used by the live dashboard summary grid. */
const StatCard = ({ icon: Icon, label, value, sub, color }) => (
  <div className="stat-card">
    <div className={`w-12 h-12 rounded-lg flex items-center justify-center ${color} shrink-0`}>
      <Icon size={22} />
    </div>
    <div>
      <p className="text-white/50 text-xs font-medium uppercase tracking-wider">{label}</p>
      <p className="text-2xl font-bold text-white mt-0.5">{value}</p>
      {sub && <p className="text-white/40 text-xs mt-0.5">{sub}</p>}
    </div>
  </div>
);

/**
 * Dashboard page showing:
 * - KPI cards across inventory, HR, finance, CRM, and auth
 * - revenue analytics
 * - inventory category charts
 * - invoice status distribution
 * - live refresh through AppLayout's WebSocket event bridge
 */
const Dashboard = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  const isStaff = user?.role === 'STAFF';

  /** Fetches the latest aggregate stats from the backend dashboard endpoint. */
  const fetchStats = useCallback(async () => {
    const res = await api.get('/dashboard/stats');
    setStats(res.data.data);
    setLoading(false);
  }, []);

  useEffect(() => {
    fetchStats().catch(() => setLoading(false));
  }, [fetchStats]);

  useEffect(() => {
    const handleDashboardUpdate = (event) => {
      if (event.detail?.totalProducts !== undefined) {
        setStats(event.detail);
      } else {
        fetchStats().catch(() => {});
      }
    };

    window.addEventListener('dashboard:update', handleDashboardUpdate);
    return () => window.removeEventListener('dashboard:update', handleDashboardUpdate);
  }, [fetchStats]);

  /**
   * Converts backend aggregate maps into Chart.js datasets.
   */
  const chartData = useMemo(() => {
    const monthlyRevenue = Object.entries(stats?.monthlyRevenue || {});
    const categories = Object.entries(stats?.categoryCounts || {});
    const invoiceStatuses = Object.entries(stats?.invoiceStatusCounts || {});

    return {
      revenue: {
        labels: monthlyRevenue.length ? monthlyRevenue.map(([month]) => month) : ['No paid invoices'],
        datasets: [{
          label: 'Revenue',
          data: monthlyRevenue.length ? monthlyRevenue.map(([, value]) => Number(value)) : [0],
          borderColor: '#22c55e',
          backgroundColor: 'rgba(34,197,94,0.12)',
          fill: true,
          tension: 0.35,
          pointBackgroundColor: '#22c55e',
        }],
      },
      inventory: {
        labels: categories.length ? categories.map(([category]) => category) : ['No products'],
        datasets: [{
          label: 'Products',
          data: categories.length ? categories.map(([, value]) => Number(value)) : [0],
          backgroundColor: ['#4f46e5', '#0891b2', '#16a34a', '#d97706', '#dc2626', '#9333ea'],
          borderWidth: 0,
        }],
      },
      invoiceStatus: {
        labels: invoiceStatuses.length ? invoiceStatuses.map(([status]) => status) : ['PENDING'],
        datasets: [{
          data: invoiceStatuses.length ? invoiceStatuses.map(([, value]) => Number(value)) : [0],
          backgroundColor: ['#16a34a', '#d97706', '#dc2626', '#64748b'],
          borderWidth: 0,
        }],
      },
    };
  }, [stats]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-10 h-10 border-4 border-primary-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Dashboard</h1>
        <p className="text-white/40 text-sm mt-1">
          {isStaff ? 'Staff operational overview' : 'Live operational snapshot'}
        </p>
      </div>

      {isStaff ? (
        /* Staff Dashboard Cards (Financial details hidden) */
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <StatCard icon={Package} label="Total Products" value={stats?.totalProducts ?? 0} sub="In inventory" color="bg-primary-500/20 text-primary-200" />
          <StatCard icon={AlertTriangle} label="Low Stock" value={stats?.lowStockItems ?? 0} sub="Needs attention" color="bg-amber-500/20 text-amber-200" />
          <StatCard icon={Users} label="Customers" value={stats?.totalCustomers ?? 0} sub="CRM accounts" color="bg-pink-500/20 text-pink-200" />
        </div>
      ) : (
        /* Admin & Manager Dashboard Cards (Full operational views) */
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <StatCard icon={Package} label="Total Products" value={stats?.totalProducts ?? 0} sub="In inventory" color="bg-primary-500/20 text-primary-200" />
            <StatCard icon={Users} label="Employees" value={stats?.totalEmployees ?? 0} sub="HR records" color="bg-cyan-500/20 text-cyan-200" />
            <StatCard icon={Wallet} label="Total Revenue" value={`$${Number(stats?.totalRevenue ?? 0).toLocaleString()}`} sub="Paid invoices" color="bg-emerald-500/20 text-emerald-200" />
            <StatCard icon={AlertTriangle} label="Low Stock" value={stats?.lowStockItems ?? 0} sub="Needs attention" color="bg-amber-500/20 text-amber-200" />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <StatCard icon={FileText} label="Invoices" value={stats?.totalInvoices ?? 0} sub="All statuses" color="bg-blue-500/20 text-blue-200" />
            <StatCard icon={FileText} label="Pending" value={stats?.pendingInvoices ?? 0} sub="Awaiting payment" color="bg-orange-500/20 text-orange-200" />
            <StatCard icon={Users} label="Customers" value={stats?.totalCustomers ?? 0} sub="CRM accounts" color="bg-pink-500/20 text-pink-200" />
          </div>
        </>
      )}

      {/* Analytics Graphs */}
      <div className={`grid ${isStaff ? 'grid-cols-1' : 'grid-cols-1 lg:grid-cols-2'} gap-6`}>
        {!isStaff && (
          <div className="card">
            <h2 className="text-white font-semibold mb-4">Revenue Trend</h2>
            <div className="h-64">
              <Line data={chartData.revenue} options={CHART_DEFAULTS} />
            </div>
          </div>
        )}
        <div className="card">
          <h2 className="text-white font-semibold mb-4">Inventory by Category</h2>
          <div className="h-64">
            <Bar data={chartData.inventory} options={CHART_DEFAULTS} />
          </div>
        </div>
      </div>

      <div className={`grid ${isStaff ? 'grid-cols-1' : 'grid-cols-1 lg:grid-cols-3'} gap-6`}>
        {!isStaff && (
          <div className="card flex flex-col items-center">
            <h2 className="text-white font-semibold mb-4 self-start">Invoice Status</h2>
            <div className="h-52 w-52">
              <Doughnut
                data={chartData.invoiceStatus}
                options={{
                  responsive: true,
                  maintainAspectRatio: false,
                  plugins: { legend: { position: 'bottom', labels: { color: 'rgba(255,255,255,0.6)', font: { size: 11 } } } },
                  cutout: '70%',
                }}
              />
            </div>
          </div>
        )}

        <div className={`card ${isStaff ? '' : 'lg:col-span-2'}`}>
          <h2 className="text-white font-semibold mb-4">Quick Actions</h2>
          <div className="grid grid-cols-2 gap-3">
            {[
              { label: 'View Products', href: '/inventory', roles: ['SUPER_ADMIN', 'MANAGER', 'STAFF'] },
              { label: 'View Customers', href: '/sales', roles: ['SUPER_ADMIN', 'MANAGER', 'STAFF'] },
              { label: 'Add Product', href: '/inventory', roles: ['SUPER_ADMIN', 'MANAGER'] },
              { label: 'New Invoice', href: '/finance', roles: ['SUPER_ADMIN', 'MANAGER'] },
              { label: 'Add Employee', href: '/hr', roles: ['SUPER_ADMIN', 'MANAGER'] },
              { label: 'Download Report', href: '/reports', roles: ['SUPER_ADMIN', 'MANAGER'] },
            ].filter(act => act.roles.includes(user?.role)).map(({ label, href }) => (
              <Link
                key={label}
                to={href}
                className="border border-white/10 rounded-lg p-4 flex items-center gap-3 hover:border-primary-500/50 hover:bg-white/5 transition-all group"
              >
                <Plus size={18} className="text-primary-400 group-hover:scale-110 transition-transform" />
                <span className="text-white/80 text-sm font-medium">{label}</span>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
