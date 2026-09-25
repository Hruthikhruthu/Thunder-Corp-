import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldX, Home, ArrowLeft } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const Forbidden = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const handleGoHome = () => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (user.role === 'SUPER_ADMIN') navigate('/admin/dashboard');
    else if (user.role === 'MANAGER') navigate('/manager/dashboard');
    else if (user.role === 'STAFF') navigate('/staff/dashboard');
    else navigate('/');
  };

  return (
    <div className="min-h-screen bg-dark-950 flex items-center justify-center p-4">
      <div className="w-full max-w-md text-center animate-fade-in">
        <div className="inline-flex items-center justify-center w-16 h-16 bg-red-500/10 border border-red-500/20 text-red-400 rounded-full mb-6">
          <ShieldX size={32} />
        </div>
        
        <h1 className="text-4xl font-extrabold text-white tracking-tight">403</h1>
        <h2 className="text-xl font-semibold text-white/90 mt-2">Access Forbidden</h2>
        <p className="text-white/40 mt-3 text-sm max-w-sm mx-auto">
          Your current account role <span className="text-primary-400 font-semibold font-mono">({user?.role || 'NONE'})</span> does not possess the permissions required to access this module.
        </p>

        <div className="mt-8 flex gap-3">
          <button
            onClick={handleGoHome}
            className="btn-primary flex-1 flex items-center justify-center gap-2"
          >
            <Home size={16} />
            Go to Dashboard
          </button>
          <button
            onClick={() => navigate(-1)}
            className="btn-secondary flex-1 flex items-center justify-center gap-2"
          >
            <ArrowLeft size={16} />
            Go Back
          </button>
        </div>

        <p className="text-center text-white/20 text-xs mt-8">
          (c) 2026 ThunderCore ERP. All rights reserved.
        </p>
      </div>
    </div>
  );
};

export default Forbidden;
