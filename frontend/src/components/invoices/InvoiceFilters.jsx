import React from 'react';
import { Select, Space } from 'antd';
import { getMonthOptions, getYearOptions } from '../../utils/formatters';
import { INVOICE_STATUSES } from '../../utils/constants';

const InvoiceFilters = ({ filters, onFilterChange }) => {
  const monthOptions = getMonthOptions();
  const yearOptions = getYearOptions();

  const handleChange = (key, value) => {
    onFilterChange({ [key]: value });
  };

  return (
    <Space wrap>
      <Select
        placeholder="Month"
        value={filters.month}
        onChange={(value) => handleChange('month', value)}
        options={monthOptions}
        style={{ width: 120 }}
        allowClear
      />
      <Select
        placeholder="Year"
        value={filters.year}
        onChange={(value) => handleChange('year', value)}
        options={yearOptions}
        style={{ width: 100 }}
        allowClear
      />
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