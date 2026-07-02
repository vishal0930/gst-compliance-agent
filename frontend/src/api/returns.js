import client from './client';

export const returnsApi = {
  draftReturn: (month, year) =>
    client.post('/returns/draft', { month, year }),

  getDrafts: (params = {}) =>
    client.get('/returns', { params }),

  getDraft: (id) =>
    client.get(`/returns/${id}`),

  getGstr3bDraft: (id) =>
    client.get(`/returns/${id}/gstr3b`),

  approveDraft: (id) =>
    client.post(`/returns/${id}/approve`),

  exportDraft: (id, format = 'pdf') =>
    client.get(`/returns/${id}/export`, { params: { format }, responseType: 'blob' }),
};