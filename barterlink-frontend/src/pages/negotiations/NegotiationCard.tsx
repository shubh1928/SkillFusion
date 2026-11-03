import {
  Clock,
  Coins,
  Handshake,
  MessageCircle,
} from "lucide-react";

import type Negotiation
from "@/models/negotiation/Negotiation";

import NegotiationStatus
from "@/models/negotiation/NegotiationStatus";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

import { Badge }
from "@/components/ui/badge";

import { Button }
from "@/components/ui/button";

interface NegotiationCardProps {

  negotiation: Negotiation;

  onOpenConversation: (
    negotiation: Negotiation
  ) => void;

}

function NegotiationCard({

  negotiation,

  onOpenConversation,

}: NegotiationCardProps) {

    const offerTitle = (negotiation.offerTitle ?? "").trim();
  const offerDescription = (
    negotiation.offerDescription ?? ""
  ).trim();

  const tokenAmountLabel =
    typeof negotiation.tokenAmount === "number" &&
    Number.isFinite(negotiation.tokenAmount)
      ? `${negotiation.tokenAmount.toLocaleString()} ${
          negotiation.tokenAmount === 1 ? "Token" : "Tokens"
        }`
      : "Token amount not specified";

  const offerHeading =
    negotiation.exchangeMode === "TOKEN_BASED"
      ? "Token-based exchange"
      : negotiation.exchangeMode === "HYBRID" && !offerTitle
        ? "Hybrid exchange"
        : offerTitle || "Offer details unavailable";

  const statusVariant = ():
    | "default"
    | "secondary"
    | "destructive"
    | "outline" => {

    switch (negotiation.status) {

      case NegotiationStatus.ACTIVE:
        return "default";

      case NegotiationStatus.ACCEPTED:
        return "secondary";

      case NegotiationStatus.SESSION_REQUESTED:
        return "outline";

      case NegotiationStatus.REJECTED:
      case NegotiationStatus.CANCELLED:
        return "destructive";

      case NegotiationStatus.CLOSED:
        return "secondary";

      default:
        return "outline";

    }

  };

  const exchangeModeColor = () => {

    switch (negotiation.exchangeMode) {

      case "DIRECT_BARTER":
        return "bg-emerald-100 text-emerald-700";

      case "TOKEN_BASED":
        return "bg-sky-100 text-sky-700";

      case "HYBRID":
        return "bg-violet-100 text-violet-700";

      default:
        return "";

    }

  };
  const exchangeModeLabel = () => {

    switch (negotiation.exchangeMode) {

      case "DIRECT_BARTER":
        return "Direct Barter";

      case "TOKEN_BASED":
        return "Token Based";

      case "HYBRID":
        return "Hybrid";

      default:
        return negotiation.exchangeMode;

    }

  };

  return (

    <Card
      className="
      transition-all
      duration-300
      hover:shadow-xl
      hover:-translate-y-1
      "
    >

      <CardContent className="space-y-6 p-6">

        {/* Header */}

        <div
          className="
          flex
          flex-col
          gap-4
          md:flex-row
          md:items-start
          md:justify-between
          "
        >

          <div className="space-y-3">

            <div className="flex flex-wrap gap-3">

              <Badge
                className={exchangeModeColor()}
              >

                <Handshake
                  className="mr-1 h-3 w-3"
                />

                {exchangeModeLabel()}

              </Badge>

              <Badge
                variant={statusVariant()}
              >

                {negotiation.status}

              </Badge>

            </div>

            <div>

              <h2 className="text-xl font-semibold">
  {offerHeading}
</h2>

              <p className="mt-1 text-muted-foreground">

                Looking for{" "}

                <span className="font-medium text-foreground">

                  {negotiation.requestTitle}

                </span>

              </p>

            </div>

          </div>

          <Button
            onClick={() =>
              onOpenConversation(
                negotiation
              )
            }
          >

            <MessageCircle
              className="mr-2 h-4 w-4"
            />

            Open Conversation

          </Button>

        </div>

        {/* Proposal */}

               {/* Proposal */}

        <div className="grid gap-4 md:grid-cols-2">

          {/* Offering: render according to exchange mode */}

          <div className="rounded-lg border p-4">

            <h3
              className="
                mb-2
                font-semibold
                text-emerald-600
              "
            >
              {negotiation.exchangeMode === "TOKEN_BASED"
                ? "Token Offering"
                : negotiation.exchangeMode === "HYBRID"
                  ? "Skill + Token Offering"
                  : "Offering"}
            </h3>

            {negotiation.exchangeMode === "TOKEN_BASED" ? (

              <div className="space-y-2">

                <p className="font-semibold">
                  {tokenAmountLabel}
                </p>

                <p className="text-sm text-muted-foreground">
                  Token payment for the requested service.
                </p>

              </div>

            ) : negotiation.exchangeMode === "HYBRID" ? (

              <div className="space-y-2">

                <p className="font-medium">
                  {offerTitle || "Skill or service not specified"}
                </p>

                {offerDescription ? (

                  <p className="text-sm text-muted-foreground">
                    {offerDescription}
                  </p>

                ) : (

                  <p className="text-sm text-muted-foreground">
                    No additional service description provided.
                  </p>

                )}

                <div className="pt-1">

                  <Badge
                    variant="secondary"
                    className="inline-flex items-center gap-1"
                  >
                    <Coins className="h-3 w-3" />
                    {tokenAmountLabel}
                  </Badge>

                </div>

              </div>

            ) : (

              <div className="space-y-2">

                <p className="font-medium">
                  {offerTitle || "Skill or service not specified"}
                </p>

                <p className="text-sm text-muted-foreground">
                  {offerDescription ||
                    "No additional service description provided."}
                </p>

              </div>

            )}

          </div>

          {/* Looking For */}

          <div className="rounded-lg border p-4">

            <h3
              className="
                mb-2
                font-semibold
                text-sky-600
              "
            >
              Looking For
            </h3>

            <p className="font-medium">
              {negotiation.requestTitle || "Requested service not specified"}
            </p>

            <p className="mt-2 text-sm text-muted-foreground">
              {negotiation.requestDescription ||
                "No additional request description provided."}
            </p>

          </div>

        </div>

        {/* Footer */}

        <div
          className="
          flex
          flex-wrap
          items-center
          justify-between
          gap-4
          border-t
          pt-5
          "
        >

          <div className="flex flex-wrap gap-5">

            <div className="flex items-center gap-2 text-sm">

              <Clock size={18} />

              <span>

                {negotiation.estimatedHours} Hours

              </span>

            </div>

            {(negotiation.exchangeMode === "TOKEN_BASED" ||

              negotiation.exchangeMode === "HYBRID") && (

              <div className="flex items-center gap-2 text-sm">

                <Coins size={18} />

                <span>

                  {tokenAmountLabel}

                </span>

              </div>

            )}

          </div>

          <div className="text-sm text-muted-foreground">

            {new Date(
              negotiation.createdAt
            ).toLocaleDateString()}

          </div>

        </div>

      </CardContent>

    </Card>

  );

}

export default NegotiationCard;