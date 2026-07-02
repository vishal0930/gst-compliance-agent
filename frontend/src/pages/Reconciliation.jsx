import React, { useState } from 'react';
import { Button, Card, Select, Spin, Space, message } from 'antd';
import { RefreshCw } from 'lucide-react';
import { getMonthOptions, getYearOptions } from '../utils/formatters';
import { useReconciliation } from '../hooks/useReconciliation';
import { downloadFile } from '../api/client';
import ReconciliationReport from '../components/reconciliation/ReconciliationReport';
import useUiStore from '../store/uiStore';

const Reconciliation = () => {
  const { gstPeriod } = useUiStore();
  const [month, setMonth] = useState(gstPeriod.month);
  const [year, setYear] = useState(gstPeriod.year);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const { reconcile } = useReconciliation();

  const handleReconcile = async () => {
    setLoading(true);
    try {
      const data = await reconcile(month, year);
      setResult(data);
      message.success('Reconciliation completed successfully');
    } catch (error) {
      message.error(error.message || 'Reconciliation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ background: 'var(--accent-soft)', borderRadius: 10, padding: 10 }}>
          <RefreshCw size={20} style={{ color: 'var(--accent)' }} />
        </div>
        <div>
          <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: 'var(--text-primary)' }}>
            Reconciliation
          </h1>
          <p style={{ margin: 0, fontSize: 12, color: 'var(--text-muted)' }}>
            Compare your books against GSTR-2B portal data
          </p>
        </div>
      </div>

      <Card style={{ background: 'var(--bg-card)', borderColor: 'var(--border)' }}>
        <Space wrap>
          <Select
            value={month}
            onChange={setMonth}
            options={getMonthOptions()}
            style={{ width: 140 }}
          />
          <Select
            value={year}
            onChange={setYear}
            options={getYearOptions()}
            style={{ width: 100 }}
          />
          <Button
            type="primary"
            icon={<RefreshCw size={14} />}
            onClick={handleReconcile}
            loading={loading}
          >
            Run Reconciliation
          </Button>
        </Space>
      </Card>

      {loading ? (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 200 }}>
          <Spin size="large" tip="Running reconciliation..." />
        </div>
      ) : result && (
        <ReconciliationReport result={result} recordId={result?.id || null} month={month} year={year} />
      )}
    </div>
  );
};

export default Reconciliation;
