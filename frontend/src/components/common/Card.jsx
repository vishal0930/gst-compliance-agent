import React from 'react';
import { Card as AntCard } from 'antd';
import { cn } from '../../utils/cn';

const Card = ({
  title,
  children,
  className = '',
  loading = false,
  extra = null,
  bordered = true,
  hoverable = false,
  size = 'default',
  actions = [],
  cover = null,
  ...props
}) => {
  return (
    <AntCard
      title={title}
      className={cn(
        'rounded-xl shadow-sm transition-all duration-200',
        hoverable && 'hover:shadow-lg cursor-pointer',
        className
      )}
      loading={loading}
      extra={extra}
      bordered={bordered}
      size={size}
      actions={actions}
      cover={cover}
      {...props}
    >
      {children}
    </AntCard>
  );
};

export default Card;