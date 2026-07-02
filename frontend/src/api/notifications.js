import client from "./client";

export const notificationsApi = {
  getNotifications: async (params = {}) => {
    return client.get("/notifications", { params });
  },

  markAsRead: async (id) => {
    return client.put(`/notifications/${id}/read`);
  },

  markAllAsRead: async () => {
    return client.put("/notifications/read-all");
  },

  deleteNotification: async (id) => {
    return client.delete(`/notifications/${id}`);
  },
};
