import apiClient from "@/config/ApiClient";

import type CreateServiceRequest from "@/models/marketplace/CreateServiceRequest";
import type ServiceRequest from "@/models/marketplace/ServiceRequest";

/**
 * Create a new marketplace request
 */
export const createRequest = async (
  request: CreateServiceRequest
): Promise<ServiceRequest> => {

  const response = await apiClient.post(
    "/marketplace/requests",
    request
  );

  return response.data.data;
};

/**
 * Get all open marketplace requests
 */
export const getOpenRequests = async (): Promise<ServiceRequest[]> => {

  const response = await apiClient.get(
    "/marketplace/requests"
  );

  return response.data.data;
};

/**
 * Get a marketplace request by id
 */
export const getRequestById = async (
  requestId: string
): Promise<ServiceRequest> => {

  const response = await apiClient.get(
    `/marketplace/requests/${requestId}`
  );

  return response.data.data;
};

/**
 * Get current user's requests
 */
export const getMyRequests = async (): Promise<ServiceRequest[]> => {

  const response = await apiClient.get(
    "/marketplace/requests/my"
  );

  return response.data.data;
};

/**
 * Get requests assigned to current user
 */
export const getAssignedRequests = async (): Promise<ServiceRequest[]> => {

  const response = await apiClient.get(
    "/marketplace/requests/assigned"
  );

  return response.data.data;
};

/**
 * Accept a marketplace request
 */
export const acceptRequest = async (
  requestId: string
): Promise<ServiceRequest> => {

  const response = await apiClient.post(
    `/marketplace/requests/${requestId}/accept`
  );

  return response.data.data;
};

/**
 * Cancel a marketplace request
 */
export const cancelRequest = async (
  requestId: string
): Promise<void> => {

  await apiClient.post(
    `/marketplace/requests/${requestId}/cancel`
  );
};