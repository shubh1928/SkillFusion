import {
  Client,
  type IMessage,
  type StompSubscription,
} from "@stomp/stompjs";

import SockJS from "sockjs-client";

import type {
  NotificationItem,
  NotificationWebSocketMessage,
} from "@/models/notifications/Notification";

const getWebSocketUrl = (): string => {
  const configuredUrl = import.meta.env.VITE_WS_URL as
    | string
    | undefined;

  if (configuredUrl) {
    return configuredUrl;
  }

  const apiBaseUrl =
    import.meta.env.VITE_API_BASE_URL ||
    "http://localhost:8084/api/v1";

  // The API base URL ends in /api/v1, while SockJS uses /ws.
  const origin = new URL(
    apiBaseUrl,
    window.location.origin,
  ).origin;

  return `${origin}/ws`;
};

class NotificationWebSocketService {
  private client: Client | null = null;

  private subscription: StompSubscription | null = null;

  private activeUserId: string | null = null;

  private activeToken: string | null = null;

  private onNotification:
    | ((notification: NotificationItem) => void)
    | null = null;

  connect(
    userId: string,
    accessToken: string,
    onNotification: (notification: NotificationItem) => void,
  ): void {
    // Reuse the current connection when the user and token are unchanged.
    if (
      this.client &&
      this.activeUserId === userId &&
      this.activeToken === accessToken
    ) {
      this.onNotification = onNotification;

      if (this.client.connected) {
        this.subscribe(userId);
      }

      return;
    }

    this.disconnect();

    this.onNotification = onNotification;
    this.activeUserId = userId;
    this.activeToken = accessToken;

    const client = new Client({
      webSocketFactory: () => new SockJS(getWebSocketUrl()),

      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },

      reconnectDelay: 5000,

      debug: import.meta.env.DEV
        ? (message) =>
            console.debug("[NOTIFICATION STOMP]", message)
        : () => undefined,
    });

    this.client = client;

    client.onConnect = () => {
      if (this.client !== client) {
        return;
      }

      console.info("Notification WebSocket connected.");

      this.subscribe(userId);
    };

    client.onStompError = (frame) => {
      console.error(
        "Notification STOMP error:",
        frame.headers["message"] ||
          frame.body ||
          "The notification connection was rejected.",
      );
    };

    client.onWebSocketError = (error) => {
      console.error(
        "Notification WebSocket connection error:",
        error,
      );
    };

    client.onWebSocketClose = () => {
      this.subscription = null;
    };

    client.activate();
  }

  private subscribe(userId: string): void {
    if (!this.client?.connected) {
      return;
    }

    this.subscription?.unsubscribe();

    this.subscription = this.client.subscribe(
      `/topic/notifications/${userId}`,
      (message: IMessage) => {
        this.handleMessage(message, userId);
      },
    );
  }

  private handleMessage(
    message: IMessage,
    expectedUserId: string,
  ): void {
    try {
      const payload =
        JSON.parse(message.body) as NotificationWebSocketMessage;

      // Ignore malformed messages or messages for a different user.
      if (
        !payload.notificationId ||
        !payload.userId ||
        payload.userId !== expectedUserId
      ) {
        console.warn(
          "Ignored invalid notification WebSocket payload.",
        );
        return;
      }

      this.onNotification?.({
        notificationId: payload.notificationId,
        type: payload.notificationType || "SYSTEM",
        title: payload.title || "Notification",
        message: payload.message || "",
        isRead: Boolean(payload.isRead),
        createdAt: payload.createdAt,
      });
    } catch (error) {
      console.error(
        "Failed to parse notification WebSocket payload:",
        error,
      );
    }
  }

  disconnect(): void {
    const client = this.client;

    this.client = null;

    this.subscription?.unsubscribe();
    this.subscription = null;

    this.activeUserId = null;
    this.activeToken = null;
    this.onNotification = null;

    if (client) {
      void client.deactivate().catch((error: unknown) => {
        console.warn(
          "Could not cleanly close notification WebSocket:",
          error,
        );
      });
    }
  }
}

export default new NotificationWebSocketService();