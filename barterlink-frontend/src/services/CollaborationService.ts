import apiClient from "@/config/ApiClient";

import type CollaborationRoom
  from "@/models/collaboration/CollaborationRoom";

/**
 * Get collaboration room for a session
 */
export const getCollaborationRoom =
  async (
    sessionId: string
  ): Promise<CollaborationRoom> => {

    const response =
      await apiClient.get(

        `/collaboration/session/${sessionId}`

      );

    return response.data.data;

  };