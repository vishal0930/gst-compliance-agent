import React, { useState } from 'react';
import { Button, Card, Select, Spin, Space, message } from 'antd';
import { RefreshCw } from 'lucide-react';
import { getCurrentPeriod, getMonthOptions, getYearOptions } from '../utils/formatters';
import { useReconciliation } from '../hooks/useReconciliation';
import ReconciliationReport from '../components/reconciliation/ReconciliationReport';

const Reconciliation = () => {
  const period = getCurrentPeriod();
  const [month, setMonth] = useState(period.month);
  const [year, setYear] = useState(period.year);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const { reconcile } = useReconciliation();

  const monthOptions = getMonthOptions();
  const yearOptions = getYearOptions();

  const handleReconcile = async () => {
    setLoading(true);
    try {
      const data = await reconcile(month, year);
      setResult(data);
      message.success('Reconciliation completed successfully');
    } catch (error) {
      message.error(error.response?.data?.message || 'Reconciliation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center gap-2 mb-4">
        <RefreshCw className="text-amber-400" size={20} />
        <h1 className="text-2xl font-bold text-white">Reconciliation</h1>
      </div>

      <Card className="bg-slate-900 border-slate-800">
        <Space>
          <Select
            value={month}
            onChange={setMonth}
            options={monthOptions}
            className="w-32 bg-slate-800 border-slate-700"
          />
          <Select
            value={year}
            onChange={setYear}
            options={yearOptions}
            className="w-24 bg-slate-800 border-slate-700"
          />
          <Button
            type="primary"
            onClick={handleReconcile}
            loading={loading}
            className="bg-amber-500 text-slate-950 border-amber-500 hover:bg-amber-400"
          >
            Run Reconciliation
          </Button>
        </Space>
      </Card>

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <Spin size="large" tip="Running reconciliation..." />
        </div>
      ) : result && (
        <ReconciliationReport result={result} month={month} year={year} />
      )}
    </div>
  );
};

export default Reconciliation;