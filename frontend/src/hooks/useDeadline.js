import { useQuery } from '@tanstack/react-query';
import { deadlineApi } from '../api/deadline';

// Placeholder for future deadline functionality
// According to specification, deadlines are already implemented on backend
export const useDeadline = (params = {}) => {
  return {
    data: null,
    loading: false,
    error: null,
  };
};