import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { returnsApi } from '../api/returns';
import { message } from 'antd';

export const useReturns = () => {
  const queryClient = useQueryClient();

  const draftMutation = useMutation({
    mutationFn: ({ month, year }) => returnsApi.draftReturn(month, year),
    onSuccess: () => {
      message.success('Return draft generation started — check the list in a few seconds');
      queryClient.invalidateQueries({ queryKey: ['returns'] });
    },
    onError: (err) => message.error(err.message || 'Failed to generate draft'),
  });

  // expose with correct signature
  const draftReturn = (month, year) =>
    draftMutation.mutateAsync({ month, year });

  const approveMutation = useMutation({
    mutationFn: (id) => returnsApi.approveDraft(id),
    onSuccess: () => {
      message.success('Draft approved');
      queryClient.invalidateQueries({ queryKey: ['returns'] });
    },
    onError: (err) => message.error(err.message || 'Approval failed'),
  });

  const useDrafts = (params = {}) =>
    useQuery({
      queryKey: ['returns', params],
      queryFn: () => returnsApi.getDrafts(params),
      staleTime: 60000,
    });

  const useDraft = (id) =>
    useQuery({
      queryKey: ['returns', id],
      queryFn: () => returnsApi.getDraft(id),
      enabled: !!id,
    });

  const useGstr3b = (id) =>
    useQuery({
      queryKey: ['returns-gstr3b', id],
      queryFn: () => returnsApi.getGstr3bDraft(id),
      enabled: !!id,
    });

  return {
    useDrafts,
    useDraft,
    useGstr3b,
    draftReturn,
    drafting: draftMutation.isPending,
    approveDraft: approveMutation.mutateAsync,
    approving: approveMutation.isPending,
  };
};
