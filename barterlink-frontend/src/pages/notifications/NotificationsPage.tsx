import { useEffect } from "react";

import {
  Bell,
  Check,
  CheckCheck,
  RefreshCw,
} from "lucide-react";

import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";

import useNotificationStore from "@/stores/notificationStore";

import type {
  NotificationItem,
} from "@/models/notifications/Notification";

const formatDate = (value: string): string => {
  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? "Date unavailable"
    : date.toLocaleString();
};

function NotificationsPage() {
  const notifications = useNotificationStore(
    (state) => state.notifications,
  );

  const unreadCount = useNotificationStore(
    (state) => state.unreadCount,
  );

  const isLoading = useNotificationStore(
    (state) => state.isLoading,
  );

  const error = useNotificationStore(
    (state) => state.error,
  );

  const loadNotifications = useNotificationStore(
    (state) => state.loadNotifications,
  );

  const markAsRead = useNotificationStore(
    (state) => state.markAsRead,
  );

  const markAllAsRead = useNotificationStore(
    (state) => state.markAllAsRead,
  );

  useEffect(() => {
    void loadNotifications();
  }, [loadNotifications]);

  const handleMarkRead = async (
    notification: NotificationItem,
  ) => {
    if (notification.isRead) {
      return;
    }

    try {
      await markAsRead(notification.notificationId);
    } catch {
      toast.error(
        "Could not mark the notification as read.",
      );
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllAsRead();

      toast.success(
        "All notifications marked as read.",
      );
    } catch {
      toast.error(
        "Could not mark notifications as read.",
      );
    }
  };

  return (
    <section className="mx-auto max-w-4xl space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <Bell className="text-primary" size={23} />

            <h2 className="text-2xl font-bold">
              Notifications
            </h2>
          </div>

          <p className="mt-1 text-sm text-muted-foreground">
            Updates about your negotiations, agreements,
            sessions, and account.
          </p>
        </div>

        <div className="flex gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => void loadNotifications()}
            disabled={isLoading}
          >
            <RefreshCw
              size={16}
              className={
                isLoading ? "animate-spin" : ""
              }
            />
            Refresh
          </Button>

          <Button
            type="button"
            onClick={() => void handleMarkAllRead()}
            disabled={unreadCount === 0}
          >
            <CheckCheck size={16} />
            Mark all read
          </Button>
        </div>
      </header>

      <div className="rounded-xl border bg-background">
        <div className="flex items-center justify-between border-b px-5 py-3">
          <span className="text-sm text-muted-foreground">
            {notifications.length} total
          </span>

          <span className="text-sm font-medium">
            {unreadCount} unread
          </span>
        </div>

        {error && (
          <div className="m-4 rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm">
            <p className="font-medium">
              Notifications could not be loaded.
            </p>

            <p className="mt-1 text-muted-foreground">
              {error}
            </p>

            <Button
              type="button"
              variant="outline"
              size="sm"
              className="mt-3"
              onClick={() => void loadNotifications()}
            >
              Try again
            </Button>
          </div>
        )}

        {isLoading && notifications.length === 0 ? (
          <p className="px-5 py-12 text-center text-sm text-muted-foreground">
            Loading notifications…
          </p>
        ) : notifications.length === 0 ? (
          <div className="px-5 py-14 text-center">
            <Bell
              className="mx-auto mb-3 text-muted-foreground"
              size={30}
            />

            <h3 className="font-semibold">
              You're all caught up
            </h3>

            <p className="mt-1 text-sm text-muted-foreground">
              New notifications will appear here as
              activity happens.
            </p>
          </div>
        ) : (
          <ul className="divide-y">
            {notifications.map((notification) => (
              <li
                key={notification.notificationId}
                className={`flex items-start gap-4 px-5 py-4 ${
                  notification.isRead
                    ? ""
                    : "bg-primary/5"
                }`}
              >
                <span
                  className={`mt-1 rounded-full p-2 ${
                    notification.isRead
                      ? "bg-muted text-muted-foreground"
                      : "bg-primary/10 text-primary"
                  }`}
                >
                  <Bell size={17} />
                </span>

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-semibold">
                      {notification.title}
                    </h3>

                    {!notification.isRead && (
                      <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                        New
                      </span>
                    )}

                    <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                      {notification.type}
                    </span>
                  </div>

                  <p className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">
                    {notification.message}
                  </p>

                  <p className="mt-2 text-xs text-muted-foreground">
                    {formatDate(notification.createdAt)}
                  </p>
                </div>

                {!notification.isRead && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="shrink-0"
                    aria-label={`Mark ${notification.title} as read`}
                    onClick={() =>
                      void handleMarkRead(notification)
                    }
                  >
                    <Check size={16} />
                    <span className="hidden sm:inline">
                      Mark read
                    </span>
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}

export default NotificationsPage;