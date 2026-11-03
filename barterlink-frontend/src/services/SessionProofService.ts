
import apiClient from "@/config/ApiClient";

import type {
  SessionProof,
  UploadSessionProofRequest,
} from "@/models/sessions/SessionProof";

/**
 * Upload a file attachment or submit an external-link proof.
 */
export const uploadSessionProof = async (
  sessionId: string,
  request: UploadSessionProofRequest,
  file?: File,
): Promise<SessionProof> => {
  const formData = new FormData();

  formData.append(
    "request",
    new Blob([JSON.stringify(request)], {
      type: "application/json",
    }),
  );

  if (file) {
    formData.append("file", file);
  }

  const response = await apiClient.post(
    `/session-proofs/${sessionId}`,
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    },
  );

  return response.data.data;
};

/**
 * Retrieve proof metadata for a session.
 */
export const getSessionProofs = async (
  sessionId: string,
): Promise<SessionProof[]> => {
  const response = await apiClient.get(
    `/session-proofs/${sessionId}`,
  );

  return response.data.data;
};

/**
 * Download an uploaded attachment through the authenticated API.
 */
export const downloadSessionProof = async (
  proofId: string,
): Promise<Blob> => {
  const response = await apiClient.get(
    `/session-proofs/${proofId}/content`,
    {
      responseType: "blob",
    },
  );

  return response.data;
};

/**
 * Delete a proof submitted by the current user.
 */
export const deleteSessionProof = async (
  proofId: string,
): Promise<void> => {
  await apiClient.delete(
    `/session-proofs/${proofId}`,
  );
};
