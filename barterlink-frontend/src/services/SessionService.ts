import apiClient from "@/config/ApiClient";

import type Session from "@/models/sessions/Session";

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
export const disputeSession =
  async (
    sessionId: string
  ): Promise<Session> => {

    const response = await apiClient.put(
      `/sessions/${sessionId}/dispute`
    );

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