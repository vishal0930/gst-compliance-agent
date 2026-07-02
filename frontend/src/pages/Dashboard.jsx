import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Row, Col, Card, Spin, Progress, Timeline, Button, Badge, Alert } from 'antd';
import {
  FileText, CheckCircle, AlertTriangle, TrendingUp,
  Cpu, Clock, UploadCloud, RefreshCw, FileSpreadsheet, Activity,
} from 'lucide-react';
import StatsCard from '../components/dashboard/StatsCard';
import { useInvoices } from '../hooks/useInvoices';
import { useGstr2b } from '../hooks/useGstr2b';
import { useReconciliation } from '../hooks/useReconciliation';
import { useDeadline } from '../hooks/useDeadline';
import { formatCurrency } from '../utils/formatters';
import useUiStore from '../store/uiStore';

const Dashboard = () => {
  const { gstPeriod: period } = useUiStore();
  const navigate = useNavigate();

  // --- Core Custom Hooks Connecting to Backend ---
  const { invoices, loading: invoicesLoading } = useInvoices({
    month: period.month,
    year: period.year,
  });

  const { useSummary } = useGstr2b();
  const { data: gstr2bSummary, isLoading: summaryLoading } = useSummary(
    period.month,
    period.year
  );

  const { useHistory } = useReconciliation();
  const { data: history, isLoading: historyLoading } = useHistory({
    month: period.month,
    year: period.year,
  });

  // Fetch true portal deadlines from your DeadlineController
  const { data: deadlines, isLoading: deadlinesLoading } = useDeadline({
    month: period.month,
    year: period.year,
  });

  const deadlineList = Array.isArray(deadlines) ? deadlines : [];

  const latestRecon = history?.content?.[0] || null;
  const loading = invoicesLoading || summaryLoading || historyLoading || deadlinesLoading;

  // --- Aggregate Metrics Engine ---
  const stats = {
    totalInvoices: invoices?.length || 0,
    pendingInvoices: invoices?.filter((i) => i.parseStatus === 'PROCESSING' || i.parseStatus === 'PENDING').length || 0,
    reconciledInvoices: invoices?.filter((i) => i.parseStatus === 'DONE' || i.parseStatus === 'MATCHED').length || 0,
    mismatches: latestRecon?.mismatchCount || 0,
    itcAtRisk: latestRecon?.itcAtRisk || 0,
    totalItc: gstr2bSummary?.totalItc || 0,
    eligibleItc: gstr2bSummary?.eligibleItc || 0,
    gstr2bCount: gstr2bSummary?.invoiceCount || 0,
  };

  // Dynamically derive compliance scores based on actual backend reconciliation states
  const totalProcessed = stats.reconciledInvoices + stats.mismatches;
  const complianceScore = totalProcessed > 0
    ? Math.max(0, Math.round(((stats.reconciledInvoices) / totalProcessed) * 100))
    : 100;

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-[70vh] gap-4">
        <Spin size="large" />
        <span className="text-slate-400 text-sm animate-pulse font-mono">Syncing enterprise compliance ledger states...</span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Branded Header Block */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between pb-4 border-b border-[var(--border)] gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight flex items-center gap-2">
            GST Compliance Intelligence Platform
            <Badge status="processing" text={<span className="text-[var(--text-muted)] text-xs">AI Agent Framework Active</span>} />
          </h1>
          <p className="text-[var(--text-muted)] text-xs mt-1">
            Fiscal Period Operational Analysis: <span className="text-[var(--yellow)] font-mono font-bold">{period.month}/{period.year}</span>
          </p>
        </div>

        {/* Quick Action Matrix Panel using standard SPA Router Transitions */}
        <div className="flex flex-wrap gap-2">
          <Button type="primary" icon={<UploadCloud size={16} />} onClick={() => navigate('/invoices')}>
            Upload Invoice
          </Button>
          <Button ghost icon={<FileSpreadsheet size={16} />} onClick={() => navigate('/gstr2b')} className="border-[var(--border)] text-[var(--yellow)] hover:border-[var(--yellow)]">
            Ingest GSTR-2B
          </Button>
          <Button type="default" icon={<RefreshCw size={16} />} onClick={() => navigate('/reconciliation')} className="border-[var(--border)] bg-[var(--bg-input)] text-[var(--text-secondary)] hover:text-[var(--text-primary)]">
            Run Reconciliation
          </Button>
        </div>
      </div>

      {/* Primary Metrics Layer */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatsCard
            title="Total ERP Invoices"
            value={stats.totalInvoices}
            icon={<FileText className="w-5 h-5" />}
            color="blue"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatsCard
            title="Fully Reconciled Units"
            value={`${stats.reconciledInvoices} / ${stats.totalInvoices}`}
            icon={<CheckCircle className="w-5 h-5" />}
            color="green"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatsCard
            title="Active Ledger Mismatches"
            value={stats.mismatches}
            icon={<AlertTriangle className="w-5 h-5" />}
            color={stats.mismatches > 0 ? 'red' : 'green'}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatsCard
            title="Total ITC Pool Available"
            value={formatCurrency(stats.totalItc)}
            icon={<TrendingUp className="w-5 h-5" />}
            color="purple"
          />
        </Col>
      </Row>

      {/* Advanced Middle Section: Health Indicators & Analytics Maps */}
      <Row gutter={[16, 16]}>
        {/* Compliance Health Radial Gauge */}
        <Col xs={24} md={8}>
          <Card title={<span className="flex items-center gap-2"><Activity size={16} className="text-[var(--yellow)]" />Compliance Health Index</span>} className="text-center h-full">
            <div className="py-6 flex flex-col items-center justify-center">
              <Progress
                type="circle"
                percent={complianceScore}
                strokeColor={{ '0%': '#ef4444', '50%': '#eab308', '100%': '#10b981' }}
                trailColor="var(--border)"
                width={140}
                format={(percent) => (
                  <div className="flex flex-col">
                    <span className="text-3xl font-extrabold text-[var(--text-primary)] font-mono">{percent}%</span>
                    <span className="text-[10px] uppercase text-[var(--text-muted)] tracking-wider">Score</span>
                  </div>
                )}
              />
            </div>
          </Card>
        </Col>

        {/* Recent Activity Timeline */}
        <Col xs={24} md={16}>
          <Card
            title={<span className="flex items-center gap-2"><Clock size={16} className="text-[var(--accent)]" />Recent Invoices</span>}
            className="h-full"
          >
            {invoices.length === 0 ? (
              <p className="text-[var(--text-muted)] text-sm py-4 text-center">No invoices yet. Upload your first invoice.</p>
            ) : (
              <Timeline
                items={invoices.slice(0, 5).map((inv) => ({
                  color: inv.parseStatus === 'DONE' ? 'green' : inv.parseStatus === 'FAILED' ? 'red' : 'blue',
                  children: (
                    <div>
                      <p className="text-[var(--text-primary)] font-medium text-sm">{inv.vendorName || 'Processing...'}</p>
                      <p className="text-[var(--text-muted)] text-xs">
                        {inv.invoiceNumber || '—'} &bull; {inv.parseStatus} &bull; {inv.invoiceDate || ''}
                      </p>
                    </div>
                  ),
                }))}
              />
            )}
          </Card>
        </Col>
      </Row>

      {/* AI Recommendations — real deadline alerts */}
      <Card
        title={<span className="flex items-center gap-2"><Cpu size={16} className="text-[var(--pink)]" />Upcoming Deadlines & Alerts</span>}
      >
        {deadlineList.length === 0 ? (
          <Alert message="No upcoming deadlines found." type="info" showIcon />
        ) : (
          <div className="space-y-3">
            {deadlineList.slice(0, 3).map((d, i) => (
              <Alert
                key={i}
                message={`${d.formType} — Due ${d.dueDate}`}
                description={
                  d.isOverdue
                    ? `Overdue by ${d.daysOverdue} days. Penalty: ₹${d.totalPenalty}`
                    : `${d.daysRemaining} days remaining — Priority: ${d.priority}`
                }
                type={d.isOverdue ? 'error' : d.daysRemaining <= 3 ? 'warning' : 'info'}
                showIcon
              />
            ))}
          </div>
        )}
      </Card>
    </div>
  );
};

export default Dashboard;