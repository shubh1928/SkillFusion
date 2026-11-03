import apiClient from "@/config/ApiClient";

import type AgreementVersion from "@/models/agreement/AgreementVersion";
import type {
  CreateAgreementVersionPayload,
} from "@/models/agreement/AgreementVersion";

const versionsPath = (negotiationId: string) =>
  `/negotiations/${negotiationId}/versions`;

export const getAgreementVersions = async (
  negotiationId: string,
): Promise<AgreementVersion[]> => {
  const response = await apiClient.get(
    versionsPath(negotiationId),
  );

  return response.data.data;
};

export const createAgreementVersionDraft = async (
  negotiationId: string,
  payload: CreateAgreementVersionPayload,
): Promise<AgreementVersion> => {
  const response = await apiClient.post(
    versionsPath(negotiationId),
    payload,
  );

  return response.data.data;
};

export interface SubmitAgreementVersionPayload {
  agreedTerms?: string;
  agreedDeadline: string;
  changeSummary: string;

  milestones: Array<{
    milestoneId: string;
    milestoneNumber: number;
    title: string;
    deliverable: string;
    deadline: string;
    tokenAllocation: number;
  }>;
}

export const submitAgreementVersion = async (
  negotiationId: string,
  versionId: string,
  payload: SubmitAgreementVersionPayload,
): Promise<AgreementVersion> => {
  const response = await apiClient.post(
    `${versionsPath(negotiationId)}/${versionId}/submit`,
    payload,
  );

  return response.data.data;
};

export const acceptAgreementVersion = async (
  negotiationId: string,
  versionId: string,
): Promise<AgreementVersion> => {
  const response = await apiClient.post(
    `${versionsPath(negotiationId)}/${versionId}/accept`,
  );

  return response.data.data;
};

export const rejectAgreementVersion = async (
  negotiationId: string,
  versionId: string,
): Promise<AgreementVersion> => {
  const response = await apiClient.post(
    `${versionsPath(negotiationId)}/${versionId}/reject`,
  );

  return response.data.data;
};
