import type SessionStatus from "./SessionStatus";

export default interface Session {
  sessionId: string;

  agreementId: string;

  requesterId: string;

  requesterName: string;

  providerId: string;

  providerName: string;

  status: SessionStatus;

  providerConfirmed: boolean;

  requesterConfirmed: boolean;

  startedAt: string | null;

  providerConfirmedAt: string | null;

  requesterConfirmedAt: string | null;

  completedAt: string | null;

  createdAt: string;
}