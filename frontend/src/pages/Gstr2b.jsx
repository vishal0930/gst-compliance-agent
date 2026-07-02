import React, { useState } from 'react';
import {
  Alert, Badge, Button, Card, Col, Divider, Modal,
  Row, Select, Space, Spin, Statistic, Table, Tag, Tooltip, Input,
} from 'antd';
import {
  FileSpreadsheet, Upload, RefreshCw, Eye,
  Building2, TrendingUp, CheckCircle2, AlertTriangle,
  Package, Calendar, IndianRupee, ShieldCheck,
} from 'lucide-react';
import { useGstr2b } from '../hooks/useGstr2b';
import useUiStore from '../store/uiStore';
import Gstr2bImport from '../components/gstr2b/Gstr2bImport';
import Gstr2bDetail from '../components/gstr2b/Gstr2bDetail';
import { formatCurrency, formatDate, getMonthOptions, getYearOptions } from '../utils/formatters';

// ── Status tag a─────────────────────────────────────────────────────────────
const statusMeta = {
  IMPORTED: { color: 'blue', label: 'Imported' },
  MATCHED: { color: 'green', label: 'Matched' },
  MISMATCH: { color: 'orange', label: 'Mismatch' },
  PENDING: { color: 'default', label: 'Pending' },
  DUPLICATE: { color: 'red', label: 'Duplicate' },
  ITC_BLOCKED: { color: 'volcano', label: 'ITC Blocked' },
};

const StatusTag = ({ status }) => {
  const m = statusMeta[status] || { color: 'default', label: status || '—' };
  return <Tag color={m.color} style={{ fontSize: 11 }}>{m.label}</Tag>;
};

// ── KPI Card ───────────────────────────────────────────────────────────────
const KpiCard = ({ title, value, icon: Icon, color, suffix }) => (
  <Card
    size="small"
    style={{ borderTop: `3px solid ${color}`, background: 'var(--bg-card)', borderColor: 'var(--border)' }}
  >
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
      <div>
        <p style={{
          margin: 0, fontSize: 10, fontWeight: 700, textTransform: 'uppercase',
          letterSpacing: '0.07em', color: 'var(--text-muted)', marginBottom: 6
        }}>
          {title}
        </p>
        <p style={{ margin: 0, fontSize: 20, fontWeight: 800, color: 'var(--text-primary)', lineHeight: 1 }}>
          {value}
        </p>
        {suffix && <p style={{ margin: '2px 0 0', fontSize: 10, color: 'var(--text-muted)' }}>{suffix}</p>}
      </div>
      <div style={{ background: color + '18', borderRadius: 8, padding: 8 }}>
        <Icon size={18} style={{ color }} />
      </div>
    </div>
  </Card>
);

// ── Import History Row ─────────────────────────────────────────────────────
const HistoryRow = ({ item }) => (
  <div style={{
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '10px 0', borderBottom: '1px solid var(--border)',
  }}>
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <div style={{ background: 'var(--accent-soft)', borderRadius: 8, padding: 8 }}>
        <FileSpreadsheet size={16} style={{ color: 'var(--accent)' }} />
      </div>
      <div>
        <p style={{ margin: 0, fontWeight: 600, fontSize: 13, color: 'var(--text-primary)' }}>
          {item.taxPeriod}
        </p>
        <p style={{ margin: 0, fontSize: 11, color: 'var(--text-muted)' }}>
          {formatDate(item.importedAt)} · {item.totalInvoices} invoices
          {item.failed > 0 && <span style={{ color: '#ef4444', marginLeft: 6 }}>({item.failed} failed)</span>}
        </p>
      </div>
    </div>
    <div style={{ textAlign: 'right' }}>
      <p style={{ margin: 0, fontWeight: 700, fontSize: 13, color: '#22c55e' }}>
        {formatCurrency(item.totalItc)}
      </p>
      <p style={{ margin: 0, fontSize: 10, color: 'var(--text-muted)' }}>Eligible ITC</p>
    </div>
  </div>
);

// ── Main page ──────────────────────────────────────────────────────────────
const Gstr2b = () => {
  const { gstPeriod, setGstPeriod } = useUiStore();
  const [showImport, setShowImport] = useState(false);
  const [detail, setDetail] = useState(null);
  const [search, setSearch] = useState('');
  const [pageSize, setPageSize] = useState(25);

  const { useInvoices, useSummary, useImportHistory, uploadGstr2b, uploading } = useGstr2b();

  // Define periodLabel early — used throughout the page
  const periodLabel = `${String(gstPeriod.month).padStart(2, '0')}-${gstPeriod.year}`;

  const { data: invoices, isLoading: invLoading, refetch } = useInvoices({
    month: gstPeriod.month,
    year: gstPeriod.year,
  });

  const { data: summary, isLoading: sumLoading } = useSummary(
    gstPeriod.month, gstPeriod.year
  );

  // Real import history from /gstr2b/import-history
  const { data: historyData, refetch: refetchHistory } = useImportHistory({ size: 10 });
  const importHistory = historyData || [];

  // Normalise invoice response shape (Page<> or plain array)
  const invoiceList = Array.isArray(invoices)
    ? invoices
    : invoices?.content || [];

  // Client-side search filter
  const filtered = invoiceList.filter((inv) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      inv.invoiceNumber?.toLowerCase().includes(q) ||
      inv.supplierName?.toLowerCase().includes(q) ||
      inv.supplierGstin?.toLowerCase().includes(q)
    );
  });

  // Table columns
  const columns = [
    {
      title: 'Invoice No', dataIndex: 'invoiceNumber', key: 'invoiceNumber', width: 140,
      fixed: 'left',
      render: (v) => <span style={{ fontFamily: 'monospace', fontWeight: 600, fontSize: 12 }}>{v}</span>,
    },
    {
      title: 'Invoice Date', dataIndex: 'invoiceDate', key: 'invoiceDate', width: 110,
      render: (v) => formatDate(v),
    },
    {
      title: 'Supplier GSTIN', dataIndex: 'supplierGstin', key: 'supplierGstin', width: 155,
      render: (v) => <Tag color="blue" style={{ fontFamily: 'monospace', fontSize: 11 }}>{v}</Tag>,
    },
    { title: 'Supplier Name', dataIndex: 'supplierName', key: 'supplierName', ellipsis: true },
    {
      title: 'Taxable Value', dataIndex: 'taxableValue', key: 'taxableValue', width: 130, align: 'right',
      render: (v) => <span style={{ fontWeight: 600 }}>{formatCurrency(v)}</span>,
      sorter: (a, b) => (a.taxableValue || 0) - (b.taxableValue || 0),
    },
    {
      title: 'CGST', dataIndex: 'cgst', key: 'cgst', width: 110, align: 'right',
      render: (v) => formatCurrency(v || 0),
    },
    {
      title: 'SGST', dataIndex: 'sgst', key: 'sgst', width: 110, align: 'right',
      render: (v) => formatCurrency(v || 0),
    },
    {
      title: 'IGST', dataIndex: 'igst', key: 'igst', width: 110, align: 'right',
      render: (v) => formatCurrency(v || 0),
    },
    {
      title: 'Invoice Value', dataIndex: 'grandTotal', key: 'grandTotal', width: 130, align: 'right',
      render: (v) => <span style={{ fontWeight: 700, color: 'var(--accent)' }}>{formatCurrency(v)}</span>,
      sorter: (a, b) => (a.grandTotal || 0) - (b.grandTotal || 0),
    },
    {
      title: 'Status', key: 'status', width: 110,
      render: (_, r) => <StatusTag status={r.importStatus || 'IMPORTED'} />,
    },
    {
      title: '', key: 'action', width: 50, fixed: 'right',
      render: (_, r) => (
        <Tooltip title="View details">
          <Button type="text" size="small" icon={<Eye size={14} />}
            onClick={() => setDetail(r)} />
        </Tooltip>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* ── Page header ─────────────────────────────────────────────── */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ background: 'var(--accent-soft)', borderRadius: 10, padding: 10 }}>
            <FileSpreadsheet size={20} style={{ color: 'var(--accent)' }} />
          </div>
          <div>
            <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: 'var(--text-primary)' }}>
              GSTR-2B Management
            </h1>
            <p style={{ margin: 0, fontSize: 12, color: 'var(--text-muted)' }}>
              Purchase register from the GST portal
            </p>
          </div>
          <span className="period-chip">{periodLabel}</span>
        </div>

        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <Button icon={<RefreshCw size={14} />} onClick={() => refetch()}>
            Refresh
          </Button>
          <Button type="primary" icon={<Upload size={14} />}
            onClick={() => setShowImport(true)}>
            Import GSTR-2B
          </Button>
        </div>
      </div>

      {/* ── KPI cards ───────────────────────────────────────────────── */}
      {sumLoading ? (
        <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>
      ) : (
        <Row gutter={[14, 14]}>
          <Col xs={12} sm={8} md={6} lg={4}>
            <KpiCard title="Invoices" value={summary?.invoiceCount || 0}
              icon={Package} color="#6366f1" />
          </Col>
          <Col xs={12} sm={8} md={6} lg={4}>
            <KpiCard title="Suppliers" value={summary?.supplierCount || '—'}
              icon={Building2} color="#8b5cf6" />
          </Col>
          <Col xs={12} sm={8} md={6} lg={4}>
            <KpiCard title="Taxable Value" value={formatCurrency(summary?.taxableValue || 0)}
              icon={IndianRupee} color="#06b6d4" />
          </Col>
          <Col xs={12} sm={8} md={6} lg={4}>
            <KpiCard title="Eligible ITC" value={formatCurrency(summary?.totalItc || 0)}
              icon={CheckCircle2} color="#22c55e" />
          </Col>
          <Col xs={12} sm={8} md={6} lg={4}>
            <KpiCard title="CGST" value={formatCurrency(summary?.cgst || 0)}
              icon={TrendingUp} color="#f59e0b" />
          </Col>
          <Col xs={12} sm={8} md={6} lg={4}>
            <KpiCard title="IGST" value={formatCurrency(summary?.igst || 0)}
              icon={ShieldCheck} color="#ef4444" />
          </Col>
        </Row>
      )}

      {/* ── Import history ──────────────────────────────────────────── */}
      {importHistory.length > 0 && (
        <Card size="small"
          title={<span style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-primary)' }}>Import History</span>}
          style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}>
          {importHistory.map((h, i) => <HistoryRow key={i} item={h} />)}
        </Card>
      )}

      {/* ── Invoice table ───────────────────────────────────────────── */}
      <Card
        style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}
        title={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
            <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-primary)' }}>
              Invoices — {periodLabel}
              {!invLoading && (
                <Badge count={filtered.length} color="var(--accent)"
                  style={{ marginLeft: 8, fontSize: 10 }} />
              )}
            </span>
            <Input.Search
              placeholder="Search invoice, supplier, GSTIN…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              allowClear
              style={{ width: 280, fontSize: 12 }}
              size="small"
            />
          </div>
        }
      >
        {invoiceList.length === 0 && !invLoading && (
          <Alert
            type="info" showIcon
            message={`No GSTR-2B data for ${periodLabel}`}
            description="Import your GSTR-2B JSON or Excel file for this period."
            action={
              <Button size="small" type="primary" icon={<Upload size={12} />}
                onClick={() => setShowImport(true)}>
                Import Now
              </Button>
            }
          />
        )}

        {(invoiceList.length > 0 || invLoading) && (
          <Table
            columns={columns}
            dataSource={filtered}
            loading={invLoading}
            rowKey="id"
            size="small"
            scroll={{ x: 1300 }}
            pagination={{
              pageSize,
              showSizeChanger: true,
              pageSizeOptions: [25, 50, 100, 250],
              onShowSizeChange: (_, size) => setPageSize(size),
              showTotal: (total) => `${total} invoices`,
              style: { padding: '12px 0 0' },
            }}
          />
        )}
      </Card>

      {/* ── Import modal ────────────────────────────────────────────── */}
      <Modal
        open={showImport}
        onCancel={() => setShowImport(false)}
        footer={null}
        width={620}
        destroyOnClose
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <Upload size={16} style={{ color: 'var(--accent)' }} />
            <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
              Import GSTR-2B Statement
            </span>
          </div>
        }
      >
        <Gstr2bImport
          period={gstPeriod}
          onSuccess={() => { setShowImport(false); refetch(); refetchHistory(); }}
          uploadGstr2b={uploadGstr2b}
          uploading={uploading}
        />
      </Modal>

      {/* ── Detail modal ────────────────────────────────────────────── */}
      <Gstr2bDetail
        visible={!!detail}
        onClose={() => setDetail(null)}
        invoice={detail}
      />
    </div>
  );
};

export default Gstr2b;
