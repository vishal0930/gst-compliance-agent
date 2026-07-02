import React from 'react';
import { Card, Form, Switch, Button, Divider, Select, message } from 'antd';
import { Settings as SettingsIcon, Bell, Palette, Database } from 'lucide-react';
import useUiStore from '../store/uiStore';

/**
 * Settings page.
 * - Theme toggle is wired to uiStore (persisted via zustand/persist).
 * - Notification preferences and data preferences are stored in localStorage
 *   since the backend does not yet expose a user-preferences endpoint.
 * - Two-Factor Authentication is shown as coming soon (backend not implemented).
 */
const PREFS_KEY = 'gst_user_prefs';

const loadPrefs = () => {
  try { return JSON.parse(localStorage.getItem(PREFS_KEY)) || {}; } catch { return {}; }
};

const Settings = () => {
  const { theme, toggleTheme, gstPeriod, setGstPeriod } = useUiStore();
  const [notifForm] = Form.useForm();
  const [dataForm] = Form.useForm();

  const saved = loadPrefs();

  const saveNotifications = (values) => {
    localStorage.setItem(PREFS_KEY, JSON.stringify({ ...loadPrefs(), notifications: values }));
    message.success('Notification preferences saved');
  };

  const saveDataPrefs = (values) => {
    localStorage.setItem(PREFS_KEY, JSON.stringify({ ...loadPrefs(), data: values }));
    message.success('Data preferences saved');
  };

  const sectionCard = (icon, title, children) => (
    <Card
      title={<span style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 700, fontSize: 14, color: 'var(--text-primary)' }}>
        {icon}{title}
      </span>}
      style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}
    >
      {children}
    </Card>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: 720 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ background: 'var(--accent-soft)', borderRadius: 10, padding: 10 }}>
          <SettingsIcon size={20} style={{ color: 'var(--accent)' }} />
        </div>
        <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: 'var(--text-primary)' }}>
          Settings
        </h1>
      </div>

      {/* ── Appearance ─────────────────────────────────────── */}
      {sectionCard(
        <Palette size={16} style={{ color: 'var(--accent)' }} />,
        'Appearance',
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '4px 0' }}>
          <div>
            <p style={{ margin: 0, fontWeight: 600, fontSize: 13, color: 'var(--text-primary)' }}>
              {theme === 'dark' ? 'Dark Mode' : 'Light Mode'}
            </p>
            <p style={{ margin: '2px 0 0', fontSize: 12, color: 'var(--text-muted)' }}>
              Persisted across sessions
            </p>
          </div>
          <Switch
            checked={theme === 'dark'}
            onChange={toggleTheme}
            checkedChildren="Dark"
            unCheckedChildren="Light"
          />
        </div>
      )}

      {/* ── Notifications ───────────────────────────────────── */}
      {sectionCard(
        <Bell size={16} style={{ color: 'var(--accent)' }} />,
        'Notifications',
        <Form
          form={notifForm}
          layout="vertical"
          onFinish={saveNotifications}
          initialValues={saved.notifications || {
            emailNotifications: true,
            deadlineAlerts: true,
            mismatchAlerts: true,
            reconciliationComplete: true,
          }}
        >
          {[
            { name: 'emailNotifications', label: 'Email Notifications', desc: 'Receive emails for important compliance events' },
            { name: 'deadlineAlerts', label: 'Deadline Alerts', desc: 'Get notified before GST filing deadlines' },
            { name: 'mismatchAlerts', label: 'Mismatch Alerts', desc: 'Alert when reconciliation finds mismatches' },
            { name: 'reconciliationComplete', label: 'Reconciliation Complete', desc: 'Notify when a reconciliation run finishes' },
          ].map(({ name, label, desc }) => (
            <Form.Item key={name} name={name} valuePropName="checked" style={{ marginBottom: 14 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <p style={{ margin: 0, fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>{label}</p>
                  <p style={{ margin: '1px 0 0', fontSize: 11, color: 'var(--text-muted)' }}>{desc}</p>
                </div>
                <Form.Item name={name} valuePropName="checked" noStyle>
                  <Switch size="small" />
                </Form.Item>
              </div>
            </Form.Item>
          ))}
          <p style={{ margin: '0 0 10px', fontSize: 11, color: 'var(--text-muted)' }}>
            ℹ️ Stored locally — backend notification endpoint not yet available.
          </p>
          <Button type="primary" htmlType="submit">Save Preferences</Button>
        </Form>
      )}

      {/* ── Data & Preferences ──────────────────────────────── */}
      {sectionCard(
        <Database size={16} style={{ color: 'var(--accent)' }} />,
        'Data & Preferences',
        <Form
          form={dataForm}
          layout="vertical"
          onFinish={saveDataPrefs}
          initialValues={saved.data || { defaultPageSize: 25 }}
        >
          <Form.Item
            label={<span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>Default Page Size</span>}
            name="defaultPageSize"
          >
            <Select
              options={[
                { value: 25, label: '25 rows' },
                { value: 50, label: '50 rows' },
                { value: 100, label: '100 rows' },
                { value: 250, label: '250 rows' },
              ]}
              style={{ width: 140 }}
            />
          </Form.Item>
          <p style={{ margin: '0 0 10px', fontSize: 11, color: 'var(--text-muted)' }}>
            ℹ️ Stored locally.
          </p>
          <Button type="primary" htmlType="submit">Save Preferences</Button>
        </Form>
      )}
    </div>
  );
};

export default Settings;
