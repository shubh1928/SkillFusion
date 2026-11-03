
import apiClient from "@/config/ApiClient";

export interface AgreementCollaborationUpdate {
  clientUpdateId: string;
  senderId: string;
  updatePayload: string;
  createdAt: string;
}

export interface AgreementCollaborationUpdateMessage {
  clientUpdateId: string;
  updatePayload: string;
}

export interface InitializeAgreementDocumentPayload {
  clientUpdateId: string;
  updatePayload: string;
}

/**
 * Initializes the shared version-level agreement document.
 */
export const initializeAgreementCollaborationDocument = async (
  negotiationId: string,
  versionId: string,
  payload: InitializeAgreementDocumentPayload,
): Promise<AgreementCollaborationUpdate> => {
  const response = await apiClient.post(
    `/negotiations/${negotiationId}/versions/${versionId}/collaboration/initialize`,
    payload,
  );

  return response.data.data;
};

/**
 * Retrieves persisted version-level Yjs updates.
 */
export const getAgreementCollaborationUpdates = async (
  negotiationId: string,
  versionId: string,
): Promise<AgreementCollaborationUpdate[]> => {
  const response = await apiClient.get(
    `/negotiations/${negotiationId}/versions/${versionId}/collaboration/updates`,
  );

  return response.data.data;
};


/**
 * Initializes the shared Current Deal document.
 * This document exists before an AgreementVersion is created.
 */
export const initializeNegotiationAgreementCollaborationDocument = async (
  negotiationId: string,
  payload: InitializeAgreementDocumentPayload,
): Promise<AgreementCollaborationUpdate> => {
  const response = await apiClient.post(
    `/negotiations/${negotiationId}/collaboration/initialize`,
    payload,
  );

  return response.data.data;
};

/**
 * Retrieves persisted Current Deal Yjs updates.
 */
export const getNegotiationAgreementCollaborationUpdates = async (
  negotiationId: string,
): Promise<AgreementCollaborationUpdate[]> => {
  const response = await apiClient.get(
    `/negotiations/${negotiationId}/collaboration/updates`,
  );

  return response.data.data;
};
