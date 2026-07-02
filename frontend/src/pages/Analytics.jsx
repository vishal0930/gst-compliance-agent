import React from 'react';
import { Card, Col, Row, Spin, Statistic } from 'antd';
import { BarChart3, TrendingUp, AlertTriangle, CheckCircle2, FileText } from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip as ReTooltip,
  ResponsiveContainer, PieChart, Pie, Cell, Legend,
} from 'recharts';
import { useAnalytics } from '../hooks/useAnalytics';
import { formatCurrency } from '../utils/formatters';
import useUiStore from '../store/uiStore';

const COLORS = ['var(--success)', 'var(--danger)'];

const Analytics = () => {
  const { gstPeriod } = useUiStore();
  const { month, year } = gstPeriod;

  const {
    loading, totalInvoices, totalGst, totalAmount,
    totalMatched, totalMismatch, totalItcAtRisk,
    itcAvailable, reconList,
  } = useAnalytics(month, year);

  const barData = reconList.slice(-6).map((r) => ({
    period: r.taxPeriod || '-',
    Matched: r.matchedCount || 0,
    Mismatch: r.mismatchCount || 0,
    ItcRisk: Number(r.itcAtRisk || 0),
  }));

  const pieData = [
    { name: 'Matched', value: totalMatched },
    { name: 'Mismatch', value: totalMismatch },
  ].filter((d) => d.value > 0);

  const periodLabel = `${String(month).padStart(2, '0')}-${year}`;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ background: 'var(--accent-soft)', borderRadius: 10, padding: 10 }}>
          <BarChart3 size={20} style={{ color: 'var(--accent)' }} />
        </div>
        <div>
          <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: 'var(--text-primary)' }}>
            Analytics
          </h1>
          <p style={{ margin: 0, fontSize: 12, color: 'var(--text-muted)' }}>
            Period: <span style={{ color: 'var(--accent)', fontWeight: 600 }}>{periodLabel}</span>
            {' '}· Change in the header period selector
          </p>
        </div>
      </div>

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 200 }}>
          <Spin size="large" tip="Loading analytics..." />
        </div>
      ) : (
        <>
          <Row gutter={[14, 14]}>
            {[
              { title: 'Total Invoices', value: totalInvoices, icon: <FileText size={18} style={{ color: 'var(--accent)' }} />, color: 'var(--accent)' },
              { title: 'Total Amount', value: formatCurrency(totalAmount), icon: <TrendingUp size={18} style={{ color: 'var(--warning)' }} />, color: 'var(--warning)' },
              { title: 'Total GST', value: formatCurrency(totalGst), icon: <TrendingUp size={18} style={{ color: 'var(--success)' }} />, color: 'var(--success)' },
              { title: 'ITC Available', value: formatCurrency(itcAvailable), icon: <CheckCircle2 size={18} style={{ color: 'var(--accent)' }} />, color: 'var(--accent)' },
              { title: 'ITC at Risk', value: formatCurrency(totalItcAtRisk), icon: <AlertTriangle size={18} style={{ color: 'var(--danger)' }} />, color: 'var(--danger)' },
            ].map((kpi) => (
              <Col key={kpi.title} xs={24} sm={12} lg={Math.floor(24 / 5)}>
                <Card 
                  className="hover:scale-[1.02] hover:border-[var(--accent)] hover:shadow-lg transition-all duration-300 rounded-2xl"
                  style={{ 
                    borderTop: `4px solid ${kpi.color}`, 
                    background: 'var(--bg-card)', 
                    borderColor: 'var(--border)',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <Statistic
                      title={<span style={{ color: 'var(--text-muted)', fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em' }}>{kpi.title}</span>}
                      value={kpi.value}
                      valueStyle={{ color: 'var(--text-primary)', fontSize: 18, fontWeight: 800 }}
                    />
                    <div style={{ background: kpi.color + '18', borderRadius: 8, padding: 8 }}>{kpi.icon}</div>
                  </div>
                </Card>
              </Col>
            ))}
          </Row>

          <Row gutter={[14, 14]}>
            <Col xs={24} lg={16}>
              <Card
                title={<span style={{ color: 'var(--text-primary)', fontWeight: 700, fontSize: 13 }}>Reconciliation Trend (last 6 periods)</span>}
                style={{ background: 'var(--bg-card)', borderColor: 'var(--border)', borderRadius: '16px' }}
              >
                {barData.length === 0 ? (
                  <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: 40, fontSize: 13 }}>
                    No reconciliation data yet. Run reconciliation for at least one period.
                  </p>
                ) : (
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={barData} margin={{ top: 5, right: 10, left: 0, bottom: 5 }}>
                      <defs>
                        <linearGradient id="colorMatched" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="var(--success)" stopOpacity={0.8}/>
                          <stop offset="95%" stopColor="var(--success)" stopOpacity={0.2}/>
                        </linearGradient>
                        <linearGradient id="colorMismatch" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="var(--danger)" stopOpacity={0.8}/>
                          <stop offset="95%" stopColor="var(--danger)" stopOpacity={0.2}/>
                        </linearGradient>
                      </defs>
                      <XAxis dataKey="period" stroke="var(--text-muted)" tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} />
                      <YAxis stroke="var(--text-muted)" tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} />
                      <ReTooltip
                        contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8, color: 'var(--text-primary)' }}
                        labelStyle={{ color: 'var(--accent)', fontWeight: 600 }}
                      />
                      <Legend wrapperStyle={{ color: 'var(--text-secondary)', fontSize: 12 }} />
                      <Bar dataKey="Matched" fill="url(#colorMatched)" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="Mismatch" fill="url(#colorMismatch)" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </Card>
            </Col>
            <Col xs={24} lg={8}>
              <Card
                title={<span style={{ color: 'var(--text-primary)', fontWeight: 700, fontSize: 13 }}>Match Distribution</span>}
                style={{ background: 'var(--bg-card)', borderColor: 'var(--border)', borderRadius: '16px' }}
              >
                {pieData.length === 0 ? (
                  <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: 40, fontSize: 13 }}>
                    No data yet.
                  </p>
                ) : (
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <Pie data={pieData} cx="50%" cy="50%" innerRadius={60} outerRadius={95} paddingAngle={3} dataKey="value">
                        {pieData.map((_, idx) => <Cell key={idx} fill={COLORS[idx % COLORS.length]} />)}
                      </Pie>
                      <Legend wrapperStyle={{ color: 'var(--text-secondary)', fontSize: 12 }} />
                      <ReTooltip contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8 }} />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </Card>
            </Col>
          </Row>
        </>
      )}
    </div>
  );
};

export default Analytics;
