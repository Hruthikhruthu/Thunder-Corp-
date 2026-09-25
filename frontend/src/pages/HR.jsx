import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';

const EMPTY_EMPLOYEE_FORM = {
  employeeCode: '',
  department: '',
  designation: '',
  baseSalary: '',
  status: 'ACTIVE',
  joiningDate: '',
};

/**
 * HR page for employee profile and payroll baseline management.
 *
 * <p>The page mirrors the backend Employee entity fields, summarizes department
 * distribution, and refreshes records after each create/update/delete action.</p>
 */
const HR = () => {
  const { canManage } = useAuth();
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editEmp, setEditEmp] = useState(null);
  const [search, setSearch] = useState('');
  const [form, setForm] = useState(EMPTY_EMPLOYEE_FORM);

  useEffect(() => {
    fetchEmployees();
  }, []);

  /** Loads employee records used by the HR table and department summary. */
  const fetchEmployees = async () => {
    try {
      const res = await api.get('/hr/employees');
      setEmployees(res.data.data || []);
    } catch {
      setEmployees([]);
    }
    setLoading(false);
  };

  /** Opens the employee modal with a blank form. */
  const openAdd = () => {
    setEditEmp(null);
    setForm(EMPTY_EMPLOYEE_FORM);
    setShowModal(true);
  };

  /** Opens the employee modal in edit mode with normalized field defaults. */
  const openEdit = (emp) => {
    setEditEmp(emp);
    setForm({
      employeeCode: emp.employeeCode || '',
      department: emp.department || '',
      designation: emp.designation || '',
      baseSalary: emp.baseSalary || '',
      status: emp.status || 'ACTIVE',
      joiningDate: emp.joiningDate || ''
    });
    setShowModal(true);
  };

  /** Creates or updates an employee through the HR API. */
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editEmp) {
        await api.put(`/hr/employees/${editEmp.id}`, form);
        toast.success('Employee updated successfully');
      } else {
        await api.post('/hr/employees', form);
        toast.success('Employee added successfully');
      }
      setShowModal(false);
      fetchEmployees();
    } catch (err) {
      const msg = err.response?.data?.message || 'Employee save failed. Please try again.';
      toast.error(msg);
    }
  };

  /** Removes an employee after confirmation and refreshes HR state. */
  const handleDelete = async (id) => {
    if (!window.confirm('Remove this employee?')) return;
    try {
      await api.delete(`/hr/employees/${id}`);
      toast.success('Employee removed');
      fetchEmployees();
    } catch (err) {
      toast.error('Remove failed. Please try again.');
    }
  };

  // Filter locally by HR identifiers that users naturally search for.
  const filtered = employees.filter(e =>
    e.employeeCode?.toLowerCase().includes(search.toLowerCase()) ||
    e.department?.toLowerCase().includes(search.toLowerCase()) ||
    e.designation?.toLowerCase().includes(search.toLowerCase())
  );

  const departments = [...new Set(employees.map(e => e.department).filter(Boolean))];

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">HR Management</h1>
          <p className="text-white/40 text-sm mt-1">{employees.length} employees - {departments.length} departments</p>
        </div>
        {canManage && (
          <button id="add-employee-btn" onClick={openAdd} className="btn-primary flex items-center gap-2">
            <span>+</span> Add Employee
          </button>
        )}
      </div>

      <div className="flex flex-wrap gap-2">
        {departments.map(dept => (
          <span key={dept} className="badge-info px-3 py-1">{dept}: {employees.filter(e => e.department === dept).length}</span>
        ))}
      </div>

      <input
        type="text"
        placeholder="Search by code, department, or designation..."
        value={search}
        onChange={e => setSearch(e.target.value)}
        className="input-field max-w-sm"
      />

      <div className="table-container">
        <table className="w-full">
          <thead>
            <tr>
              {['Code', 'Department', 'Designation', 'Salary', 'Join Date', 'Status', ...(canManage ? ['Actions'] : [])].map(h => (
                <th key={h} className="table-header text-left">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="text-center py-12 text-white/40">Loading...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-12 text-white/40">No employees found</td></tr>
            ) : (
              filtered.map(emp => (
                <tr key={emp.id} className="data-row">
                  <td><span className="font-mono text-primary-500 text-xs">{emp.employeeCode}</span></td>
                  <td className="text-white/80">{emp.department}</td>
                  <td className="text-white/80">{emp.designation}</td>
                  <td className="text-emerald-400 font-medium">${Number(emp.baseSalary || 0).toLocaleString()}</td>
                  <td className="text-white/60 text-sm">{emp.joiningDate || '-'}</td>
                  <td>
                    <span className={emp.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'}>{emp.status}</span>
                  </td>
                  {canManage && (
                    <td>
                      <div className="flex gap-2">
                        <button onClick={() => openEdit(emp)} className="text-white/50 hover:text-primary-400 transition-colors text-sm px-2 py-1 rounded-lg hover:bg-white/5">Edit</button>
                        <button onClick={() => handleDelete(emp.id)} className="text-white/50 hover:text-red-400 transition-colors text-sm px-2 py-1 rounded-lg hover:bg-white/5">Remove</button>
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
              <h2 className="text-white font-bold text-lg">{editEmp ? 'Edit Employee' : 'Add Employee'}</h2>
              <button onClick={() => setShowModal(false)} className="text-white/40 hover:text-white text-xl">x</button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div><label className="text-white/60 text-xs mb-1 block">Employee Code *</label><input className="input-field" value={form.employeeCode} onChange={e => setForm({...form, employeeCode: e.target.value})} required /></div>
                <div><label className="text-white/60 text-xs mb-1 block">Department *</label><input className="input-field" value={form.department} onChange={e => setForm({...form, department: e.target.value})} required /></div>
                <div><label className="text-white/60 text-xs mb-1 block">Designation *</label><input className="input-field" value={form.designation} onChange={e => setForm({...form, designation: e.target.value})} required /></div>
                <div><label className="text-white/60 text-xs mb-1 block">Base Salary</label><input type="number" className="input-field" value={form.baseSalary} onChange={e => setForm({...form, baseSalary: e.target.value})} /></div>
                <div><label className="text-white/60 text-xs mb-1 block">Joining Date</label><input type="date" className="input-field" value={form.joiningDate} onChange={e => setForm({...form, joiningDate: e.target.value})} /></div>
                <div>
                  <label className="text-white/60 text-xs mb-1 block">Status</label>
                  <select className="input-field" value={form.status} onChange={e => setForm({...form, status: e.target.value})}>
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                    <option value="ON_LEAVE">On Leave</option>
                  </select>
                </div>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="submit" className="btn-primary flex-1">{editEmp ? 'Update' : 'Add'} Employee</button>
                <button type="button" onClick={() => setShowModal(false)} className="btn-secondary flex-1">Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default HR;
