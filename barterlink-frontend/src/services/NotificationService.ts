import apiClient from "@/config/ApiClient";
import type { NotificationItem } from "@/models/notifications/Notification";

/** Fetch all notifications for the authenticated user. */
export const getMyNotifications = async (): Promise<NotificationItem[]> => {
  const response = await apiClient.get("/notifications");
  return response.data.data as NotificationItem[];
};

/** Mark one notification as read. */
export const markNotificationAsRead = async (
  notificationId: string,
): Promise<NotificationItem> => {
  const response = await apiClient.put(
    `/notifications/${notificationId}/read`,
  );

  return response.data.data as NotificationItem;
};

/** Mark every notification as read. */
export const markAllNotificationsAsRead = async (): Promise<void> => {
  await apiClient.put("/notifications/read-all");
};