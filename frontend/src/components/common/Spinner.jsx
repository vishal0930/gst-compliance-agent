import React from 'react';
import { Spin } from 'antd';
import { cn } from '../../utils/cn';
import { LoadingOutlined } from '@ant-design/icons';

const Spinner = ({ 
  size = 'default', 
  className = '', 
  tip = 'Loading...',
  fullScreen = false,
  children = null,
  spinning = true,
}) => {
  const sizeMap = {
    small: 'small',
    default: 'default',
    large: 'large',
  };

  const spinner = (
    <Spin
      size={sizeMap[size] || 'default'}
      tip={tip}
      indicator={<LoadingOutlined style={{ fontSize: size === 'large' ? 32 : 24 }} spin />}
      spinning={spinning}
      className={cn(
        fullScreen && 'fixed inset-0 flex items-center justify-center bg-white/80 z-50',
        className
      )}
    >
      {children}
    </Spin>
  );

  if (fullScreen) {
    return (
      <div className="fixed inset-0 flex items-center justify-center bg-white/80 z-50">
        <Spin size={sizeMap[size] || 'default'} tip={tip} />
      </div>
    );
  }

  return spinner;
};

export default Spinner;