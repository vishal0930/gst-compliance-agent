import React, { useState } from 'react';
import { Tabs, Table, Card, Statistic, Row, Col } from 'antd';
import { FileSpreadsheet, Upload } from 'lucide-react';
import { useGstr2b } from '../hooks/useGstr2b';
import Gstr2bUpload from '../components/gstr2b/Gstr2bUpload';
import { getCurrentPeriod, formatCurrency } from '../utils/formatters';

const Gstr2b = () => {
  const period = getCurrentPeriod();
  const [activeTab, setActiveTab] = useState('list');
  const { useInvoices, useSummary } = useGstr2b();
  const { data: invoices, isLoading } = useInvoices({
    month: period.month,
    year: period.year,
  });
  const { data: summary } = useSummary(period.month, period.year);

  const columns = [
    { title: 'Invoice #', dataIndex: 'invoiceNumber', key: 'invoiceNumber' },
    { title: 'Supplier', dataIndex: 'supplierName', key: 'supplierName' },
    { title: 'Supplier GSTIN', dataIndex: 'supplierGstin', key: 'supplierGstin' },
    {
      title: 'Taxable Value',
      dataIndex: 'taxableValue',
      key: 'taxableValue',
      render: (val) => formatCurrency(val),
    },
    {
      title: 'Grand Total',
      dataIndex: 'grandTotal',
      key: 'grandTotal',
      render: (val) => formatCurrency(val),
    },
    { title: 'Invoice Date', dataIndex: 'invoiceDate', key: 'invoiceDate' },
  ];

  const tabItems = [
    {
      key: 'list',
      label: (
        <div className="flex items-center gap-2">
          <FileSpreadsheet size={16} />
          <span>GSTR-2B Invoices</span>
        </div>
      ),
      children: (
        <Table
          columns={columns}
          dataSource={invoices}
          loading={isLoading}
          rowKey="id"
          pagination={{ pageSize: 10 }}
        />
      ),
    },
    {
      key: 'upload',
      label: (
        <div className="flex items-center gap-2">
          <Upload size={16} />
          <span>Upload GSTR-2B</span>
        </div>
      ),
      children: <Gstr2bUpload />,
    },
  ];

  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center gap-2 mb-4">
        <FileSpreadsheet className="text-amber-400" size={20} />
        <h1 className="text-2xl font-bold text-white">GSTR-2B Management</h1>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card className="bg-slate-900 border-slate-800">
            <Statistic
              title="Total Invoices"
              value={summary?.totalInvoices || 0}
              valueStyle={{ color: '#fff' }}
              className="text-slate-400"
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card className="bg-slate-900 border-slate-800">
            <Statistic
              title="Total Taxable Value"
              value={formatCurrency(summary?.totalTaxableValue || 0)}
              valueStyle={{ color: '#fff' }}
              className="text-slate-400"
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card className="bg-slate-900 border-slate-800">
            <Statistic
              title="Total ITC"
              value={formatCurrency(summary?.totalItc || 0)}
              valueStyle={{ color: '#fff' }}
              className="text-slate-400"
            />
          </Card>
        </Col>
      </Row>

      <Card className="bg-slate-900 border-slate-800">
        <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
      </Card>
    </div>
  );
};

export default Gstr2b;