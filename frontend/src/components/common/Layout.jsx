import React, { useState, useMemo } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';

// Static translation ledger for production route presentation
const BREADCRUMB_MAP = {
  dashboard: 'Dashboard',
  invoices: 'Invoices',
  reconciliation: 'Reconciliation',
  gstr2b: 'GSTR-2B',
  returns: 'Return Draft',
  deadlines: 'Compliance Deadlines',
  analytics: 'Analytics Portal',
  insights: 'AI Insights',
  profile: 'User Profile',
  settings: 'System Settings',
  processing: 'AI Agent Pipeline'
};

const Layout = () => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const breadcrumbs = useMemo(() => {
    return location.pathname
      .split('/')
      .filter(Boolean)
      .map((slug) => ({
        label: BREADCRUMB_MAP[slug] || slug.charAt(0).toUpperCase() + slug.slice(1),
        path: `/${slug}`,
      }));
  }, [location.pathname]);

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-slate-900 font-sans text-slate-100 antialiased">
      <Sidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />

      <div className="flex flex-col flex-1 min-w-0 overflow-hidden bg-slate-950/20">
        <Header collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />

        {breadcrumbs.length > 0 && location.pathname !== '/dashboard' && (
          <div className="px-6 pt-4 flex items-center gap-2 text-xs text-slate-500 font-mono tracking-wide bg-slate-900/40 border-b border-slate-800/40 pb-2 selection:bg-amber-500/30">
            <span
              className="hover:text-amber-400 cursor-pointer transition-colors"
              onClick={() => navigate('/dashboard')}
            >
              Platform
            </span>
            {breadcrumbs.map((crumb, index) => (
              <React.Fragment key={index}>
                <span className="text-slate-600">/</span>
                <span
                  className={index === breadcrumbs.length - 1 ? "text-slate-300 font-medium" : "hover:text-amber-400 cursor-pointer transition-colors"}
                  onClick={() => index !== breadcrumbs.length - 1 && navigate(crumb.path)}
                >
                  {crumb.label}
                </span>
              </React.Fragment>
            ))}
          </div>
        )}

        <main className="flex-1 overflow-y-auto px-4 py-6 md:px-6 md:py-8 bg-slate-900 bg-gradient-to-b from-slate-900 via-slate-900/95 to-slate-950 scrollbar-thin scrollbar-thumb-slate-800">
          <div className="max-w-[1600px] mx-auto animate-fade-in space-y-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

export default Layout;