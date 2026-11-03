export default interface BlockchainTransaction {
  transactionId: string;

  transactionType:
    | "MINT_SIGNUP_BONUS"
    | "LOCK"
    | "RELEASE"
    | "REFUND";

  status:
    | "REQUESTED"
    | "SUBMITTED"
    | "CONFIRMED"
    | "FAILED";

  amount: number | null;

  transactionHash: string | null;

  escrowId: string | null;

  fromAddress: string | null;

  toAddress: string | null;

  failureReason: string | null;

  requestedAt: string | null;

  submittedAt: string | null;

  confirmedAt: string | null;

  failedAt: string | null;
}