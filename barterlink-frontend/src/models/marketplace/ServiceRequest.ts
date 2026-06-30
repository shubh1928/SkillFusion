import ServiceRequestStatus from "./ServiceRequestStatus";

export default interface ServiceRequest {
  requestId: string;

  requesterId: string;
  requesterName: string;

  selectedProviderId?: string;
  selectedProviderName?: string;

  title: string;
  description: string;

  estimatedHours: number;
  tokenCost: number;

  selectiveRequest: boolean;

  status: ServiceRequestStatus;

  createdAt: string;
}