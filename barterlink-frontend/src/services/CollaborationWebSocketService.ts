import { Client } from "@stomp/stompjs";
import type {
    IMessage,
    StompSubscription,
} from "@stomp/stompjs";

import SockJS from "sockjs-client";

import useAuth from "@/auth/store";

import type CollaborationSignalMessage
    from "@/models/collaboration/CollaborationSignalMessage";

class CollaborationWebSocketService {

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
                new SockJS(
                    "http://localhost:8084/ws"
                ),

            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },

            reconnectDelay: 5000,

            debug: (message) => {

                console.log("[STOMP]", message);

            },

            onConnect: () => {

    console.log("========== CONNECTED ==========");

    onConnected?.();

},

            onStompError: (frame) => {

                console.error(
                    "Broker Error",
                    frame.headers["message"]
                );

            },

            onWebSocketError: (error) => {

                console.error(
                    "WebSocket Error",
                    error
                );

            },

        });

        this.client.activate();

    }


    subscribeToRoom(

    roomId: string,

    callback: (message: IMessage) => void

) {

    if (!this.client?.connected) {

        console.warn(
            "STOMP client is not connected."
        );

        return;

    }

    this.subscription?.unsubscribe();

this.subscription =
    this.client.subscribe(
        `/topic/collaboration/${roomId}`,
        (message) => {

            console.log("========== RAW STOMP MESSAGE ==========");
            console.log(message.body);
            console.log("=======================================");

            callback(message);
        }
    );

    console.log(
        "Subscribed:",
        `/topic/collaboration/${roomId}`
    );

}


sendSignal(
    signal: CollaborationSignalMessage
) {

    if (!this.client?.connected) {

        console.warn(
            "STOMP client is not connected."
        );

        return;

    }

    this.client.publish({

        destination: "/app/collaboration.signal",

        body: JSON.stringify(signal),

    });

    console.log(
        "Signal Sent:",
        signal
    );

}


    disconnect() {

            this.subscription?.unsubscribe();

            this.subscription = null;

            this.client?.deactivate();

            this.client = null;

        }

}

export default new CollaborationWebSocketService();