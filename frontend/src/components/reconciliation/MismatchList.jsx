import React, { useState } from 'react';
import { Table, Tag, Button, Space } from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import { getMismatchTypeLabel, getMismatchTypeColor, formatCurrency, formatDate } from '../../utils/formatters';

const MismatchList = ({ mismatches, onViewDetail }) => {
  const [filterType, setFilterType] = useState(null);

  const columns = [
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={getMismatchTypeColor(status)}>
          {getMismatchTypeLabel(status)}
        </Tag>
      ),
      filters: [...new Set(mismatches.map(m => m.status))].map(s => ({
        text: getMismatchTypeLabel(s),
        value: s,
      })),
      onFilter: (value, record) => record.status === value,
    },
    {
      title: 'Invoice #',
      dataIndex: 'invoiceNumber',
      key: 'invoiceNumber',
    },
    {
      title: 'Supplier GSTIN',
      dataIndex: 'supplierGstin',
      key: 'supplierGstin',
    },
    {
      title: 'Book Amount',
      dataIndex: 'bookAmount',
      key: 'bookAmount',
      render: (val) => val ? formatCurrency(val) : '-',
    },
    {
      title: 'Portal Amount',
      dataIndex: 'portalAmount',
      key: 'portalAmount',
      render: (val) => val ? formatCurrency(val) : '-',
    },
    {
      title: 'Risk Amount',
      dataIndex: 'riskAmount',
      key: 'riskAmount',
      render: (val) => val ? (
        <span className="text-red-500 font-medium">{formatCurrency(val)}</span>
      ) : '-',
    },
    {
      title: 'Date',
      dataIndex: 'bookInvoiceDate',
      key: 'bookInvoiceDate',
      render: (date) => formatDate(date),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Button
          type="text"
          icon={<EyeOutlined />}
          onClick={() => onViewDetail(record)}
        >
          View
        </Button>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={mismatches}
      rowKey={(record, index) => `${record.invoiceNumber}-${index}`}
      pagination={{
        pageSize: 10,
        showTotal: (total) => `Total ${total} mismatches`,
      }}
      scroll={{ x: 1000 }}
    />
  );
};

export default MismatchList;