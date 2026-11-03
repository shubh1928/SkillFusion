import apiClient from "@/config/ApiClient";

import type Wallet from "@/models/wallet/Wallet";
import type WalletSummary from "@/models/wallet/WalletSummary";
import type TokenTransaction from "@/models/wallet/TokenTransaction";
import type BlockchainWallet from "@/models/wallet/BlockchainWallet";

import type BlockchainTransaction from "@/models/wallet/BlockchainTransaction";

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

  
/**
 * Get blockchain-primary wallet information
 */
export const getBlockchainWallet =
  async (): Promise<BlockchainWallet> => {

    const response = await apiClient.get(
      "/wallets/blockchain"
    );

    return response.data.data;
  };


/**
 * Get blockchain transaction history
 */
export const getBlockchainTransactions =
  async (): Promise<BlockchainTransaction[]> => {

    const response = await apiClient.get(
      "/wallets/blockchain/transactions"
    );

    return response.data.data;
  };