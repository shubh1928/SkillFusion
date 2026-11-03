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
  

export const getUserRatings =
  async (
    userId: string
  ): Promise<Rating[]> => {

    const response = await apiClient.get(
      `/ratings/user/${userId}`
    );

    return response.data.data;

  };



export const getUserReputation =
  async (
    userId: string
  ): Promise<number> => {

    const response = await apiClient.get(
      `/ratings/user/${userId}/reputation`
    );

    return response.data.data;

  };


/**
 * Check whether the current user has
 * already rated a session.
 */
export const hasCurrentUserRated =
  async (
    sessionId: string
  ): Promise<boolean> => {

    const response = await apiClient.get(
      `/ratings/session/${sessionId}/status`
    );

    return response.data.data;

  };