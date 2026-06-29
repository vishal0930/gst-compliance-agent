import { useQuery } from '@tanstack/react-query';

// Dashboard data is composed from other hooks (useInvoices, useGstr2b, useReconciliation)
// This hook serves as a placeholder for future dashboard-specific endpoints
export const useDashboard = () => {
  return {
    data: null,
    loading: false,
    error: null,
  };
};