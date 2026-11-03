import { Client } from "@stomp/stompjs";

import type {
    IMessage,
    StompSubscription,
} from "@stomp/stompjs";

import SockJS from "sockjs-client";

import useAuth from "@/auth/store";

class SessionWebSocketService {

    private client: Client | null = null;

    private subscription: StompSubscription | null = null;

    connect(onConnected?: () => void) {

        if (this.client?.connected) {
            return;
        }

        const token =
            useAuth.getState().accessToken;

        this.client = new Client({

            webSocketFactory: () =>
                new SockJS("http://localhost:8084/ws"),

            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },

            reconnectDelay: 5000,

            debug: (message) => {
                console.log("[SESSION STOMP]", message);
            },

            onConnect: () => {

                console.log("SESSION SOCKET CONNECTED");

                onConnected?.();

            },

            onStompError: frame => {

                console.error(
                    "Session STOMP Error",
                    frame.headers["message"]
                );

            }

        });

        this.client.activate();

    }

    subscribeToSession(

        sessionId: string,

        callback: (message: IMessage) => void

    ) {

        if (!this.client?.connected) {
            return;
        }

        this.subscription?.unsubscribe();

        this.subscription =
            this.client.subscribe(

                `/topic/session/${sessionId}`,

                callback

            );

    }

    disconnect() {

        this.subscription?.unsubscribe();

        this.subscription = null;

        this.client?.deactivate();

        this.client = null;

    }

}

export default new SessionWebSocketService();