import React from 'react';
import { Alert, Card, Col, Row, Spin, Tag, Tooltip } from 'antd';
import { CalendarClock, AlertTriangle, CheckCircle2, Clock } from 'lucide-react';
import { useDeadline } from '../hooks/useDeadline';
import { formatCurrency } from '../utils/formatters';
import useUiStore from '../store/uiStore';

const priorityStyle = {
  CRITICAL: { color: '#ef4444', bg: 'rgba(239,68,68,0.10)', tagColor: 'red' },
  HIGH: { color: '#f97316', bg: 'rgba(249,115,22,0.10)', tagColor: 'orange' },
  MEDIUM: { color: '#f59e0b', bg: 'rgba(245,158,11,0.10)', tagColor: 'gold' },
  LOW: { color: '#22c55e', bg: 'rgba(34,197,94,0.10)', tagColor: 'green' },
};

const PriorityIcon = ({ priority }) => {
  const style = { color: (priorityStyle[priority] || priorityStyle.LOW).color };
  if (priority === 'CRITICAL' || priority === 'HIGH')
    return <AlertTriangle size={14} style={style} />;
  if (priority === 'MEDIUM')
    return <Clock size={14} style={style} />;
  return <CheckCircle2 size={14} style={style} />;
};

const Deadlines = () => {
  const { gstPeriod } = useUiStore();
  const { month, year } = gstPeriod;

  const { data: deadlines, isLoading, error } = useDeadline({ month, year });

  const list = Array.isArray(deadlines) ? deadlines : deadlines?.content || [];

  const periodLabel = `${String(month).padStart(2, '0')}-${year}`;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ background: 'var(--accent-soft)', borderRadius: 10, padding: 10 }}>
          <CalendarClock size={20} style={{ color: 'var(--accent)' }} />
        </div>
        <div>
          <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: 'var(--text-primary)' }}>
            GST Deadlines
          </h1>
          <p style={{ margin: 0, fontSize: 12, color: 'var(--text-muted)' }}>
            Period: <span style={{ color: 'var(--accent)', fontWeight: 600 }}>{periodLabel}</span>
            {' '}· Change in the header period selector
          </p>
        </div>
      </div>

      {error && (
        <Alert type="error" showIcon message="Could not load deadlines" description={error.message} />
      )}

      {isLoading && (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 200 }}>
          <Spin size="large" tip="Calculating deadlines..." />
        </div>
      )}

      {!isLoading && !error && list.length === 0 && (
        <Alert type="info" showIcon message="No upcoming deadlines found for this period." />
      )}

      <Row gutter={[14, 14]}>
        {list.map((d, i) => {
          const ps = priorityStyle[d.priority] || priorityStyle.LOW;
          return (
            <Col key={i} xs={24} md={12} lg={8}>
              <Card
                style={{
                  background: 'var(--bg-card)',
                  borderColor: 'var(--border)',
                  borderTop: `3px solid ${ps.color}`,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 14 }}>
                  <span style={{ fontSize: 15, fontWeight: 800, color: 'var(--text-primary)' }}>
                    {d.formType}
                  </span>
                  <Tag color={ps.tagColor} style={{ fontSize: 11 }}>{d.priority}</Tag>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: 13 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--text-muted)' }}>Due Date</span>
                    <span style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--text-primary)' }}>
                      {d.dueDate}
                    </span>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--text-muted)' }}>Days Remaining</span>
                    <span style={{
                      fontWeight: 800,
                      color: d.isOverdue ? '#ef4444' : d.daysRemaining <= 3 ? '#f97316' : '#22c55e',
                    }}>
                      {d.isOverdue
                        ? `${Math.abs(d.daysOverdue || 0)} days overdue`
                        : `${d.daysRemaining} days`}
                    </span>
                  </div>

                  {d.description && (
                    <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{d.description}</div>
                  )}

                  {d.isOverdue && Number(d.totalPenalty) > 0 && (
                    <div style={{
                      marginTop: 4, padding: 10, borderRadius: 8,
                      background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.25)',
                    }}>
                      <p style={{ margin: '0 0 4px', fontSize: 11, fontWeight: 700, color: '#ef4444' }}>
                        Penalty Accrued
                      </p>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--text-secondary)' }}>
                        <span>Per Day</span>
                        <span>{formatCurrency(d.penaltyPerDay)}</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, fontWeight: 800, color: '#ef4444', marginTop: 2 }}>
                        <span>Total</span>
                        <span>{formatCurrency(d.totalPenalty)}</span>
                      </div>
                    </div>
                  )}

                  <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 2 }}>
                    <PriorityIcon priority={d.priority} />
                    <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{d.priority} Priority</span>
                  </div>
                </div>
              </Card>
            </Col>
          );
        })}
      </Row>
    </div>
  );
};

export default Deadlines;
