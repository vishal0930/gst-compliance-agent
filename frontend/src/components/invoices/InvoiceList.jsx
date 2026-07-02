import React, { useState, useEffect, useRef } from 'react';
import { Table, Button, Space, Tag, Modal, Popconfirm, message, Tooltip } from 'antd';
import { EyeOutlined, DeleteOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useInvoices } from '../../hooks/useInvoices';
import { getCurrentPeriod, formatCurrency, formatDate, getStatusBadge } from '../../utils/formatters';
import InvoiceFilters from './InvoiceFilters';

const InvoiceList = () => {
  const period = getCurrentPeriod();
  const navigate = useNavigate();
  const [filters, setFilters] = useState({
    month: period.month,
    year: period.year,
    status: null,
  });
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);
  const pollRef = useRef(null);

  const { invoices, loading, total, deleteInvoice, refetch } = useInvoices(filters);

  // Auto-refresh every 3s while any invoice is in PENDING/PROCESSING state
  const hasProcessing = invoices.some(
    (i) => i.parseStatus === 'PENDING' || i.parseStatus === 'PROCESSING'
  );
  useEffect(() => {
    if (hasProcessing) {
      pollRef.current = setInterval(() => refetch(), 3000);
    } else {
      clearInterval(pollRef.current);
    }
    return () => clearInterval(pollRef.current);
  }, [hasProcessing, refetch]);

  const getRowBadgeColor = (record, index) => {
    if (record.parseStatus === 'FAILED') return 'red';
    
    // Alternating colorful themes for each row
    const colors = ['green', 'blue', 'orange', 'purple', 'gold', 'magenta'];
    const baseColor = colors[index % colors.length];
    return baseColor;
  };

  const columns = [
    {
      title: 'Invoice #',
      dataIndex: 'invoiceNumber',
      key: 'invoiceNumber',
      fixed: 'left',
      width: 150,
      render: (val, record, index) => {
        const color = getRowBadgeColor(record, index);
        return <Tag color={color} className="font-semibold">{val}</Tag>;
      }
    },
    {
      title: 'Supplier',
      dataIndex: 'vendorName',
      key: 'vendorName',
      width: 200,
      render: (val, record, index) => {
        const color = getRowBadgeColor(record, index);
        return <Tag color={color} className="font-semibold">{val || '-'}</Tag>;
      }
    },
    {
      title: 'GSTIN',
      dataIndex: 'vendorGstin',
      key: 'vendorGstin',
      width: 150,
      render: (val, record, index) => {
        const color = getRowBadgeColor(record, index);
        return <Tag color={color} className="font-mono">{val || '-'}</Tag>;
      }
    },
    {
      title: 'Invoice Date',
      dataIndex: 'invoiceDate',
      key: 'invoiceDate',
      render: (val, record, index) => {
        const color = getRowBadgeColor(record, index);
        return <Tag color={color}>{formatDate(val)}</Tag>;
      },
      width: 120,
    },
    {
      title: 'Taxable Value',
      key: 'taxableValue',
      width: 150,
      render: (_, record, index) => {
        const tot = Number(record.totalAmount || 0);
        const gst = Number(record.totalGst || 0);
        const val = tot - gst;
        const color = val < 0 ? 'red' : getRowBadgeColor(record, index);
        return <Tag color={color} className="font-mono">{formatCurrency(val)}</Tag>;
      },
    },
    {
      title: 'Total Amount',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      render: (val, record, index) => {
        const color = val < 0 ? 'red' : getRowBadgeColor(record, index);
        return <Tag color={color} className="font-mono font-bold">{formatCurrency(val)}</Tag>;
      },
      width: 150,
    },
    {
      title: 'Total GST',
      dataIndex: 'totalGst',
      key: 'totalGst',
      render: (val, record, index) => {
        const color = val < 0 ? 'red' : getRowBadgeColor(record, index);
        return <Tag color={color} className="font-mono">{formatCurrency(val)}</Tag>;
      },
      width: 130,
    },
    {
      title: 'Confidence',
      dataIndex: 'confidenceScore',
      key: 'confidenceScore',
      width: 120,
      render: (val, record, index) => {
        if (val == null) return '-';
        const pct = Math.round(Number(val) * 100);
        let color = getRowBadgeColor(record, index);
        if (pct < 50) color = 'red';
        else if (pct < 80) color = 'orange';
        return <Tag color={color} className="font-mono">{pct}%</Tag>;
      }
    },
    {
      title: 'Needs Review',
      key: 'needsReview',
      width: 120,
      render: (_, record, index) => {
        const score = record.confidenceScore != null ? Number(record.confidenceScore) : 1.0;
        const lineNeedsReview = record.lineItems && record.lineItems.some(item => item.needsReview === true);
        const needsReview = score < 0.80 || lineNeedsReview;
        
        const color = needsReview ? 'red' : getRowBadgeColor(record, index);
        return (
          <Tag color={color} className={needsReview ? 'font-semibold' : ''}>
            {needsReview ? 'Yes' : 'No'}
          </Tag>
        );
      }
    },
    {
      title: 'Status',
      dataIndex: 'parseStatus',
      key: 'parseStatus',
      render: (status, record, index) => {
        const badge = getStatusBadge(status);
        const color = status === 'FAILED' ? 'red' : getRowBadgeColor(record, index);
        return <Tag color={color}>{badge.text}</Tag>;
      },
      width: 120,
    },
    {
      title: 'Actions',
      key: 'actions',
      fixed: 'right',
      width: 150,
      render: (_, record) => (
        <Space>
          <Tooltip title="View Details">
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => { setSelectedInvoice(record); setModalVisible(true); }}
            />
          </Tooltip>
          {(record.parseStatus === 'PENDING' || record.parseStatus === 'PROCESSING') && record.jobId && (
            <Tooltip title="Track Processing">
              <Button
                type="text"
                icon={<SyncOutlined spin />}
                className="text-[var(--yellow)]"
                onClick={() => navigate(`/processing/${record.jobId}`)}
              />
            </Tooltip>
          )}
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
        <div className="flex items-center gap-3">
          <InvoiceFilters filters={filters} onFilterChange={handleFilterChange} />
          {hasProcessing && (
            <span className="flex items-center gap-1 text-xs text-[var(--accent)] bg-[var(--accent-soft)] px-3 py-1 rounded-full animate-pulse">
              <SyncOutlined spin /> AI processing active — auto-refreshing
            </span>
          )}
        </div>
        <Button icon={<ReloadOutlined />} onClick={() => refetch()} className="border-[var(--border)] text-[var(--text-secondary)] hover:text-[var(--accent)] hover:border-[var(--accent)] bg-transparent">
          Refresh
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={invoices}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1200 }}
        className="border-[var(--border)]"
        pagination={{
          total: total,
          pageSize: 10,
          showSizeChanger: true,
          showTotal: (total) => `Total ${total} invoices`,
        }}
      />

      <Modal
        title={
          <span className="font-semibold text-base">
            Invoice — {selectedInvoice?.invoiceNumber}
          </span>
        }
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={[
          <Button key="detail" type="primary" onClick={() => {
            setModalVisible(false);
            navigate(`/invoices/${selectedInvoice?.id}`);
          }}>
            Full Details
          </Button>,
        ]}
        width={900}
        destroyOnClose
      >
        {selectedInvoice && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
              
              {/* Left Side: Supplier & Invoice Metadata */}
              <div className="md:col-span-7 space-y-4 p-5 rounded-2xl border border-[var(--border)] bg-[var(--bg-input)]/40">
                <h3 className="text-xs uppercase tracking-wider text-[var(--text-muted)] font-bold mb-2">Invoice Metadata</h3>
                
                <div className="grid grid-cols-2 gap-y-4 gap-x-6 text-sm">
                  <div className="col-span-2">
                    <span className="text-[var(--text-muted)] text-xs block mb-1">Supplier</span>
                    <span className="font-semibold text-[var(--text-primary)] text-base">{selectedInvoice.vendorName || '-'}</span>
                  </div>
                  
                  <div className="col-span-2">
                    <span className="text-[var(--text-muted)] text-xs block mb-1">Supplier GSTIN</span>
                    <span className="font-mono font-semibold px-2 py-0.5 bg-[var(--accent-soft)] text-[var(--accent)] rounded-lg text-xs border border-[var(--accent-soft)] inline-block">
                      {selectedInvoice.vendorGstin || '-'}
                    </span>
                  </div>
                  
                  <div>
                    <span className="text-[var(--text-muted)] text-xs block mb-1">Invoice Number</span>
                    <span className="font-mono font-semibold text-[var(--text-primary)]">{selectedInvoice.invoiceNumber || '-'}</span>
                  </div>
                  
                  <div>
                    <span className="text-[var(--text-muted)] text-xs block mb-1">Invoice Date</span>
                    <span className="font-semibold text-[var(--text-primary)]">{formatDate(selectedInvoice.invoiceDate)}</span>
                  </div>
                  
                  <div>
                    <span className="text-[var(--text-muted)] text-xs block mb-1">Extraction Status</span>
                    <div className="mt-1">
                      <Tag color={getStatusBadge(selectedInvoice.parseStatus).color}>
                        {getStatusBadge(selectedInvoice.parseStatus).text}
                      </Tag>
                    </div>
                  </div>
                </div>
              </div>

              {/* Right Side: Financial Receipt Summary */}
              <div className="md:col-span-5 p-5 rounded-2xl border border-[var(--border)] bg-[var(--bg-input)] flex flex-col justify-between">
                <div>
                  <h3 className="text-xs uppercase tracking-wider text-[var(--text-muted)] font-bold mb-4">Financial Summary</h3>
                  
                  <div className="space-y-2.5 text-sm">
                    <div className="flex justify-between items-center">
                      <span className="text-[var(--text-secondary)]">Taxable Value</span>
                      <span className="font-semibold text-[var(--text-primary)]">{formatCurrency(selectedInvoice.taxableValue)}</span>
                    </div>
                    
                    {selectedInvoice.cgstAmount > 0 && (
                      <div className="flex justify-between items-center text-xs text-[var(--text-muted)]">
                        <span>CGST</span>
                        <span>{formatCurrency(selectedInvoice.cgstAmount)}</span>
                      </div>
                    )}
                    
                    {selectedInvoice.sgstAmount > 0 && (
                      <div className="flex justify-between items-center text-xs text-[var(--text-muted)]">
                        <span>SGST</span>
                        <span>{formatCurrency(selectedInvoice.sgstAmount)}</span>
                      </div>
                    )}
                    
                    {selectedInvoice.igstAmount > 0 && (
                      <div className="flex justify-between items-center text-xs text-[var(--text-muted)]">
                        <span>IGST</span>
                        <span>{formatCurrency(selectedInvoice.igstAmount)}</span>
                      </div>
                    )}
                    
                    <div className="flex justify-between items-center text-xs">
                      <span className="text-[var(--text-secondary)]">Total GST</span>
                      <span className="font-semibold text-[var(--text-primary)]">{formatCurrency(selectedInvoice.totalGst)}</span>
                    </div>
                  </div>
                </div>

                <div>
                  <div className="h-px bg-[var(--border)] my-4" />

                  <div className="flex justify-between items-end">
                    <span className="text-[var(--text-secondary)] font-bold text-xs uppercase tracking-wider">Total Amount</span>
                    <span className="text-2xl font-extrabold text-[var(--accent)] font-mono">
                      {formatCurrency(selectedInvoice.totalAmount)}
                    </span>
                  </div>
                </div>
              </div>

            </div>

            {/* Bottom Section: Line Items */}
            <div>
              <p className="font-semibold text-[var(--text-primary)] mb-3 flex items-center gap-2 text-base">
                Line Items
                <Tag color="purple" className="ml-2 font-mono">{(selectedInvoice.lineItems || []).length}</Tag>
              </p>
              <Table
                columns={[
                  { title: 'Description', dataIndex: 'description', key: 'description', ellipsis: true },
                  { 
                    title: 'HSN', 
                    dataIndex: 'hsnCode', 
                    key: 'hsnCode', 
                    width: 90,
                    render: (v) => v ? <span className="font-mono font-semibold text-blue-600">{v}</span> : <span className="text-slate-400">-</span>
                  },
                  {
                    title: 'HSN Conf.',
                    dataIndex: 'hsnConfidence',
                    key: 'hsnConfidence',
                    width: 95,
                    align: 'center',
                    render: (v) => v != null ? (
                      <Tag color={Number(v) >= 0.8 ? 'green' : 'orange'} className="font-mono">
                        {(Number(v) * 100).toFixed(0)}%
                      </Tag>
                    ) : '-',
                  },
                  { title: 'Qty', dataIndex: 'quantity', key: 'quantity', width: 70, align: 'right' },
                  { title: 'Unit Price', dataIndex: 'unitPrice', key: 'unitPrice', width: 110, align: 'right', render: (v) => formatCurrency(v) },
                  { 
                    title: 'GST %', 
                    dataIndex: 'gstRate', 
                    key: 'gstRate', 
                    width: 75, 
                    align: 'center', 
                    render: (v) => v != null ? <span className="font-semibold text-purple-600">{v}%</span> : <span className="text-slate-400">-</span>
                  },
                  { title: 'Taxable', dataIndex: 'taxableValue', key: 'taxableValue', width: 120, align: 'right', render: (v) => formatCurrency(v) },
                  { title: 'CGST', dataIndex: 'cgstAmount', key: 'cgstAmount', width: 110, align: 'right', render: (v) => formatCurrency(v || 0) },
                  { title: 'SGST', dataIndex: 'sgstAmount', key: 'sgstAmount', width: 110, align: 'right', render: (v) => formatCurrency(v || 0) },
                  { title: 'IGST', dataIndex: 'igstAmount', key: 'igstAmount', width: 110, align: 'right', render: (v) => formatCurrency(v || 0) },
                  {
                    title: 'Flag', dataIndex: 'needsReview', key: 'needsReview', width: 70, align: 'center',
                    render: (v) => v ? <Tag color="orange">⚠</Tag> : <Tag color="green">✓</Tag>,
                  },
                ]}
                dataSource={selectedInvoice.lineItems || []}
                rowKey={(r, i) => r.id || i}
                pagination={false}
                size="small"
                scroll={{ x: 950 }}
                locale={{ emptyText: selectedInvoice.parseStatus === 'DONE' ? 'No line items extracted' : 'Processing...' }}
              />
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default InvoiceList;