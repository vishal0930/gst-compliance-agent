import React, { useState } from 'react';
import { Card, Select, Spin, Statistic, Row, Col, Button, Space, Divider, List, Tag } from 'antd';
import { 
  CheckCircleOutlined, 
  CloseCircleOutlined, 
  WarningOutlined,
  FileTextOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import { useReconciliation } from '../hooks/useReconciliation';
import { getCurrentPeriod, getMonthOptions, getYearOptions, formatCurrency } from '../utils/formatters';

const Compliance = () => {
  const period = getCurrentPeriod();
  const [month, setMonth] = useState(period.month);
  const [year, setYear] = useState(period.year);
  const { useComplianceBrief } = useReconciliation();
  const { data, isLoading, error } = useComplianceBrief(month, year);

  const monthOptions = getMonthOptions();
  const yearOptions = getYearOptions();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spin size="large" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <WarningOutlined className="text-4xl text-yellow-500 mb-4" />
        <p className="text-gray-500">No compliance data available for this period.</p>
        <p className="text-sm text-gray-400">Run reconciliation first to generate compliance brief.</p>
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Compliance Brief</h1>
        <Space>
          <Select
            value={month}
            onChange={setMonth}
            options={monthOptions}
            style={{ width: 150 }}
          />
          <Select
            value={year}
            onChange={setYear}
            options={yearOptions}
            style={{ width: 120 }}
          />
          <Button icon={<DownloadOutlined />}>Export PDF</Button>
        </Space>
      </div>

      {data && (
        <>
          <Row gutter={[16, 16]} className="mb-6">
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Total Sales"
                  value={formatCurrency(data.totalSales)}
                  prefix={<FileTextOutlined />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Total GST"
                  value={formatCurrency(data.totalGst)}
                  prefix={<FileTextOutlined />}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Total ITC"
                  value={formatCurrency(data.totalItc)}
                  prefix={<CheckCircleOutlined />}
                  valueStyle={{ color: '#52c41a' }}
                />
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic
                  title="Tax Liability"
                  value={formatCurrency(data.taxLiability)}
                  prefix={data.taxLiability > 0 ? <CloseCircleOutlined /> : <CheckCircleOutlined />}
                  valueStyle={{ color: data.taxLiability > 0 ? '#ff4d4f' : '#52c41a' }}
                />
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={16}>
              <Card title="Compliance Summary" className="h-full">
                <div className="prose max-w-none">
                  <p className="text-gray-700 whitespace-pre-wrap">{data.brief}</p>
                </div>
              </Card>
            </Col>
            <Col xs={24} lg={8}>
              <Card title="Action Items" className="h-full">
                {data.actionItems && data.actionItems.length > 0 ? (
                  <List
                    dataSource={data.actionItems}
                    renderItem={(item, index) => (
                      <List.Item>
                        <div className="flex items-start">
                          <Tag color={index === 0 ? 'red' : index === 1 ? 'orange' : 'blue'} className="mt-0.5">
                            {index + 1}
                          </Tag>
                          <span className="text-gray-700">{item}</span>
                        </div>
                      </List.Item>
                    )}
                  />
                ) : (
                  <div className="text-center py-8 text-gray-400">
                    <CheckCircleOutlined className="text-3xl text-green-500" />
                    <p className="mt-2">No action items. All compliance requirements are met.</p>
                  </div>
                )}
              </Card>
            </Col>
          </Row>

          <Divider />

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <h4 className="font-medium text-blue-800 mb-2">Compliance Status Summary</h4>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <span className="text-gray-500">ITC at Risk</span>
                <p className="font-semibold text-red-600">{formatCurrency(data.itcAtRisk)}</p>
              </div>
              <div>
                <span className="text-gray-500">Period</span>
                <p className="font-semibold">{month}/{year}</p>
              </div>
              <div>
                <span className="text-gray-500">Status</span>
                <p className="font-semibold">
                  {data.itcAtRisk > 0 ? (
                    <Tag color="orange">Needs Review</Tag>
                  ) : (
                    <Tag color="green">Compliant</Tag>
                  )}
                </p>
              </div>
              <div>
                <span className="text-gray-500">Generated</span>
                <p className="font-semibold">{new Date().toLocaleDateString()}</p>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default Compliance;