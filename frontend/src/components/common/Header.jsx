import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Select, Tooltip } from 'antd';
import { Menu, Bell, Sun, Moon, LogOut, ChevronDown } from 'lucide-react';
import useAuthStore from '../../store/authStore';
import useUiStore from '../../store/uiStore';
import { getMonthOptions, getYearOptions } from '../../utils/formatters';
import { useNotifications } from '../../hooks/useNotifications';

const PAGE_TITLES = {
  '/dashboard': 'Dashboard',
  '/invoices': 'Invoices',
  '/gstr2b': 'GSTR-2B',
  '/reconciliation': 'Reconciliation',
  '/returns': 'Return Draft',
  '/deadlines': 'Deadlines',
  '/analytics': 'Analytics',
  '/insights': 'AI Insights',
  '/settings': 'Settings',
  '/profile': 'Profile',
};

const Header = ({ collapsed, setCollapsed }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { theme, toggleTheme, gstPeriod, setGstPeriod } = useUiStore();

  const [showNotifications, setShowNotifications] = useState(false);
  const { notifications, unreadCount, markAsRead, markAllAsRead } = useNotifications();

  const title = PAGE_TITLES[location.pathname] || 'GST Compliance';
  const isDark = theme === 'dark';

  const headerStyle = {
    height: 72,
    background: 'var(--bg-card)',
    border: '1px solid var(--border)',
    borderRadius: '16px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 20px',
    margin: '16px 16px 8px 8px',
    gap: 16,
    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
  };

  const iconBtn = {
    width: 38,
    height: 38,
    borderRadius: 10,
    border: 'none',
    background: 'transparent',
    cursor: 'pointer',
    color: 'var(--text-secondary)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    transition: 'all 0.2s ease',
  };

  return (
    <header style={headerStyle}>
      {/* Left — burger + title */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, minWidth: 0 }}>
        <button 
          style={iconBtn} 
          onClick={() => setCollapsed(!collapsed)}
          onMouseEnter={(e) => e.currentTarget.style.background = 'var(--bg-input)'}
          onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
        >
          <Menu size={20} />
        </button>
        <div>
          <p style={{ margin: 0, fontWeight: 700, fontSize: 16, color: 'var(--text-primary)', lineHeight: 1.3 }}>
            {title}
          </p>
          <p style={{ margin: 0, fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.3 }}>
            AI-Powered GST Compliance Platform
          </p>
        </div>
      </div>

      {/* Centre — Global GST Period selector */}
      <div style={{ 
        display: 'flex', 
        alignItems: 'center', 
        gap: 8, 
        background: 'var(--bg-input)', 
        padding: '5px 14px', 
        borderRadius: '20px', 
        border: '1px solid var(--border)' 
      }}>
        <span style={{ fontSize: 10, fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', whiteSpace: 'nowrap' }}>
          GST Period
        </span>
        <Select
          size="small"
          variant="borderless"
          value={gstPeriod.month}
          onChange={(v) => setGstPeriod(v, gstPeriod.year)}
          options={getMonthOptions()}
          style={{ width: 90, color: 'var(--accent)', fontWeight: 700 }}
          popupMatchSelectWidth={false}
        />
        <Select
          size="small"
          variant="borderless"
          value={gstPeriod.year}
          onChange={(v) => setGstPeriod(gstPeriod.month, v)}
          options={getYearOptions()}
          style={{ width: 68, color: 'var(--accent)', fontWeight: 700 }}
          popupMatchSelectWidth={false}
        />
      </div>

      {/* Right — actions */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        {/* Theme toggle pill */}
        <Tooltip title={isDark ? 'Switch to Light' : 'Switch to Dark'}>
          <button 
            onClick={toggleTheme}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              background: 'var(--bg-input)',
              border: '1px solid var(--border)',
              padding: '6px 12px',
              borderRadius: '20px',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
              color: 'var(--text-primary)',
              height: 34,
            }}
            onMouseEnter={(e) => e.currentTarget.style.borderColor = 'var(--accent)'}
            onMouseLeave={(e) => e.currentTarget.style.borderColor = 'var(--border)'}
          >
            {isDark ? (
              <>
                <Moon size={14} style={{ color: 'var(--accent)' }} />
                <span style={{ fontSize: 11, fontWeight: 700 }}>Dark</span>
              </>
            ) : (
              <>
                <Sun size={14} style={{ color: '#eab308' }} />
                <span style={{ fontSize: 11, fontWeight: 700 }}>Light</span>
              </>
            )}
          </button>
        </Tooltip>

        {/* Notifications */}
        <div style={{ position: 'relative' }}>
          <button 
            style={{ ...iconBtn, position: 'relative' }}
            onClick={() => setShowNotifications(!showNotifications)}
            onMouseEnter={(e) => e.currentTarget.style.background = 'var(--bg-input)'}
            onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
          >
            <Bell size={18} />
            {unreadCount > 0 && (
              <span style={{
                position: 'absolute', top: 8, right: 8,
                width: 7, height: 7, borderRadius: '50%',
                background: 'var(--pink)', border: '1.5px solid var(--bg-card)',
              }} />
            )}
          </button>
          
          {showNotifications && (
            <div style={{
              position: 'absolute',
              top: '46px',
              right: 0,
              width: 320,
              backgroundColor: 'var(--bg-card)',
              border: '1px solid var(--border)',
              borderRadius: '12px',
              boxShadow: '0 8px 30px rgba(0, 0, 0, 0.3)',
              zIndex: 1000,
              padding: '12px 0',
              maxHeight: 400,
              display: 'flex',
              flexDirection: 'column',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 16px 8px 16px', borderBottom: '1px solid var(--border)' }}>
                <span style={{ fontWeight: 700, fontSize: 13, color: 'var(--text-primary)' }}>Notifications</span>
                {unreadCount > 0 && (
                  <button 
                    onClick={() => markAllAsRead()}
                    style={{ background: 'none', border: 'none', color: 'var(--accent)', cursor: 'pointer', fontSize: 11, fontWeight: 600 }}
                  >
                    Mark all read
                  </button>
                )}
              </div>
              <div style={{ overflowY: 'auto', flex: 1, maxHeight: 300 }}>
                {notifications.length === 0 ? (
                  <div style={{ padding: '24px 16px', textAlign: 'center', color: 'var(--text-muted)', fontSize: 12 }}>
                    No notifications
                  </div>
                ) : (
                  notifications.map(n => (
                    <div 
                      key={n.id} 
                      onClick={() => {
                        if (n.status === 'UNREAD') markAsRead(n.id);
                        if (n.actionUrl) {
                          navigate(n.actionUrl);
                          setShowNotifications(false);
                        }
                      }}
                      style={{
                        padding: '10px 16px',
                        borderBottom: '1px solid var(--border)',
                        cursor: 'pointer',
                        background: n.status === 'UNREAD' ? 'var(--accent-soft)' : 'transparent',
                        transition: 'background 0.2s',
                      }}
                      onMouseEnter={(e) => e.currentTarget.style.background = 'var(--bg-input)'}
                      onMouseLeave={(e) => e.currentTarget.style.background = n.status === 'UNREAD' ? 'var(--accent-soft)' : 'transparent'}
                    >
                      <div style={{ fontWeight: 600, fontSize: 12, color: 'var(--text-primary)', marginBottom: 2 }}>{n.title}</div>
                      <div style={{ fontSize: 11, color: 'var(--text-secondary)', lineHeight: 1.3 }}>{n.message}</div>
                      <div style={{ fontSize: 9, color: 'var(--text-muted)', marginTop: 4 }}>
                        {new Date(n.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* User */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginLeft: 4 }}>
          <div style={{
            width: 34, height: 34, borderRadius: '50%',
            background: 'linear-gradient(135deg, var(--accent) 0%, var(--pink) 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: '#fff', fontWeight: 700, fontSize: 13,
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
          }}>
            {user?.businessName?.charAt(0).toUpperCase() || user?.email?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div className="hidden md:block">
            <p style={{ margin: 0, fontSize: 12, fontWeight: 600, color: 'var(--text-primary)', lineHeight: 1.3 }}>
              {user?.businessName || 'User'}
            </p>
            <p style={{ margin: 0, fontSize: 9, color: 'var(--text-muted)', lineHeight: 1.3 }}>
              {user?.gstin || user?.email}
            </p>
          </div>
          <ChevronDown size={14} style={{ color: 'var(--text-muted)' }} />
        </div>

        <div style={{ width: 1, height: 24, background: 'var(--border)', margin: '0 4px' }} />

        <Tooltip title="Log out">
          <button 
            style={{ ...iconBtn, color: 'var(--danger)', width: 34, height: 34 }}
            onClick={async () => { await logout(); navigate('/login'); }}
            onMouseEnter={(e) => e.currentTarget.style.background = 'var(--danger-soft)'}
            onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
          >
            <LogOut size={16} />
          </button>
        </Tooltip>
      </div>
    </header>
  );
};

export default Header;
