import React, { useState } from 'react';
import { Download, FileSpreadsheet, FileText } from 'lucide-react';
import api from '../api/axiosConfig';

const REPORTS = [
  {
    id: 'inventory-excel',
    title: 'Inventory Report',
    description: 'Stock levels, prices, suppliers, and reorder thresholds.',
    icon: FileSpreadsheet,
    format: 'Excel (.xlsx)',
    accent: 'border-emerald-500/20',
    action: { url: '/reports/inventory/excel', filename: 'inventory-report.xlsx' },
  },
  {
    id: 'invoice-pdf',
    title: 'Invoice Report',
    description: 'Invoice totals, tax, customers, and payment status.',
    icon: FileText,
    format: 'PDF (.pdf)',
    accent: 'border-blue-500/20',
    action: { url: '/reports/invoices/pdf', filename: 'invoice-report.pdf' },
  },
];

/**
 * Reports page for binary Excel/PDF downloads.
 *
 * <p>The page requests authenticated report endpoints as blobs, creates a
 * temporary object URL, and lets the browser download production-ready exports.</p>
 */
const Reports = () => {
  const [downloading, setDownloading] = useState(null);
  const [filters, setFilters] = useState({ from: '', to: '' });

  /**
   * Downloads a report file from the API and releases the temporary URL.
   *
   * @param url report endpoint path under /api
   * @param filename browser download filename
   */
  const downloadFile = async ({ url, filename }) => {
    try {
      setDownloading(filename);
      const params = {};
      if (filters.from) params.from = filters.from;
      if (filters.to) params.to = filters.to;
      const res = await api.get(url, { responseType: 'blob', params });
      const blob = new Blob([res.data], { type: res.headers['content-type'] });
      const objectUrl = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = filename;
      link.click();
      URL.revokeObjectURL(objectUrl);
    } catch {
      alert('Download failed. Please try again after confirming the API is running.');
    } finally {
      setDownloading(null);
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Reports & Analytics</h1>
        <p className="text-white/40 text-sm mt-1">Download live business reports</p>
      </div>

      <div className="card">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="text-white/60 text-xs mb-1 block">From</label>
            <input type="date" className="input-field" value={filters.from} onChange={(event) => setFilters({ ...filters, from: event.target.value })} />
          </div>
          <div>
            <label className="text-white/60 text-xs mb-1 block">To</label>
            <input type="date" className="input-field" value={filters.to} onChange={(event) => setFilters({ ...filters, to: event.target.value })} />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {REPORTS.map((report) => {
          const Icon = report.icon;
          const isDownloading = downloading === report.action.filename;

          return (
            <div key={report.id} className={`card border ${report.accent}`}>
              <div className="flex items-start gap-4">
                <div className="w-11 h-11 rounded-lg bg-white/5 flex items-center justify-center text-primary-300 shrink-0">
                  <Icon size={22} />
                </div>
                <div className="flex-1">
                  <h3 className="text-white font-semibold text-lg">{report.title}</h3>
                  <p className="text-white/50 text-sm mt-1">{report.description}</p>
                  <div className="mt-4 flex items-center justify-between">
                    <span className="badge-info text-xs">{report.format}</span>
                    <button
                      id={`download-${report.id}`}
                      onClick={() => downloadFile(report.action)}
                      disabled={isDownloading}
                      className="btn-primary text-sm py-2 px-4 flex items-center gap-1.5 disabled:opacity-60"
                    >
                      <Download size={15} />
                      {isDownloading ? 'Downloading...' : 'Download'}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default Reports;
