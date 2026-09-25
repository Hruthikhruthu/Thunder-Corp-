import React, { useEffect, useState } from 'react';
import { Plus, Trash2, X } from 'lucide-react';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';

const EMPTY_FORM = { customerName: '', customerEmail: '', totalAmount: '', taxAmount: '', notes: '' };
const STATUS_COLORS = {
  PAID: 'badge-success',
  PENDING: 'badge-warning',
  OVERDUE: 'badge-danger',
  CANCELLED: 'text-white/30 text-xs',
};

/**
 * Finance page for invoice creation, payment state management, and revenue KPI.
 *
 * <p>Invoice status updates call a dedicated backend endpoint so overdue
 * transitions can trigger manager notifications and realtime dashboard events.</p>
 */
const Finance = () => {
  const { canManage } = useAuth();
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [revenue, setRevenue] = useState(0);
  const [form, setForm] = useState(EMPTY_FORM);

  /** Loads invoices and recognized revenue from finance APIs. */
  const fetchInvoices = async () => {
    try {
      const res = await api.get('/finance/invoices');
      setInvoices(res.data.data || []);
      const revenueRes = await api.get('/finance/stats/revenue');
      setRevenue(revenueRes.data.data || 0);
    } catch {
      setInvoices([]);
      setRevenue(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices();
  }, []);

  /** Creates a new invoice and lets the backend generate invoice number/net. */
  const handleCreate = async (event) => {
    event.preventDefault();
    await api.post('/finance/invoices', form);
    setShowModal(false);
    setForm(EMPTY_FORM);
    fetchInvoices();
  };

  /** Updates the invoice lifecycle state such as PAID or OVERDUE. */
  const updateStatus = async (id, status) => {
    await api.patch(`/finance/invoices/${id}/status`, { status });
    fetchInvoices();
  };

  /** Deletes an invoice after user confirmation. */
  const deleteInvoice = async (id) => {
    if (!window.confirm('Delete this invoice?')) return;
    await api.delete(`/finance/invoices/${id}`);
    fetchInvoices();
  };

  const paid = invoices.filter((invoice) => invoice.status === 'PAID').length;
  const pending = invoices.filter((invoice) => invoice.status === 'PENDING').length;
  const overdue = invoices.filter((invoice) => invoice.status === 'OVERDUE').length;

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Finance & Invoicing</h1>
          <p className="text-white/40 text-sm mt-1">{invoices.length} total invoices</p>
        </div>
        {canManage && (
          <button id="add-invoice-btn" onClick={() => setShowModal(true)} className="btn-primary flex items-center gap-2">
            <Plus size={16} /> New Invoice
          </button>
        )}
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="card text-center">
          <p className="text-emerald-400 text-2xl font-bold">${Number(revenue).toLocaleString()}</p>
          <p className="text-white/50 text-xs mt-1">Total Revenue</p>
        </div>
        <div className="card text-center">
          <p className="text-emerald-400 text-2xl font-bold">{paid}</p>
          <p className="text-white/50 text-xs mt-1">Paid</p>
        </div>
        <div className="card text-center">
          <p className="text-amber-400 text-2xl font-bold">{pending}</p>
          <p className="text-white/50 text-xs mt-1">Pending</p>
        </div>
        <div className="card text-center">
          <p className="text-red-400 text-2xl font-bold">{overdue}</p>
          <p className="text-white/50 text-xs mt-1">Overdue</p>
        </div>
      </div>

      <div className="table-container">
        <table className="w-full">
          <thead>
            <tr>
              {['Invoice #', 'Customer', 'Total', 'Tax', 'Net', 'Status', ...(canManage ? ['Actions'] : [])].map((heading) => (
                <th key={heading} className="table-header text-left">{heading}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="text-center py-12 text-white/40">Loading...</td></tr>
            ) : invoices.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-12 text-white/40">No invoices found</td></tr>
            ) : (
              invoices.map((invoice) => (
                <tr key={invoice.id} className="data-row">
                  <td><span className="font-mono text-primary-500 text-xs">{invoice.invoiceNumber}</span></td>
                  <td>
                    <div>
                      <p className="text-white text-sm font-medium">{invoice.customerName}</p>
                      <p className="text-white/40 text-xs">{invoice.customerEmail}</p>
                    </div>
                  </td>
                  <td className="text-white/80">${Number(invoice.totalAmount).toFixed(2)}</td>
                  <td className="text-white/60">${Number(invoice.taxAmount).toFixed(2)}</td>
                  <td className="text-emerald-400 font-medium">${Number(invoice.netAmount).toFixed(2)}</td>
                  <td><span className={STATUS_COLORS[invoice.status] || 'badge-info'}>{invoice.status}</span></td>
                  {canManage && (
                    <td>
                      <div className="flex items-center gap-2">
                        <select
                          value={invoice.status}
                          onChange={(event) => updateStatus(invoice.id, event.target.value)}
                          className="bg-white/5 border border-white/10 text-white/70 text-xs rounded-lg px-2 py-1 focus:outline-none"
                        >
                          {['PENDING', 'PAID', 'OVERDUE', 'CANCELLED'].map((status) => (
                            <option key={status} value={status} className="bg-dark-800">{status}</option>
                          ))}
                        </select>
                        <button onClick={() => deleteInvoice(invoice.id)} className="text-white/50 hover:text-red-400 transition-colors" title="Delete invoice">
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="card w-full max-w-md animate-slide-up">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-white font-bold text-lg">New Invoice</h2>
              <button onClick={() => setShowModal(false)} className="text-white/40 hover:text-white" title="Close">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="text-white/60 text-xs mb-1 block">Customer Name *</label>
                <input className="input-field" value={form.customerName} onChange={(event) => setForm({ ...form, customerName: event.target.value })} required />
              </div>
              <div>
                <label className="text-white/60 text-xs mb-1 block">Customer Email</label>
                <input type="email" className="input-field" value={form.customerEmail} onChange={(event) => setForm({ ...form, customerEmail: event.target.value })} />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-white/60 text-xs mb-1 block">Total Amount *</label>
                  <input type="number" step="0.01" min="0" className="input-field" value={form.totalAmount} onChange={(event) => setForm({ ...form, totalAmount: event.target.value })} required />
                </div>
                <div>
                  <label className="text-white/60 text-xs mb-1 block">Tax Amount</label>
                  <input type="number" step="0.01" min="0" className="input-field" value={form.taxAmount} onChange={(event) => setForm({ ...form, taxAmount: event.target.value })} />
                </div>
              </div>
              <div>
                <label className="text-white/60 text-xs mb-1 block">Notes</label>
                <textarea className="input-field" rows={2} value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} />
              </div>
              <div className="flex gap-3 pt-2">
                <button type="submit" className="btn-primary flex-1">Create Invoice</button>
                <button type="button" onClick={() => setShowModal(false)} className="btn-secondary flex-1">Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Finance;
