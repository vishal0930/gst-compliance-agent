import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { gstr2bApi } from '../api/gstr2b';
import { message } from 'antd';

export const useGstr2b = () => {
  const queryClient = useQueryClient();

  // ── Paged + filtered invoices ──────────────────────────────────────
  const useInvoices = (params = {}) =>
    useQuery({
      queryKey: ['gstr2b-invoices', params],
      queryFn: () => gstr2bApi.getInvoices(params),
      staleTime: 60000,
      enabled: !!(params.month && params.year),
    });

  // ── Typed summary (supplier count, CGST/SGST/IGST/CESS) ───────────
  const useSummary = (month, year) =>
    useQuery({
      queryKey: ['gstr2b-summary', month, year],
      queryFn: () => gstr2bApi.getSummary(month, year),
      staleTime: 300000,
      enabled: !!(month && year),
    });

  // ── Import history ─────────────────────────────────────────────────
  const useImportHistory = (params = {}) =>
    useQuery({
      queryKey: ['gstr2b-import-history', params],
      queryFn: () => gstr2bApi.getImportHistory(params),
      staleTime: 60000,
    });

  // ── Upload mutation — handles 409 (period exists) separately ───────
  const uploadMutation = useMutation({
    mutationFn: (payload) => gstr2bApi.upload(payload),
    onSuccess: (_, variables) => {
      message.success('GSTR-2B imported successfully');
      queryClient.invalidateQueries({ queryKey: ['gstr2b-invoices'] });
      queryClient.invalidateQueries({ queryKey: ['gstr2b-summary'] });
      queryClient.invalidateQueries({ queryKey: ['gstr2b-import-history'] });
    },
    onError: (err) => {
      // 409 is handled in GstrImport component — surface others here
      if (err?.status !== 409) {
        message.error(err?.message || 'Upload failed');
      }
    },
  });

  return {
    useInvoices,
    useSummary,
    useImportHistory,
    uploadGstr2b: uploadMutation.mutateAsync,
    uploading: uploadMutation.isPending,
    uploadError: uploadMutation.error,
  };
};
