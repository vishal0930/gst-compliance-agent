import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '../api/analytics';

// Placeholder for future analytics functionality
// Remove when backend provides analytics endpoints
export const useAnalytics = () => {
  return {
    data: null,
    loading: false,
    error: null,
  };
};