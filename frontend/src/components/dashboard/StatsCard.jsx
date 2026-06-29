import React from 'react';
import { Card, Statistic } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';

const StatsCard = ({ title, value, icon, change, color = 'blue' }) => {
  const colorMap = {
    blue: 'border-blue-500',
    green: 'border-green-500',
    red: 'border-red-500',
    yellow: 'border-yellow-500',
    purple: 'border-purple-500',
  };

  return (
    <Card className="border-t-4" rootClassName={colorMap[color]}>
      <div className="flex items-center justify-between">
        <div>
          <Statistic title={title} value={value} />
          {change !== undefined && (
            <div className={`text-sm mt-1 ${change >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {change >= 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
              {Math.abs(change).toFixed(1)}%
            </div>
          )}
        </div>
        <div className={`p-3 rounded-lg bg-${color}-50 text-${color}-600`}>
          {icon}
        </div>
      </div>
    </Card>
  );
};

export default StatsCard;