import { useQuery } from '@tanstack/react-query';
import { deadlineApi } from '../api/deadline';

export const useDeadline = (params = {}) => {
  return useQuery({
    queryKey: ['deadlines', params.month, params.year],
    queryFn: () => deadlineApi.getUpcoming(params.month, params.year),
    staleTime: 300000,
    enabled: true,
  });
};
