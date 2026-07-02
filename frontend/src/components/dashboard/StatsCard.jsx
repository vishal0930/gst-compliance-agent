import React from 'react';
import { Card, Statistic } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';

const StatsCard = ({ title, value, icon, change, color = 'blue' }) => {
  const colorMap = {
    blue: 'border-t-4 border-[var(--accent)]',
    green: 'border-t-4 border-[var(--success)]',
    red: 'border-t-4 border-[var(--danger)]',
    yellow: 'border-t-4 border-[var(--yellow)]',
    purple: 'border-t-4 border-[var(--pink)]',
  };

  const iconStyleMap = {
    blue: { background: 'var(--accent-soft)', color: 'var(--accent)' },
    green: { background: 'var(--success-soft)', color: 'var(--success)' },
    red: { background: 'var(--danger-soft)', color: 'var(--danger)' },
    yellow: { background: 'var(--yellow-soft)', color: 'var(--yellow)' },
    purple: { background: 'var(--pink-soft)', color: 'var(--pink)' },
  };

  const style = iconStyleMap[color] || iconStyleMap.blue;

  return (
    <Card className={colorMap[color] || colorMap.blue}>
      <div className="flex items-center justify-between">
        <div>
          <Statistic title={title} value={value} />
          {change !== undefined && (
            <div className={`text-sm mt-1 flex items-center gap-1 ${change >= 0 ? 'text-[var(--success)]' : 'text-[var(--danger)]'}`}>
              {change >= 0 ? <ArrowUpOutlined style={{ fontSize: 10 }} /> : <ArrowDownOutlined style={{ fontSize: 10 }} />}
              <span className="font-mono font-semibold">{Math.abs(change).toFixed(1)}%</span>
            </div>
          )}
        </div>
        <div style={{ padding: 12, borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', ...style }}>
          {icon}
        </div>
      </div>
    </Card>
  );
};

export default StatsCard;