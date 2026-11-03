export interface NotificationItem {
  notificationId: string;
  type: string;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

/** Notification payload emitted by the backend's STOMP topic. */
export interface NotificationWebSocketMessage {
  notificationId: string;
  userId: string;
  title: string;
  message: string;
  notificationType: string;
  isRead: boolean;
  createdAt: string;
}