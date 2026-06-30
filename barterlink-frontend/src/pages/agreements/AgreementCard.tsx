import {
  User,
  Coins,
  Calendar,
} from "lucide-react";

import type Agreement from "@/models/agreement/Agreement";

import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

interface AgreementCardProps {

  agreement: Agreement;

}

function AgreementCard({

  agreement,

}: AgreementCardProps) {

  return (

    <Card className="transition-all hover:shadow-lg">

      <CardContent className="space-y-5 p-6">

        <div className="flex items-center justify-between">

          <h3 className="text-lg font-semibold">

            Agreement

          </h3>

          <Badge>

            {agreement.status}

          </Badge>

        </div>

        <div className="space-y-3">

          <div className="flex items-center gap-2">

            <User size={18} />

            <span>

              Requester:

            </span>

            <strong>

              {agreement.requesterName}

            </strong>

          </div>

          <div className="flex items-center gap-2">

            <User size={18} />

            <span>

              Provider:

            </span>

            <strong>

              {agreement.providerName}

            </strong>

          </div>

          <div className="flex items-center gap-2">

            <Coins size={18} />

            <span>

              {agreement.escrowLockedTokens}

              {" "}Tokens

            </span>

          </div>

          <div className="flex items-center gap-2">

            <Calendar size={18} />

            <span>

              {new Date(
                agreement.acceptedAt
              ).toLocaleDateString()}

            </span>

          </div>

        </div>

      </CardContent>

    </Card>

  );

}

export default AgreementCard;