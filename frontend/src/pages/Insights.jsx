import React from 'react';
import { Alert, Card, Col, Row, Spin, Tag, Timeline } from 'antd';
import { BrainCircuit, TrendingDown, TrendingUp, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { useAnalytics } from '../hooks/useAnalytics';
import { formatCurrency } from '../utils/formatters';
import useUiStore from '../store/uiStore';

const Insights = () => {
  const { gstPeriod } = useUiStore();
  const { month, year } = gstPeriod;

  const {
    loading, totalInvoices, totalMismatch, totalMatched,
    totalItcAtRisk, itcAvailable, reconList, invoiceList,
  } = useAnalytics(month, year);

  const periodLabel = `${String(month).padStart(2, '0')}-${year}`;

  const complianceRate = (totalMatched + totalMismatch) > 0
    ? Math.round((totalMatched / (totalMatched + totalMismatch)) * 100)
    : 100;

  const riskySorted = [...reconList]
    .sort((a, b) => Number(b.itcAtRisk || 0) - Number(a.itcAtRisk || 0))
    .slice(0, 5);

  const statusGroups = invoiceList.reduce((acc, inv) => {
    acc[inv.parseStatus] = (acc[inv.parseStatus] || 0) + 1;
    return acc;
  }, {});

  const insights = [];
  if (totalItcAtRisk > 0) {
    insights.push({
      color: 'red', icon: <TrendingDown size={14} style={{ color: '#ef4444' }} />,
      title: `${formatCurrency(totalItcAtRisk)} ITC at Risk`,
      desc: `${totalMismatch} mismatched invoice(s) could result in ITC disallowance. Resolve before filing GSTR-3B.`,
      priority: 'HIGH',
    });
  }
  if (complianceRate >= 95) {
    insights.push({
      color: 'green', icon: <CheckCircle2 size={14} style={{ color: '#22c55e' }} />,
      title: `Compliance Rate: ${complianceRate}%`,
      desc: 'Excellent reconciliation health. Most invoices are matched with GSTR-2B portal.',
      priority: 'INFO',
    });
  } else if (complianceRate < 80) {
    insights.push({
      color: 'orange', icon: <AlertTriangle size={14} style={{ color: '#f97316' }} />,
      title: `Low Compliance Rate: ${complianceRate}%`,
      desc: 'Too many invoices are mismatched. Contact suppliers to file their GST returns correctly.',
      priority: 'MEDIUM',
    });
  }
  if (itcAvailable > 0) {
    insights.push({
      color: 'blue', icon: <TrendingUp size={14} style={{ color: '#6366f1' }} />,
      title: `Eligible ITC: ${formatCurrency(itcAvailable)}`,
      desc: 'Claim this ITC in your next GSTR-3B filing to reduce tax liability.',
      priority: 'INFO',
    });
  }
  if (statusGroups['FAILED'] > 0) {
    insights.push({
      color: 'red', icon: <AlertTriangle size={14} style={{ color: '#ef4444' }} />,
      title: `${statusGroups['FAILED']} Invoice(s) Failed Parsing`,
      desc: 'Some invoices could not be parsed by the AI engine. Re-upload with clearer files.',
      priority: 'HIGH',
    });
  }

  const priorityTagColor = { HIGH: 'red', MEDIUM: 'orange', INFO: 'blue' };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ background: 'var(--accent-soft)', borderRadius: 10, padding: 10 }}>
          <BrainCircuit size={20} style={{ color: 'var(--accent)' }} />
        </div>
        <div>
          <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: 'var(--text-primary)' }}>
            AI Insights
          </h1>
          <p style={{ margin: 0, fontSize: 12, color: 'var(--text-muted)' }}>
            Period: <span style={{ color: 'var(--accent)', fontWeight: 600 }}>{periodLabel}</span>
            {' '}· Derived from live reconciliation + invoice data
          </p>
        </div>
      </div>

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 200 }}>
          <Spin size="large" tip="Analysing compliance data..." />
        </div>
      ) : (
        <>
          {insights.length === 0 && (
            <Alert type="success" showIcon message="No issues detected. Your compliance is on track for this period." />
          )}

          {insights.length > 0 && (
            <Card
              title={<span style={{ color: 'var(--text-primary)', fontSize: 13, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8 }}>
                <BrainCircuit size={14} style={{ color: 'var(--accent)' }} /> AI Recommendations
              </span>}
              style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {insights.map((ins, i) => (
                  <div key={i} style={{
                    display: 'flex', gap: 12, padding: 14, borderRadius: 8,
                    background: 'var(--bg-input)', border: '1px solid var(--border)',
                  }}>
                    <div style={{ marginTop: 2 }}>{ins.icon}</div>
                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                        <span style={{ fontWeight: 700, fontSize: 13, color: 'var(--text-primary)' }}>{ins.title}</span>
                        <Tag color={priorityTagColor[ins.priority]} style={{ fontSize: 10 }}>{ins.priority}</Tag>
                      </div>
                      <p style={{ margin: 0, fontSize: 12, color: 'var(--text-secondary)' }}>{ins.desc}</p>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          )}

          <Row gutter={[14, 14]}>
            <Col xs={24} md={12}>
              <Card
                title={<span style={{ color: 'var(--text-primary)', fontSize: 13, fontWeight: 700 }}>Invoice Status Breakdown</span>}
                style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}
              >
                {Object.keys(statusGroups).length === 0 ? (
                  <p style={{ color: 'var(--text-muted)', fontSize: 12 }}>No invoices found for this period.</p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {Object.entries(statusGroups).map(([status, count]) => (
                      <div key={status} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
                        <span style={{ color: 'var(--text-secondary)' }}>{status}</span>
                        <span style={{
                          fontWeight: 700,
                          color: status === 'DONE' ? '#22c55e' : status === 'FAILED' ? '#ef4444' : status === 'PROCESSING' ? '#f59e0b' : 'var(--text-primary)',
                        }}>
                          {count}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card
                title={<span style={{ color: 'var(--text-primary)', fontSize: 13, fontWeight: 700 }}>Top ITC Risk Periods</span>}
                style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}
              >
                {riskySorted.length === 0 ? (
                  <p style={{ color: 'var(--text-muted)', fontSize: 12 }}>No reconciliation data yet.</p>
                ) : (
                  <Timeline items={riskySorted.map((r) => ({
                    color: Number(r.itcAtRisk) > 0 ? 'red' : 'green',
                    children: (
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
                        <span style={{ fontFamily: 'monospace', color: 'var(--text-primary)', fontWeight: 600 }}>{r.taxPeriod}</span>
                        <span style={{ color: Number(r.itcAtRisk) > 0 ? '#ef4444' : '#22c55e', fontWeight: 700 }}>
                          {formatCurrency(r.itcAtRisk)}
                        </span>
                      </div>
                    ),
                  }))} />
                )}
              </Card>
            </Col>
          </Row>
        </>
      )}
    </div>
  );
};

export default Insights;
