import apiClient from "@/config/ApiClient";

import type ServiceRequest
from "@/models/marketplace/ServiceRequest";

export const getRecommendations = async (): Promise<ServiceRequest[]> => {

    const response = await apiClient.get(
        "/recommendations"
    );

    return response.data.data;

};