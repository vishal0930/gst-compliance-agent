import React from 'react';
import { Modal, Descriptions, Tag, Alert, Space, Button } from 'antd';
import { 
  CheckCircleOutlined, 
  CloseCircleOutlined, 
  WarningOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import { formatCurrency, formatDate, getMismatchTypeLabel, getMismatchTypeColor } from '../../utils/formatters';

const MismatchDetail = ({ visible, onClose, mismatch }) => {
  if (!mismatch) return null;

  const getRiskLevel = (riskAmount) => {
    if (!riskAmount || riskAmount === 0) return { level: 'low', color: 'green', label: 'Low' };
    if (riskAmount < 1000) return { level: 'low', color: 'green', label: 'Low' };
    if (riskAmount < 5000) return { level: 'medium', color: 'orange', label: 'Medium' };
    return { level: 'high', color: 'red', label: 'High' };
  };

  const risk = getRiskLevel(mismatch.riskAmount);

  return (
    <Modal
      title={
        <Space>
          <Tag color={getMismatchTypeColor(mismatch.type)} className="text-base px-3 py-1">
            {getMismatchTypeLabel(mismatch.type)}
          </Tag>
          <span className="text-gray-600">- Invoice {mismatch.invoiceNumber}</span>
        </Space>
      }
      open={visible}
      onCancel={onClose}
      footer={[
        <Button key="close" onClick={onClose}>
          Close
        </Button>,
        <Button key="action" type="primary" icon={<FileTextOutlined />}>
          View Related Invoice
        </Button>,
      ]}
      width={800}
      destroyOnClose
    >
      {mismatch.riskAmount > 0 && (
        <Alert
          message={
            <div className="flex items-center justify-between">
              <span className="font-medium">ITC at Risk: {formatCurrency(mismatch.riskAmount)}</span>
              <Tag color={risk.color} className="text-sm">
                {risk.level.toUpperCase()} Risk
              </Tag>
            </div>
          }
          type={risk.level === 'high' ? 'error' : risk.level === 'medium' ? 'warning' : 'success'}
          showIcon
          className="mb-6"
        />
      )}

      <Descriptions bordered column={2} className="mb-4">
        <Descriptions.Item label="Status" span={1}>
          <Tag color={getMismatchTypeColor(mismatch.type)} className="text-sm">
            {getMismatchTypeLabel(mismatch.type)}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Invoice Number" span={1}>
          <strong>{mismatch.invoiceNumber}</strong>
        </Descriptions.Item>
        <Descriptions.Item label="Supplier GSTIN" span={2}>
          <Tag color="blue">{mismatch.supplierGstin}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Book Amount" span={1}>
          <span className="font-medium">{formatCurrency(mismatch.bookAmount)}</span>
        </Descriptions.Item>
        <Descriptions.Item label="Portal Amount" span={1}>
          <span className="font-medium">{formatCurrency(mismatch.portalAmount)}</span>
        </Descriptions.Item>
        {mismatch.diffAmount && (
          <Descriptions.Item label="Difference" span={1}>
            <span className={mismatch.diffAmount > 0 ? 'text-red-600' : 'text-green-600'}>
              {formatCurrency(mismatch.diffAmount)}
            </span>
          </Descriptions.Item>
        )}
        {mismatch.diffPercent && (
          <Descriptions.Item label="Diff %" span={1}>
            <span className={mismatch.diffPercent > 5 ? 'text-red-600' : 'text-yellow-600'}>
              {mismatch.diffPercent.toFixed(2)}%
            </span>
          </Descriptions.Item>
        )}
        <Descriptions.Item label="Book Date" span={1}>
          {formatDate(mismatch.bookInvoiceDate)}
        </Descriptions.Item>
        <Descriptions.Item label="Portal Date" span={1}>
          {formatDate(mismatch.portalInvoiceDate)}
        </Descriptions.Item>
        {mismatch.expectedGst && (
          <Descriptions.Item label="Expected GST" span={1}>
            {formatCurrency(mismatch.expectedGst)}
          </Descriptions.Item>
        )}
        {mismatch.actualGst && (
          <Descriptions.Item label="Actual GST" span={1}>
            {formatCurrency(mismatch.actualGst)}
          </Descriptions.Item>
        )}
        {mismatch.lineItemCount && (
          <Descriptions.Item label="Line Items" span={2}>
            {mismatch.lineItemCount} items
          </Descriptions.Item>
        )}
      </Descriptions>

      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
        <p className="text-blue-800 font-medium">📝 Description</p>
        <p className="text-blue-700 mt-1">{mismatch.description}</p>
      </div>

      <div className="bg-green-50 border border-green-200 rounded-lg p-4">
        <p className="text-green-800 font-medium">💡 Recommendation</p>
        <p className="text-green-700 mt-1">{mismatch.recommendation}</p>
      </div>
    </Modal>
  );
};

export default MismatchDetail;