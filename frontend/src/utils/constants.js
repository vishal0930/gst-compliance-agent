export const INVOICE_STATUSES = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'PROCESSED', label: 'Processed' },
  { value: 'RECONCILED', label: 'Reconciled' },
  { value: 'MISMATCH', label: 'Mismatch' },
];

export const MISMATCH_TYPES = [
  { value: 'AMOUNT_MISMATCH', label: 'Amount Mismatch' },
  { value: 'GST_MISMATCH', label: 'GST Mismatch' },
  { value: 'DATE_MISMATCH', label: 'Date Mismatch' },
  { value: 'HSN_MISMATCH', label: 'HSN Code Mismatch' },
  { value: 'LINE_ITEM_MISMATCH', label: 'Line Item Mismatch' },
  { value: 'MISSING_IN_ERP', label: 'Missing in ERP' },
  { value: 'MISSING_IN_GSTR2B', label: 'Missing in GSTR-2B' },
];

export const GST_RATES = [0, 3, 5, 12, 18, 28];