import React, { useState } from 'react';
import { Table, Button, Space, Tag, Input, Select, DatePicker, Card } from 'antd';
import { SearchOutlined, ReloadOutlined, EyeOutlined } from '@ant-design/icons';
import { useGstr2b } from '../../hooks/useGstr2b';
import { formatCurrency, formatDate, getCurrentPeriod } from '../../utils/formatters';
import Gstr2bDetail from './Gstr2bDetail';

const { RangePicker } = DatePicker;

const Gstr2bList = () => {
  const period = getCurrentPeriod();
  const [filters, setFilters] = useState({
    month: period.month,
    year: period.year,
    supplierName: '',
    invoiceNumber: '',
  });
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [detailVisible, setDetailVisible] = useState(false);

  const { useInvoices } = useGstr2b();
  const { data: invoices, isLoading, refetch } = useInvoices({
    month: filters.month,
    year: filters.year,
  });

  const columns = [
    {
      title: 'Invoice #',
      dataIndex: 'invoiceNumber',
      key: 'invoiceNumber',
      sorter: (a, b) => a.invoiceNumber.localeCompare(b.invoiceNumber),
    },
    {
      title: 'Supplier',
      dataIndex: 'supplierName',
      key: 'supplierName',
      sorter: (a, b) => a.supplierName.localeCompare(b.supplierName),
    },
    {
      title: 'Supplier GSTIN',
      dataIndex: 'supplierGstin',
      key: 'supplierGstin',
      width: 150,
    },
    {
      title: 'Date',
      dataIndex: 'invoiceDate',
      key: 'invoiceDate',
      render: (date) => formatDate(date),
      sorter: (a, b) => new Date(a.invoiceDate) - new Date(b.invoiceDate),
    },
    {
      title: 'Taxable Value',
      dataIndex: 'taxableValue',
      key: 'taxableValue',
      render: (val) => formatCurrency(val),
      sorter: (a, b) => a.taxableValue - b.taxableValue,
    },
    {
      title: 'Grand Total',
      dataIndex: 'grandTotal',
      key: 'grandTotal',
      render: (val) => formatCurrency(val),
      sorter: (a, b) => a.grandTotal - b.grandTotal,
    },
    {
      title: 'GST',
      key: 'gst',
      render: (_, record) => {
        const totalGst = (record.cgst || 0) + (record.sgst || 0) + (record.igst || 0);
        return formatCurrency(totalGst);
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 100,
      render: (_, record) => (
        <Button
          type="text"
          icon={<EyeOutlined />}
          onClick={() => {
            setSelectedInvoice(record);
            setDetailVisible(true);
          }}
        >
          View
        </Button>
      ),
    },
  ];

  const handleSearch = (key, value) => {
    setFilters({ ...filters, [key]: value });
  };

  const handleFilter = (key, value) => {
    setFilters({ ...filters, [key]: value });
  };

  return (
    <div>
      <Card className="mb-4">
        <div className="flex flex-wrap gap-4 items-center">
          <Input
            placeholder="Search by supplier"
            prefix={<SearchOutlined className="text-gray-400" />}
            value={filters.supplierName}
            onChange={(e) => handleSearch('supplierName', e.target.value)}
            className="w-64"
          />
          <Input
            placeholder="Search by invoice number"
            prefix={<SearchOutlined className="text-gray-400" />}
            value={filters.invoiceNumber}
            onChange={(e) => handleSearch('invoiceNumber', e.target.value)}
            className="w-56"
          />
          <Select
            placeholder="Month"
            value={filters.month}
            onChange={(value) => handleFilter('month', value)}
            className="w-32"
          >
            {Array.from({ length: 12 }, (_, i) => (
              <Select.Option key={i + 1} value={i + 1}>
                {new Date(0, i).toLocaleString('en', { month: 'long' })}
              </Select.Option>
            ))}
          </Select>
          <Select
            placeholder="Year"
            value={filters.year}
            onChange={(value) => handleFilter('year', value)}
            className="w-28"
          >
            {Array.from({ length: 5 }, (_, i) => {
              const year = new Date().getFullYear() - 2 + i;
              return (
                <Select.Option key={year} value={year}>
                  {year}
                </Select.Option>
              );
            })}
          </Select>
          <Button icon={<ReloadOutlined />} onClick={() => refetch()}>
            Refresh
          </Button>
        </div>
      </Card>

      <Table
        columns={columns}
        dataSource={invoices || []}
        loading={isLoading}
        rowKey="id"
        scroll={{ x: 1000 }}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showTotal: (total) => `Total ${total} invoices`,
        }}
      />

      <Gstr2bDetail
        visible={detailVisible}
        onClose={() => {
          setDetailVisible(false);
          setSelectedInvoice(null);
        }}
        invoice={selectedInvoice}
      />
    </div>
  );
};

export default Gstr2bList;