import { useQuery, useMutation } from '@tanstack/react-query';
import { returnsApi } from '../api/returns';

// Placeholder for future returns functionality
// Remove when backend provides returns endpoints
export const useReturns = () => {
  return {
    data: null,
    loading: false,
    error: null,
  };
};