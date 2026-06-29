import dayjs from 'dayjs';

export const formatCurrency = (amount) => {
  if (amount === undefined || amount === null) return '₹0.00';
  return `₹${Number(amount).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
};

export const formatDate = (date) => {
  if (!date) return '-';
  return dayjs(date).format('DD MMM YYYY');
};

export const formatDateShort = (date) => {
  if (!date) return '-';
  return dayjs(date).format('DD/MM/YY');
};

export const formatPeriod = (month, year) => {
  return `${String(month).padStart(2, '0')}-${year}`;
};

export const getCurrentPeriod = () => {
  const now = dayjs();
  return { month: now.month() + 1, year: now.year() };
};

export const getPreviousPeriod = () => {
  const prev = dayjs().subtract(1, 'month');
  return { month: prev.month() + 1, year: prev.year() };
};

export const getMonthOptions = () => {
  return Array.from({ length: 12 }, (_, i) => ({
    value: i + 1,
    label: dayjs().month(i).format('MMMM'),
  }));
};

export const getYearOptions = () => {
  const currentYear = dayjs().year();
  return Array.from({ length: 10 }, (_, i) => ({
    value: currentYear - 5 + i,
    label: String(currentYear - 5 + i),
  }));
};

export const getStatusColor = (status) => {
  const colors = {
    PENDING: 'gold',
    PROCESSED: 'blue',
    RECONCILED: 'green',
    MISMATCH: 'red',
    COMPLETED: 'green',
    RUNNING: 'processing',
    FAILED: 'red',
  };
  return colors[status] || 'default';
};

export const getStatusBadge = (status) => {
  const badges = {
    PENDING: { color: 'gold', text: 'Pending' },
    PROCESSED: { color: 'blue', text: 'Processed' },
    RECONCILED: { color: 'green', text: 'Reconciled' },
    MISMATCH: { color: 'red', text: 'Mismatch' },
    COMPLETED: { color: 'green', text: 'Completed' },
    RUNNING: { color: 'processing', text: 'Running' },
    FAILED: { color: 'red', text: 'Failed' },
  };
  return badges[status] || { color: 'default', text: status };
};

export const getMismatchTypeLabel = (type) => {
  const labels = {
    AMOUNT_MISMATCH: 'Amount Mismatch',
    GST_MISMATCH: 'GST Mismatch',
    DATE_MISMATCH: 'Date Mismatch',
    HSN_MISMATCH: 'HSN Code Mismatch',
    LINE_ITEM_MISMATCH: 'Line Item Mismatch',
    MISSING_IN_ERP: 'Missing in ERP',
    MISSING_IN_GSTR2B: 'Missing in GSTR-2B',
  };
  return labels[type] || type;
};

export const getMismatchTypeColor = (type) => {
  const colors = {
    AMOUNT_MISMATCH: 'red',
    GST_MISMATCH: 'orange',
    DATE_MISMATCH: 'gold',
    HSN_MISMATCH: 'purple',
    LINE_ITEM_MISMATCH: 'magenta',
    MISSING_IN_ERP: 'red',
    MISSING_IN_GSTR2B: 'blue',
  };
  return colors[type] || 'default';
};