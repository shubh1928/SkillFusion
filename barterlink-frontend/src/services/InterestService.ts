import apiClient from "@/config/ApiClient";

import type Interest
from "@/models/interest/Interest";

/**
 * Express interest in a marketplace request
 */
export const expressInterest = async (
  requestId: string
): Promise<Interest> => {

  const response =
    await apiClient.post(
      `/interests/request/${requestId}`
    );

  return response.data.data;

};

/**
 * Get all interests for a marketplace request
 */
export const getRequestInterests = async (
  requestId: string
) => {

  const response = await apiClient.get(
    `/interests/request/${requestId}`
  );

  return response.data.data;

};

/**
 * Get interests of current provider
 */
export const getMyInterests = async () => {

  const response = await apiClient.get(
    "/interests/my"
  );

  return response.data.data;

};

/**
 * Cancel an existing interest
 */
export const cancelInterest = async (
  interestId: string
): Promise<void> => {

  await apiClient.delete(
    `/interests/${interestId}`
  );

};