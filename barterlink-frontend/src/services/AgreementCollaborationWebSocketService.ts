
import { Client } from "@stomp/stompjs";
import type {
  IMessage,
  StompSubscription,
} from "@stomp/stompjs";

import SockJS from "sockjs-client";

import useAuth from "@/auth/store";

import type {
  AgreementCollaborationUpdateMessage,
} from "@/services/AgreementCollaborationService";

type AgreementUpdateCallback = (
  update: AgreementCollaborationUpdateMessage,
) => void;

type ConnectionCallback = () => void;

type AgreementDocumentScope = "version" | "negotiation";

interface ConnectionWaiter {
  resolve: () => void;
  reject: (error: Error) => void;
}

class AgreementCollaborationWebSocketService {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;

  private activeNegotiationId: string | null = null;
  private activeVersionId: string | null = null;
  private activeDocumentScope: AgreementDocumentScope | null = null;

  private activeCallback: AgreementUpdateCallback | null = null;
  private activeConnectedCallback: ConnectionCallback | null = null;

  private connectionWaiters: ConnectionWaiter[] = [];

  /**
   * Existing version-level agreement subscription.
   */
  async subscribeToAgreement(
    negotiationId: string,
    versionId: string,
    callback: AgreementUpdateCallback,
    onConnected?: ConnectionCallback,
  ): Promise<void> {
    const token = useAuth.getState().accessToken;

    if (!token) {
      throw new Error(
        "No access token available for agreement collaboration.",
      );
    }

    this.activeNegotiationId = negotiationId;
    this.activeVersionId = versionId;
    this.activeDocumentScope = "version";
    this.activeCallback = callback;
    this.activeConnectedCallback = onConnected ?? null;

    if (this.client?.connected) {
      this.subscribeCurrentAgreement();
      this.activeConnectedCallback?.();
      return;
    }

    const connectionPromise = new Promise<void>(
      (resolve, reject) => {
        this.connectionWaiters.push({ resolve, reject });
      },
    );

    if (!this.client) {
      this.createClient(token);
    }

    await connectionPromise;
  }

  /**
   * New negotiation-level subscription for the Current Deal form.
   * It does not require an AgreementVersion ID.
   */
  async subscribeToNegotiationDraft(
    negotiationId: string,
    callback: AgreementUpdateCallback,
    onConnected?: ConnectionCallback,
  ): Promise<void> {
    const token = useAuth.getState().accessToken;

    if (!token) {
      throw new Error(
        "No access token available for agreement collaboration.",
      );
    }

    this.activeNegotiationId = negotiationId;
    this.activeVersionId = null;
    this.activeDocumentScope = "negotiation";
    this.activeCallback = callback;
    this.activeConnectedCallback = onConnected ?? null;

    if (this.client?.connected) {
      this.subscribeCurrentAgreement();
      this.activeConnectedCallback?.();
      return;
    }

    const connectionPromise = new Promise<void>(
      (resolve, reject) => {
        this.connectionWaiters.push({ resolve, reject });
      },
    );

    if (!this.client) {
      this.createClient(token);
    }

    await connectionPromise;
  }

  private createClient(token: string): void {
    const client = new Client({
      webSocketFactory: () =>
        new SockJS("http://localhost:8084/ws"),

      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },

      reconnectDelay: 5000,

      debug: (message) => {
        console.log(
          "[AGREEMENT COLLABORATION STOMP]",
          message,
        );
      },

      onConnect: () => {
        console.log(
          "Agreement collaboration WebSocket connected.",
        );

        this.subscribeCurrentAgreement();

        this.resolveConnectionWaiters();

        /*
         * Called on initial connection and every reconnection.
         * The Yjs manager uses this to resend pending updates.
         */
        this.activeConnectedCallback?.();
      },

      onStompError: (frame) => {
        const message =
          frame.headers["message"] ??
          "Agreement collaboration STOMP error.";

        console.error(message);

        this.rejectConnectionWaiters(new Error(message));
      },

      onWebSocketError: (event) => {
        console.error(
          "Agreement collaboration WebSocket error.",
          event,
        );

        this.rejectConnectionWaiters(
          new Error(
            "Agreement collaboration connection failed.",
          ),
        );
      },

      onWebSocketClose: () => {
        console.warn(
          "Agreement collaboration WebSocket closed.",
        );

        this.subscription = null;
      },
    });

    this.client = client;
    client.activate();
  }

  private subscribeCurrentAgreement(): void {
    if (
      !this.client?.connected ||
      !this.activeNegotiationId ||
      !this.activeCallback ||
      !this.activeDocumentScope
    ) {
      return;
    }

    let destination: string;

    if (this.activeDocumentScope === "negotiation") {
      destination =
        `/topic/agreement-draft/${this.activeNegotiationId}`;
    } else {
      if (!this.activeVersionId) {
        return;
      }

      destination =
        `/topic/agreement/${this.activeNegotiationId}/${this.activeVersionId}`;
    }

    this.subscription?.unsubscribe();

    this.subscription = this.client.subscribe(
      destination,
      (message: IMessage) => {
        try {
          const update =
            JSON.parse(
              message.body,
            ) as AgreementCollaborationUpdateMessage;

          if (
            !update.clientUpdateId ||
            !update.updatePayload
          ) {
            console.error(
              "Invalid agreement collaboration update.",
            );
            return;
          }

          this.activeCallback?.(update);
        } catch (error) {
          console.error(
            "Could not parse agreement collaboration update.",
            error,
          );
        }
      },
    );

    console.log(
      "Subscribed to agreement collaboration:",
      destination,
    );
  }


  sendUpdate(
    update: AgreementCollaborationUpdateMessage,
  ): void {
    if (!this.activeNegotiationId || !this.activeDocumentScope) {
      throw new Error(
        "No agreement collaboration document is selected.",
      );
    }

    if (this.activeDocumentScope === "negotiation") {
      this.sendNegotiationDraftUpdate(
        this.activeNegotiationId,
        update,
      );
      return;
    }

    if (!this.activeVersionId) {
      throw new Error(
        "No agreement version collaboration document is selected.",
      );
    }

    this.sendAgreementVersionUpdate(
      this.activeNegotiationId,
      this.activeVersionId,
      update,
    );
  }

  sendAgreementVersionUpdate(
    negotiationId: string,
    versionId: string,
    update: AgreementCollaborationUpdateMessage,
  ): void {
    this.publishScopedUpdate(
      "version",
      negotiationId,
      versionId,
      update,
    );
  }

  sendNegotiationDraftUpdate(
    negotiationId: string,
    update: AgreementCollaborationUpdateMessage,
  ): void {
    this.publishScopedUpdate(
      "negotiation",
      negotiationId,
      null,
      update,
    );
  }

  private publishScopedUpdate(
    expectedScope: AgreementDocumentScope,
    negotiationId: string,
    versionId: string | null,
    update: AgreementCollaborationUpdateMessage,
  ): void {
    if (!this.client?.connected) {
      throw new Error(
        "Agreement collaboration WebSocket is not connected.",
      );
    }

    if (
      this.activeDocumentScope !== expectedScope ||
      this.activeNegotiationId !== negotiationId ||
      this.activeVersionId !== versionId
    ) {
      throw new Error(
        "This agreement collaboration document is not the active subscription.",
      );
    }

    const destination =
      expectedScope === "negotiation"
        ? `/app/agreement-draft/${negotiationId}/update`
        : `/app/agreement/${negotiationId}/${versionId}/update`;

    this.client.publish({
      destination,
      body: JSON.stringify(update),
    });
  }


  private resolveConnectionWaiters(): void {
    const waiters = this.connectionWaiters.splice(0);

    waiters.forEach((waiter) => waiter.resolve());
  }

  private rejectConnectionWaiters(error: Error): void {
    const waiters = this.connectionWaiters.splice(0);

    waiters.forEach((waiter) => waiter.reject(error));
  }

  /**
   * Disconnect only when the requested document still owns
   * the active subscription. This prevents an old manager's
   * cleanup from disconnecting a newer document subscription.
   */
  async disconnectForDocument(
    negotiationId: string,
    versionId: string | null,
  ): Promise<void> {
    const expectedScope: AgreementDocumentScope =
      versionId === null
        ? "negotiation"
        : "version";

    if (
      this.activeNegotiationId !== negotiationId ||
      this.activeDocumentScope !== expectedScope
    ) {
      return;
    }

    if (
      expectedScope === "version" &&
      this.activeVersionId !== versionId
    ) {
      return;
    }

    await this.disconnect();
  }

  async disconnect(): Promise<void> {
    this.subscription?.unsubscribe();
    this.subscription = null;

    this.activeNegotiationId = null;
    this.activeVersionId = null;
    this.activeDocumentScope = null;

    this.activeCallback = null;
    this.activeConnectedCallback = null;

    this.rejectConnectionWaiters(
      new Error(
        "Agreement collaboration disconnected.",
      ),
    );

    if (this.client) {
      const client = this.client;
      this.client = null;

      await client.deactivate();
    }
  }
}

export default new AgreementCollaborationWebSocketService();
