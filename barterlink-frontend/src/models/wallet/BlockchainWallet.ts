export default interface BlockchainWallet {
  walletId: string;
  blockchainAddress: string;
  chainId: number;
  tokenContractAddress: string;
  escrowContractAddress: string;
  onChainBalance: number;
  lockedInEscrow: number;
  totalBalance: number;
}