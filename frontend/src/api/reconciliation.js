import client from './client';

export const reconciliationApi = {
  reconcile: (month, year) =>
    client.post('/reconciliation/run', { month, year }),

  getHistory: (params = {}) =>
    client.get('/reconciliation', { params }),

  getReconciliation: (id) =>
    client.get(`/reconciliation/${id}`),

  getMismatches: (id, type) =>
    client.get(`/reconciliation/${id}/mismatches`, { params: type ? { type } : {} }),

  resolveMismatch: (id, mismatchId, note) =>
    client.post(`/reconciliation/${id}/resolve/${mismatchId}`, note, {
      headers: { 'Content-Type': 'text/plain' },
    }),

  exportReconciliation: (id) =>
    client.get(`/reconciliation/${id}/export`, { responseType: 'blob' }),
};