import React, { useState } from 'react';
import { Table, Button, Space, Tag, Modal, Popconfirm, message } from 'antd';
import { EyeOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import { useInvoices } from '../../hooks/useInvoices';
import { getCurrentPeriod, formatCurrency, formatDate, getStatusBadge } from '../../utils/formatters';
import InvoiceFilters from './InvoiceFilters';

const InvoiceList = () => {
  const period = getCurrentPeriod();
  const [filters, setFilters] = useState({
    month: period.month,
    year: period.year,
    status: null,
  });
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);

  const { invoices, loading, total, deleteInvoice, refetch } = useInvoices(filters);

  const columns = [
    {
      title: 'Invoice #',
      dataIndex: 'invoiceNumber',
      key: 'invoiceNumber',
      fixed: 'left',
      width: 150,
    },
    {
      title: 'Supplier',
      dataIndex: 'vendorName',
      key: 'vendorName',
      width: 200,
    },
    {
      title: 'GSTIN',
      dataIndex: 'vendorGstin',
      key: 'vendorGstin',
      width: 150,
    },
    {
      title: 'Invoice Date',
      dataIndex: 'invoiceDate',
      key: 'invoiceDate',
      render: (date) => formatDate(date),
      width: 120,
    },
{
  title: 'Taxable Value',
  key: 'taxableValue',
  width: 150,
  render: (_, record) => {
    const total = Number(record.totalAmount || 0);
    const gst = Number(record.totalGst || 0);

    return formatCurrency(total - gst);
  },
},   // <-- THIS COMMA WAS MISSING

{
  title: 'Total Amount',
  dataIndex: 'totalAmount',
  key: 'totalAmount',
  render: (val) => formatCurrency(val),
  width: 150,
},
    {
      title: 'Total Amount',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      render: (val) => formatCurrency(val),
      width: 150,
    },
    {
      title: 'Total GST',
      dataIndex: 'totalGst',
      key: 'totalGst',
      render: (val) => formatCurrency(val),
      width: 130,
    },
    {
     title: 'Status',
      dataIndex: 'parseStatus',
      key: 'parseStatus',
       render: (status) => {
       const badge = getStatusBadge(status);
       return <Tag color={badge.color}>{badge.text}</Tag>;
      },
      width: 120,
    },
    {
      title: 'Actions',
      key: 'actions',
      fixed: 'right',
      width: 120,
      render: (_, record) => (
        <Space>
          <Button
            type="text"
            icon={<EyeOutlined />}
            onClick={() => {
              setSelectedInvoice(record);
              setModalVisible(true);
            }}
          />
          <Popconfirm
            title="Delete Invoice"
            description="Are you sure you want to delete this invoice?"
            onConfirm={() => handleDelete(record.id)}
            okText="Yes"
            cancelText="No"
          >
            <Button type="text" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleDelete = async (id) => {
    try {
      await deleteInvoice(id);
      message.success('Invoice deleted successfully');
    } catch (error) {
      message.error('Failed to delete invoice');
    }
  };

  const handleFilterChange = (newFilters) => {
    setFilters({ ...filters, ...newFilters });
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <InvoiceFilters filters={filters} onFilterChange={handleFilterChange} />
        <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
          Refresh
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={invoices}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1200 }}
        pagination={{
          total: total,
          pageSize: 10,
          showSizeChanger: true,
          showTotal: (total) => `Total ${total} invoices`,
        }}
      />

      <Modal
        title="Invoice Details"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={800}
      >
        {selectedInvoice && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-gray-500">Invoice Number</p>
                <p className="font-medium">{selectedInvoice.invoiceNumber}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Invoice Date</p>
                <p className="font-medium">{formatDate(selectedInvoice.invoiceDate)}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Supplier</p>
                <p className="font-medium">{selectedInvoice.vendorName}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Supplier GSTIN</p>
                <p className="font-medium">{selectedInvoice.vendorGstin}</p>
              </div>
            <div>
                <p className="text-sm text-gray-500">Taxable Value</p>
                <p className="font-medium">
                    {formatCurrency(
                        Number(selectedInvoice.totalAmount || 0) -
                      Number(selectedInvoice.totalGst || 0)
                        )}
                        </p>
                      </div>
              <div>
                <p className="text-sm text-gray-500">Total Amount</p>
                <p className="font-medium">{formatCurrency(selectedInvoice.totalAmount)}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">CGST</p>
                <p className="font-medium">{formatCurrency(selectedInvoice.cgstAmount)}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">SGST</p>
                <p className="font-medium">{formatCurrency(selectedInvoice.sgstAmount)}</p>
              </div>
              {selectedInvoice.igstAmount > 0 && (
                <div>
                  <p className="text-sm text-gray-500">IGST</p>
                  <p className="font-medium">{formatCurrency(selectedInvoice.igstAmount)}</p>
                </div>
              )}
              <div>
            <p className="text-sm text-gray-500">Status</p>
          <Tag color={getStatusBadge(selectedInvoice.parseStatus).color}>
             {getStatusBadge(selectedInvoice.parseStatus).text}
              </Tag>
          </div>
            </div>

            <div>
              <h4 className="font-medium mb-2">Line Items</h4>
              <Table
                columns={[
                  { title: 'Description', dataIndex: 'description', key: 'description' },
                  { title: 'HSN Code', dataIndex: 'hsnCode', key: 'hsnCode' },
                  { title: 'Qty', dataIndex: 'quantity', key: 'quantity' },
                  { title: 'Unit Price', dataIndex: 'unitPrice', key: 'unitPrice', render: (val) => formatCurrency(val) },
                  { title: 'GST Rate', dataIndex: 'gstRate', key: 'gstRate', render: (val) => `${val}%` },
                  { title: 'Taxable Value', dataIndex: 'taxableValue', key: 'taxableValue', render: (val) => formatCurrency(val) },
                ]}
                dataSource={selectedInvoice.lineItems}
                rowKey="id"
                pagination={false}
                size="small"
              />
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default InvoiceList;