import client from './client';

export const gstr2bApi = {
  /**
   * POST /gstr2b/upload
   * Body: { month, year, replace, invoices: [...] }
   * Returns: Gstr2bUploadResponse
   */
  upload: (data) =>
    client.post('/gstr2b/upload', data),

  /**
   * GET /gstr2b/invoices?month=&year=&page=&size=&supplierGstin=&supplierName=&invoiceNumber=&importStatus=&matchStatus=
   * Returns: Page<Gstr2bInvoiceResponse>
   */
  getInvoices: (params = {}) =>
    client.get('/gstr2b/invoices', { params }),

  /**
   * GET /gstr2b/summary?month=&year=
   * Returns: Gstr2bSummaryResponse (typed — includes supplierCount, cgst, sgst, igst, cess)
   */
  getSummary: (month, year) =>
    client.get('/gstr2b/summary', { params: { month, year } }),

  /**
   * GET /gstr2b/import-history?page=&size=
   * Returns: List<Gstr2bImportSession>
   */
  getImportHistory: (params = {}) =>
    client.get('/gstr2b/import-history', { params }),
};
