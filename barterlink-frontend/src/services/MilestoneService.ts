
import apiClient from "@/config/ApiClient";

import type {
  AgreementMilestoneStatus,
} from "@/models/agreement/AgreementVersion";

export interface MilestoneCompletionResult {
  milestoneId: string;
  milestoneNumber: number;
  status: AgreementMilestoneStatus;
  requesterConfirmed: boolean;
  providerConfirmed: boolean;
  settled: boolean;
  message: string;
}

export interface NextMilestoneFundingResult {
  milestoneFunded: boolean;
  allMilestonesReleased: boolean;
  message: string;
}

export const confirmMilestoneCompletion = async (
  milestoneId: string,
): Promise<MilestoneCompletionResult> => {
  const response = await apiClient.post(
    `/milestones/${milestoneId}/confirm-completion`,
  );

  return response.data.data;
};

export const retryNextMilestoneFunding = async (
  agreementId: string,
): Promise<NextMilestoneFundingResult> => {
  const response = await apiClient.post(
    `/milestones/agreements/${agreementId}/fund-next`,
  );

  return response.data.data;
};
