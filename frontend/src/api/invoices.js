import client, { uploadMultipart } from "./client";

export const invoiceApi = {
  /**
   * 1. Upload Invoices with Progress Tracking
   * Pipes upload progress tracking percentages safely down into UI state.
   */
  uploadInvoices: async (file, onProgress) => {
    const formData = new FormData();
    formData.append("file", file);
    
    return uploadMultipart("/invoices/upload", formData, onProgress);
  },

  /**
   * 2. Get Invoices List
   * Leverages clean Axios object mapping for complex filter parameters.
   */
  getInvoices: async (params = {}) => {
    return client.get("/invoices", { params });
  },

  /**
   * 3. Get Single Invoice Details
   */
  getInvoice: async (id) => {
    return client.get(`/invoices/${id}`);
  },

  /**
   * 4. Delete Invoice
   */
  deleteInvoice: async (id) => {
    return client.delete(`/invoices/${id}`);
  },

  /**
   * 5. Poll Async Processing Job Status
   * GET /api/v1/invoices/jobs/{jobId}
   */
  getJobStatus: async (jobId) => {
    return client.get(`/invoices/jobs/${jobId}`);
  },
};