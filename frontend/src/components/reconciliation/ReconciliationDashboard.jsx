import React, { useState } from 'react';
import { Card, Row, Col, Statistic, Tabs, Table, Tag, Space, Button, Modal, Descriptions } from 'antd';
import { 
  CheckCircleOutlined, 
  CloseCircleOutlined, 
  ExclamationCircleOutlined,
  FileSearchOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import { formatCurrency, getMismatchTypeLabel, getMismatchTypeColor, formatDate } from '../../utils/formatters';
import MismatchList from './MismatchList';

const ReconciliationReport = ({ result, month, year }) => {
  const [selectedMismatch, setSelectedMismatch] = useState(null);
  const [detailVisible, setDetailVisible] = useState(false);

  const { 
    totalInvoices, 
    matchedCount, 
    mismatchCount, 
    itcAtRisk, 
    mismatches = [],
    summary 
  } = result;

  const matchRate = totalInvoices > 0 ? (matchedCount / totalInvoices * 100) : 0;

  const tabItems = [
    {
      key: 'summary',
      label: 'Summary',
      children: (
        <div>
          <Row gutter={[16, 16]} className="mb-6">
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

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={12}>
              <Card title="ITC at Risk" className="h-full">
                <div className="text-center py-8">
                  <div className="text-4xl font-bold text-red-500">
                    {formatCurrency(itcAtRisk)}
                  </div>
                  <p className="text-gray-500 mt-2">
                    {itcAtRisk > 0 ? 'ITC that may be disallowed if mismatches are not resolved' : 'No ITC at risk'}
                  </p>
                </div>
              </Card>
            </Col>
            <Col xs={24} lg={12}>
              <Card title="AI Summary" className="h-full">
                <div className="p-4 bg-blue-50 rounded-lg">
                  <p className="text-gray-700">
                    {summary || 'No mismatches found. All invoices reconciled successfully.'}
                  </p>
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
        <MismatchList 
          mismatches={mismatches} 
          onViewDetail={(mismatch) => {
            setSelectedMismatch(mismatch);
            setDetailVisible(true);
          }}
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
          <Button icon={<DownloadOutlined />}>Export PDF</Button>
          <Button icon={<DownloadOutlined />}>Export Excel</Button>
        </Space>
      </div>

      <Tabs items={tabItems} />

      <Modal
        title="Mismatch Details"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={700}
      >
        {selectedMismatch && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="Status">
              <Tag color={getMismatchTypeColor(selectedMismatch.type)}>
                {getMismatchTypeLabel(selectedMismatch.type)}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Invoice Number">
              {selectedMismatch.invoiceNumber}
            </Descriptions.Item>
            <Descriptions.Item label="Supplier GSTIN" span={2}>
              {selectedMismatch.supplierGstin}
            </Descriptions.Item>
            <Descriptions.Item label="Book Amount">
              {formatCurrency(selectedMismatch.bookAmount)}
            </Descriptions.Item>
            <Descriptions.Item label="Portal Amount">
              {formatCurrency(selectedMismatch.portalAmount)}
            </Descriptions.Item>
            {selectedMismatch.diffAmount && (
              <Descriptions.Item label="Difference">
                {formatCurrency(selectedMismatch.diffAmount)}
              </Descriptions.Item>
            )}
            {selectedMismatch.diffPercent && (
              <Descriptions.Item label="Diff %">
                {selectedMismatch.diffPercent.toFixed(2)}%
              </Descriptions.Item>
            )}
            <Descriptions.Item label="Book Date" span={1}>
              {formatDate(selectedMismatch.bookInvoiceDate)}
            </Descriptions.Item>
            <Descriptions.Item label="Portal Date" span={1}>
              {formatDate(selectedMismatch.portalInvoiceDate)}
            </Descriptions.Item>
            {selectedMismatch.expectedGst && (
              <Descriptions.Item label="Expected GST">
                {formatCurrency(selectedMismatch.expectedGst)}
              </Descriptions.Item>
            )}
            {selectedMismatch.actualGst && (
              <Descriptions.Item label="Actual GST">
                {formatCurrency(selectedMismatch.actualGst)}
              </Descriptions.Item>
            )}
            <Descriptions.Item label="Risk Amount" span={2}>
              <span className="text-red-500 font-bold">
                {formatCurrency(selectedMismatch.riskAmount)}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="Description" span={2}>
              {selectedMismatch.description}
            </Descriptions.Item>
            <Descriptions.Item label="Recommendation" span={2}>
              <span className="text-blue-600">{selectedMismatch.recommendation}</span>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default ReconciliationReport;