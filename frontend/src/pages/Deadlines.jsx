import React from 'react';
import { Card, Empty, Alert } from 'antd';
import { Calendar } from 'lucide-react';

const Deadlines = () => {
  return (
    <div className="p-4 space-y-6">
      <div className="flex items-center gap-2 mb-4">
        <Calendar className="text-amber-400" size={20} />
        <h1 className="text-2xl font-bold text-white">GST Deadlines</h1>
      </div>

      <Card className="bg-slate-900 border-slate-800">
        <Empty
          description={
            <div className="text-slate-400">
              <p className="mb-2">Deadlines module is not yet available</p>
              <p className="text-sm">This feature will be enabled when the backend provides deadline endpoints</p>
            </div>
          }
        />
      </Card>

      <Alert
        message="Feature Pending Backend Implementation"
        description="The Deadlines module requires backend API endpoints to be implemented. According to the specification, deadlines are already implemented on the backend. Please verify the backend API availability."
        type="info"
        showIcon
      />
    </div>
  );
};

export default Deadlines;