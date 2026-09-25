import React, { useEffect, useState } from 'react';
import { Pencil, Plus, Trash2, X } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';

const EMPTY_FORM = { name: '', email: '', phone: '', address: '', tier: 'STANDARD' };
const TIER_COLORS = { STANDARD: 'badge-info', PREMIUM: 'badge-warning', VIP: 'badge-success' };

/**
 * Sales page for CRM customer management.
 *
 * <p>The page handles customer CRUD, tier summaries, and search. Backend
 * customer changes broadcast dashboard refresh events for CRM KPI accuracy.</p>
 */
const Sales = () => {
  const { canManage } = useAuth();
  const [customers, setCustomers] = useState([]);
  const [orders, setOrders] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [orderForm, setOrderForm] = useState({ customerName: '', customerEmail: '', productId: '', quantity: 1, status: 'CONFIRMED' });
  const [editingCustomer, setEditingCustomer] = useState(null);
  const [search, setSearch] = useState('');
  const [form, setForm] = useState(EMPTY_FORM);

  /** Loads customers for the CRM table and tier summary cards. */
  const fetchCustomers = async () => {
    setLoading(true);
    const [customersResult, ordersResult, productsResult] = await Promise.allSettled([
      api.get('/sales/customers'),
      api.get('/sales/orders'),
      api.get('/inventory/products'),
    ]);

    if (customersResult.status === 'fulfilled') {
      setCustomers(customersResult.value.data.data || []);
    } else {
      setCustomers([]);
      toast.error('Could not load customers');
    }

    if (ordersResult.status === 'fulfilled') {
      setOrders(ordersResult.value.data.data || []);
    } else {
      setOrders([]);
    }

    if (productsResult.status === 'fulfilled') {
      setProducts(productsResult.value.data.data || []);
    } else {
      setProducts([]);
    }

    setLoading(false);
  };

  useEffect(() => {
    fetchCustomers();
  }, []);

  /** Opens the customer modal in create mode. */
  const openAdd = () => {
    setEditingCustomer(null);
    setForm(EMPTY_FORM);
    setShowModal(true);
  };

  /** Opens the customer modal with existing CRM values. */
  const openEdit = (customer) => {
    setEditingCustomer(customer);
    setForm({
      name: customer.name || '',
      email: customer.email || '',
      phone: customer.phone || '',
      address: customer.address || '',
      tier: customer.tier || 'STANDARD',
    });
    setShowModal(true);
  };

  /** Creates or updates a customer, then refreshes the CRM table. */
  const handleSubmit = async (event) => {
    event.preventDefault();
    try {
      if (editingCustomer) {
        await api.put(`/sales/customers/${editingCustomer.id}`, form);
        toast.success('Customer updated successfully');
      } else {
        await api.post('/sales/customers', form);
        toast.success('Customer added successfully');
      }
      setShowModal(false);
      setEditingCustomer(null);
      setForm(EMPTY_FORM);
      fetchCustomers();
    } catch (err) {
      const msg = err.response?.data?.message || 'Customer save failed. Please try again.';
      toast.error(msg);
    }
  };

  /** Deletes a customer after confirmation. */
  const handleDelete = async (id) => {
    if (!window.confirm('Delete this customer?')) return;
    try {
      await api.delete(`/sales/customers/${id}`);
      toast.success('Customer deleted');
      fetchCustomers();
    } catch (err) {
      toast.error('Delete failed. Please try again.');
    }
  };

  const createOrder = async (event) => {
    event.preventDefault();
    try {
      await api.post('/sales/orders', {
        customerName: orderForm.customerName,
        customerEmail: orderForm.customerEmail,
        quantity: Number(orderForm.quantity),
        status: orderForm.status,
        product: { id: Number(orderForm.productId) },
      });
      toast.success('Sales order created');
      setOrderForm({ customerName: '', customerEmail: '', productId: '', quantity: 1, status: 'CONFIRMED' });
      fetchCustomers();
    } catch (err) {
      const msg = err.response?.data?.message || 'Sales order creation failed. Please try again.';
      toast.error(msg);
    }
  };

  // Local search keeps CRM browsing quick without extra backend round trips.
  const filtered = customers.filter((customer) =>
    customer.name?.toLowerCase().includes(search.toLowerCase()) ||
    customer.email?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Sales & CRM</h1>
          <p className="text-white/40 text-sm mt-1">{customers.length} customers</p>
        </div>
        {canManage && (
          <button id="add-customer-btn" onClick={openAdd} className="btn-primary flex items-center gap-2">
            <Plus size={16} /> Add Customer
          </button>
        )}
      </div>

      <div className="grid grid-cols-3 gap-4">
        {['STANDARD', 'PREMIUM', 'VIP'].map((tier) => (
          <div key={tier} className="card text-center">
            <p className="text-white text-xl font-bold">{customers.filter((customer) => customer.tier === tier).length}</p>
            <span className={`${TIER_COLORS[tier]} mt-1`}>{tier}</span>
          </div>
        ))}
      </div>

      <input
        type="text"
        placeholder="Search customers by name or email..."
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        className="input-field max-w-sm"
      />

      <div className="table-container">
        <table className="w-full">
          <thead>
            <tr>
              {['Name', 'Email', 'Phone', 'Tier', 'Joined', ...(canManage ? ['Actions'] : [])].map((heading) => (
                <th key={heading} className="table-header text-left">{heading}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="text-center py-12 text-white/40">Loading...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={6} className="text-center py-12 text-white/40">No customers found</td></tr>
            ) : (
              filtered.map((customer) => (
                <tr key={customer.id} className="data-row">
                  <td className="font-medium text-white">{customer.name}</td>
                  <td className="text-white/60">{customer.email}</td>
                  <td className="text-white/60">{customer.phone || '-'}</td>
                  <td><span className={TIER_COLORS[customer.tier] || 'badge-info'}>{customer.tier}</span></td>
                  <td className="text-white/40 text-xs">{customer.createdAt ? new Date(customer.createdAt).toLocaleDateString() : '-'}</td>
                  {canManage && (
                    <td>
                      <div className="flex gap-2">
                        <button onClick={() => openEdit(customer)} className="text-white/50 hover:text-primary-400 transition-colors" title="Edit customer">
                          <Pencil size={16} />
                        </button>
                        <button onClick={() => handleDelete(customer.id)} className="text-white/50 hover:text-red-400 transition-colors" title="Delete customer">
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

      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-white font-semibold">Sales Orders</h2>
            <p className="text-white/40 text-xs mt-1">{orders.length} lifecycle records</p>
          </div>
        </div>
        {canManage && (
          <form onSubmit={createOrder} className="grid grid-cols-1 md:grid-cols-5 gap-3 mb-5">
            <input className="input-field" placeholder="Customer name" value={orderForm.customerName} onChange={(event) => setOrderForm({ ...orderForm, customerName: event.target.value })} required />
            <input className="input-field" type="email" placeholder="Customer email" value={orderForm.customerEmail} onChange={(event) => setOrderForm({ ...orderForm, customerEmail: event.target.value })} />
            <select className="input-field" value={orderForm.productId} onChange={(event) => setOrderForm({ ...orderForm, productId: event.target.value })} required>
              <option value="">Product</option>
              {products.map((product) => <option key={product.id} value={product.id}>{product.name}</option>)}
            </select>
            <input className="input-field" type="number" min="1" value={orderForm.quantity} onChange={(event) => setOrderForm({ ...orderForm, quantity: event.target.value })} required />
            <button className="btn-primary" type="submit">Create Order</button>
          </form>
        )}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                {['Order #', 'Customer', 'Product', 'Qty', 'Total', 'Status'].map((heading) => (
                  <th key={heading} className="table-header text-left">{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {orders.length === 0 ? (
                <tr><td colSpan={6} className="text-center py-8 text-white/40">No sales orders found</td></tr>
              ) : orders.map((order) => (
                <tr key={order.id} className="data-row">
                  <td><span className="font-mono text-primary-500 text-xs">{order.orderNumber}</span></td>
                  <td className="text-white/80">{order.customerName}</td>
                  <td className="text-white/60">{order.product?.name || '-'}</td>
                  <td className="text-white/80">{order.quantity}</td>
                  <td className="text-emerald-400">${Number(order.totalAmount || 0).toFixed(2)}</td>
                  <td><span className="badge-info">{order.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="card w-full max-w-md animate-slide-up">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-white font-bold text-lg">{editingCustomer ? 'Edit Customer' : 'Add Customer'}</h2>
              <button onClick={() => setShowModal(false)} className="text-white/40 hover:text-white" title="Close">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="text-white/60 text-xs mb-1 block">Full Name *</label>
                <input className="input-field" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
              </div>
              <div>
                <label className="text-white/60 text-xs mb-1 block">Email *</label>
                <input type="email" className="input-field" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required />
              </div>
              <div>
                <label className="text-white/60 text-xs mb-1 block">Phone</label>
                <input className="input-field" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} />
              </div>
              <div>
                <label className="text-white/60 text-xs mb-1 block">Address</label>
                <textarea className="input-field" rows={2} value={form.address} onChange={(event) => setForm({ ...form, address: event.target.value })} />
              </div>
              <div>
                <label className="text-white/60 text-xs mb-1 block">Tier</label>
                <select className="input-field" value={form.tier} onChange={(event) => setForm({ ...form, tier: event.target.value })}>
                  <option value="STANDARD">Standard</option>
                  <option value="PREMIUM">Premium</option>
                  <option value="VIP">VIP</option>
                </select>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="submit" className="btn-primary flex-1">{editingCustomer ? 'Update' : 'Add'} Customer</button>
                <button type="button" onClick={() => setShowModal(false)} className="btn-secondary flex-1">Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Sales;
