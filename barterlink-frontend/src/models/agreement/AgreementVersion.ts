
export type AgreementReleaseMode =
  | "DIRECT_RELEASE"
  | "MILESTONE_RELEASE";

export type AgreementVersionStatus =
  | "DRAFT"
  | "PENDING_ACCEPTANCE"
  | "READY_FOR_ACTIVATION"
  | "FUNDING_FAILED"
  | "ACTIVE"
  | "SUPERSEDED"
  | "REJECTED";

export type AgreementMilestoneStatus =
  | "PLANNED"
  | "FUNDING_PENDING"
  | "FUNDING_FAILED"
  | "FUNDED"
  | "SUBMITTED"
  | "CORRECTION_REQUESTED"
  | "DISPUTED"
  | "RELEASE_PENDING"
  | "RELEASED"
  | "REFUND_PENDING"
  | "REFUNDED"
  | "CANCELLED";

export interface AgreementVersionMilestone {
  versionMilestoneId: string;
  milestoneId: string;
  milestoneNumber: number;
  title: string;
  deliverable: string;
  deadline: string;
  tokenAllocation: number;
  executionStatus: AgreementMilestoneStatus;
}

export default interface AgreementVersion {
  versionId: string;
  negotiationId: string;
  versionNumber: number;
  status: AgreementVersionStatus;
  exchangeMode: "DIRECT_BARTER" | "TOKEN_BASED" | "HYBRID";
  releaseMode: AgreementReleaseMode | null;

  offerTitle?: string | null;
  offerDescription?: string | null;
  requestTitle: string;
  requestDescription: string;
  agreedTerms?: string | null;

  estimatedHours: number;
  agreedTokenAmount: number;
  agreedDeadline: string;

  createdById: string;
  submittedById?: string | null;
  changeSummary: string;
  createdAt: string;

  submittedAt?: string | null;
  requesterAcceptedAt?: string | null;
  providerAcceptedAt?: string | null;
  activatedAt?: string | null;
  supersededAt?: string | null;
  activationFailureReason?: string | null;

  milestones: AgreementVersionMilestone[];
}

export interface CreateAgreementVersionPayload {
  exchangeMode: "DIRECT_BARTER" | "TOKEN_BASED" | "HYBRID";
  offerTitle?: string;
  offerDescription?: string;
  requestTitle: string;
  requestDescription: string;
  agreedTerms?: string;
  estimatedHours: number;
  agreedTokenAmount: number;
  agreedDeadline: string;
  changeSummary: string;

  milestones: Array<{
    milestoneId?: string;
    milestoneNumber: number;
    title: string;
    deliverable: string;
    deadline: string;
    tokenAllocation: number;
  }>;
}
