import React, { useState } from 'react';
import {
  Alert, Button, Card, Col, Divider,
  Modal, Row, Spin, Statistic, Table, Tag, Timeline,
} from 'antd';
import { ClipboardList, CheckCircle2, Eye, RefreshCw } from 'lucide-react';
import { useReturns } from '../hooks/useReturns';
import { formatCurrency, formatDate, getMonthOptions, getYearOptions } from '../utils/formatters';
import useUiStore from '../store/uiStore';
import { Select } from 'antd';

const priorityTagColor = { HIGH: 'red', MEDIUM: 'orange', INFO: 'blue' };

const ReturnDraft = () => {
  const { gstPeriod } = useUiStore();
  const [month, setMonth] = useState(gstPeriod.month);
  const [year, setYear] = useState(gstPeriod.year);
  const [preview, setPreview] = useState(null);
  const [generating, setGenerating] = useState(false);

  const { useDrafts, draftReturn, drafting, approveDraft, approving } = useReturns();
  const { data, isLoading, refetch } = useDrafts({ page: 0, size: 20 });

  const drafts = data?.content || (Array.isArray(data) ? data : []);

  const handleDraft = async () => {
    try {
      setGenerating(true);
      await draftReturn(month, year);
      const poll = (n) => {
        if (n <= 0) { setGenerating(false); return; }
        setTimeout(() => { refetch(); poll(n - 1); }, 3000);
      };
      poll(4);
    } catch (_) {
      setGenerating(false);
    }
  };

  const periodLabel = `${String(month).padStart(2, '0')}-${year}`;

  const columns = [
    {
      title: 'Period', dataIndex: 'period', key: 'period',
      render: (v) => <span style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--accent)' }}>{v}</span>,
    },
    { title: 'Total GST', dataIndex: 'totalGst', key: 'totalGst', render: (v) => formatCurrency(v) },
    { title: 'ITC Available', dataIndex: 'totalItc', key: 'totalItc', render: (v) => <span style={{ color: '#22c55e', fontWeight: 600 }}>{formatCurrency(v)}</span> },
    {
      title: 'ITC at Risk', dataIndex: 'itcAtRisk', key: 'itcAtRisk',
      render: (v) => <span style={{ color: Number(v) > 0 ? '#ef4444' : '#22c55e', fontWeight: 600 }}>{formatCurrency(v)}</span>,
    },
    {
      title: 'Status', key: 'status',
      render: (_, r) => r.isApproved
        ? <Tag color="green">Approved</Tag>
        : <Tag color="gold">Pending Review</Tag>,
    },
    { title: 'Generated', dataIndex: 'generatedAt', key: 'generatedAt', render: (v) => formatDate(v) },
    {
      title: 'Actions', key: 'actions',
      render: (_, record) => (
        <div style={{ display: 'flex', gap: 6 }}>
          <Button size="small" icon={<Eye size={14} />} onClick={() => setPreview(record)}>View</Button>
          {!record.isApproved && (
            <Button size="small" type="primary" icon={<CheckCircle2 size={14} />}
              loading={approving}
              onClick={() => approveDraft(record.id).then(() => refetch())}>
              Approve
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ background: 'var(--accent-soft)', borderRadius: 10, padding: 10 }}>
          <ClipboardList size={20} style={{ color: 'var(--accent)' }} />
        </div>
        <div>
          <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: 'var(--text-primary)' }}>
            Return Drafts
          </h1>
          <p style={{ margin: 0, fontSize: 12, color: 'var(--text-muted)' }}>
            AI-generated GSTR-3B compliance briefs
          </p>
        </div>
      </div>

      {/* Generator */}
      <Card style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}>
        <p style={{ margin: '0 0 14px', fontSize: 13, color: 'var(--text-secondary)' }}>
          Generate an AI compliance brief for a filing period. Run reconciliation first.
        </p>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
          <Select value={month} onChange={setMonth} options={getMonthOptions()} style={{ width: 140 }} />
          <Select value={year} onChange={setYear} options={getYearOptions()} style={{ width: 100 }} />
          <Button type="primary" loading={drafting || generating}
            icon={<ClipboardList size={14} />} onClick={handleDraft}>
            Generate Draft
          </Button>
          <Button icon={<RefreshCw size={14} />} onClick={() => refetch()}>Refresh</Button>
        </div>
        {generating && (
          <p style={{ margin: '12px 0 0', fontSize: 12, color: 'var(--accent)' }}>
            ⚙️ AI is generating your compliance brief — this may take 10–30 seconds…
          </p>
        )}
      </Card>

      {/* List */}
      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', height: 200, alignItems: 'center' }}>
          <Spin size="large" tip="Loading drafts..." />
        </div>
      ) : drafts.length === 0 ? (
        <Alert type="info" showIcon
          message="No return drafts yet."
          description="Generate a draft above after running reconciliation." />
      ) : (
        <Card style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}>
          <Table columns={columns} dataSource={drafts} rowKey="id" pagination={{ pageSize: 10 }} />
        </Card>
      )}

      {/* Preview modal */}
      <Modal
        open={!!preview}
        onCancel={() => setPreview(null)}
        footer={[
          !preview?.isApproved && (
            <Button key="approve" type="primary" icon={<CheckCircle2 size={14} />}
              loading={approving}
              onClick={() => approveDraft(preview.id).then(() => { refetch(); setPreview(null); })}>
              Approve Draft
            </Button>
          ),
        ].filter(Boolean)}
        width={760}
        destroyOnClose
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <ClipboardList size={16} style={{ color: 'var(--accent)' }} />
            <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
              Compliance Brief —{' '}
              <span style={{ color: 'var(--accent)', fontFamily: 'monospace' }}>{preview?.period}</span>
            </span>
            {preview?.isApproved && <Tag color="green">Approved</Tag>}
          </div>
        }
      >
        {preview && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Row gutter={[12, 12]}>
              {[
                { title: 'Total Sales', value: preview.totalSales, color: 'var(--text-primary)' },
                { title: 'Total GST', value: preview.totalGst, color: 'var(--text-primary)' },
                { title: 'ITC Available', value: preview.totalItc, color: '#22c55e' },
                { title: 'ITC at Risk', value: preview.itcAtRisk, color: Number(preview.itcAtRisk) > 0 ? '#ef4444' : '#22c55e' },
                { title: 'Tax Liability', value: preview.taxLiability, color: 'var(--accent)' },
              ].map(({ title, value, color }) => (
                <Col key={title} span={12}>
                  <Statistic
                    title={<span style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>{title}</span>}
                    value={formatCurrency(value)}
                    valueStyle={{ color, fontSize: 15, fontWeight: 800 }}
                  />
                </Col>
              ))}
            </Row>

            <Divider style={{ margin: '4px 0', borderColor: 'var(--border)' }} />

            <div>
              <p style={{
                margin: '0 0 6px', fontSize: 11, fontWeight: 700, textTransform: 'uppercase',
                letterSpacing: '0.06em', color: 'var(--text-muted)'
              }}>
                AI Compliance Brief
              </p>
              <div style={{
                borderRadius: 8, background: 'var(--bg-input)', border: '1px solid var(--border)',
                padding: 14, whiteSpace: 'pre-wrap', fontSize: 13, lineHeight: 1.6,
                color: 'var(--text-primary)', fontFamily: 'monospace',
              }}>
                {preview.brief || 'Brief not available.'}
              </div>
            </div>

            {preview.actionItems?.length > 0 && (
              <div>
                <p style={{
                  margin: '0 0 10px', fontSize: 11, fontWeight: 700, textTransform: 'uppercase',
                  letterSpacing: '0.06em', color: 'var(--text-muted)'
                }}>
                  Action Items
                </p>
                <Timeline items={preview.actionItems.map((item) => ({
                  color: item.priority === 'HIGH' ? 'red' : item.priority === 'MEDIUM' ? 'orange' : 'blue',
                  children: (
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                      <Tag color={priorityTagColor[item.priority] || 'blue'} style={{ fontSize: 10, flexShrink: 0 }}>
                        {item.priority}
                      </Tag>
                      <span style={{ fontSize: 13, color: 'var(--text-primary)' }}>{item.title}</span>
                    </div>
                  ),
                }))} />
              </div>
            )}

            {preview.approvedAt && (
              <p style={{ fontSize: 11, color: 'var(--text-muted)', margin: 0 }}>
                Approved on {formatDate(preview.approvedAt)}
              </p>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default ReturnDraft;
