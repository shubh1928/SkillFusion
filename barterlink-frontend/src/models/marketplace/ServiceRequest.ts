import ServiceRequestStatus from "./ServiceRequestStatus";
import type TrustAssessment from "@/models/trust/TrustAssessment";

export type ExchangeMode =
  | "DIRECT_BARTER"
  | "TOKEN_BASED"
  | "HYBRID";

export default interface ServiceRequest {

  requestId: string;

  requesterId: string;

  requesterName: string;

  title: string;

  estimatedHours: number;

  exchangeMode: ExchangeMode;

  offerTitle: string;

  offerDescription: string;

  requestTitle: string;

  requestDescription: string;

  tokenAmount?: number;

  requesterHasSufficientTokens?: boolean | null;

  requiredSkills: string[];

  selectiveRequest: boolean;

  status: ServiceRequestStatus;

  createdAt: string;

  trustAssessment: TrustAssessment | null;
}