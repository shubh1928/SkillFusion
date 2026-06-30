export default interface CreateServiceRequest {
  title: string;
  description: string;
  estimatedHours: number;
  tokenCost: number;
  selectiveRequest: boolean;
}