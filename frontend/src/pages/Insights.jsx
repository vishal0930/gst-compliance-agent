import React from 'react';
import { Card, Empty, Alert } from 'antd';
import { Cpu } from 'lucide-react';

const Insights = () => {
  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center gap-2 mb-4">
        <Cpu className="text-amber-400" size={20} />
        <h1 className="text-2xl font-bold text-white">AI Insights</h1>
      </div>

      <Card className="bg-slate-900 border-slate-800">
        <Empty
          description={
            <div className="text-slate-400">
              <p className="mb-2">AI Insights module is not yet available</p>
              <p className="text-sm">This feature will be enabled when the backend provides AI insights endpoints</p>
            </div>
          }
        />
      </Card>

      <Alert
        message="Feature Pending Backend Implementation"
        description="The AI Insights module requires backend API endpoints to be implemented. AI recommendations should be derived from backend data only."
        type="info"
        showIcon
      />
    </div>
  );
};

export default Insights;