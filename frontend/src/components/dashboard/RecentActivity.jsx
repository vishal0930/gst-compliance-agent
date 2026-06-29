import React from 'react';
import { List, Avatar, Tag, Button, Empty } from 'antd';
import { 
  CheckCircleOutlined, 
  CloseCircleOutlined, 
  UploadOutlined,
  SyncOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import { formatDate, formatCurrency } from '../../utils/formatters';
import { useNavigate } from 'react-router-dom';

const RecentActivity = ({ activities = [], loading = false, limit = 5 }) => {
  const navigate = useNavigate();

  const getIcon = (type) => {
    const icons = {
      upload: <UploadOutlined className="text-blue-500" />,
      reconcile: <SyncOutlined className="text-green-500" />,
      mismatch: <CloseCircleOutlined className="text-red-500" />,
      matched: <CheckCircleOutlined className="text-green-500" />,
      default: <FileTextOutlined className="text-gray-500" />,
    };
    return icons[type] || icons.default;
  };

  const getColor = (type) => {
    const colors = {
      upload: 'blue',
      reconcile: 'green',
      mismatch: 'red',
      matched: 'green',
      default: 'gray',
    };
    return colors[type] || colors.default;
  };

  const getTag = (type) => {
    const tags = {
      upload: 'Upload',
      reconcile: 'Reconciliation',
      mismatch: 'Mismatch',
      matched: 'Matched',
    };
    return tags[type] || 'Activity';
  };

  const displayedActivities = activities.slice(0, limit);

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold text-gray-900">Recent Activity</h3>
        <Button type="link" onClick={() => navigate('/invoices')} className="text-blue-600">
          View All
        </Button>
      </div>

      {loading ? (
        <div className="text-center py-8">
          <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto" />
        </div>
      ) : displayedActivities.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="No recent activity"
          className="py-8"
        />
      ) : (
        <List
          dataSource={displayedActivities}
          renderItem={(item) => (
            <List.Item className="hover:bg-gray-50 rounded-lg px-3 py-2 transition-colors">
              <List.Item.Meta
                avatar={
                  <Avatar
                    icon={getIcon(item.type)}
                    style={{ backgroundColor: `${getColor(item.type)}50` }}
                    className="flex items-center justify-center"
                  />
                }
                title={
                  <div className="flex items-center justify-between">
                    <span className="font-medium">{item.title}</span>
                    <Tag color={getColor(item.type)} className="text-xs">
                      {getTag(item.type)}
                    </Tag>
                  </div>
                }
                description={
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-600">{item.description}</span>
                    <span className="text-gray-400 text-xs">{formatDate(item.timestamp)}</span>
                  </div>
                }
              />
              {item.amount && (
                <div className="text-right ml-4">
                  <div className="font-semibold">{formatCurrency(item.amount)}</div>
                  {item.status && (
                    <Tag color={item.status === 'success' ? 'green' : 'red'} className="text-xs">
                      {item.status}
                    </Tag>
                  )}
                </div>
              )}
            </List.Item>
          )}
        />
      )}
    </div>
  );
};

export default RecentActivity;