import React from 'react';
import { Card, Empty, Alert } from 'antd';
import { BarChart3 } from 'lucide-react';

const Analytics = () => {
  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center gap-2 mb-4">
        <BarChart3 className="text-amber-400" size={20} />
        <h1 className="text-2xl font-bold text-white">Analytics</h1>
      </div>

      <Card className="bg-slate-900 border-slate-800">
        <Empty
          description={
            <div className="text-slate-400">
              <p className="mb-2">Analytics module is not yet available</p>
              <p className="text-sm">This feature will be enabled when the backend provides analytics endpoints</p>
            </div>
          }
        />
      </Card>

      <Alert
        message="Feature Pending Backend Implementation"
        description="The Analytics module requires backend API endpoints to be implemented. Please check the backend documentation for available analytics APIs."
        type="info"
        showIcon
      />
    </div>
  );
};

export default Analytics;