import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { message } from 'antd';



import { invoiceApi } from '../api/invoices';

export const useInvoices = (params = {}) => {
  const queryClient = useQueryClient();

  // 1. Fetch Invoices Query
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['invoices', params],
    queryFn: () => invoiceApi.getInvoices(params),
    staleTime: 60000, // 1 minute stale time
  });

  // 2. Upload Invoice Mutation
  const uploadMutation = useMutation({
    mutationFn: invoiceApi.uploadInvoices,
    onSuccess: () => {
      message.success('Invoice uploaded and queued successfully');
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
    onError: (err) => {
      message.error(err.response?.data?.message || err.message || 'Upload failed');
    },
  });

  // 3. Delete Invoice Mutation
  const deleteMutation = useMutation({
    mutationFn: invoiceApi.deleteInvoice,
    onSuccess: () => {
      message.success('Invoice deleted successfully');
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
    },
    onError: (err) => {
      message.error(err.response?.data?.message || err.message || 'Delete failed');
    },
  });

  // 4. Robust Response Data Normalization
  // Matches Spring's: ApiResponse { data: Page { content: [...], totalElements: X } }
const apiResponse = data;

  const invoices = Array.isArray(apiResponse)
    ? apiResponse
    : apiResponse?.content || [];

  const total = Array.isArray(apiResponse)
    ? apiResponse.length
    : apiResponse?.totalElements || apiResponse?.total || 0;

  return {
    invoices,
    total,
    loading: isLoading,
    error,
    refetch,
    uploadInvoices: uploadMutation.mutateAsync,
    uploading: uploadMutation.isPending,  
    deleteInvoice: deleteMutation.mutateAsync,
    deleting: deleteMutation.isPending,
  };
};