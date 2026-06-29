import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Row, Col, Card, Spin, Progress, Timeline, Button, Badge, Alert, Tooltip, Empty } from 'antd';
import { 
  FileText, CheckCircle, AlertTriangle, TrendingUp, 
  Cpu, Clock, ShieldCheck, ArrowUpRight, UploadCloud, 
  RefreshCw, FileSpreadsheet, Calendar, Activity, BarChart3, HelpCircle
} from 'lucide-react';
import StatsCard from '../components/dashboard/StatsCard';
import { useInvoices } from '../hooks/useInvoices';
import { useGstr2b } from '../hooks/useGstr2b';
import { useReconciliation } from '../hooks/useReconciliation';
import { useDeadline } from '../hooks/useDeadline'; // Connected to DeadlineController
import { getCurrentPeriod, formatCurrency } from '../utils/formatters';

const Dashboard = () => {
  const period = getCurrentPeriod();
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
  const { data: deadlines, isLoading: deadlinesLoading } = useDeadline ? useDeadline({
    month: period.month,
    year: period.year
  }) : { data: null, isLoading: false };

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
    <div className="p-4 space-y-6">
      {/* Branded Header Block */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between pb-4 border-b border-slate-800 gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2">
            GST Compliance Intelligence Platform 
            <Badge status="processing" text={<span className="text-slate-400 text-xs">AI Agent Framework Active</span>} />
          </h1>
          <p className="text-slate-400 text-xs mt-1">
            Fiscal Period Operational Analysis: <span className="text-amber-400 font-mono font-bold">{period.month}/{period.year}</span>
          </p>
        </div>
        
        {/* Quick Action Matrix Panel using standard SPA Router Transitions */}
        <div className="flex flex-wrap gap-2">
          <Button type="primary" icon={<UploadCloud size={16} />} onClick={() => navigate('/invoices')}>
            Upload Invoice
          </Button>
          <Button ghost icon={<FileSpreadsheet size={16} />} onClick={() => navigate('/gstr2b')} className="border-slate-700 text-amber-400 hover:border-amber-400">
            Ingest GSTR-2B
          </Button>
          <Button type="default" icon={<RefreshCw size={16} />} onClick={() => navigate('/reconciliation')} className="border-slate-700 bg-slate-800 text-slate-200 hover:text-white">
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
            icon={<FileText className="w-5 h-5 text-blue-400" />}
            color="blue"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatsCard
            title="Fully Reconciled Units"
            value={`${stats.reconciledInvoices} / ${stats.totalInvoices}`}
            icon={<CheckCircle className="w-5 h-5 text-emerald-400" />}
            color="green"
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatsCard
            title="Active Ledger Mismatches"
            value={stats.mismatches}
            icon={<AlertTriangle className="w-5 h-5 text-red-400" />}
            color={stats.mismatches > 0 ? 'orange' : 'green'}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatsCard
            title="Total ITC Pool Available"
            value={formatCurrency(stats.totalItc)}
            icon={<TrendingUp className="w-5 h-5 text-amber-400" />}
            color="purple"
          />
        </Col>
      </Row>

      {/* Advanced Middle Section: Health Indicators & Analytics Maps */}
      <Row gutter={[16, 16]}>
        {/* Compliance Health Radial Gauge */}
        <Col xs={24} md={8}>
          <Card title={<span className="flex items-center gap-2"><Activity size={16} className="text-amber-400" />Compliance Health Index</span>} className="text-center h-full bg-slate-900/40 border-slate-800">
            <div className="py-6 flex flex-col items-center justify-center">
              <Progress
                type="circle"
                percent={complianceScore}
                strokeColor={{ '0%': '#EF4444', '50%': '#FACC15', '100%': '#22C55E' }}
                trailColor="#334155"
                width={140}
                format={(percent) => (
                  <div className="flex flex-col">
                    <span className="text-3xl font-extrabold text-white font-mono">{percent}%</span>
                    <span className="text-[10px] uppercase text-slate-400 tracking-wider">Score</span>
                  </div>
                )}
              />
            </div>
          </Card>
        </Col>

        {/* Recent Activity Timeline */}
        <Col xs={24} md={16}>
          <Card 
            title={<span className="flex items-center gap-2"><Clock size={16} className="text-amber-400" />Recent Activity</span>} 
            className="bg-slate-900/40 border-slate-800 h-full"
          >
            <Timeline
              items={[
                {
                  color: 'green',
                  children: (
                    <div>
                      <p className="text-white font-medium">Invoice reconciliation completed</p>
                      <p className="text-slate-400 text-xs">2 minutes ago</p>
                    </div>
                  ),
                },
                {
                  color: 'blue',
                  children: (
                    <div>
                      <p className="text-white font-medium">GSTR-2B data imported</p>
                      <p className="text-slate-400 text-xs">1 hour ago</p>
                    </div>
                  ),
                },
                {
                  color: 'amber',
                  children: (
                    <div>
                      <p className="text-white font-medium">New invoice uploaded</p>
                      <p className="text-slate-400 text-xs">3 hours ago</p>
                    </div>
                  ),
                },
              ]}
            />
          </Card>
        </Col>
      </Row>

      {/* AI Recommendations Section */}
      <Card 
        title={<span className="flex items-center gap-2"><Cpu size={16} className="text-amber-400" />AI Recommendations</span>} 
        className="bg-slate-900/40 border-slate-800"
      >
        <Alert
          message="Action Required"
          description="3 invoices have potential ITC discrepancies. Review reconciliation details for more information."
          type="warning"
          showIcon
          className="mb-4"
        />
        <Alert
          message="Optimization Opportunity"
          description="Consider consolidating invoice uploads to improve processing efficiency."
          type="info"
          showIcon
        />
      </Card>
    </div>
  );
};

export default Dashboard;