import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';

const EMPTY_PRODUCT_FORM = {
  sku: '',
  name: '',
  category: '',
  unitPrice: '',
  quantity: '',
  reorderThreshold: 10,
  supplier: '',
  description: '',
};

/**
 * Inventory page for product catalog and stock health management.
 *
 * <p>The page supports product CRUD, client-side search, and low-stock visual
 * status based on the same quantity/reorderThreshold rule used by the backend
 * notification workflow.</p>
 */
const Inventory = () => {
  const { canManage } = useAuth();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editProduct, setEditProduct] = useState(null);
  const [search, setSearch] = useState('');
  const [form, setForm] = useState(EMPTY_PRODUCT_FORM);

  useEffect(() => {
    fetchProducts();
  }, []);

  /** Loads products from the inventory API into the table view. */
  const fetchProducts = async () => {
    try {
      const res = await api.get('/inventory/products');
      setProducts(res.data.data || []);
    } catch {
      setProducts([]);
    }
    setLoading(false);
  };

  /** Opens the modal in create mode with a clean product form. */
  const openAdd = () => {
    setEditProduct(null);
    setForm(EMPTY_PRODUCT_FORM);
    setShowModal(true);
  };

  /** Opens the modal in edit mode using the selected product values. */
  const openEdit = (product) => {
    setEditProduct(product);
    setForm({ ...product });
    setShowModal(true);
  };

  /** Creates or updates a product, then refreshes the table from the API. */
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editProduct) {
        await api.put(`/inventory/products/${editProduct.id}`, form);
        toast.success('Product updated successfully');
      } else {
        await api.post('/inventory/products', form);
        toast.success('Product created successfully');
      }
      setShowModal(false);
      fetchProducts();
    } catch (err) {
      const msg = err.response?.data?.message || 'Product save failed. Please try again.';
      toast.error(msg);
    }
  };

  /** Deletes a product after confirmation and reloads inventory state. */
  const handleDelete = async (id) => {
    if (!window.confirm('Delete this product?')) return;
    try {
      await api.delete(`/inventory/products/${id}`);
      toast.success('Product deleted');
      fetchProducts();
    } catch (err) {
      toast.error('Delete failed. Please try again.');
    }
  };

  // Client-side filtering keeps the inventory table responsive during demos.
  const filtered = products.filter(p =>
    p.name?.toLowerCase().includes(search.toLowerCase()) ||
    p.sku?.toLowerCase().includes(search.toLowerCase()) ||
    p.category?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Inventory Management</h1>
          <p className="text-white/40 text-sm mt-1">{products.length} products total</p>
        </div>
        {canManage && (
          <button id="add-product-btn" onClick={openAdd} className="btn-primary flex items-center gap-2">
            <span>+</span> Add Product
          </button>
        )}
      </div>

      <input
        type="text"
        placeholder="Search products by name, SKU, or category..."
        value={search}
        onChange={e => setSearch(e.target.value)}
        className="input-field max-w-sm"
      />

      <div className="table-container">
        <table className="w-full">
          <thead>
            <tr>
              {['SKU', 'Name', 'Category', 'Qty', 'Price', 'Status', ...(canManage ? ['Actions'] : [])].map(h => (
                <th key={h} className="table-header text-left">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="text-center py-12 text-white/40">Loading...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-12 text-white/40">No products found</td></tr>
            ) : (
              filtered.map(p => (
                <tr key={p.id} className="data-row">
                  <td><span className="font-mono text-primary-500 text-xs">{p.sku}</span></td>
                  <td className="font-medium text-white">{p.name}</td>
                  <td className="text-white/60">{p.category || '-'}</td>
                  <td>
                    <span className={`font-bold ${p.quantity <= p.reorderThreshold ? 'text-red-400' : 'text-emerald-400'}`}>
                      {p.quantity}
                    </span>
                  </td>
                  <td className="text-white/80">${Number(p.unitPrice).toFixed(2)}</td>
                  <td>
                    {p.quantity <= p.reorderThreshold
                      ? <span className="badge-danger">Low Stock</span>
                      : <span className="badge-success">In Stock</span>}
                  </td>
                  {canManage && (
                    <td>
                      <div className="flex gap-2">
                        <button onClick={() => openEdit(p)} className="text-white/50 hover:text-primary-400 transition-colors text-sm px-2 py-1 rounded-lg hover:bg-white/5">Edit</button>
                        <button onClick={() => handleDelete(p.id)} className="text-white/50 hover:text-red-400 transition-colors text-sm px-2 py-1 rounded-lg hover:bg-white/5">Delete</button>
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
          <div className="card w-full max-w-lg animate-slide-up">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-white font-bold text-lg">{editProduct ? 'Edit Product' : 'Add Product'}</h2>
              <button onClick={() => setShowModal(false)} className="text-white/40 hover:text-white text-xl">x</button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div><label className="text-white/60 text-xs mb-1 block">SKU *</label><input className="input-field" value={form.sku} onChange={e => setForm({...form, sku: e.target.value})} required /></div>
                <div><label className="text-white/60 text-xs mb-1 block">Name *</label><input className="input-field" value={form.name} onChange={e => setForm({...form, name: e.target.value})} required /></div>
                <div><label className="text-white/60 text-xs mb-1 block">Category</label><input className="input-field" value={form.category} onChange={e => setForm({...form, category: e.target.value})} /></div>
                <div><label className="text-white/60 text-xs mb-1 block">Supplier</label><input className="input-field" value={form.supplier} onChange={e => setForm({...form, supplier: e.target.value})} /></div>
                <div><label className="text-white/60 text-xs mb-1 block">Unit Price *</label><input type="number" step="0.01" className="input-field" value={form.unitPrice} onChange={e => setForm({...form, unitPrice: e.target.value})} required /></div>
                <div><label className="text-white/60 text-xs mb-1 block">Quantity *</label><input type="number" className="input-field" value={form.quantity} onChange={e => setForm({...form, quantity: e.target.value})} required /></div>
                <div><label className="text-white/60 text-xs mb-1 block">Reorder Threshold</label><input type="number" className="input-field" value={form.reorderThreshold} onChange={e => setForm({...form, reorderThreshold: e.target.value})} /></div>
              </div>
              <div><label className="text-white/60 text-xs mb-1 block">Description</label><textarea className="input-field" rows={2} value={form.description} onChange={e => setForm({...form, description: e.target.value})} /></div>
              <div className="flex gap-3 pt-2">
                <button type="submit" className="btn-primary flex-1">{editProduct ? 'Update' : 'Create'} Product</button>
                <button type="button" onClick={() => setShowModal(false)} className="btn-secondary flex-1">Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Inventory;
