import React, { useState } from 'react';
import { Card, Row, Col, Statistic, Tabs, Table, Tag, Space, Button, Modal, Descriptions, Alert } from 'antd';
import { 
  CheckCircleOutlined, 
  CloseCircleOutlined, 
  ExclamationCircleOutlined,
  FileSearchOutlined,
  DownloadOutlined,
  PrinterOutlined,
} from '@ant-design/icons';
import { formatCurrency, getMismatchTypeLabel, getMismatchTypeColor, formatDate } from '../../utils/formatters';
import MismatchDetail from './MismatchDetail';

const ReconciliationReport = ({ result, month, year, loading = false }) => {
  const [selectedMismatch, setSelectedMismatch] = useState(null);
  const [detailVisible, setDetailVisible] = useState(false);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!result) {
    return (
      <div className="text-center py-12">
        <ExclamationCircleOutlined className="text-4xl text-gray-300 mb-4" />
        <p className="text-gray-500">No reconciliation data available.</p>
        <p className="text-sm text-gray-400">Run reconciliation to generate report.</p>
      </div>
    );
  }

  const { 
    totalInvoices, 
    matchedCount, 
    mismatchCount, 
    itcAtRisk, 
    mismatches = [], 
    summary 
  } = result;

  const matchRate = totalInvoices > 0 ? (matchedCount / totalInvoices * 100) : 0;
  const isCompliant = mismatchCount === 0 && itcAtRisk === 0;

  const mismatchColumns = [
    {
      title: 'Status',
      dataIndex: 'type',
      key: 'type',
      render: (type) => (
        <Tag color={getMismatchTypeColor(type)}>
          {getMismatchTypeLabel(type)}
        </Tag>
      ),
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
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          onClick={() => {
            setSelectedMismatch(record);
            setDetailVisible(true);
          }}
        >
          View Details
        </Button>
      ),
    },
  ];

  const tabItems = [
    {
      key: 'summary',
      label: 'Summary',
      children: (
        <div>
          {isCompliant ? (
            <Alert
              message="✅ All invoices reconciled successfully"
              description="No mismatches found. Your GST compliance is on track."
              type="success"
              showIcon
              className="mb-6"
            />
          ) : (
            <Alert
              message={`⚠️ ${mismatchCount} mismatch(es) found`}
              description={`ITC at risk: ${formatCurrency(itcAtRisk)}. Review the mismatches below.`}
              type="warning"
              showIcon
              className="mb-6"
            />
          )}

          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Total Invoices"
                  value={totalInvoices}
                  prefix={<FileSearchOutlined />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Matched"
                  value={matchedCount}
                  valueStyle={{ color: '#52c41a' }}
                  prefix={<CheckCircleOutlined />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Mismatches"
                  value={mismatchCount}
                  valueStyle={{ color: mismatchCount > 0 ? '#ff4d4f' : '#52c41a' }}
                  prefix={<CloseCircleOutlined />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Match Rate"
                  value={matchRate}
                  suffix="%"
                  valueStyle={{ color: matchRate >= 90 ? '#52c41a' : '#faad14' }}
                />
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]} className="mt-4">
            <Col xs={24} lg={12}>
              <Card title="ITC at Risk">
                <div className="text-center py-4">
                  <div className={`text-3xl font-bold ${itcAtRisk > 0 ? 'text-red-500' : 'text-green-500'}`}>
                    {formatCurrency(itcAtRisk)}
                  </div>
                  <p className="text-gray-500 text-sm mt-2">
                    {itcAtRisk > 0 
                      ? 'ITC that may be disallowed if mismatches are not resolved' 
                      : 'No ITC at risk'}
                  </p>
                </div>
              </Card>
            </Col>
            <Col xs={24} lg={12}>
              <Card title="AI Summary">
                <div className="p-3 bg-blue-50 rounded-lg min-h-[80px]">
                  <p className="text-gray-700">{summary || 'No mismatches found. All invoices reconciled successfully.'}</p>
                </div>
              </Card>
            </Col>
          </Row>
        </div>
      ),
    },
    {
      key: 'mismatches',
      label: `Mismatches (${mismatchCount})`,
      children: (
        <Table
          columns={mismatchColumns}
          dataSource={mismatches}
          rowKey={(record, index) => `${record.invoiceNumber}-${index}`}
          pagination={{
            pageSize: 10,
            showTotal: (total) => `Total ${total} mismatches`,
          }}
          scroll={{ x: 900 }}
        />
      ),
    },
  ];

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold">
          Reconciliation Report - {month}/{year}
        </h3>
        <Space>
          <Button icon={<PrinterOutlined />} onClick={() => window.print()}>
            Print
          </Button>
          <Button type="primary" icon={<DownloadOutlined />}>
            Export Report
          </Button>
        </Space>
      </div>

      <Tabs items={tabItems} />

      <MismatchDetail
        visible={detailVisible}
        onClose={() => {
          setDetailVisible(false);
          setSelectedMismatch(null);
        }}
        mismatch={selectedMismatch}
      />
    </div>
  );
};

export default ReconciliationReport;