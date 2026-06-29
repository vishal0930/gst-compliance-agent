import client from './client';

export const reconciliationApi = {
  reconcile: async (month, year) => {
    const response = await client.post('/reconciliation/reconcile', { month, year });
    return response.data;
  },

  getHistory: async (params = {}) => {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        queryParams.append(key, String(value));
      }
    });
    const response = await client.get(`/reconciliation/history?${queryParams.toString()}`);
    return response.data;
  },

  getReconciliation: async (id) => {
    const response = await client.get(`/reconciliation/${id}`);
    return response.data;
  },

  getMismatches: async (id) => {
    const response = await client.get(`/reconciliation/${id}/mismatches`);
    return response.data;
  },

  resolveMismatch: async (id, mismatchId) => {
    const response = await client.post(`/reconciliation/${id}/resolve/${mismatchId}`);
    return response.data;
  },

  exportReconciliation: async (id) => {
    const response = await client.get(`/reconciliation/${id}/export`, {
      responseType: 'blob'
    });
    return response.data;
  },
};