import client from './client';

export const gstr2bApi = {
  upload: async (data) => {
    const response = await client.post('/gstr2b/upload', data);
    return response.data;
  },

  getInvoices: async (params = {}) => {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        queryParams.append(key, String(value));
      }
    });
    const response = await client.get(`/gstr2b/invoices?${queryParams.toString()}`);
    return response.data;
  },

  getSummary: async (month, year) => {
    const response = await client.get(`/gstr2b/summary?month=${month}&year=${year}`);
    return response.data;
  },
};