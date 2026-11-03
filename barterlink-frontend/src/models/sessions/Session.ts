import type SessionStatus from "./SessionStatus";
import ExchangeMode from "./ExchangeMode";
import SessionMode from "./SessionMode";

export default interface Session {

  sessionId: string;

  /**
 * Present when the session-dispute endpoint returns the created case reference.
 */
disputeId?: string | null;

  agreementId: string;

  negotiationId: string;

  requesterId: string;

  requesterName: string;

  providerId: string;

  providerName: string;

  status: SessionStatus;

  exchangeMode: ExchangeMode;

    // Session mode selection and mutual agreement
  requesterSelectedMode: SessionMode | null;

  providerSelectedMode: SessionMode | null;

  agreedSessionMode: SessionMode | null;

  requesterModeSelectedAt: string | null;

  providerModeSelectedAt: string | null;

  modeAgreedAt: string | null;

  // NEW
  providerStarted: boolean;

  requesterStarted: boolean;

  providerConfirmed: boolean;

  requesterConfirmed: boolean;

  startedAt: string | null;

  providerConfirmedAt: string | null;

  requesterConfirmedAt: string | null;

  providerCompletedAt: string | null;

  requesterCompletedAt: string | null;

  completedAt: string | null;

  createdAt: string;

}