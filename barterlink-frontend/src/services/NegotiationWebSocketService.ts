import { Client } from "@stomp/stompjs";
import type {
    IMessage,
    StompSubscription,
} from "@stomp/stompjs";

import SockJS from "sockjs-client";

import useAuth from "@/auth/store";


class NegotiationWebSocketService {

    private client: Client | null = null;

    private subscription: StompSubscription | null = null;

    private activeNegotiationId: string | null = null;

    private activeCallback:
        ((message: IMessage) => void) | null = null;


    async connect(): Promise<void> {

        if (this.client?.connected) {
            return;
        }

        const token =
            useAuth.getState().accessToken;


        if (!token) {

            throw new Error(
                "No access token available for negotiation WebSocket."
            );

        }


        if (this.client) {

            /*
             * A client already exists but is currently reconnecting.
             * The existing STOMP client will handle reconnection.
             */
            return;

        }


        await new Promise<void>((resolve, reject) => {

            let connected = false;


            const client = new Client({

                webSocketFactory: () =>
                    new SockJS(
                        "http://localhost:8084/ws"
                    ),


                connectHeaders: {
                    Authorization:
                        `Bearer ${token}`,
                },


                reconnectDelay: 5000,


                debug: (message) => {

                    console.log(
                        "[NEGOTIATION STOMP]",
                        message
                    );

                },


                onConnect: () => {

                    connected = true;


                    console.log(
                        "========== NEGOTIATION SOCKET CONNECTED =========="
                    );


                    resolve();


                    /*
                     * If a negotiation was already selected,
                     * subscribe immediately.
                     *
                     * This also handles reconnects.
                     */
                    this.subscribeCurrentNegotiation();

                },


                onStompError: (frame) => {

                    console.error(
                        "Negotiation STOMP Error",
                        frame.headers["message"]
                    );


                    if (!connected) {

                        client.deactivate();

                        this.client = null;

                        reject(
                            new Error(
                                frame.headers["message"] ||
                                "Negotiation WebSocket connection failed."
                            )
                        );

                    }

                },


                onWebSocketError: (error) => {

                    console.error(
                        "Negotiation WebSocket Error",
                        error
                    );


                    if (!connected) {

                        client.deactivate();

                        this.client = null;

                        reject(
                            new Error(
                                "Negotiation WebSocket connection failed."
                            )
                        );

                    }

                },


                onWebSocketClose: () => {

                    console.warn(
                        "Negotiation WebSocket connection closed."
                    );


                    this.subscription = null;

                },

            });


            this.client = client;

            client.activate();

        });

    }


    async subscribeToNegotiation(

        negotiationId: string,

        callback: (message: IMessage) => void

    ): Promise<void> {

        this.activeNegotiationId =
            negotiationId;

        this.activeCallback =
            callback;


        /*
         * First connection.
         */
        if (!this.client) {

            await this.connect();

            return;

        }


        /*
         * Already connected.
         */
        if (this.client.connected) {

            this.subscribeCurrentNegotiation();

        }

        /*
         * Otherwise the existing client is
         * reconnecting. onConnect() will
         * subscribe automatically.
         */

    }


    private subscribeCurrentNegotiation() {

        if (

            !this.client?.connected ||

            !this.activeNegotiationId ||

            !this.activeCallback

        ) {

            return;

        }


        this.subscription?.unsubscribe();


        const destination =
            `/topic/negotiation/${this.activeNegotiationId}`;


        this.subscription =
            this.client.subscribe(

                destination,

                (message) => {

                    console.log(
                        "========== RAW NEGOTIATION MESSAGE =========="
                    );

                    console.log(
                        message.body
                    );

                    console.log(
                        "=============================================="
                    );


                    this.activeCallback?.(
                        message
                    );

                }

            );


        console.log(
            "Subscribed:",
            destination
        );

    }


    sendMessage(

        negotiationId: string,

        message: string

    ): void {

        if (!this.client?.connected) {

            throw new Error(
                "Negotiation WebSocket is not connected."
            );

        }


        const destination =
            `/app/negotiation/${negotiationId}/message`;


        this.client.publish({

            destination,

            body: JSON.stringify({

                message: message.trim(),

            }),

        });


        console.log(
            "Negotiation message sent:",
            {
                destination,
                negotiationId,
            }
        );

    }


    disconnect() {

        this.subscription?.unsubscribe();

        this.subscription = null;

        this.activeNegotiationId = null;

        this.activeCallback = null;

        this.client?.deactivate();

        this.client = null;

    }

}


export default new NegotiationWebSocketService();