import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldAlert, LogIn, ArrowRight } from 'lucide-react';

const Unauthorized = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-dark-950 flex items-center justify-center p-4">
      <div className="w-full max-w-md text-center animate-fade-in">
        <div className="inline-flex items-center justify-center w-16 h-16 bg-red-500/10 border border-red-500/20 text-red-400 rounded-full mb-6 animate-pulse">
          <ShieldAlert size={32} />
        </div>
        
        <h1 className="text-4xl font-extrabold text-white tracking-tight">401</h1>
        <h2 className="text-xl font-semibold text-white/90 mt-2">Unauthorized Access</h2>
        <p className="text-white/40 mt-3 text-sm max-w-sm mx-auto">
          You are not currently logged in. Please sign in with one of the dedicated portal links below.
        </p>

        <div className="mt-8 space-y-3">
          {[
            { label: 'Admin Portal', path: '/login/admin', color: 'border-primary-500/20 hover:border-primary-500/50 hover:bg-primary-500/5' },
            { label: 'Manager Portal', path: '/login/manager', color: 'border-cyan-500/20 hover:border-cyan-500/50 hover:bg-cyan-500/5' },
            { label: 'Staff Portal', path: '/login/staff', color: 'border-emerald-500/20 hover:border-emerald-500/50 hover:bg-emerald-500/5' },
          ].map((portal) => (
            <button
              key={portal.label}
              onClick={() => navigate(portal.path)}
              className={`w-full p-4 border rounded-xl flex items-center justify-between text-white/80 font-medium transition-all group ${portal.color}`}
            >
              <span>{portal.label}</span>
              <ArrowRight size={16} className="text-white/40 group-hover:text-white group-hover:translate-x-1 transition-all" />
            </button>
          ))}
        </div>

        <p className="text-center text-white/20 text-xs mt-8">
          (c) 2026 ThunderCore ERP. All rights reserved.
        </p>
      </div>
    </div>
  );
};

export default Unauthorized;
