import { useEffect, useState } from "react";
import toast from "react-hot-toast";

import {
  RefreshCw,
  Wallet as WalletIcon,
  Coins,
  Lock,
  ArrowUpRight,
  RotateCcw,
  CheckCircle2,
  Clock3,
  XCircle,
  Copy,
  ExternalLink,
  X,
} from "lucide-react";

import {
  getBlockchainWallet,
  getBlockchainTransactions,
} from "@/services/WalletService";

import type BlockchainWallet from "@/models/wallet/BlockchainWallet";
import type BlockchainTransaction from "@/models/wallet/BlockchainTransaction";

import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

import { Badge } from "@/components/ui/badge";

function WalletPage() {

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [wallet, setWallet] =
    useState<BlockchainWallet | null>(null);

  const [transactions, setTransactions] =
    useState<BlockchainTransaction[]>([]);

  const [selectedTransaction, setSelectedTransaction] =
    useState<BlockchainTransaction | null>(null);

  const loadWallet = async () => {

    try {

      setLoading(true);

      const [
        walletResponse,
        transactionResponse,
      ] = await Promise.all([

        getBlockchainWallet(),

        getBlockchainTransactions(),

      ]);

      setWallet(walletResponse);

      setTransactions(transactionResponse);

    } catch (error: any) {

      toast.error(
        error?.response?.data?.message ??
        "Failed to load blockchain wallet"
      );

    } finally {

      setLoading(false);

    }

  };

  useEffect(() => {

    loadWallet();

  }, []);

  const handleRefresh = async () => {

    try {

      setRefreshing(true);

      await loadWallet();

      toast.success(
        "Wallet refreshed"
      );

    } finally {

      setRefreshing(false);

    }

  };

  const formatTransactionType = (
    type: BlockchainTransaction["transactionType"]
  ) => {

    switch (type) {

      case "MINT_SIGNUP_BONUS":
        return "Signup Bonus";

      case "LOCK":
        return "Tokens Locked";

      case "RELEASE":
        return "Payment Released";

      case "REFUND":
        return "Tokens Refunded";

      default:
        return type;

    }

  };

  const getTransactionIcon = (
    type: BlockchainTransaction["transactionType"]
  ) => {

    switch (type) {

      case "MINT_SIGNUP_BONUS":
        return (
          <Coins
            size={20}
            className="text-yellow-500"
          />
        );

      case "LOCK":
        return (
          <Lock
            size={20}
            className="text-orange-500"
          />
        );

      case "RELEASE":
        return (
          <ArrowUpRight
            size={20}
            className="text-blue-500"
          />
        );

      case "REFUND":
        return (
          <RotateCcw
            size={20}
            className="text-purple-500"
          />
        );

      default:
        return (
          <Coins size={20} />
        );

    }

  };

  const getStatusIcon = (
    status: BlockchainTransaction["status"]
  ) => {

    switch (status) {

      case "CONFIRMED":
        return (
          <CheckCircle2
            size={15}
          />
        );

      case "SUBMITTED":
      case "REQUESTED":
        return (
          <Clock3
            size={15}
          />
        );

      case "FAILED":
        return (
          <XCircle
            size={15}
          />
        );

      default:
        return null;

    }

  };

  const getStatusVariant = (
    status: BlockchainTransaction["status"]
  ):
    | "default"
    | "secondary"
    | "destructive"
    | "outline" => {

    switch (status) {

      case "CONFIRMED":
        return "default";

      case "SUBMITTED":
      case "REQUESTED":
        return "secondary";

      case "FAILED":
        return "destructive";

      default:
        return "outline";

    }

  };

  const getAmountPrefix = (
    transaction: BlockchainTransaction
  ) => {

    switch (transaction.transactionType) {

      case "MINT_SIGNUP_BONUS":
      case "RELEASE":
      case "REFUND":
        return "+";

      case "LOCK":
        return "-";

      default:
        return "";

    }

  };

  const formatDate = (
    date: string | null
  ) => {

    if (!date) {
      return "-";
    }

    return new Date(date).toLocaleString(
      undefined,
      {
        dateStyle: "medium",
        timeStyle: "short",
      }
    );

  };

  const shortenAddress = (
    address: string | null
  ) => {

    if (!address) {
      return "-";
    }

    if (address.length <= 14) {
      return address;
    }

    return `${address.slice(0, 8)}...${address.slice(-6)}`;

  };

  const copyToClipboard = async (
    value: string,
    label: string
  ) => {

    try {

      await navigator.clipboard.writeText(value);

      toast.success(
        `${label} copied`
      );

    } catch {

      toast.error(
        `Unable to copy ${label.toLowerCase()}`
      );

    }

  };

  const verifyOnSepolia = (
    transactionHash: string
  ) => {

    window.open(
      `https://sepolia.etherscan.io/tx/${transactionHash}`,
      "_blank",
      "noopener,noreferrer"
    );

  };

  return (

    <div className="space-y-6 sm:space-y-8">

      {/* Header */}

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">

        <div>

          <div className="flex items-center gap-3">

            <WalletIcon
              className="text-primary"
              size={28}
            />

            <h1 className="text-2xl font-bold sm:text-3xl">
              Wallet
            </h1>

          </div>

          <p className="mt-2 text-sm text-muted-foreground sm:text-base">
            Your blockchain wallet and transaction activity.
          </p>

        </div>

        <Button
          variant="outline"
          onClick={handleRefresh}
          disabled={refreshing}
          className="w-full sm:w-auto"
        >

          {refreshing ? (
            <Spinner />
          ) : (
            <RefreshCw size={18} />
          )}

          Refresh

        </Button>

      </div>

      {/* Loading */}

      {loading ? (

        <Card>

          <CardContent className="flex min-h-[300px] items-center justify-center">

            <div className="flex flex-col items-center gap-4">

              <Spinner className="h-10 w-10" />

              <p className="text-muted-foreground">
                Loading blockchain wallet...
              </p>

            </div>

          </CardContent>

        </Card>

      ) : (

        <>

          {/* Primary Wallet Card */}

          <Card className="overflow-hidden">

            <CardContent className="p-0">

              <div className="bg-primary p-6 text-primary-foreground sm:p-8">

                <div className="flex flex-col gap-6">

                  <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">

                    <div>

                      <p className="text-sm opacity-80">
                        Blockchain Wallet
                      </p>

                      <div className="mt-1 flex flex-wrap items-center gap-2">

                        <h2 className="text-xl font-semibold sm:text-2xl">
                          Ethereum Sepolia
                        </h2>

                        <Badge
                          variant="secondary"
                        >
                          Chain {wallet?.chainId}
                        </Badge>

                      </div>

                    </div>

                    <WalletIcon
                      size={36}
                      className="opacity-80"
                    />

                  </div>

                  <div>

                    <p className="text-sm opacity-80">
                      Total Position
                    </p>

                    <div className="mt-1 text-4xl font-bold sm:text-5xl">
                      {wallet?.totalBalance ?? 0} SFT
                    </div>

                  </div>

                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center">

                    <div className="min-w-0 flex-1 rounded-lg bg-white/10 px-4 py-3">

                      <p className="text-xs opacity-70">
                        Wallet Address
                      </p>

                      <p className="mt-1 truncate font-mono text-sm">
                        {wallet?.blockchainAddress ?? "-"}
                      </p>

                    </div>

                    {wallet?.blockchainAddress && (

                      <Button
                        type="button"
                        variant="secondary"
                        size="sm"
                        className="shrink-0"
                        onClick={() =>
                          copyToClipboard(
                            wallet.blockchainAddress,
                            "Wallet address"
                          )
                        }
                      >

                        <Copy size={16} />

                        Copy

                      </Button>

                    )}

                  </div>

                </div>

              </div>

              {/* Balance Summary */}

              <div className="grid grid-cols-1 divide-y sm:grid-cols-3 sm:divide-x sm:divide-y-0">

                <div className="p-5 sm:p-6">

                  <div className="flex items-center gap-3">

                    <Coins
                      size={26}
                      className="text-green-500"
                    />

                    <div>

                      <p className="text-sm text-muted-foreground">
                        On-chain Balance
                      </p>

                      <p className="text-2xl font-bold">
                        {wallet?.onChainBalance ?? 0} SFT
                      </p>

                    </div>

                  </div>

                </div>

                <div className="p-5 sm:p-6">

                  <div className="flex items-center gap-3">

                    <Lock
                      size={26}
                      className="text-orange-500"
                    />

                    <div>

                      <p className="text-sm text-muted-foreground">
                        Locked in Escrow
                      </p>

                      <p className="text-2xl font-bold">
                        {wallet?.lockedInEscrow ?? 0} SFT
                      </p>

                    </div>

                  </div>

                </div>

                <div className="p-5 sm:p-6">

                  <div className="flex items-center gap-3">

                    <WalletIcon
                      size={26}
                      className="text-primary"
                    />

                    <div>

                      <p className="text-sm text-muted-foreground">
                        Total Position
                      </p>

                      <p className="text-2xl font-bold">
                        {wallet?.totalBalance ?? 0} SFT
                      </p>

                    </div>

                  </div>

                </div>

              </div>

            </CardContent>

          </Card>

          {/* Contract Information */}

          <Card>

            <CardContent className="p-5 sm:p-6">

              <div className="space-y-4">

                <div>

                  <h2 className="text-lg font-semibold">
                    Blockchain Contracts
                  </h2>

                  <p className="mt-1 text-sm text-muted-foreground">
                    Addresses used by the SkillFusion blockchain wallet.
                  </p>

                </div>

                <div className="grid gap-4 lg:grid-cols-2">

                  <div className="rounded-lg border p-4">

                    <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                      BLT Token Contract
                    </p>

                    <div className="mt-2 flex items-center gap-2">

                      <p className="min-w-0 flex-1 truncate font-mono text-sm">
                        {wallet?.tokenContractAddress ?? "-"}
                      </p>

                      {wallet?.tokenContractAddress && (

                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          onClick={() =>
                            copyToClipboard(
                              wallet.tokenContractAddress,
                              "Token contract address"
                            )
                          }
                        >

                          <Copy size={16} />

                        </Button>

                      )}

                    </div>

                  </div>

                  <div className="rounded-lg border p-4">

                    <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                      Escrow Contract
                    </p>

                    <div className="mt-2 flex items-center gap-2">

                      <p className="min-w-0 flex-1 truncate font-mono text-sm">
                        {wallet?.escrowContractAddress ?? "-"}
                      </p>

                      {wallet?.escrowContractAddress && (

                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          onClick={() =>
                            copyToClipboard(
                              wallet.escrowContractAddress,
                              "Escrow contract address"
                            )
                          }
                        >

                          <Copy size={16} />

                        </Button>

                      )}

                    </div>

                  </div>

                </div>

              </div>

            </CardContent>

          </Card>

          {/* Transactions */}

          <div className="space-y-4">

            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">

              <div>

                <h2 className="text-xl font-semibold sm:text-2xl">
                  Blockchain Transactions
                </h2>

                <p className="text-sm text-muted-foreground">
                  Select a transaction to view its full blockchain details.
                </p>

              </div>

              <span className="text-sm text-muted-foreground">
                {transactions.length} transaction
                {transactions.length !== 1 ? "s" : ""}
              </span>

            </div>

            {transactions.length === 0 ? (

              <Card>

                <CardContent className="flex min-h-[220px] flex-col items-center justify-center text-center">

                  <WalletIcon
                    className="mb-4 text-muted-foreground"
                    size={52}
                  />

                  <h3 className="text-lg font-semibold">
                    No Blockchain Transactions
                  </h3>

                  <p className="mt-2 max-w-md text-sm text-muted-foreground">
                    Your blockchain wallet does not have any recorded transactions yet.
                  </p>

                </CardContent>

              </Card>

            ) : (

              <Card>

                <CardContent className="p-0">

                  <div className="divide-y">

                    {transactions.map((transaction) => (

                      <button
                        key={transaction.transactionId}
                        type="button"
                        onClick={() =>
                          setSelectedTransaction(transaction)
                        }
                        className="w-full text-left transition-colors hover:bg-muted/40 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:ring-inset"
                      >

                        <div className="flex items-center gap-3 p-4 sm:gap-4 sm:p-5">

                          {/* Icon */}

                          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-muted">

                            {getTransactionIcon(
                              transaction.transactionType
                            )}

                          </div>

                          {/* Main content */}

                          <div className="min-w-0 flex-1">

                            <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:gap-2">

                              <h3 className="truncate font-semibold">
                                {formatTransactionType(
                                  transaction.transactionType
                                )}
                              </h3>

                              <Badge
                                variant={getStatusVariant(
                                  transaction.status
                                )}
                                className="w-fit gap-1"
                              >

                                {getStatusIcon(
                                  transaction.status
                                )}

                                {transaction.status}

                              </Badge>

                            </div>

                            <p className="mt-1 text-xs text-muted-foreground sm:text-sm">

                              {transaction.confirmedAt
                                ? formatDate(transaction.confirmedAt)
                                : formatDate(transaction.requestedAt)}

                            </p>

                          </div>

                          {/* Amount */}

                          <div className="shrink-0 text-right">

                            <p
                              className={`text-base font-bold sm:text-lg ${
                                transaction.transactionType === "LOCK"
                                  ? "text-red-500"
                                  : "text-green-500"
                              }`}
                            >

                              {getAmountPrefix(transaction)}
                              {transaction.amount ?? 0} SFT

                            </p>

                            <p className="hidden text-xs text-muted-foreground sm:block">
                              View details
                            </p>

                          </div>

                        </div>

                      </button>

                    ))}

                  </div>

                </CardContent>

              </Card>

            )}

          </div>

        </>

      )}

      {/* Transaction Details Overlay */}

      {selectedTransaction && (

        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 p-0 sm:items-center sm:p-4"
          onClick={() =>
            setSelectedTransaction(null)
          }
        >

          <div
            className="max-h-[92vh] w-full overflow-y-auto rounded-t-2xl bg-background shadow-2xl sm:max-w-2xl sm:rounded-2xl"
            onClick={(event) =>
              event.stopPropagation()
            }
          >

            {/* Details Header */}

            <div className="sticky top-0 z-10 flex items-center justify-between border-b bg-background px-5 py-4 sm:px-6">

              <div>

                <p className="text-xs uppercase tracking-wide text-muted-foreground">
                  Blockchain Transaction
                </p>

                <h2 className="mt-1 text-lg font-semibold sm:text-xl">
                  {formatTransactionType(
                    selectedTransaction.transactionType
                  )}
                </h2>

              </div>

              <Button
                type="button"
                variant="ghost"
                size="icon"
                onClick={() =>
                  setSelectedTransaction(null)
                }
              >

                <X size={20} />

              </Button>

            </div>

            <div className="space-y-6 p-5 sm:p-6">

              {/* Status + Amount */}

              <div className="text-center">

                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-muted">

                  {getStatusIcon(
                    selectedTransaction.status
                  )}

                </div>

                <div className="mt-3">

                  <Badge
                    variant={getStatusVariant(
                      selectedTransaction.status
                    )}
                    className="gap-1"
                  >

                    {getStatusIcon(
                      selectedTransaction.status
                    )}

                    {selectedTransaction.status}

                  </Badge>

                </div>

                <p
                  className={`mt-4 text-4xl font-bold ${
                    selectedTransaction.transactionType === "LOCK"
                      ? "text-red-500"
                      : "text-green-500"
                  }`}
                >

                  {getAmountPrefix(
                    selectedTransaction
                  )}
                  {selectedTransaction.amount ?? 0} SFT

                </p>

                <p className="mt-1 text-sm text-muted-foreground">
                  {formatTransactionType(
                    selectedTransaction.transactionType
                  )}
                </p>

              </div>

              {/* Transaction Hash */}

              <div className="rounded-xl border bg-muted/20 p-4">

                <div className="flex items-center justify-between gap-3">

                  <div className="min-w-0">

                    <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                      Transaction Hash
                    </p>

                    <p className="mt-2 break-all font-mono text-xs sm:text-sm">
                      {selectedTransaction.transactionHash ?? "Not available"}
                    </p>

                  </div>

                  {selectedTransaction.transactionHash && (

                    <div className="flex shrink-0 gap-1">

                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() =>
                          copyToClipboard(
                            selectedTransaction.transactionHash!,
                            "Transaction hash"
                          )
                        }
                      >

                        <Copy size={16} />

                      </Button>

                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() =>
                          verifyOnSepolia(
                            selectedTransaction.transactionHash!
                          )
                        }
                      >

                        <ExternalLink size={16} />

                      </Button>

                    </div>

                  )}

                </div>

              </div>

              {/* Escrow ID */}

              {selectedTransaction.escrowId && (

                <div className="rounded-xl border p-4">

                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    Escrow ID
                  </p>

                  <div className="mt-2 flex items-center gap-2">

                    <p className="min-w-0 flex-1 break-all font-mono text-xs sm:text-sm">
                      {selectedTransaction.escrowId}
                    </p>

                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="shrink-0"
                      onClick={() =>
                        copyToClipboard(
                          selectedTransaction.escrowId!,
                          "Escrow ID"
                        )
                      }
                    >

                      <Copy size={16} />

                    </Button>

                  </div>

                </div>

              )}

              {/* Addresses */}

              <div className="grid gap-4 sm:grid-cols-2">

                <div className="rounded-xl border p-4">

                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    From
                  </p>

                  <p className="mt-2 font-mono text-sm">
                    {shortenAddress(
                      selectedTransaction.fromAddress
                    )}
                  </p>

                  {selectedTransaction.fromAddress && (

                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="mt-2 px-0"
                      onClick={() =>
                        copyToClipboard(
                          selectedTransaction.fromAddress!,
                          "From address"
                        )
                      }
                    >

                      <Copy size={14} />

                      Copy

                    </Button>

                  )}

                </div>

                <div className="rounded-xl border p-4">

                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    To
                  </p>

                  <p className="mt-2 font-mono text-sm">
                    {shortenAddress(
                      selectedTransaction.toAddress
                    )}
                  </p>

                  {selectedTransaction.toAddress && (

                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="mt-2 px-0"
                      onClick={() =>
                        copyToClipboard(
                          selectedTransaction.toAddress!,
                          "To address"
                        )
                      }
                    >

                      <Copy size={14} />

                      Copy

                    </Button>

                  )}

                </div>

              </div>

              {/* Timeline */}

              <div className="space-y-3">

                <h3 className="font-semibold">
                  Transaction Timeline
                </h3>

                <div className="space-y-3 rounded-xl border p-4">

                  <div className="flex items-start justify-between gap-4 text-sm">

                    <span className="text-muted-foreground">
                      Requested
                    </span>

                    <span className="text-right">
                      {formatDate(
                        selectedTransaction.requestedAt
                      )}
                    </span>

                  </div>

                  <div className="flex items-start justify-between gap-4 text-sm">

                    <span className="text-muted-foreground">
                      Submitted
                    </span>

                    <span className="text-right">
                      {formatDate(
                        selectedTransaction.submittedAt
                      )}
                    </span>

                  </div>

                  <div className="flex items-start justify-between gap-4 text-sm">

                    <span className="text-muted-foreground">
                      Confirmed
                    </span>

                    <span className="text-right">
                      {formatDate(
                        selectedTransaction.confirmedAt
                      )}
                    </span>

                  </div>

                  {selectedTransaction.failedAt && (

                    <div className="flex items-start justify-between gap-4 text-sm">

                      <span className="text-muted-foreground">
                        Failed
                      </span>

                      <span className="text-right">
                        {formatDate(
                          selectedTransaction.failedAt
                        )}
                      </span>

                    </div>

                  )}

                </div>

              </div>

              {/* Failure */}

              {selectedTransaction.failureReason && (

                <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4">

                  <p className="text-sm font-semibold text-destructive">
                    Failure Reason
                  </p>

                  <p className="mt-2 break-words text-sm">
                    {selectedTransaction.failureReason}
                  </p>

                </div>

              )}

              {/* Verify */}

              {selectedTransaction.transactionHash ? (

                <Button
                  type="button"
                  className="w-full"
                  onClick={() =>
                    verifyOnSepolia(
                      selectedTransaction.transactionHash!
                    )
                  }
                >

                  <ExternalLink size={17} />

                  Verify on Sepolia

                </Button>

              ) : (

                <Button
                  type="button"
                  variant="outline"
                  disabled
                  className="w-full"
                >

                  Transaction hash unavailable

                </Button>

              )}

            </div>

          </div>

        </div>

      )}

    </div>

  );

}

export default WalletPage;