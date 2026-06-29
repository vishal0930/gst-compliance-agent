import React from 'react';
import { Button as AntButton } from 'antd';
import { cn } from '../../utils/cn';

const Button = ({
  children,
  variant = 'primary',
  size = 'middle',
  loading = false,
  disabled = false,
  icon = null,
  className = '',
  onClick,
  htmlType = 'button',
  block = false,
  danger = false,
  ghost = false,
  ...props
}) => {
  const variantMap = {
    primary: 'primary',
    secondary: 'default',
    success: 'primary',
    danger: 'primary',
    warning: 'default',
    ghost: 'default',
    link: 'link',
    text: 'text',
  };

  return (
    <AntButton
      type={variantMap[variant] || 'primary'}
      size={size}
      loading={loading}
      disabled={disabled}
      icon={icon}
      className={cn(
        'font-medium transition-all duration-200',
        variant === 'success' && 'bg-green-500 hover:bg-green-600 border-green-500',
        variant === 'warning' && 'bg-yellow-500 hover:bg-yellow-600 border-yellow-500',
        className
      )}
      onClick={onClick}
      htmlType={htmlType}
      block={block}
      danger={danger}
      ghost={ghost}
      {...props}
    >
      {children}
    </AntButton>
  );
};

export default Button;