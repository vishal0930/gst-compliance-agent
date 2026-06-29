import React from 'react';
import { Card, Form, Switch, Button, Space, Divider, Select, InputNumber } from 'antd';
import { Settings as SettingsIcon, Bell, Shield, Database } from 'lucide-react';
import useAuthStore from '../store/authStore';

const Settings = () => {
  const { user } = useAuthStore();
  const [form] = Form.useForm();

  const onFinish = (values) => {
    console.log('Settings update:', values);
    // Settings update requires backend endpoint
  };

  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center gap-2 mb-4">
        <SettingsIcon className="text-amber-400" size={20} />
        <h1 className="text-2xl font-bold text-white">Settings</h1>
      </div>

      <Card 
        title={
          <div className="flex items-center gap-2">
            <Bell size={16} className="text-amber-400" />
            <span>Notifications</span>
          </div>
        }
        className="bg-slate-900 border-slate-800"
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          initialValues={{
            emailNotifications: true,
            deadlineAlerts: true,
            mismatchAlerts: true,
            reconciliationComplete: true,
          }}
        >
          <Form.Item
            label="Email Notifications"
            name="emailNotifications"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label="Deadline Alerts"
            name="deadlineAlerts"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label="Mismatch Alerts"
            name="mismatchAlerts"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label="Reconciliation Complete"
            name="reconciliationComplete"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
        </Form>
      </Card>

      <Card
        title={
          <div className="flex items-center gap-2">
            <Shield size={16} className="text-amber-400" />
            <span>Security</span>
          </div>
        }
        className="bg-slate-900 border-slate-800"
      >
        <Form layout="vertical">
          <Form.Item label="Session Timeout (minutes)">
            <InputNumber min={5} max={120} defaultValue={30} className="w-full bg-slate-800 border-slate-700" />
          </Form.Item>

          <Form.Item label="Two-Factor Authentication">
            <Switch disabled />
          </Form.Item>
        </Form>
      </Card>

      <Card
        title={
          <div className="flex items-center gap-2">
            <Database size={16} className="text-amber-400" />
            <span>Data & Preferences</span>
          </div>
        }
        className="bg-slate-900 border-slate-800"
      >
        <Form layout="vertical">
          <Form.Item label="Default GST Period">
            <Select
              defaultValue="current"
              options={[
                { label: 'Current Period', value: 'current' },
                { label: 'Previous Period', value: 'previous' },
              ]}
              className="bg-slate-800 border-slate-700"
            />
          </Form.Item>

          <Form.Item label="Items Per Page">
            <InputNumber min={10} max={100} step={10} defaultValue={20} className="w-full bg-slate-800 border-slate-700" />
          </Form.Item>
        </Form>
      </Card>

      <div className="flex justify-end gap-2">
        <Button type="primary" className="bg-amber-500 text-slate-950 border-amber-500 hover:bg-amber-400">
          Save Changes
        </Button>
        <Button className="border-slate-700 text-slate-300">
          Reset to Defaults
        </Button>
      </div>
    </div>
  );
};

export default Settings;