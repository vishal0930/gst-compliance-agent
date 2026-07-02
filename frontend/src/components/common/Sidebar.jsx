import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard, FileText, FileCheck, RefreshCw,
  ClipboardList, CalendarClock, BarChart3, BrainCircuit,
  User, Settings, ShieldCheck,
} from 'lucide-react';
import useAuthStore from '../../store/authStore';

const SECTIONS = [
  {
    title: 'WORKSPACE',
    items: [
      { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
      { to: '/invoices', icon: FileText, label: 'Invoices' },
      { to: '/gstr2b', icon: FileCheck, label: 'GSTR-2B' },
      { to: '/reconciliation', icon: RefreshCw, label: 'Reconciliation' },
    ],
  },
  {
    title: 'OPERATIONS',
    items: [
      { to: '/returns', icon: ClipboardList, label: 'Return Draft' },
      { to: '/deadlines', icon: CalendarClock, label: 'Deadlines' },
      { to: '/analytics', icon: BarChart3, label: 'Analytics' },
      { to: '/insights', icon: BrainCircuit, label: 'AI Insights' },
    ],
  },
  {
    title: 'SYSTEM',
    items: [
      { to: '/profile', icon: User, label: 'Profile' },
      { to: '/settings', icon: Settings, label: 'Settings' },
    ],
  },
];

const Sidebar = ({ collapsed }) => {
  const { user } = useAuthStore();

  const sidebarStyle = {
    width: collapsed ? 76 : 240,
    background: 'var(--bg-sidebar)',
    border: '1px solid var(--border)',
    borderRadius: '20px',
    margin: '16px 8px 16px 16px',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.15)',
    transition: 'all 0.3s cubic-bezier(0.4,0,0.2,1)',
    display: 'flex',
    flexDirection: 'column',
    height: 'calc(100vh - 32px)',
    flexShrink: 0,
    overflow: 'hidden',
  };

  return (
    <aside style={sidebarStyle}>
      {/* Logo */}
      <div style={{
        height: 72, borderBottom: '1px solid var(--border)',
        display: 'flex', alignItems: 'center',
        padding: collapsed ? '0 14px' : '0 24px', gap: 12,
        justifyContent: collapsed ? 'center' : 'flex-start',
      }}>
        <div style={{
          width: 38, height: 38, borderRadius: 12,
          background: 'linear-gradient(135deg, var(--accent) 0%, var(--pink) 100%)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          flexShrink: 0,
          boxShadow: '0 4px 12px rgba(139, 92, 246, 0.3)',
        }}>
          <ShieldCheck size={20} color="#fff" />
        </div>
        {!collapsed && (
          <div>
            <p style={{ margin: 0, fontWeight: 800, fontSize: 16, color: 'var(--text-primary)', letterSpacing: '-0.02em', lineHeight: 1.1 }}>
              gst<span style={{ color: 'var(--accent)' }}>.</span>agent
            </p>
            <p style={{ margin: 0, fontSize: 9, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginTop: 2 }}>
              Compliance Engine
            </p>
          </div>
        )}
      </div>

      {/* Nav */}
      <nav style={{ flex: 1, overflowY: 'auto', padding: '16px 12px' }}>
        {SECTIONS.map((sec) => (
          <div key={sec.title} style={{ marginBottom: 24 }}>
            {!collapsed && (
              <p style={{
                margin: '0 0 8px 12px', fontSize: 10, fontWeight: 700,
                textTransform: 'uppercase', letterSpacing: '0.08em',
                color: 'var(--text-muted)',
              }}>
                {sec.title}
              </p>
            )}
            {sec.items.map((item) => (
              <NavLink key={item.to} to={item.to} style={{ textDecoration: 'none' }}>
                {({ isActive }) => (
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 12,
                    padding: collapsed ? '12px 0' : '10px 16px',
                    justifyContent: collapsed ? 'center' : 'flex-start',
                    borderRadius: 12,
                    marginBottom: 4,
                    background: isActive ? 'var(--accent)' : 'transparent',
                    color: isActive ? '#ffffff' : 'var(--text-secondary)',
                    cursor: 'pointer',
                    boxShadow: isActive ? '0 4px 12px rgba(139, 92, 246, 0.25)' : 'none',
                    transition: 'all 0.2s ease',
                  }}
                    onMouseEnter={(e) => {
                      if (!isActive) {
                        e.currentTarget.style.background = 'var(--accent-soft)';
                        e.currentTarget.style.color = 'var(--text-primary)';
                      }
                    }}
                    onMouseLeave={(e) => {
                      if (!isActive) {
                        e.currentTarget.style.background = 'transparent';
                        e.currentTarget.style.color = 'var(--text-secondary)';
                      }
                    }}
                  >
                    <item.icon
                      size={18}
                      style={{ color: isActive ? '#ffffff' : 'var(--text-muted)', flexShrink: 0, transition: 'color 0.2s' }}
                    />
                    {!collapsed && (
                      <span style={{
                        fontSize: 13, fontWeight: isActive ? 600 : 500,
                        whiteSpace: 'nowrap',
                      }}>
                        {item.label}
                      </span>
                    )}
                  </div>
                )}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      {/* User card */}
      <div style={{ borderTop: '1px solid var(--border)', padding: '16px 12px' }}>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          justifyContent: collapsed ? 'center' : 'flex-start',
        }}>
          <div style={{
            width: 36, height: 36, borderRadius: '50%',
            background: 'linear-gradient(135deg, var(--accent) 0%, var(--pink) 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: '#fff', fontWeight: 700, fontSize: 13, flexShrink: 0,
            boxShadow: '0 4px 10px rgba(0, 0, 0, 0.1)',
          }}>
            {user?.businessName?.charAt(0).toUpperCase() || 'U'}
          </div>
          {!collapsed && (
            <div style={{ minWidth: 0 }}>
              <p style={{ margin: 0, fontSize: 12, fontWeight: 600, color: 'var(--text-primary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user?.businessName || 'User'}
              </p>
              <p style={{ margin: 0, fontSize: 10, color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user?.gstin || user?.email || 'GSTIN'}
              </p>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
