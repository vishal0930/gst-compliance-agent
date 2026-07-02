import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { reconciliationApi } from '../api/reconciliation';
import { message } from 'antd';

export const useReconciliation = () => {
  const queryClient = useQueryClient();

  const reconcileMutation = useMutation({
    mutationFn: ({ month, year }) => reconciliationApi.reconcile(month, year),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reconciliation-history'] });
    },
    onError: (err) => {
      message.error(err.message || 'Reconciliation failed');
    },
  });

  const reconcile = (month, year) =>
    reconcileMutation.mutateAsync({ month, year });

  // GET /reconciliation — paged list
  const useHistory = (params = {}) =>
    useQuery({
      queryKey: ['reconciliation-history', params],
      queryFn: () => reconciliationApi.getHistory(params),
      staleTime: 300000,
    });

  // GET /reconciliation/:id
  const useDetail = (id) =>
    useQuery({
      queryKey: ['reconciliation', id],
      queryFn: () => reconciliationApi.getReconciliation(id),
      enabled: !!id,
    });

  // GET /reconciliation/:id/mismatches
  const useMismatches = (id, type) =>
    useQuery({
      queryKey: ['reconciliation-mismatches', id, type],
      queryFn: () => reconciliationApi.getMismatches(id, type),
      enabled: !!id,
    });

  const resolveMutation = useMutation({
    mutationFn: ({ id, mismatchId, note }) =>
      reconciliationApi.resolveMismatch(id, mismatchId, note),
    onSuccess: (_, { id }) => {
      message.success('Mismatch resolved');
      queryClient.invalidateQueries({ queryKey: ['reconciliation-mismatches', id] });
      queryClient.invalidateQueries({ queryKey: ['reconciliation', id] });
    },
    onError: (err) => message.error(err.message || 'Resolve failed'),
  });

  return {
    reconcile,
    reconciling: reconcileMutation.isPending,
    result: reconcileMutation.data,
    error: reconcileMutation.error,
    useHistory,
    useDetail,
    useMismatches,
    resolveMismatch: resolveMutation.mutateAsync,
    resolving: resolveMutation.isPending,
  };
};
