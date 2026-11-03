import { useEffect, useState } from "react";
import { Outlet } from "react-router";
import toast from "react-hot-toast";

import useAuth from "@/auth/store";

import NotificationWebSocketService from "@/services/NotificationWebSocketService";

import useNotificationStore from "@/stores/notificationStore";

import Sidebar from "./Sidebar";
import Topbar from "./Topbar";

function DashboardLayout() {
  const user = useAuth((state) => state.user);

  const accessToken = useAuth(
    (state) => state.accessToken,
  );

  const loadNotifications = useNotificationStore(
    (state) => state.loadNotifications,
  );

  const receiveNotification = useNotificationStore(
    (state) => state.receiveNotification,
  );

  const clearNotifications = useNotificationStore(
    (state) => state.clear,
  );

  const [sidebarOpen, setSidebarOpen] = useState(() => {
    const saved = localStorage.getItem("sidebar-open");

    return saved === null ? true : saved === "true";
  });

  useEffect(() => {
    localStorage.setItem(
      "sidebar-open",
      String(sidebarOpen),
    );
  }, [sidebarOpen]);

  useEffect(() => {
    if (!user?.id || !accessToken) {
      NotificationWebSocketService.disconnect();
      clearNotifications();
      return;
    }

    // Load notifications already stored by the backend.
    void loadNotifications();

    // Listen for new notifications pushed by the backend.
    NotificationWebSocketService.connect(
      user.id,
      accessToken,
      (notification) => {
        receiveNotification(notification);

        toast(notification.title, {
          icon: "🔔",
          duration: 5000,
        });
      },
    );

    return () => {
      NotificationWebSocketService.disconnect();
    };
  }, [
    user?.id,
    accessToken,
    loadNotifications,
    receiveNotification,
    clearNotifications,
  ]);

  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="flex h-screen overflow-hidden">
        {sidebarOpen && (
          <div
            className="
              fixed
              inset-0
              z-40
              bg-black/50
              backdrop-blur-[2px]
              md:hidden
            "
            onClick={() => setSidebarOpen(false)}
          />
        )}

        <Sidebar
          sidebarOpen={sidebarOpen}
          setSidebarOpen={setSidebarOpen}
        />

        <div className="flex flex-1 flex-col overflow-hidden">
          <Topbar setSidebarOpen={setSidebarOpen} />

          <main className="flex-1 overflow-y-auto bg-muted/20 p-4 md:p-6">
            <div className="mx-auto w-full max-w-7xl">
              <Outlet />
            </div>
          </main>
        </div>
      </div>
    </div>
  );
}

export default DashboardLayout;