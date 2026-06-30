import apiClient from "@/config/ApiClient";

import type Agreement from "@/models/agreement/Agreement";

/**
 * Get agreements where current user is requester
 */
export const getRequesterAgreements =
  async (): Promise<Agreement[]> => {

    const response = await apiClient.get(
      "/agreements/requester"
    );

    return response.data.data;

  };

/**
 * Get agreements where current user is provider
 */
export const getProviderAgreements =
  async (): Promise<Agreement[]> => {

    const response = await apiClient.get(
      "/agreements/provider"
    );

    return response.data.data;

  };

/**
 * Get agreement by id
 */
export const getAgreement =
  async (
    agreementId: string
  ): Promise<Agreement> => {

    const response = await apiClient.get(
      `/agreements/${agreementId}`
    );

    return response.data.data;

  };