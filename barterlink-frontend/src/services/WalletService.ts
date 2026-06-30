import apiClient from "@/config/ApiClient";

import type Wallet from "@/models/wallet/Wallet";
import type WalletSummary from "@/models/wallet/WalletSummary";
import type TokenTransaction from "@/models/wallet/TokenTransaction";

/**
 * Get current user's wallet
 */
export const getWallet =
  async (): Promise<Wallet> => {

    const response = await apiClient.get(
      "/wallets"
    );

    return response.data.data;

  };

/**
 * Get wallet summary
 */
export const getWalletSummary =
  async (): Promise<WalletSummary> => {

    const response = await apiClient.get(
      "/wallets/summary"
    );

    return response.data.data;

  };

/**
 * Get wallet transaction history
 */
export const getTransactions =
  async (): Promise<TokenTransaction[]> => {

    const response = await apiClient.get(
      "/wallets/transactions"
    );

    return response.data.data;

  };