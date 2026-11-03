export type ExchangeMode =
  | "DIRECT_BARTER"
  | "TOKEN_BASED"
  | "HYBRID";

export type AgreementReleaseMode =
  | "DIRECT_RELEASE"
  | "MILESTONE_RELEASE";

export default interface Agreement {
  agreementId: string;
  negotiationId: string;
  requestId: string;

  requesterId: string;
  requesterName: string;

  providerId: string;
  providerName: string;

  escrowLockedTokens: number | null;
  status: string;

  acceptedAt: string | null;
  completedAt?: string | null;

  offerTitle: string | null;
  offerDescription: string | null;

  requestTitle: string | null;
  requestDescription: string | null;

  exchangeMode: ExchangeMode;
  releaseMode: AgreementReleaseMode | null;

  estimatedHours: number | null;
  agreedTokenAmount: number | null;
}