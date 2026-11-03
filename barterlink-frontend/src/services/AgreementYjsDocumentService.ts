
import * as Y from "yjs";
import { IndexeddbPersistence } from "y-indexeddb";

import type AgreementVersion from "@/models/agreement/AgreementVersion";

import {
  getAgreementCollaborationUpdates,
  initializeAgreementCollaborationDocument,
  getNegotiationAgreementCollaborationUpdates,
  initializeNegotiationAgreementCollaborationDocument,
} from "@/services/AgreementCollaborationService";

import AgreementCollaborationWebSocketService from "@/services/AgreementCollaborationWebSocketService";

import type {
  AgreementCollaborationUpdateMessage,
} from "@/services/AgreementCollaborationService";

const REMOTE_ORIGIN = "remote-agreement-update";
const LOCAL_BATCH_DELAY = 150;
const MAX_PENDING_UPDATES = 100;

type SyncStatus =
  | "loading"
  | "connected"
  | "saving"
  | "saved"
  | "disconnected"
  | "error";

export interface AgreementYjsMilestone {
  milestoneId: string;
  milestoneNumber: number;
  title: string;
  deliverable: string;
  deadline: string;
  tokenAllocation: number;
}

export interface AgreementYjsSnapshot {
  agreedTerms: string;
  agreedDeadline: string;
  changeSummary: string;
  milestones: AgreementYjsMilestone[];
}

export interface AgreementYjsDocumentCallbacks {
  onChange?: (snapshot: AgreementYjsSnapshot) => void;
  onStatusChange?: (status: SyncStatus) => void;
  onError?: (error: Error) => void;
}

interface PendingUpdate {
  message: AgreementCollaborationUpdateMessage;
  sent: boolean;
}

const FIELD_NAMES = {
  agreedTerms: "field:agreedTerms",
  agreedDeadline: "field:agreedDeadline",
  changeSummary: "field:changeSummary",
};

const milestoneField = (
  milestoneId: string,
  field: string,
) => `milestone:${milestoneId}:${field}`;

function bytesToBase64(bytes: Uint8Array): string {
  let binary = "";

  const chunkSize = 0x8000;

  for (let i = 0; i < bytes.length; i += chunkSize) {
    const chunk = bytes.subarray(
      i,
      Math.min(i + chunkSize, bytes.length),
    );

    binary += String.fromCharCode(...chunk);
  }

  return btoa(binary);
}

function base64ToBytes(value: string): Uint8Array {
  const binary = atob(value);
  const bytes = new Uint8Array(binary.length);

  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }

  return bytes;
}

function createSeedDocument(
  version: AgreementVersion,
): Y.Doc {
  const seedDoc = new Y.Doc();

  seedDoc.transact(() => {
    const fields = [
      {
        key: FIELD_NAMES.agreedTerms,
        value: version.agreedTerms ?? "",
      },
      {
        key: FIELD_NAMES.agreedDeadline,
        value: version.agreedDeadline ?? "",
      },
      {
        key: FIELD_NAMES.changeSummary,
        value: version.changeSummary ?? "",
      },
    ];

    fields.forEach(({ key, value }) => {
      if (value) {
        seedDoc.getText(key).insert(0, value);
      }
    });

    const order = seedDoc.getArray<string>(
      "milestoneOrder",
    );

    version.milestones.forEach((milestone) => {
      const milestoneId = milestone.milestoneId;

      order.push([milestoneId]);

      const values: Array<[string, string]> = [
        [
          "milestoneNumber",
          String(milestone.milestoneNumber),
        ],
        ["title", milestone.title ?? ""],
        ["deliverable", milestone.deliverable ?? ""],
        ["deadline", milestone.deadline ?? ""],
        [
          "tokenAllocation",
          String(milestone.tokenAllocation ?? 0),
        ],
      ];

      values.forEach(([field, value]) => {
        if (value) {
          seedDoc
            .getText(milestoneField(milestoneId, field))
            .insert(0, value);
        }
      });
    });
  });

  return seedDoc;
}


function createNegotiationSeedDocument(
  snapshot: AgreementYjsSnapshot,
): Y.Doc {
  const seedDoc = new Y.Doc();

  seedDoc.transact(() => {
    const fields = [
      {
        key: FIELD_NAMES.agreedTerms,
        value: snapshot.agreedTerms ?? "",
      },
      {
        key: FIELD_NAMES.agreedDeadline,
        value: snapshot.agreedDeadline ?? "",
      },
      {
        key: FIELD_NAMES.changeSummary,
        value: snapshot.changeSummary ?? "",
      },
    ];

    fields.forEach(({ key, value }) => {
      if (value) {
        seedDoc.getText(key).insert(0, value);
      }
    });

    const order = seedDoc.getArray<string>(
      "milestoneOrder",
    );

    snapshot.milestones.forEach((milestone) => {
      if (!milestone.milestoneId) {
        throw new Error(
          "Every shared milestone requires a stable milestone ID.",
        );
      }

      order.push([milestone.milestoneId]);

      const values: Array<[string, string]> = [
        [
          "milestoneNumber",
          String(milestone.milestoneNumber),
        ],
        ["title", milestone.title ?? ""],
        ["deliverable", milestone.deliverable ?? ""],
        ["deadline", milestone.deadline ?? ""],
        [
  "tokenAllocation",
  milestone.tokenAllocation > 0
    ? String(milestone.tokenAllocation)
    : "",
],
      ];

      values.forEach(([field, value]) => {
        if (value) {
          seedDoc
            .getText(
              milestoneField(
                milestone.milestoneId,
                field,
              ),
            )
            .insert(0, value);
        }
      });
    });
  });

  return seedDoc;
}


export class AgreementYjsDocumentService {
  readonly doc: Y.Doc;

  private readonly persistence: IndexeddbPersistence;

private readonly negotiationId: string;
private readonly versionId: string | null;
private readonly documentScope: "version" | "negotiation";

private callbacks: AgreementYjsDocumentCallbacks;

  private ready = false;
  private destroyed = false;

  private status: SyncStatus = "loading";

  private updateBuffer: Uint8Array[] = [];

  private pendingUpdates = new Map<
    string,
    PendingUpdate
  >();

  private batchTimer: ReturnType<typeof setTimeout> | null =
    null;

  private unsubscribeUpdate?: () => void;


constructor(
  negotiationId: string,
  versionId: string | null,
  callbacks: AgreementYjsDocumentCallbacks = {},
  documentScope: "version" | "negotiation" = "version",
) {
  if (documentScope === "version" && !versionId) {
    throw new Error(
      "A version ID is required for version-level collaboration.",
    );
  }

  if (documentScope === "negotiation" && versionId !== null) {
    throw new Error(
      "Negotiation-level collaboration must not use a version ID.",
    );
  }

this.negotiationId = negotiationId;
this.versionId = versionId;
this.documentScope = documentScope;
this.callbacks = callbacks;

  const documentKey =
    documentScope === "negotiation"
      ? `skillfusion-agreement-${negotiationId}-current-deal`
      : `skillfusion-agreement-${negotiationId}-${versionId}`;

  this.doc = new Y.Doc({
    guid: documentKey,
  });

  this.persistence = new IndexeddbPersistence(
    documentKey,
    this.doc,
  );

  this.doc.on("update", this.handleDocumentUpdate);
}


  static async open(
    negotiationId: string,
    version: AgreementVersion,
    callbacks: AgreementYjsDocumentCallbacks = {},
  ): Promise<AgreementYjsDocumentService> {
    const manager = new AgreementYjsDocumentService(
      negotiationId,
      version.versionId,
      callbacks,
    );

    try {
      await manager.initialize(version);
      return manager;
    } catch (error) {
      await manager.destroy();

      throw error instanceof Error
        ? error
        : new Error(
            "Could not initialize the collaborative agreement.",
          );
    }
  }


  
static async openNegotiationDraft(
  negotiationId: string,
  initialSnapshot: AgreementYjsSnapshot,
  callbacks: AgreementYjsDocumentCallbacks = {},
): Promise<AgreementYjsDocumentService> {
  const manager = new AgreementYjsDocumentService(
    negotiationId,
    null,
    callbacks,
    "negotiation",
  );

  try {
    await manager.initializeNegotiationDraft(
      initialSnapshot,
    );

    return manager;
  } catch (error) {
    await manager.destroy();

    throw error instanceof Error
      ? error
      : new Error(
          "Could not initialize the shared Current Deal document.",
        );
  }
}


  private setStatus(status: SyncStatus): void {
    if (this.destroyed) {
      return;
    }

    this.status = status;
    this.callbacks.onStatusChange?.(status);
  }

  getStatus(): SyncStatus {
    return this.status;
  }

  private async initialize(
    version: AgreementVersion,
  ): Promise<void> {

const versionId = this.versionId;
if (!versionId) {
  throw new Error(
    "Version-level collaboration requires a version ID.",
  );
}

    this.setStatus("loading");

    /*
     * 1. Restore browser-local Yjs data first.
     */
    await this.persistence.whenSynced;

    /*
     * 2. Subscribe before retrieving server updates.
     * Any concurrent update is received here or by the
     * subsequent REST retrieval.
     */
    await AgreementCollaborationWebSocketService.subscribeToAgreement(
      this.negotiationId,
      versionId,
      (message) => this.handleRemoteUpdate(message),
      () => this.handleReconnection(),
    );

    /*
     * 3. Retrieve the authoritative server update history.
     */
    let serverUpdates =
      await getAgreementCollaborationUpdates(
        this.negotiationId,
        versionId,
      );

    /*
     * 4. Initialize the common starting document only
     * when the backend does not already have one.
     *
     * A temporary Y.Doc prevents both participants from
     * independently inserting duplicate seed content.
     */
    const hasInitialUpdate = serverUpdates.some(
      (update) => update.clientUpdateId === "INITIAL",
    );

    if (!hasInitialUpdate) {
      const seedDoc = createSeedDocument(version);

      try {
        const initialPayload = bytesToBase64(
          Y.encodeStateAsUpdate(seedDoc),
        );

        await initializeAgreementCollaborationDocument(
          this.negotiationId,
         versionId,
          {
            clientUpdateId: "INITIAL",
            updatePayload: initialPayload,
          },
        );
      } finally {
        seedDoc.destroy();
      }

      serverUpdates =
        await getAgreementCollaborationUpdates(
          this.negotiationId,
          versionId,
        );
    }

    /*
     * 5. Build a separate server document so we can
     * identify browser-local changes that have not yet
     * reached MySQL.
     */
    const serverDoc = new Y.Doc();

    try {
      serverUpdates.forEach((update) => {
        Y.applyUpdate(
          serverDoc,
          base64ToBytes(update.updatePayload),
          REMOTE_ORIGIN,
        );

        Y.applyUpdate(
          this.doc,
          base64ToBytes(update.updatePayload),
          REMOTE_ORIGIN,
        );
      });

      /*
       * 6. Compute changes present locally but absent
       * from the server state.
       *
       * This recovers changes made during an offline
       * period or before a previous connection closed.
       */
      const serverStateVector =
  Y.encodeStateVector(serverDoc);

/*
 * Always calculate the local changes missing from the server.
 * This also accounts for deletion information.
 */
const recoveryUpdate = Y.encodeStateAsUpdate(
  this.doc,
  serverStateVector,
);

if (recoveryUpdate.length > 2) {
  this.queueUpdate(recoveryUpdate);
}
    } finally {
      serverDoc.destroy();
    }

    this.callbacks.onChange?.(
  this.getSnapshot(),
);

    /*
     * 7. Only now start observing local document changes.
     * Initial loading and remote updates will not be
     * accidentally sent back as new local edits.
     */
    this.unsubscribeUpdate = () => {
      this.doc.off(
        "update",
        this.handleDocumentUpdate,
      );
    };

    this.ready = true;

    this.setStatus(
      this.updateBuffer.length > 0
        ? "saving"
        : "connected",
    );

    /*
     * Send any changes recovered from local persistence.
     */
    this.flushPendingUpdates();
  }


  
private async initializeNegotiationDraft(
  initialSnapshot: AgreementYjsSnapshot,
): Promise<void> {
  this.setStatus("loading");

  /*
   * 1. Restore browser-local Yjs data.
   */
  await this.persistence.whenSynced;

  /*
   * 2. Subscribe to the shared Current Deal document.
   */
  await AgreementCollaborationWebSocketService.subscribeToNegotiationDraft(
    this.negotiationId,
    (message) => this.handleRemoteUpdate(message),
    () => this.handleReconnection(),
  );

  /*
   * 3. Retrieve the authoritative server updates.
   */
  let serverUpdates =
    await getNegotiationAgreementCollaborationUpdates(
      this.negotiationId,
    );

  /*
   * 4. Initialize the common document only once.
   */
  const hasInitialUpdate = serverUpdates.some(
    (update) => update.clientUpdateId === "INITIAL",
  );

  if (!hasInitialUpdate) {
    const seedDoc =
      createNegotiationSeedDocument(initialSnapshot);

    try {
      const initialPayload = bytesToBase64(
        Y.encodeStateAsUpdate(seedDoc),
      );

      await initializeNegotiationAgreementCollaborationDocument(
        this.negotiationId,
        {
          clientUpdateId: "INITIAL",
          updatePayload: initialPayload,
        },
      );
    } finally {
      seedDoc.destroy();
    }

    serverUpdates =
      await getNegotiationAgreementCollaborationUpdates(
        this.negotiationId,
      );
  }

  /*
   * 5. Apply authoritative server updates to the
   * local document and a separate server document.
   */
  const serverDoc = new Y.Doc();

  try {
    serverUpdates.forEach((update) => {
      const bytes = base64ToBytes(
        update.updatePayload,
      );

      Y.applyUpdate(
        serverDoc,
        bytes,
        REMOTE_ORIGIN,
      );

      Y.applyUpdate(
        this.doc,
        bytes,
        REMOTE_ORIGIN,
      );
    });

    /*
     * 6. Recover browser-local changes missing
     * from the server document.
     */
    const serverStateVector =
      Y.encodeStateVector(serverDoc);

    const recoveryUpdate = Y.encodeStateAsUpdate(
      this.doc,
      serverStateVector,
    );

    if (recoveryUpdate.length > 2) {
      this.queueUpdate(recoveryUpdate);
    }
  } finally {
    serverDoc.destroy();
  }

  this.callbacks.onChange?.(
  this.getSnapshot(),
);

  /*
   * 7. Enable normal local change synchronization.
   */
  this.unsubscribeUpdate = () => {
    this.doc.off(
      "update",
      this.handleDocumentUpdate,
    );
  };

  this.ready = true;

  this.setStatus(
    this.updateBuffer.length > 0
      ? "saving"
      : "connected",
  );

  this.flushPendingUpdates();
}


  private handleDocumentUpdate = (
    update: Uint8Array,
    origin: unknown,
  ): void => {
    if (
      this.destroyed ||
      !this.ready ||
      origin === REMOTE_ORIGIN
    ) {
      return;
    }

    this.queueUpdate(update);

    this.callbacks.onChange?.(
      this.getSnapshot(),
    );
  };

  private handleRemoteUpdate(
    message: AgreementCollaborationUpdateMessage,
  ): void {
    if (this.destroyed) {
      return;
    }

    /*
     * The sender receives the same broadcast as the other
     * participant. Remove its pending update when that
     * broadcast confirms that MySQL accepted it.
     */
    const wasPending =
      this.pendingUpdates.delete(
        message.clientUpdateId,
      );

    if (wasPending && this.isFullySynchronized()) {
      this.setStatus("saved");
    }

    try {
      Y.applyUpdate(
        this.doc,
        base64ToBytes(message.updatePayload),
        REMOTE_ORIGIN,
      );

      this.callbacks.onChange?.(
  this.getSnapshot(),
);
    } catch (error) {
      this.callbacks.onError?.(
        error instanceof Error
          ? error
          : new Error(
              "Could not apply remote agreement changes.",
            ),
      );

      this.setStatus("error");
    }
  }

  private queueUpdate(update: Uint8Array): void {
    if (this.destroyed) {
      return;
    }

    if (this.updateBuffer.length >= MAX_PENDING_UPDATES) {
      this.flushUpdateBuffer();
    }

    this.updateBuffer.push(update);

    this.setStatus("saving");

    if (this.batchTimer) {
      clearTimeout(this.batchTimer);
    }

    this.batchTimer = setTimeout(
      () => this.flushUpdateBuffer(),
      LOCAL_BATCH_DELAY,
    );
  }

  private flushUpdateBuffer(): void {
    if (
      this.destroyed ||
      this.updateBuffer.length === 0
    ) {
      return;
    }

    if (this.batchTimer) {
      clearTimeout(this.batchTimer);
      this.batchTimer = null;
    }

    const updates = this.updateBuffer.splice(0);

    const mergedUpdate = Y.mergeUpdates(updates);

    const clientUpdateId = crypto.randomUUID();

    this.pendingUpdates.set(clientUpdateId, {
      message: {
        clientUpdateId,
        updatePayload: bytesToBase64(mergedUpdate),
      },
      sent: false,
    });

    this.flushPendingUpdates();
  }

  private flushPendingUpdates(): void {
    if (
      this.destroyed ||
      !this.ready
    ) {
      return;
    }

    if (this.updateBuffer.length > 0) {
      this.flushUpdateBuffer();
      return;
    }

    for (const pending of this.pendingUpdates.values()) {
      if (pending.sent) {
        continue;
      }

      try {
        if (this.documentScope === "negotiation") {
  AgreementCollaborationWebSocketService.sendNegotiationDraftUpdate(
    this.negotiationId,
    pending.message,
  );
} else {
  const versionId = this.versionId;

  if (!versionId) {
    throw new Error(
      "Version-level collaboration requires a version ID.",
    );
  }

  AgreementCollaborationWebSocketService.sendAgreementVersionUpdate(
    this.negotiationId,
    versionId,
    pending.message,
  );
}

        pending.sent = true;

      } catch {
        /*
         * Keep the update pending.
         * The reconnect callback will retry it.
         */
        this.setStatus("disconnected");
        return;
      }
    }

    if (this.pendingUpdates.size > 0) {
      this.setStatus("saving");
    }
  }

  private handleReconnection = (): void => {
    if (!this.ready || this.destroyed) {
      return;
    }

    /*
     * Messages without a server broadcast acknowledgement
     * are resent using their original update identifiers.
     * The backend deduplicates repeated identifiers.
     */
    for (const pending of this.pendingUpdates.values()) {
      pending.sent = false;
    }

    this.flushPendingUpdates();

    if (this.isFullySynchronized()) {
      this.setStatus("connected");
    }
  };

  private isFullySynchronized(): boolean {
    return (
      this.updateBuffer.length === 0 &&
      this.pendingUpdates.size === 0
    );
  }

  getSnapshot(): AgreementYjsSnapshot {
    const agreedTerms = this.doc
      .getText(FIELD_NAMES.agreedTerms)
      .toString();

    const agreedDeadline = this.doc
      .getText(FIELD_NAMES.agreedDeadline)
      .toString();

    const changeSummary = this.doc
      .getText(FIELD_NAMES.changeSummary)
      .toString();

    const order = this.doc
      .getArray<string>("milestoneOrder")
      .toArray();

    const milestones = order.map((milestoneId) => {
      const read = (field: string) =>
        this.doc
          .getText(
            milestoneField(milestoneId, field),
          )
          .toString();

      return {
        milestoneId,
        milestoneNumber: Number(
          read("milestoneNumber"),
        ),
        title: read("title"),
        deliverable: read("deliverable"),
        deadline: read("deadline"),
        tokenAllocation: Number(
          read("tokenAllocation"),
        ),
      };
    });

    return {
      agreedTerms,
      agreedDeadline,
      changeSummary,
      milestones,
    };
  }

  /*
   * Returns a Y.Text object for a collaboratively editable field.
   * React components should observe this shared text instead of
   * maintaining an independent string as the source of truth.
   */
  getTextField(
    field: keyof typeof FIELD_NAMES,
  ): Y.Text {
    return this.doc.getText(
      FIELD_NAMES[field],
    );
  }

  getMilestoneTextField(
    milestoneId: string,
    field:
      | "title"
      | "deliverable"
      | "deadline"
      | "tokenAllocation"
      | "milestoneNumber",
  ): Y.Text {
    return this.doc.getText(
      milestoneField(milestoneId, field),
    );
  }

  async destroy(): Promise<void> {
    if (this.destroyed) {
      return;
    }

    this.destroyed = true;
    this.ready = false;

    if (this.batchTimer) {
      clearTimeout(this.batchTimer);
      this.batchTimer = null;
    }

    /*
     * Preserve pending updates in IndexedDB.
     * They are recovered by comparing state vectors
     * when the agreement is opened again.
     */

    this.unsubscribeUpdate?.();

    await AgreementCollaborationWebSocketService.disconnectForDocument(
  this.negotiationId,
  this.versionId,
);

    await this.persistence.destroy();

    this.doc.destroy();

    this.pendingUpdates.clear();
    this.updateBuffer = [];
  }
}
