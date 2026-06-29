import React, { useState } from 'react';
import { Card, Form, Input, Button, Table, Space, message, Modal, Select, InputNumber } from 'antd';
import { PlusOutlined, DeleteOutlined, UploadOutlined } from '@ant-design/icons';
import { useGstr2b } from '../../hooks/useGstr2b';
import { useAuth } from '../../hooks/useAuth';
import { formatDate } from '../../utils/formatters';
import { GST_RATES } from '../../utils/constants';

const Gstr2bUpload = () => {
  const { user } = useAuth();
  const { uploadGstr2b, uploading } = useGstr2b();
  const [form] = Form.useForm();
  const [invoices, setInvoices] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);

  const columns = [
    { title: 'Invoice #', dataIndex: 'invoiceNumber', key: 'invoiceNumber' },
    { title: 'Supplier', dataIndex: 'supplierName', key: 'supplierName' },
    { title: 'Supplier GSTIN', dataIndex: 'supplierGstin', key: 'supplierGstin' },
    { 
      title: 'Taxable Value', 
      dataIndex: 'taxableValue', 
      key: 'taxableValue',
      render: (val) => `₹${val.toFixed(2)}`,
    },
    { 
      title: 'Grand Total', 
      dataIndex: 'grandTotal', 
      key: 'grandTotal',
      render: (val) => `₹${val.toFixed(2)}`,
    },
    { title: 'Date', dataIndex: 'invoiceDate', key: 'invoiceDate', render: formatDate },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record, index) => (
        <Button
          type="text"
          danger
          icon={<DeleteOutlined />}
          onClick={() => {
            const newInvoices = [...invoices];
            newInvoices.splice(index, 1);
            setInvoices(newInvoices);
          }}
        />
      ),
    },
  ];

  const onFinish = (values) => {
    const newInvoice = {
      ...values,
      buyerGstin: '09VISHA1234A1Z5', // Replace with actual user GSTIN
      lineItems: [], // Add line items if needed
    };
    setInvoices([...invoices, newInvoice]);
    form.resetFields();
    message.success('Invoice added successfully');
  };

  const handleUpload = async () => {
    if (invoices.length === 0) {
      message.warning('Please add at least one invoice');
      return;
    }

    try {
      await uploadGstr2b({ invoices });
      setInvoices([]);
      message.success('GSTR-2B data uploaded successfully');
    } catch (error) {
      message.error(error.response?.data?.message || 'Upload failed');
    }
  };

  return (
    <Card>
      <div className="mb-6">
        <h3 className="text-lg font-semibold mb-2">Upload GSTR-2B Data</h3>
        <p className="text-gray-500">
          Add GSTR-2B invoices manually for reconciliation.
        </p>
      </div>

      <Form form={form} onFinish={onFinish} layout="vertical" className="mb-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <Form.Item
            name="invoiceNumber"
            label="Invoice Number"
            rules={[{ required: true, message: 'Invoice number is required' }]}
          >
            <Input placeholder="INV-2024-001" />
          </Form.Item>

          <Form.Item
            name="supplierName"
            label="Supplier Name"
            rules={[{ required: true, message: 'Supplier name is required' }]}
          >
            <Input placeholder="ABC Suppliers" />
          </Form.Item>

          <Form.Item
            name="supplierGstin"
            label="Supplier GSTIN"
            rules={[
              { required: true, message: 'Supplier GSTIN is required' },
              { pattern: /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/, message: 'Invalid GSTIN format' },
            ]}
          >
            <Input placeholder="09ABCD1234E1Z5" />
          </Form.Item>

          <Form.Item
            name="invoiceDate"
            label="Invoice Date"
            rules={[{ required: true, message: 'Invoice date is required' }]}
          >
            <Input type="date" />
          </Form.Item>

          <Form.Item
            name="taxableValue"
            label="Taxable Value"
            rules={[{ required: true, message: 'Taxable value is required' }]}
          >
            <InputNumber
              className="w-full"
              min={0}
              step={0.01}
              precision={2}
              placeholder="10000.00"
              formatter={(value) => `₹ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(value) => value.replace(/₹\s?|(,*)/g, '')}
            />
          </Form.Item>

          <Form.Item
            name="grandTotal"
            label="Grand Total"
            rules={[{ required: true, message: 'Grand total is required' }]}
          >
            <InputNumber
              className="w-full"
              min={0}
              step={0.01}
              precision={2}
              placeholder="11800.00"
              formatter={(value) => `₹ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(value) => value.replace(/₹\s?|(,*)/g, '')}
            />
          </Form.Item>

          <Form.Item
            name="cgst"
            label="CGST"
          >
            <InputNumber
              className="w-full"
              min={0}
              step={0.01}
              precision={2}
              placeholder="0.00"
              formatter={(value) => `₹ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(value) => value.replace(/₹\s?|(,*)/g, '')}
            />
          </Form.Item>

          <Form.Item
            name="sgst"
            label="SGST"
          >
            <InputNumber
              className="w-full"
              min={0}
              step={0.01}
              precision={2}
              placeholder="0.00"
              formatter={(value) => `₹ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(value) => value.replace(/₹\s?|(,*)/g, '')}
            />
          </Form.Item>

          <Form.Item
            name="igst"
            label="IGST"
          >
            <InputNumber
              className="w-full"
              min={0}
              step={0.01}
              precision={2}
              placeholder="0.00"
              formatter={(value) => `₹ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(value) => value.replace(/₹\s?|(,*)/g, '')}
            />
          </Form.Item>
        </div>

        <Form.Item>
          <Button type="primary" htmlType="submit" icon={<PlusOutlined />}>
            Add Invoice
          </Button>
        </Form.Item>
      </Form>

      {invoices.length > 0 && (
        <div className="mb-4">
          <h4 className="font-medium mb-2">Added Invoices ({invoices.length})</h4>
          <Table
            columns={columns}
            dataSource={invoices}
            rowKey={(record, index) => index}
            pagination={false}
            size="small"
            scroll={{ x: 800 }}
          />
          <div className="mt-4 flex justify-end">
            <Space>
              <Button onClick={() => setInvoices([])}>Clear All</Button>
              <Button
                type="primary"
                icon={<UploadOutlined />}
                onClick={handleUpload}
                loading={uploading}
              >
                Upload to GSTR-2B
              </Button>
            </Space>
          </div>
        </div>
      )}

      {invoices.length === 0 && (
        <div className="text-center py-8 text-gray-400">
          <p>No invoices added yet. Add your GSTR-2B invoices above.</p>
        </div>
      )}
    </Card>
  );
};

export default Gstr2bUpload;