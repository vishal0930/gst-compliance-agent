import React, { useState, useMemo } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';
import useUiStore from '../../store/uiStore';

const BREADCRUMB_MAP = {
  dashboard: 'Dashboard', invoices: 'Invoices', reconciliation: 'Reconciliation',
  gstr2b: 'GSTR-2B', returns: 'Return Draft', deadlines: 'Compliance Deadlines',
  analytics: 'Analytics', insights: 'AI Insights', profile: 'Profile',
  settings: 'Settings', processing: 'AI Pipeline',
};

const Layout = () => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const breadcrumbs = useMemo(() =>
    location.pathname.split('/').filter(Boolean).map((slug) => ({
      label: BREADCRUMB_MAP[slug] || slug.charAt(0).toUpperCase() + slug.slice(1),
      path: `/${slug}`,
    })), [location.pathname]);

  return (
    <div style={{
      display: 'flex', height: '100vh', width: '100vw',
      overflow: 'hidden', background: 'var(--bg-canvas)',
      fontFamily: 'Inter, -apple-system, sans-serif', color: 'var(--text-primary)',
      padding: '0px',
    }}>
      {/* Sidebar gets its own margins inside Sidebar.jsx, but we pass properties if needed */}
      <Sidebar collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />

      <div style={{
        display: 'flex', flexDirection: 'column', flex: 1, minWidth: 0, overflow: 'hidden',
        height: '100vh',
      }}>
        {/* Header floats at the top */}
        <Header collapsed={sidebarCollapsed} setCollapsed={setSidebarCollapsed} />

        {/* Breadcrumb - integrated cleanly */}
        {breadcrumbs.length > 0 && location.pathname !== '/dashboard' && (
          <div style={{
            margin: '0 16px 8px 8px',
            padding: '8px 16px', display: 'flex', alignItems: 'center', gap: 6,
            fontSize: 11, fontFamily: 'monospace', letterSpacing: '0.04em',
            background: 'var(--bg-card)', borderRadius: '10px',
            border: '1px solid var(--border)',
            color: 'var(--text-muted)',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.05)',
          }}>
            <span onClick={() => navigate('/dashboard')}
              style={{ cursor: 'pointer', color: 'var(--accent)', fontWeight: 600 }}>
              Platform
            </span>
            {breadcrumbs.map((crumb, i) => (
              <React.Fragment key={i}>
                <span style={{ color: 'var(--border)' }}>/</span>
                <span
                  onClick={() => i !== breadcrumbs.length - 1 && navigate(crumb.path)}
                  style={{
                    cursor: i !== breadcrumbs.length - 1 ? 'pointer' : 'default',
                    color: i === breadcrumbs.length - 1 ? 'var(--text-primary)' : 'var(--text-muted)',
                    fontWeight: i === breadcrumbs.length - 1 ? 600 : 400,
                  }}
                >
                  {crumb.label}
                </span>
              </React.Fragment>
            ))}
          </div>
        )}

        {/* Main content floats as a unified card */}
        <main style={{
          flex: 1,
          overflowY: 'auto',
          margin: '0 16px 16px 8px',
          padding: '24px',
          background: 'var(--bg-card)',
          borderRadius: '20px',
          border: '1px solid var(--border)',
          boxShadow: '0 8px 30px rgba(0, 0, 0, 0.05)',
          display: 'flex',
          flexDirection: 'column',
        }}>
          <div style={{ maxWidth: 1600, width: '100%', margin: '0 auto', flex: 1 }} className="animate-fade-in">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

export default Layout;
