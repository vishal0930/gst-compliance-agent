import React from 'react';
import { Card, Form, Input, Button, Avatar, Space, Divider } from 'antd';
import { User, Mail, Building, Phone } from 'lucide-react';
import useAuthStore from '../store/authStore';

const Profile = () => {
  const { user } = useAuthStore();
  const [form] = Form.useForm();

  const onFinish = (values) => {
    console.log('Profile update:', values);
    // Profile update requires backend endpoint
  };

  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center gap-2 mb-4">
        <User className="text-amber-400" size={20} />
        <h1 className="text-2xl font-bold text-white">Profile</h1>
      </div>

      <Card className="bg-slate-900 border-slate-800">
        <div className="flex flex-col items-center mb-6">
          <Avatar size={80} className="bg-amber-500 text-slate-950 text-2xl font-bold mb-4">
            {user?.name?.charAt(0).toUpperCase() || 'U'}
          </Avatar>
          <h2 className="text-xl font-semibold text-white">{user?.name || 'User'}</h2>
          <p className="text-slate-400">{user?.email || 'user@example.com'}</p>
        </div>

        <Divider className="border-slate-700" />

        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          initialValues={{
            name: user?.name || '',
            email: user?.email || '',
            company: '',
            phone: '',
          }}
        >
          <Form.Item
            label="Full Name"
            name="name"
            rules={[{ required: true, message: 'Please enter your name' }]}
          >
            <Input
              prefix={<User size={16} className="text-slate-400" />}
              placeholder="Enter your full name"
              className="bg-slate-800 border-slate-700 text-white"
            />
          </Form.Item>

          <Form.Item
            label="Email Address"
            name="email"
            rules={[
              { required: true, message: 'Please enter your email' },
              { type: 'email', message: 'Please enter a valid email' },
            ]}
          >
            <Input
              prefix={<Mail size={16} className="text-slate-400" />}
              placeholder="Enter your email"
              className="bg-slate-800 border-slate-700 text-white"
              disabled
            />
          </Form.Item>

          <Form.Item label="Company Name" name="company">
            <Input
              prefix={<Building size={16} className="text-slate-400" />}
              placeholder="Enter your company name"
              className="bg-slate-800 border-slate-700 text-white"
            />
          </Form.Item>

          <Form.Item label="Phone Number" name="phone">
            <Input
              prefix={<Phone size={16} className="text-slate-400" />}
              placeholder="Enter your phone number"
              className="bg-slate-800 border-slate-700 text-white"
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" className="bg-amber-500 text-slate-950 border-amber-500 hover:bg-amber-400">
                Update Profile
              </Button>
              <Button className="border-slate-700 text-slate-300">
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Profile;