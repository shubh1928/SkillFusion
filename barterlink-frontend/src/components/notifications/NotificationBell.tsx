import { useEffect, useRef, useState } from "react";

import {
  Bell,
  CheckCheck,
  Circle,
} from "lucide-react";

import { useNavigate } from "react-router";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";

import useNotificationStore from "@/stores/notificationStore";

import type {
  NotificationItem,
} from "@/models/notifications/Notification";

const formatDate = (value: string): string => {
  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? ""
    : date.toLocaleString();
};

function NotificationBell() {
  const navigate = useNavigate();

  const containerRef = useRef<HTMLDivElement>(null);

  const [open, setOpen] = useState(false);

  const notifications = useNotificationStore(
    (state) => state.notifications,
  );

  const unreadCount = useNotificationStore(
    (state) => state.unreadCount,
  );

  const markAsRead = useNotificationStore(
    (state) => state.markAsRead,
  );

  const markAllAsRead = useNotificationStore(
    (state) => state.markAllAsRead,
  );

  const latestNotifications = notifications.slice(0, 5);

  useEffect(() => {
    if (!open) {
      return;
    }

    const closeIfOutside = (event: MouseEvent) => {
      if (
        event.target instanceof Node &&
        !containerRef.current?.contains(event.target)
      ) {
        setOpen(false);
      }
    };

    document.addEventListener("mousedown", closeIfOutside);

    return () => {
      document.removeEventListener(
        "mousedown",
        closeIfOutside,
      );
    };
  }, [open]);

  const openNotification = async (
    notification: NotificationItem,
  ) => {
    setOpen(false);

    if (!notification.isRead) {
      try {
        await markAsRead(notification.notificationId);
      } catch {
        toast.error(
          "Could not mark the notification as read.",
        );
      }
    }

    navigate("/dashboard/notifications");
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
    <div className="relative" ref={containerRef}>
      <Button
        type="button"
        variant="outline"
        size="icon"
        className="relative"
        aria-label={
          unreadCount
            ? `Notifications, ${unreadCount} unread`
            : "Notifications"
        }
        aria-expanded={open}
        aria-controls="notification-menu"
        onClick={() =>
          setOpen((current) => !current)
        }
      >
        <Bell size={18} />

        {unreadCount > 0 && (
          <span className="absolute -right-1.5 -top-1.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-bold text-white">
            {unreadCount > 99
              ? "99+"
              : unreadCount}
          </span>
        )}
      </Button>

      {open && (
        <div
          id="notification-menu"
          className="absolute right-0 top-12 z-50 w-[min(24rem,calc(100vw-2rem))] overflow-hidden rounded-xl border bg-background text-foreground shadow-xl"
          aria-label="Recent notifications"
        >
          <div className="flex items-center justify-between gap-3 border-b px-4 py-3">
            <div>
              <h2 className="font-semibold">
                Notifications
              </h2>

              <p className="text-xs text-muted-foreground">
                {unreadCount} unread
              </p>
            </div>

            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={unreadCount === 0}
              onClick={() =>
                void handleMarkAllRead()
              }
            >
              <CheckCheck size={15} />
              Mark all read
            </Button>
          </div>

          {latestNotifications.length === 0 ? (
            <div className="px-4 py-8 text-center text-sm text-muted-foreground">
              You don't have any notifications yet.
            </div>
          ) : (
            <div className="max-h-96 overflow-y-auto">
              {latestNotifications.map(
                (notification) => (
                  <button
                    key={notification.notificationId}
                    type="button"
                    className={`block w-full border-b px-4 py-3 text-left transition-colors hover:bg-muted/60 ${
                      notification.isRead
                        ? ""
                        : "bg-primary/5"
                    }`}
                    onClick={() =>
                      void openNotification(notification)
                    }
                  >
                    <span className="flex items-start gap-2">
                      {!notification.isRead && (
                        <Circle
                          size={8}
                          className="mt-1.5 shrink-0 fill-primary text-primary"
                        />
                      )}

                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-sm font-semibold">
                          {notification.title}
                        </span>

                        <span className="mt-1 block text-sm text-muted-foreground">
                          {notification.message}
                        </span>

                        <span className="mt-1 block text-xs text-muted-foreground">
                          {formatDate(notification.createdAt)}
                        </span>
                      </span>
                    </span>
                  </button>
                ),
              )}
            </div>
          )}

          <button
            type="button"
            className="w-full px-4 py-3 text-sm font-medium text-primary hover:bg-muted/60"
            onClick={() => {
              setOpen(false);
              navigate("/dashboard/notifications");
            }}
          >
            View all notifications
          </button>
        </div>
      )}
    </div>
  );
}

export default NotificationBell;