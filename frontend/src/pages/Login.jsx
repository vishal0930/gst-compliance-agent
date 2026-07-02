import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import useAuthStore from '../store/authStore';

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [loading, setLoading] = useState(false);

  const onFinish = async (values) => {
    setLoading(true);
    try {
      await login(values.email, values.password);
      navigate('/dashboard');
    } catch (error) {
      message.error(error.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-canvas flex items-center justify-center p-4 transition-colors duration-300">
      <Card className="w-full max-w-md bg-card border-theme shadow-2xl p-4">
        <div className="text-center mb-8">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-[var(--accent)] to-[var(--pink)] flex items-center justify-center mx-auto mb-4 shadow-lg shadow-[rgba(139,92,246,0.3)]">
            <span className="text-white font-extrabold text-lg">GST</span>
          </div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)] mb-2">GST Compliance Platform</h1>
          <p className="text-[var(--text-muted)]">Sign in to your account</p>
        </div>

        <Form name="login" onFinish={onFinish} layout="vertical">
          <Form.Item
            name="email"
            rules={[
              { required: true, message: 'Please enter your email' },
              { type: 'email', message: 'Please enter a valid email' },
            ]}
          >
            <Input
              prefix={<UserOutlined className="text-[var(--text-muted)]" />}
              placeholder="Email Address"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: 'Please enter your password' }]}
          >
            <Input.Password
              prefix={<LockOutlined className="text-[var(--text-muted)]" />}
              placeholder="Password"
              size="large"
            />
          </Form.Item>

          <Form.Item className="mt-6">
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              size="large"
              className="font-semibold h-12 shadow-md shadow-[rgba(139,92,246,0.2)]"
            >
              Sign In
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Login;