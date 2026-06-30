export default interface Agreement {

  agreementId: string;

  requestId: string;

  requesterId: string;

  requesterName: string;

  providerId: string;

  providerName: string;

  escrowLockedTokens: number;

  status: string;

  acceptedAt: string;

  completedAt?: string;

}