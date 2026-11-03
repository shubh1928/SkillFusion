
import type NegotiationStatus
from "./NegotiationStatus";

import type {
  AgreementReleaseMode,
} from "@/models/agreement/AgreementVersion";

export type ExchangeMode =
  | "DIRECT_BARTER"
  | "TOKEN_BASED"
  | "HYBRID";

export default interface Negotiation {
  negotiationId: string;
  requestId: string;
  requesterId: string;
  providerId: string;
  status: NegotiationStatus;

  offerTitle: string;
  offerDescription: string;
  requestTitle: string;
  requestDescription: string;

  exchangeMode: ExchangeMode;
  estimatedHours: number;
  tokenAmount?: number;
  createdAt: string;

  requesterReleaseMode?: AgreementReleaseMode | null;
  providerReleaseMode?: AgreementReleaseMode | null;

  requesterMilestoneCount?: number | null;
  providerMilestoneCount?: number | null;

  releaseModeSelectionRequired?: boolean | null;
}
