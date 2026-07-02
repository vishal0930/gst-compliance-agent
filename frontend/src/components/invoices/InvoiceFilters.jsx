import React from 'react';
import { Select, Space } from 'antd';
import { getMonthOptions, getYearOptions } from '../../utils/formatters';
import { INVOICE_STATUSES } from '../../utils/constants';

const InvoiceFilters = ({ filters, onFilterChange }) => {
  const handleChange = (key, value) => {
    onFilterChange({ [key]: value });
  };

  return (
    <Space wrap>
      <Select
        placeholder="Status"
        value={filters.status}
        onChange={(value) => handleChange('status', value)}
        options={INVOICE_STATUSES}
        style={{ width: 130 }}
        allowClear
      />
    </Space>
  );
};

export default InvoiceFilters;