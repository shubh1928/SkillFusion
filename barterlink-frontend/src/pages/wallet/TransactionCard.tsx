import {
  Coins,
  Lock,
  Unlock,
  RotateCcw,
  TrendingUp,
  TrendingDown,
  Calendar,
} from "lucide-react";

import type TokenTransaction from "@/models/wallet/TokenTransaction";
import TransactionType from "@/models/wallet/TransactionType";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

import { Badge } from "@/components/ui/badge";

interface TransactionCardProps {

  transaction: TokenTransaction;

}

function TransactionCard({

  transaction,

}: TransactionCardProps) {

  const transactionIcon = () => {

    switch (transaction.transactionType) {

      case TransactionType.BONUS:
        return <Coins className="text-yellow-500" size={22} />;

      case TransactionType.EARNED:
        return <TrendingUp className="text-green-500" size={22} />;

      case TransactionType.SPENT:
        return <TrendingDown className="text-red-500" size={22} />;

      case TransactionType.LOCKED:
        return <Lock className="text-orange-500" size={22} />;

      case TransactionType.RELEASED:
        return <Unlock className="text-blue-500" size={22} />;

      case TransactionType.REFUNDED:
        return <RotateCcw className="text-purple-500" size={22} />;

      default:
        return <Coins size={22} />;

    }

  };

  const badgeVariant = ():
    | "default"
    | "secondary"
    | "destructive"
    | "outline" => {

    switch (transaction.transactionType) {

      case TransactionType.BONUS:
      case TransactionType.EARNED:
        return "default";

      case TransactionType.LOCKED:
      case TransactionType.RELEASED:
        return "secondary";

      case TransactionType.SPENT:
        return "destructive";

      default:
        return "outline";

    }

  };

    return (

    <Card className="transition-all hover:-translate-y-1 hover:shadow-lg">

      <CardContent className="space-y-5 p-6">

        {/* Header */}

        <div className="flex items-start justify-between">

          <div className="flex items-center gap-3">

            {transactionIcon()}

            <div>

              <h3 className="font-semibold">

                {transaction.transactionType}

              </h3>

              <p className="text-sm text-muted-foreground">

                {transaction.description || "No description"}

              </p>

            </div>

          </div>

          <Badge variant={badgeVariant()}>

            {transaction.transactionType}

          </Badge>

        </div>

        {/* Amount */}

        <div className="rounded-lg border bg-muted/30 p-4">

          <div className="flex items-center justify-between">

            <span className="text-sm text-muted-foreground">

              Amount

            </span>

            <span className="text-2xl font-bold">

              {transaction.amount} Tokens

            </span>

          </div>

        </div>

        {/* Footer */}

        <div className="flex items-center gap-2 border-t pt-4 text-sm text-muted-foreground">

          <Calendar size={16} />

          <span>

            {transaction.createdAt
              ? new Date(
                  transaction.createdAt
                ).toLocaleString()
              : "-"}

          </span>

        </div>

      </CardContent>

    </Card>

  );

}

export default TransactionCard;