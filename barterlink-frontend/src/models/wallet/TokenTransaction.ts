import type TransactionType from "./TransactionType";

export default interface TokenTransaction {

  transactionId: string;

  transactionType: TransactionType;

  amount: number;

  description: string;

  createdAt: string;

}