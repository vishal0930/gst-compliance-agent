import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { gstr2bApi } from '../api/gstr2b';
import { message } from 'antd';

export const useGstr2b = () => {
  const queryClient = useQueryClient();

  // Fetch Invoices - Unwrapping response.data safely
  const useInvoices = (params = {}) => {
    return useQuery({
      queryKey: ['gstr2b-invoices', params],
      queryFn: async () => {
        const response = await gstr2bApi.getInvoices(params);
        return response.data; // Unbox ApiResponse wrapper
      },
      staleTime: 60000,
    });
  };

  // Fetch Summary Analytics - Unwrapping response.data safely
  const useSummary = (month, year) => {
    return useQuery({
      queryKey: ['gstr2b-summary', month, year],
      queryFn: async () => {
        const response = await gstr2bApi.getSummary(month, year);
        return response.data; // Unbox directly to expose { totalItc, ... }
      },
      staleTime: 300000,
      enabled: !!month && !!year,
    });
  };

  // GSTR-2B Upload Mutation
  const uploadMutation = useMutation({
    mutationFn: gstr2bApi.upload,
    onSuccess: () => {
      message.success('GSTR-2B data uploaded successfully');
      queryClient.invalidateQueries({ queryKey: ['gstr2b-invoices'] });
      queryClient.invalidateQueries({ queryKey: ['gstr2b-summary'] });
    },
    onError: (error) => {
      message.error(error.response?.data?.message || 'Upload failed');
    },
  });

  return {
    useInvoices,
    useSummary,
    uploadGstr2b: uploadMutation.mutateAsync,
    uploading: uploadMutation.isPending,
  };
};