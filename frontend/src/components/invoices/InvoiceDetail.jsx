import React from 'react';
import { Modal, Descriptions, Table, Tag, Divider, Button, Space } from 'antd';
import { DownloadOutlined, PrinterOutlined } from '@ant-design/icons';
import { formatCurrency, formatDate, getStatusBadge } from '../../utils/formatters';

const InvoiceDetail = ({ visible, onClose, invoice }) => {
  if (!invoice) return null;

  const lineItemColumns = [
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: 'HSN Code',
      dataIndex: 'hsnCode',
      key: 'hsnCode',
      width: 120,
    },
    {
      title: 'Qty',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 80,
      align: 'right',
    },
    {
      title: 'Unit Price',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      width: 120,
      align: 'right',
      render: (val) => formatCurrency(val),
    },
    {
      title: 'GST Rate',
      dataIndex: 'gstRate',
      key: 'gstRate',
      width: 100,
      align: 'center',
      render: (val) => `${val}%`,
    },
    {
      title: 'Taxable Value',
      dataIndex: 'taxableValue',
      key: 'taxableValue',
      width: 150,
      align: 'right',
      render: (val) => formatCurrency(val),
    },
    {
      title: 'CGST',
      dataIndex: 'cgstAmount',
      key: 'cgstAmount',
      width: 120,
      align: 'right',
      render: (val) => formatCurrency(val || 0),
    },
    {
      title: 'SGST',
      dataIndex: 'sgstAmount',
      key: 'sgstAmount',
      width: 120,
      align: 'right',
      render: (val) => formatCurrency(val || 0),
    },
    {
      title: 'IGST',
      dataIndex: 'igstAmount',
      key: 'igstAmount',
      width: 120,
      align: 'right',
      render: (val) => formatCurrency(val || 0),
    },
  ];

  return (
    <Modal
      title="Invoice Details"
      open={visible}
      onCancel={onClose}
      footer={[
        <Button key="print" icon={<PrinterOutlined />} onClick={() => window.print()}>
          Print
        </Button>,
        <Button key="download" type="primary" icon={<DownloadOutlined />}>
          Download PDF
        </Button>,
      ]}
      width={950}
      destroyOnClose
    >
      <Descriptions bordered column={2} className="mb-6">
        <Descriptions.Item label="Invoice Number" span={1}>
          <strong>{invoice.invoiceNumber}</strong>
        </Descriptions.Item>
        <Descriptions.Item label="Invoice Date" span={1}>
          {formatDate(invoice.invoiceDate)}
        </Descriptions.Item>
        <Descriptions.Item label="Supplier Name" span={1}>
          {invoice.vendorName}
        </Descriptions.Item>
        <Descriptions.Item label="Supplier GSTIN" span={1}>
          <Tag color="blue">{invoice.vendorGstin}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Taxable Value" span={1}>
          <span className="font-medium">{formatCurrency(invoice.taxableValue)}</span>
        </Descriptions.Item>
        <Descriptions.Item label="Total Amount" span={1}>
          <span className="font-bold text-blue-600">{formatCurrency(invoice.totalAmount)}</span>
        </Descriptions.Item>
        <Descriptions.Item label="CGST" span={1}>
          {formatCurrency(invoice.cgstAmount || 0)}
        </Descriptions.Item>
        <Descriptions.Item label="SGST" span={1}>
          {formatCurrency(invoice.sgstAmount || 0)}
        </Descriptions.Item>
        <Descriptions.Item label="IGST" span={1}>
          {formatCurrency(invoice.igstAmount || 0)}
        </Descriptions.Item>
        <Descriptions.Item label="Status" span={1}>
          <Tag color={getStatusBadge(invoice.status).color}>
            {getStatusBadge(invoice.status).text}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Total GST" span={2}>
          <span className="font-medium text-orange-600">
            {formatCurrency((invoice.cgstAmount || 0) + (invoice.sgstAmount || 0) + (invoice.igstAmount || 0))}
          </span>
        </Descriptions.Item>
      </Descriptions>

      <Divider orientation="left">Line Items</Divider>

      <Table
        columns={lineItemColumns}
        dataSource={invoice.lineItems || []}
        rowKey="id"
        pagination={false}
        size="small"
        scroll={{ x: 950 }}
        summary={(pageData) => {
          const totalTaxable = pageData.reduce((sum, item) => sum + (item.taxableValue || 0), 0);
          const totalCgst = pageData.reduce((sum, item) => sum + (item.cgstAmount || 0), 0);
          const totalSgst = pageData.reduce((sum, item) => sum + (item.sgstAmount || 0), 0);
          const totalIgst = pageData.reduce((sum, item) => sum + (item.igstAmount || 0), 0);
          
          return (
            <Table.Summary>
              <Table.Summary.Row className="font-bold">
                <Table.Summary.Cell index={0} colSpan={5}>
                  <strong>Total</strong>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={5} align="right">
                  <strong>{formatCurrency(totalTaxable)}</strong>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={6} align="right">
                  <strong>{formatCurrency(totalCgst)}</strong>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={7} align="right">
                  <strong>{formatCurrency(totalSgst)}</strong>
                </Table.Summary.Cell>
                <Table.Summary.Cell index={8} align="right">
                  <strong>{formatCurrency(totalIgst)}</strong>
                </Table.Summary.Cell>
              </Table.Summary.Row>
            </Table.Summary>
          );
        }}
      />

      <div className="mt-4 text-sm text-gray-500 flex justify-between">
        <span>Created: {formatDate(invoice.createdAt)}</span>
        <span>Last Updated: {formatDate(invoice.updatedAt)}</span>
      </div>
    </Modal>
  );
};

export default InvoiceDetail;