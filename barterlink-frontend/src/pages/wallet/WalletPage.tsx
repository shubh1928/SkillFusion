import { useEffect, useState } from "react";
import toast from "react-hot-toast";

import {
  RefreshCw,
  Wallet as WalletIcon,
  Coins,
  Lock,
} from "lucide-react";

import {
  getWallet,
  getWalletSummary,
  getTransactions,
} from "@/services/WalletService";

import type Wallet from "@/models/wallet/Wallet";
import type WalletSummary from "@/models/wallet/WalletSummary";
import type TokenTransaction from "@/models/wallet/TokenTransaction";

import TransactionCard from "./TransactionCard";

import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

function WalletPage() {

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [wallet, setWallet] =
    useState<Wallet | null>(null);

  const [summary, setSummary] =
    useState<WalletSummary | null>(null);

  const [transactions, setTransactions] =
    useState<TokenTransaction[]>([]);

  const loadWallet = async () => {

    try {

      setLoading(true);

      const [

        walletResponse,

        summaryResponse,

        transactionResponse,

      ] = await Promise.all([

        getWallet(),

        getWalletSummary(),

        getTransactions(),

      ]);

      setWallet(

        walletResponse

      );

      setSummary(

        summaryResponse

      );

      setTransactions(

        transactionResponse

      );

    } catch (error: any) {

      toast.error(

        error?.response?.data?.message ??

        "Failed to load wallet"

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

    return (

    <div className="space-y-8">

      {/* Header */}

      <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">

        <div>

          <div className="flex items-center gap-3">

            <WalletIcon
              className="text-primary"
              size={30}
            />

            <h1 className="text-3xl font-bold">

              Wallet

            </h1>

          </div>

          <p className="mt-2 text-muted-foreground">

            View your token balance and transaction history.

          </p>

        </div>

        <Button
          variant="outline"
          onClick={handleRefresh}
          disabled={refreshing}
        >

          {

            refreshing

              ? <Spinner />

              : <RefreshCw size={18} />

          }

          Refresh

        </Button>

      </div>

      {/* Loading */}

      {loading ? (

        <Card>

          <CardContent className="flex h-72 items-center justify-center">

            <div className="flex flex-col items-center gap-4">

              <Spinner className="h-10 w-10" />

              <p className="text-muted-foreground">

                Loading wallet...

              </p>

            </div>

          </CardContent>

        </Card>

      ) : (

        <>

          {/* Wallet Summary */}

          <div className="grid gap-6 md:grid-cols-3">

            <Card>

              <CardContent className="flex items-center gap-4 p-6">

                <Coins
                  className="text-green-500"
                  size={32}
                />

                <div>

                  <p className="text-sm text-muted-foreground">

                    Available Tokens

                  </p>

                  <h2 className="text-3xl font-bold">

                    {summary?.availableTokens ?? 0}

                  </h2>

                </div>

              </CardContent>

            </Card>

            <Card>

              <CardContent className="flex items-center gap-4 p-6">

                <Lock
                  className="text-orange-500"
                  size={32}
                />

                <div>

                  <p className="text-sm text-muted-foreground">

                    Locked Tokens

                  </p>

                  <h2 className="text-3xl font-bold">

                    {summary?.lockedTokens ?? 0}

                  </h2>

                </div>

              </CardContent>

            </Card>

            <Card>

              <CardContent className="flex items-center gap-4 p-6">

                <WalletIcon
                  className="text-primary"
                  size={32}
                />

                <div>

                  <p className="text-sm text-muted-foreground">

                    Total Tokens

                  </p>

                  <h2 className="text-3xl font-bold">

                    {summary?.totalTokens ?? 0}

                  </h2>

                </div>

              </CardContent>

            </Card>

          </div>

                    {/* Transaction History */}

          <div className="space-y-6">

            <div className="flex items-center justify-between">

              <h2 className="text-2xl font-semibold">

                Transaction History

              </h2>

              <span className="text-sm text-muted-foreground">

                {transactions.length} Transaction
                {transactions.length !== 1 ? "s" : ""}

              </span>

            </div>

            {transactions.length === 0 ? (

              <Card>

                <CardContent className="flex h-56 flex-col items-center justify-center text-center">

                  <WalletIcon
                    className="mb-4 text-muted-foreground"
                    size={60}
                  />

                  <h3 className="text-xl font-semibold">

                    No Transactions

                  </h3>

                  <p className="mt-2 max-w-md text-muted-foreground">

                    Your wallet has no transaction history yet.

                  </p>

                </CardContent>

              </Card>

            ) : (

              <div className="grid gap-6">

                {transactions.map((transaction) => (

                  <TransactionCard
                    key={transaction.transactionId}
                    transaction={transaction}
                  />

                ))}

              </div>

            )}

          </div>

        </>

      )}

    </div>

  );

}

export default WalletPage;