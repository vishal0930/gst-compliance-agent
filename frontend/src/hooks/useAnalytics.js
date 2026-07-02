import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '../api/analytics';

/**
 * Derives all analytics data from three real backend endpoints.
 * No dummy values — all aggregates computed from API responses.
 */
export const useAnalytics = (month, year) => {
  const enabled = !!month && !!year;

  // GET /invoices?month=&year=&size=200
  const invoicesQuery = useQuery({
    queryKey: ['analytics-invoices', month, year],
    queryFn: () => analyticsApi.getInvoiceSummary({ month, year, size: 200 }),
    staleTime: 300000,
    enabled,
  });

  // GET /gstr2b/summary?month=&year=
  const gstr2bQuery = useQuery({
    queryKey: ['analytics-gstr2b', month, year],
    queryFn: () => analyticsApi.getGstr2bSummary(month, year),
    staleTime: 300000,
    enabled,
  });

  // GET /reconciliation?size=50  — all records, used for trend chart
  // We also pass month/year in the query key so the chart invalidates when period changes.
  // The backend returns all records for the user; we cannot filter by period on this endpoint
  // yet (no period query param on GET /reconciliation), so we take all and sort client-side.
  const reconQuery = useQuery({
    queryKey: ['analytics-reconciliation', month, year],
    queryFn: () => analyticsApi.getReconciliationHistory({ size: 50 }),
    staleTime: 300000,
  });

  const loading = invoicesQuery.isLoading || gstr2bQuery.isLoading || reconQuery.isLoading;

  // --- Normalise invoice list ---
  const invoiceList =
    invoicesQuery.data?.content || (Array.isArray(invoicesQuery.data) ? invoicesQuery.data : []);

  const totalInvoices = invoicesQuery.data?.totalElements ?? invoiceList.length;

  const totalGst = invoiceList.reduce((s, i) => s + Number(i.totalGst || 0), 0);
  const totalAmount = invoiceList.reduce((s, i) => s + Number(i.totalAmount || 0), 0);

  // --- Normalise reconciliation list ---
  const reconList =
    reconQuery.data?.content || (Array.isArray(reconQuery.data) ? reconQuery.data : []);

  // Aggregate across all records in history (shown as trend)
  const totalMatched = reconList.reduce((s, r) => s + Number(r.matchedCount || 0), 0);
  const totalMismatch = reconList.reduce((s, r) => s + Number(r.mismatchCount || 0), 0);
  const totalItcAtRisk = reconList.reduce((s, r) => s + Number(r.itcAtRisk || 0), 0);

  return {
    loading,
    totalInvoices,
    totalGst,
    totalAmount,
    totalMatched,
    totalMismatch,
    totalItcAtRisk,
    itcAvailable: Number(gstr2bQuery.data?.totalItc || 0),
    invoiceList,
    reconList,
    gstr2bSummary: gstr2bQuery.data,
  };
};
