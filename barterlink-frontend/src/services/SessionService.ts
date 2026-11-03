import apiClient from "@/config/ApiClient";

import type Session from "@/models/sessions/Session";
import SessionMode from "@/models/sessions/SessionMode";
import type SessionRecordingConsentStatus from "@/models/sessions/SessionRecordingConsentStatus";
import type SessionRecordingReadinessStatus from
  "@/models/sessions/SessionRecordingReadinessStatus";

/**
 * Get sessions where current user is requester
 */
export const getRequesterSessions =
  async (): Promise<Session[]> => {

    const response = await apiClient.get(
      "/sessions/requester"
    );

    return response.data.data;

  };

/**
 * Get sessions where current user is provider
 */
export const getProviderSessions =
  async (): Promise<Session[]> => {

    const response = await apiClient.get(
      "/sessions/provider"
    );

    return response.data.data;

  };

/**
 * Get session by id
 */
export const getSession =
  async (
    sessionId: string
  ): Promise<Session> => {

    const response = await apiClient.get(
      `/sessions/${sessionId}`
    );

    return response.data.data;

  };


/**
 * Submit the current participant's session-mode selection.
 */
export const selectSessionMode = async (
  sessionId: string,
  selectedMode: SessionMode
): Promise<Session> => {
  const response = await apiClient.put(
    `/sessions/${sessionId}/mode`,
    { selectedMode }
  );

  return response.data.data;
};


/**
 * Submit the current participant's recording-consent decisions.
 */
export const submitRecordingConsent = async (
  sessionId: string,
  recordingConsent: boolean,
  educationalReuseConsent: boolean
): Promise<SessionRecordingConsentStatus> => {
  const response = await apiClient.post(
    `/sessions/${sessionId}/recording-consent`,
    {
      recordingConsent,
      educationalReuseConsent,
    }
  );

  return response.data.data;
};

/**
 * Retrieve the current recording-consent status for a session.
 */
export const getRecordingConsentStatus = async (
  sessionId: string
): Promise<SessionRecordingConsentStatus> => {
  const response = await apiClient.get(
    `/sessions/${sessionId}/recording-consent`
  );

  return response.data.data;
};


/**
 * Report the current participant's recorder-preparation status.
 *
 * The backend identifies the participant from authentication
 * and validates the session mode and recording consent.
 */
export const reportRecorderReady = async (
  sessionId: string
): Promise<SessionRecordingReadinessStatus> => {
  const response = await apiClient.post(
    `/sessions/${sessionId}/recording-readiness`
  );

  return response.data.data;
};


/**
 * Retrieve recorder-readiness status for the session.
 */
export const getRecordingReadiness = async (
  sessionId: string
): Promise<SessionRecordingReadinessStatus> => {
  const response = await apiClient.get(
    `/sessions/${sessionId}/recording-readiness`
  );

  return response.data.data;
};


/**
 * Provider starts the session
 */
export const startSession =
  async (
    sessionId: string
  ): Promise<Session> => {

    const response = await apiClient.put(
      `/sessions/${sessionId}/start`
    );

    return response.data.data;

  };

/**
 * Provider confirms completion
 */
export const providerConfirm =
  async (
    sessionId: string
  ): Promise<Session> => {

    const response = await apiClient.put(
      `/sessions/${sessionId}/provider-confirm`
    );

    return response.data.data;

  };

/**
 * Requester confirms completion
 */
export const requesterConfirm =
  async (
    sessionId: string
  ): Promise<Session> => {

    const response = await apiClient.put(
      `/sessions/${sessionId}/requester-confirm`
    );

    return response.data.data;

  };

/**
 * Dispute session
 */
/**
 * Open a dispute for a session.
 * The backend requires a written reason and returns the updated session,
 * including the persisted dispute ID.
 */
export const disputeSession = async (
  sessionId: string,
  reason: string,
): Promise<Session> => {
  const response = await apiClient.put(`/sessions/${sessionId}/dispute`, {
    reason,
  });

  return response.data.data;
};

/**
 * Cancel session
 */
export const cancelSession =
  async (
    sessionId: string
  ): Promise<Session> => {

    const response = await apiClient.put(
      `/sessions/${sessionId}/cancel`
    );

    return response.data.data;

  };