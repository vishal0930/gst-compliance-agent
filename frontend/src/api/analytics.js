import client from './client';

// Analytics is derived from existing backend endpoints:
// invoices, gstr2b summary, and reconciliation history.
export const analyticsApi = {
  getInvoiceSummary: (params = {}) =>
    client.get('/invoices', { params }),

  getGstr2bSummary: (month, year) =>
    client.get('/gstr2b/summary', { params: { month, year } }),

  getReconciliationHistory: (params = {}) =>
    client.get('/reconciliation', { params }),
};