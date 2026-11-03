import apiClient from "@/config/ApiClient";

import type {
  AssignedDisputeSummary,
  CastDisputeVoteRequest,
  DisputeCaseReview,
  DisputeVoteResponse,
  ParticipantDisputeSummary,
} from "@/models/disputes/DisputeTypes";

import type ApiResponse from "@/models/ApiResponse";

/**
 * Get dispute cases assigned to the authenticated panelist.
 *
 * Backend:
 * GET /api/v1/disputes/assigned
 */
export const getAssignedDisputes = async (): Promise<
  AssignedDisputeSummary[]
> => {
  const response = await apiClient.get<
    ApiResponse<AssignedDisputeSummary[]>
  >("/disputes/assigned");

  return response.data.data ?? [];
};

/**
 * Get the review information and evidence manifest for an assigned case.
 *
 * Backend:
 * GET /api/v1/disputes/{disputeId}/review
 */
export const getDisputeReview = async (
  disputeId: string,
): Promise<DisputeCaseReview> => {
  const response = await apiClient.get<ApiResponse<DisputeCaseReview>>(
    `/disputes/${disputeId}/review`,
  );

  return response.data.data;
};

/**
 * Submit a vote for the authenticated panelist.
 *
 * Backend:
 * POST /api/v1/disputes/{disputeId}/votes
 *
 * The backend remains responsible for checking assignment, case status,
 * evidence version and whether a final decision should be declared.
 */
export const castDisputeVote = async (
  disputeId: string,
  request: CastDisputeVoteRequest,
): Promise<DisputeVoteResponse> => {
  const response = await apiClient.post<ApiResponse<DisputeVoteResponse>>(
    `/disputes/${disputeId}/votes`,
    request,
  );

  return response.data.data;
};


/**
 * Retrieves disputes in which the authenticated user is the
 * claimant or respondent.
 *
 * Backend:
 * GET /api/v1/disputes/mine
 */
export const getMyDisputes = async (): Promise<
  ParticipantDisputeSummary[]
> => {
  const response = await apiClient.get<
    ApiResponse<ParticipantDisputeSummary[]>
  >("/disputes/mine");

  return response.data.data ?? [];
};