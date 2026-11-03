export type DisputeDecisionOutcome =
  | "REQUESTER_FAVORED"
  | "PROVIDER_FAVORED";

export type DisputeCaseStatus =
  | "PANEL_ASSIGNMENT_PENDING"
  | "EVIDENCE_OPEN"
  | "VOTING"
  | "DECIDED"
  | "SETTLEMENT_PENDING"
  | "RESOLVED";

export type DisputePanelAssignmentStatus =
  | "ASSIGNED"
  | "RECUSED"
  | "REPLACED"
  | "VOTED";

export type DisputeEvidenceType =
  | "SESSION_PROOF"
  | "NEGOTIATION_MESSAGE"
  | "AGREEMENT_VERSION";

export type ParticipantSettlementStatus =
  | "AWAITING_DECISION"
  | "NOT_APPLICABLE"
  | "UNSUPPORTED"
  | "PENDING"
  | "FAILED"
  | "BLOCKCHAIN_CONFIRMED"
  | "CONFIRMED"
  | "UNVERIFIED";

export interface AnonymousPanelVoteRationale {
  outcome: DisputeDecisionOutcome;
  reason: string;
}

export interface ParticipantDisputeSummary {
  disputeId: string;
  sessionId: string;
  agreementId: string;

  currentUserRole: "CLAIMANT" | "RESPONDENT" | string;

  claimantName: string;
  respondentName: string;

  requesterName: string;
  providerName: string;

  reason: string;

  status: DisputeCaseStatus;
  evidenceVersion: number;

  createdAt: string;
  panelAssignedAt: string | null;

  decisionOutcome: DisputeDecisionOutcome | null;
  decisionSummary: string | null;
  decisionDeclaredAt: string | null;

  panelSize: number;
  votesSubmitted: number;

  // These stay null while voting is open.
  requesterVotes: number | null;
  providerVotes: number | null;

  anonymousVoteRationales:
    | AnonymousPanelVoteRationale[]
    | null;

  exchangeMode: string | null;
  releaseMode: string | null;
  tokenAmount: number | null;

  settlementStatus: ParticipantSettlementStatus;
  settlementDirection: string | null;
  settlementRecipientName: string | null;
  settlementMessage: string;

  blockchainTransactionStatus: string | null;
  blockchainTransactionHash: string | null;
  blockchainTransactionUrl: string | null;
}

export interface AssignedDisputeSummary {
  disputeId: string;
  sessionId: string;
  agreementId: string;

  claimantName: string;
  respondentName: string;
  reason: string;

  caseStatus: DisputeCaseStatus;
  evidenceVersion: number | null;

  panelSeatNumber: number;
  assignmentStatus: DisputePanelAssignmentStatus;

  createdAt: string;
  panelAssignedAt: string | null;

  decisionOutcome: DisputeDecisionOutcome | null;
  decisionSummary: string | null;
  decisionDeclaredAt: string | null;
}

export interface DisputeEvidenceItem {
  evidenceReferenceId: string;
  evidenceType: DisputeEvidenceType;
  attachedAt: string | null;

  sourceId: string | null;
  title: string | null;
  content: string | null;
  sourceStatus: string | null;
  sourceCreatedAt: string | null;

  submittedByName: string | null;

  proofType: string | null;
  proofUrl: string | null;
  fileName: string | null;
  contentType: string | null;
  fileSize: number | null;
  sha256: string | null;

  attachmentUrl: string | null;
  systemGenerated: boolean | null;

  versionNumber: number | null;
}

export interface DisputeCaseReview {
  disputeId: string;
  sessionId: string;
  agreementId: string;

  claimantId: string;
  claimantName: string;

  respondentId: string;
  respondentName: string;

  reason: string;
  status: DisputeCaseStatus;

  evidenceVersion: number | null;
  createdAt: string;
  panelAssignedAt: string | null;

  decisionOutcome: DisputeDecisionOutcome | null;
  decisionSummary: string | null;
  decisionDeclaredAt: string | null;

  panelSeatNumber: number;

  currentUserHasVoted: boolean;
  currentUserVoteOutcome: DisputeDecisionOutcome | null;
  currentUserVoteReason: string | null;
  currentUserVotedAt: string | null;

  evidence: DisputeEvidenceItem[] | null;
}

export interface CastDisputeVoteRequest {
  outcome: DisputeDecisionOutcome;
  reason: string;
}

export interface DisputeVoteResponse {
  voteId: string;
  disputeId: string;

  outcome: DisputeDecisionOutcome;
  reason: string;
  evidenceVersion: number;
  createdAt: string;

  caseStatus: DisputeCaseStatus;
  decisionOutcome: DisputeDecisionOutcome | null;
  decisionSummary: string | null;
  decisionDeclaredAt: string | null;

  requesterVotes: number;
  providerVotes: number;
}