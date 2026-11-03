import InterestStatus from "./InterestStatus";

export default interface Interest {

  interestId: string;

  requestId: string;

  providerId: string;

  providerName: string;

  providerImage: string | null;

  status: InterestStatus;

  createdAt: string;

}