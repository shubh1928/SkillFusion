export type ExchangeMode =
  | "DIRECT_BARTER"
  | "TOKEN_BASED"
  | "HYBRID";

export default interface CreateServiceRequest {

  title: string;

  exchangeMode: ExchangeMode;

  offerTitle: string;

  offerDescription: string;

  requestTitle: string;

  requestDescription: string;

  estimatedHours: number;

  tokenAmount?: number;

  skillIds: string[];

  selectiveRequest: boolean;

}