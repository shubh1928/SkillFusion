import apiClient from "@/config/ApiClient";

import type Rating from "@/models/ratings/Rating";
import type CreateRating from "@/models/ratings/CreateRating";

/**
 * Submit a provider rating
 */
export const submitRating =
  async (
    request: CreateRating
  ): Promise<Rating> => {

    const response = await apiClient.post(
      "/ratings",
      request
    );

    return response.data.data;

  };

/**
 * Get all ratings for a provider
 */
export const getProviderRatings =
  async (
    providerId: string
  ): Promise<Rating[]> => {

    const response = await apiClient.get(
      `/ratings/provider/${providerId}`
    );

    return response.data.data;

  };

/**
 * Get provider reputation
 */
export const getProviderReputation =
  async (
    providerId: string
  ): Promise<number> => {

    const response = await apiClient.get(
      `/ratings/provider/${providerId}/reputation`
    );

    return response.data.data;

  };