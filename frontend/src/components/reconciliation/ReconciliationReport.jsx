import React, { useState } from 'react';
import { Alert, Button, Card, Col, Row, Space, Spin, Statistic, Table, Tabs, Tag } from 'antd';
import {
  CheckCircleOutlined, CloseCircleOutlined,
  DownloadOutlined, ExclamationCircleOutlined,
  FileSearchOutlined, PrinterOutlined,
} from '@ant-design/icons';
import { message } from 'antd';
import { formatCurrency, getMismatchTypeLabel, getMismatchTypeColor } from '../../utils/formatters';
import { reconciliationApi } from '../../api/reconciliation';
import MismatchDetail from './MismatchDetail';

const ReconciliationReport = ({ result, recordId, month, year, loading = false }) => {
  const [selectedMismatch, setSelectedMismatch] = useState(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [exporting, setExporting] = useState(false);

  /* ── Export handler — calls real API ───────────────────────── */
  const handleExport = async () => {
    if (!recordId) {
      message.warning('Save the reconciliation first before exporting.');
      return;
    }
    try {
      setExporting(true);
      const response = await reconciliationApi.exportReconciliation(recordId);
      const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `reconciliation-${String(month).padStart(2, '0')}-${year}.xlsx`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch {
      message.error('Export failed. Please try again.');
    } finally {
      setExporting(false);
    }
  };

  /* ── States ─────────────────────────────────────────────────── */
  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 200 }}>
        <Spin size="large" tip="Running reconciliation…" />
      </div>
    );
  }

  if (!result) {
    return (
      <div style={{ textAlign: 'center', padding: '48px 0' }}>
        <ExclamationCircleOutlined style={{ fontSize: 40, color: 'var(--text-muted)' }} />
        <p style={{ color: 'var(--text-muted)', marginTop: 12 }}>No reconciliation data available.</p>
        <p style={{ color: 'var(--text-muted)', fontSize: 12 }}>Run reconciliation to generate a report.</p>
      </div>
    );
  }

  const {
    totalInvoices = 0,
    matchedCount = 0,
    mismatchCount = 0,
    itcAtRisk = 0,
    mismatches = [],
    summary = '',
  } = result;

  const matchRate = totalInvoices > 0 ? ((matchedCount / totalInvoices) * 100).toFixed(1) : '0.0';
  const isCompliant = mismatchCount === 0 && Number(itcAtRisk) === 0;

  /* ── Mismatch table columns ──────────────────────────────────── */
  const mismatchColumns = [
    {
      title: 'Type', dataIndex: 'status', key: 'status', width: 160,
      render: (s) => <Tag color={getMismatchTypeColor(s)} style={{ fontSize: 11 }}>{getMismatchTypeLabel(s)}</Tag>,
      filters: [...new Set(mismatches.map((m) => m.status))].map((s) => ({ text: getMismatchTypeLabel(s), value: s })),
      onFilter: (v, r) => r.status === v,
    },
    {
      title: 'Invoice #', dataIndex: 'invoiceNumber', key: 'invoiceNumber', width: 140,
      render: (v) => <span style={{ fontFamily: 'monospace', fontWeight: 600, fontSize: 12 }}>{v}</span>
    },
    {
      title: 'Supplier GSTIN', dataIndex: 'supplierGstin', key: 'supplierGstin', width: 150,
      render: (v) => <Tag color="blue" style={{ fontFamily: 'monospace', fontSize: 11 }}>{v}</Tag>
    },
    {
      title: 'Book Amount', dataIndex: 'bookAmount', key: 'bookAmount', align: 'right', width: 130,
      render: (v) => v != null ? formatCurrency(v) : '—'
    },
    {
      title: 'Portal Amount', dataIndex: 'portalAmount', key: 'portalAmount', align: 'right', width: 130,
      render: (v) => v != null ? formatCurrency(v) : '—'
    },
    {
      title: 'Risk Amount', dataIndex: 'riskAmount', key: 'riskAmount', align: 'right', width: 120,
      render: (v) => v != null ? <span style={{ color: '#ef4444', fontWeight: 700 }}>{formatCurrency(v)}</span> : '—'
    },
    {
      title: '', key: 'actions', width: 100,
      render: (_, record) => (
        <Button type="link" size="small"
          onClick={() => { setSelectedMismatch(record); setDetailVisible(true); }}>
          Details
        </Button>
      ),
    },
  ];

  /* ── Tabs ────────────────────────────────────────────────────── */
  const tabItems = [
    {
      key: 'summary',
      label: 'Summary',
      children: (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {isCompliant ? (
            <Alert type="success" showIcon
              message="All invoices reconciled successfully"
              description="No mismatches found. Your GST compliance is on track." />
          ) : (
            <Alert type="warning" showIcon
              message={`${mismatchCount} mismatch(es) found`}
              description={`ITC at risk: ${formatCurrency(itcAtRisk)}. Review and resolve before filing.`} />
          )}

          <Row gutter={[14, 14]}>
            {[
              { title: 'Total Invoices', value: totalInvoices, prefix: <FileSearchOutlined />, valueColor: 'var(--text-primary)' },
              { title: 'Matched', value: matchedCount, prefix: <CheckCircleOutlined />, valueColor: '#22c55e' },
              { title: 'Mismatches', value: mismatchCount, prefix: <CloseCircleOutlined />, valueColor: mismatchCount > 0 ? '#ef4444' : '#22c55e' },
              { title: 'Match Rate', value: matchRate, suffix: '%', valueColor: Number(matchRate) >= 90 ? '#22c55e' : '#f59e0b' },
            ].map(({ title, value, prefix, suffix, valueColor }) => (
              <Col key={title} xs={12} sm={6}>
                <Card size="small" style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}>
                  <Statistic title={title} value={value} prefix={prefix} suffix={suffix}
                    valueStyle={{ color: valueColor, fontSize: 22, fontWeight: 800 }} />
                </Card>
              </Col>
            ))}
          </Row>

          <Row gutter={[14, 14]}>
            <Col xs={24} lg={12}>
              <Card title={<span style={{ color: 'var(--text-primary)', fontWeight: 700 }}>ITC at Risk</span>}
                style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}>
                <div style={{ textAlign: 'center', padding: '16px 0' }}>
                  <p style={{ margin: 0, fontSize: 32, fontWeight: 800, color: Number(itcAtRisk) > 0 ? '#ef4444' : '#22c55e' }}>
                    {formatCurrency(itcAtRisk)}
                  </p>
                  <p style={{ margin: '8px 0 0', fontSize: 12, color: 'var(--text-muted)' }}>
                    {Number(itcAtRisk) > 0
                      ? 'ITC that may be disallowed if mismatches are not resolved'
                      : 'No ITC at risk — all invoices matched'}
                  </p>
                </div>
              </Card>
            </Col>
            <Col xs={24} lg={12}>
              <Card title={<span style={{ color: 'var(--text-primary)', fontWeight: 700 }}>AI Summary</span>}
                style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}>
                <div style={{
                  background: 'var(--bg-input)', borderRadius: 8, padding: 14,
                  border: '1px solid var(--border)', minHeight: 80,
                  fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.6,
                }}>
                  {summary || 'Reconciliation complete. No summary available.'}
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
          rowKey={(r, i) => `${r.invoiceNumber}-${i}`}
          size="small"
          pagination={{ pageSize: 25, showSizeChanger: true, pageSizeOptions: [25, 50, 100], showTotal: (t) => `${t} mismatches` }}
          scroll={{ x: 950 }}
        />
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Header row */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: 'var(--text-primary)' }}>
          Reconciliation Report — {String(month).padStart(2, '0')}/{year}
        </h3>
        <Space>
          <Button icon={<PrinterOutlined />} onClick={() => window.print()}>
            Print
          </Button>
          <Button type="primary" icon={<DownloadOutlined />}
            loading={exporting} onClick={handleExport}>
            Export Excel
          </Button>
        </Space>
      </div>

      <Tabs items={tabItems} />

      <MismatchDetail
        visible={detailVisible}
        onClose={() => { setDetailVisible(false); setSelectedMismatch(null); }}
        mismatch={selectedMismatch}
      />
    </div>
  );
};

export default ReconciliationReport;
