import React, { useEffect } from 'react';
import { message, notification } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons';

const Toast = {
  success: (content, duration = 3) => {
    message.success({
      content,
      duration,
      icon: <CheckCircleOutlined />,
    });
  },

  error: (content, duration = 4) => {
    message.error({
      content,
      duration,
      icon: <CloseCircleOutlined />,
    });
  },

  warning: (content, duration = 3) => {
    message.warning({
      content,
      duration,
      icon: <WarningOutlined />,
    });
  },

  info: (content, duration = 3) => {
    message.info({
      content,
      duration,
      icon: <InfoCircleOutlined />,
    });
  },

  loading: (content, duration = 0) => {
    return message.loading({
      content,
      duration,
      key: 'loading',
    });
  },

  notification: {
    success: (message, description, duration = 4.5) => {
      notification.success({
        message,
        description,
        duration,
        placement: 'topRight',
      });
    },
    error: (message, description, duration = 4.5) => {
      notification.error({
        message,
        description,
        duration,
        placement: 'topRight',
      });
    },
    warning: (message, description, duration = 4.5) => {
      notification.warning({
        message,
        description,
        duration,
        placement: 'topRight',
      });
    },
    info: (message, description, duration = 4.5) => {
      notification.info({
        message,
        description,
        duration,
        placement: 'topRight',
      });
    },
  },
};

export const useToast = () => {
  return Toast;
};

export default Toast;