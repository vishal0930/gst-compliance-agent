import React from 'react';
import { Card, Empty, Alert } from 'antd';
import { FileText } from 'lucide-react';

const ReturnDraft = () => {
  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center gap-2 mb-4">
        <FileText className="text-amber-400" size={20} />
        <h1 className="text-2xl font-bold text-white">Return Draft</h1>
      </div>

      <Card className="bg-slate-900 border-slate-800">
        <Empty
          description={
            <div className="text-slate-400">
              <p className="mb-2">Return Draft module is not yet available</p>
              <p className="text-sm">This feature will be enabled when the backend provides return draft endpoints</p>
            </div>
          }
        />
      </Card>

      <Alert
        message="Feature Pending Backend Implementation"
        description="The Return Draft module requires backend API endpoints to be implemented. Please check the backend documentation for available return APIs."
        type="info"
        showIcon
      />
    </div>
  );
};

export default ReturnDraft;