import React, { useEffect } from 'react';
import { Card, Form, Input, Button, Divider, message, Tag } from 'antd';
import { User, Mail, Building, Phone, ShieldCheck } from 'lucide-react';
import useAuthStore from '../store/authStore';

/**
 * Profile page — reads from the authenticated user in Zustand (populated via /auth/me on login).
 * The backend does not currently expose a PATCH /auth/profile endpoint,
 * so the Save button shows a clear info message instead of silently doing nothing.
 */
const Profile = () => {
  const { user } = useAuthStore();
  const [form] = Form.useForm();

  // Populate form from real user data
  useEffect(() => {
    if (user) {
      form.setFieldsValue({
        businessName: user.businessName || '',
        email: user.email || '',
        gstin: user.gstin || '',
        phone: user.phone || '',
        stateCode: user.stateCode || '',
        turnoverSlab: user.turnoverSlab || '',
      });
    }
  }, [user, form]);

  const onFinish = () => {
    // Backend does not expose a profile update endpoint yet.
    // When it does, call PATCH /api/v1/auth/profile here.
    message.info('Profile update endpoint is not yet available on the backend.');
  };

  const fieldStyle = { background: 'var(--bg-input)', borderColor: 'var(--border)', color: 'var(--text-primary)' };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ background: 'var(--accent-soft)', borderRadius: 10, padding: 10 }}>
          <User size={20} style={{ color: 'var(--accent)' }} />
        </div>
        <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: 'var(--text-primary)' }}>
          Profile
        </h1>
      </div>

      <Card style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}>
        {/* Avatar */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 }}>
          <div style={{
            width: 64, height: 64, borderRadius: '50%',
            background: 'var(--accent)', display: 'flex', alignItems: 'center',
            justifyContent: 'center', fontSize: 24, fontWeight: 800, color: '#fff', flexShrink: 0,
          }}>
            {user?.businessName?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div>
            <p style={{ margin: 0, fontSize: 16, fontWeight: 700, color: 'var(--text-primary)' }}>
              {user?.businessName || '—'}
            </p>
            <p style={{ margin: 0, fontSize: 12, color: 'var(--text-muted)' }}>{user?.email}</p>
            <Tag color="blue" style={{ marginTop: 4, fontFamily: 'monospace', fontSize: 11 }}>
              {user?.gstin || 'GSTIN not set'}
            </Tag>
          </div>
        </div>

        <Divider style={{ borderColor: 'var(--border)', margin: '0 0 20px' }} />

        <Form form={form} layout="vertical" onFinish={onFinish}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
            <Form.Item label={<span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>Business Name</span>} name="businessName">
              <Input prefix={<Building size={14} style={{ color: 'var(--text-muted)' }} />}
                style={fieldStyle} disabled />
            </Form.Item>

            <Form.Item label={<span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>Email Address</span>} name="email">
              <Input prefix={<Mail size={14} style={{ color: 'var(--text-muted)' }} />}
                style={fieldStyle} disabled />
            </Form.Item>

            <Form.Item label={<span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>GSTIN</span>} name="gstin">
              <Input prefix={<ShieldCheck size={14} style={{ color: 'var(--text-muted)' }} />}
                style={{ ...fieldStyle, fontFamily: 'monospace' }} disabled />
            </Form.Item>

            <Form.Item label={<span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>Phone</span>} name="phone">
              <Input prefix={<Phone size={14} style={{ color: 'var(--text-muted)' }} />}
                style={fieldStyle} disabled />
            </Form.Item>

            <Form.Item label={<span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>State Code</span>} name="stateCode">
              <Input style={fieldStyle} disabled />
            </Form.Item>

            <Form.Item label={<span style={{ color: 'var(--text-secondary)', fontSize: 12 }}>Turnover Slab</span>} name="turnoverSlab">
              <Input style={fieldStyle} disabled />
            </Form.Item>
          </div>

          <p style={{ margin: '4px 0 12px', fontSize: 11, color: 'var(--text-muted)' }}>
            ℹ️ Profile fields are read-only. Contact your admin to update business details.
          </p>

          <Button type="primary" htmlType="submit">
            Save Changes
          </Button>
        </Form>
      </Card>
    </div>
  );
};

export default Profile;
