import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationsApi } from '../api/notifications';

export const useNotifications = (unreadOnly = false) => {
  const queryClient = useQueryClient();

  // 1. Fetch Notifications Query
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['notifications', unreadOnly],
    queryFn: () => notificationsApi.getNotifications({ unreadOnly }),
    staleTime: 30000, // 30 seconds stale time
    refetchInterval: 15000, // Auto-poll every 15s to keep it fresh
  });

  // 2. Mark as Read Mutation
  const markAsReadMutation = useMutation({
    mutationFn: notificationsApi.markAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // 3. Mark All as Read Mutation
  const markAllAsReadMutation = useMutation({
    mutationFn: notificationsApi.markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // 4. Delete Notification Mutation
  const deleteMutation = useMutation({
    mutationFn: notificationsApi.deleteNotification,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const notifications = data || [];
  const unreadCount = notifications.filter(n => n.status === 'UNREAD').length;

  return {
    notifications,
    unreadCount,
    loading: isLoading,
    error,
    refetch,
    markAsRead: markAsReadMutation.mutateAsync,
    markAllAsRead: markAllAsReadMutation.mutateAsync,
    deleteNotification: deleteMutation.mutateAsync,
  };
};
