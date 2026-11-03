import { create } from "zustand";

import type { NotificationItem } from "@/models/notifications/Notification";

import {
  getMyNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from "@/services/NotificationService";

type NotificationState = {
  notifications: NotificationItem[];
  unreadCount: number;
  isLoading: boolean;
  error: string | null;

  loadNotifications: () => Promise<void>;

  receiveNotification: (
    notification: NotificationItem,
  ) => void;

  markAsRead: (notificationId: string) => Promise<void>;

  markAllAsRead: () => Promise<void>;

  clear: () => void;
};

const getErrorMessage = (error: unknown): string =>
  error instanceof Error
    ? error.message
    : "Could not load notifications.";

const sortNewestFirst = (
  items: NotificationItem[],
): NotificationItem[] =>
  [...items].sort(
    (left, right) =>
      new Date(right.createdAt).getTime() -
      new Date(left.createdAt).getTime(),
  );

const useNotificationStore = create<NotificationState>(
  (set) => ({
    notifications: [],
    unreadCount: 0,
    isLoading: false,
    error: null,

    loadNotifications: async () => {
      set({
        isLoading: true,
        error: null,
      });

      try {
        const fetchedNotifications = await getMyNotifications();

        const serverIds = new Set(
          fetchedNotifications.map(
            (notification) => notification.notificationId,
          ),
        );

        set((state) => {
          // Preserve live notifications received while loading the API.
          const liveNotificationsNotInResponse =
            state.notifications.filter(
              (notification) =>
                !serverIds.has(notification.notificationId),
            );

          const merged = sortNewestFirst([
            ...liveNotificationsNotInResponse,
            ...fetchedNotifications,
          ]);

          return {
            notifications: merged,
            unreadCount: merged.filter(
              (notification) => !notification.isRead,
            ).length,
            error: null,
          };
        });
      } catch (error) {
        set({
          error: getErrorMessage(error),
        });
      } finally {
        set({
          isLoading: false,
        });
      }
    },

    receiveNotification: (notification) => {
      set((state) => {
        // Avoid duplicates if the same notification is received twice.
        if (
          state.notifications.some(
            (existing) =>
              existing.notificationId ===
              notification.notificationId,
          )
        ) {
          return state;
        }

        const notifications = sortNewestFirst([
          notification,
          ...state.notifications,
        ]);

        return {
          notifications,
          unreadCount: notifications.filter(
            (item) => !item.isRead,
          ).length,
        };
      });
    },

    markAsRead: async (notificationId) => {
      await markNotificationAsRead(notificationId);

      set((state) => {
        const notifications = state.notifications.map(
          (notification) =>
            notification.notificationId === notificationId
              ? { ...notification, isRead: true }
              : notification,
        );

        return {
          notifications,
          unreadCount: notifications.filter(
            (notification) => !notification.isRead,
          ).length,
        };
      });
    },

    markAllAsRead: async () => {
      await markAllNotificationsAsRead();

      set((state) => ({
        notifications: state.notifications.map(
          (notification) => ({
            ...notification,
            isRead: true,
          }),
        ),
        unreadCount: 0,
      }));
    },

    clear: () =>
      set({
        notifications: [],
        unreadCount: 0,
        isLoading: false,
        error: null,
      }),
  }),
);

export default useNotificationStore;