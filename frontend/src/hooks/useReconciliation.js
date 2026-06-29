import { useQuery, useMutation } from '@tanstack/react-query';
import { reconciliationApi } from '../api/reconciliation';
import { message } from 'antd';

export const useReconciliation = () => {
  // 1. Reconciliation Trigger Mutation
  const reconcileMutation = useMutation({
    mutationFn: ({ month, year }) => reconciliationApi.reconcile(month, year),
    onError: (error) => {
      message.error(error.response?.data?.message || 'Reconciliation failed');
    },
  });

  const reconcile = async (month, year) => {
    return reconcileMutation.mutateAsync({ month, year });
  };

  // 2. Fetch History Log - Safely unwrapping response.data to access page contents
  const useHistory = (params = {}) => {
    return useQuery({
      queryKey: ['reconciliation-history', params],
      queryFn: async () => {
        const response = await reconciliationApi.getHistory(params);
        return response.data; // Unbox ApiResponse to expose {"content": [...]}
      },
      staleTime: 300000, // 5 minutes
    });
  };

  // 3. Structured Return Object (with useComplianceBrief safely excised)
  return {
    reconcile,
    reconciling: reconcileMutation.isPending,
    result: reconcileMutation.data,
    error: reconcileMutation.error,
    useHistory,
  };
};