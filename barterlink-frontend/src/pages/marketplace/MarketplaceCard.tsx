import {
  Clock,
  User,
  Heart,
  Eye,
  XCircle,
  Handshake,
} from "lucide-react";

import type ServiceRequest
from "@/models/marketplace/ServiceRequest";

import TrustBadge
from "@/components/common/trust/TrustBadge";

import ServiceRequestStatus
from "@/models/marketplace/ServiceRequestStatus";

import { Card, CardContent }
from "@/components/ui/card";

import { Badge }
from "@/components/ui/badge";

import { Button }
from "@/components/ui/button";

interface MarketplaceCardProps {

  request: ServiceRequest;

  showInterestedButton?: boolean;

  showCancelButton?: boolean;

  onInterested?: (
    request: ServiceRequest
  ) => void;

  onCancel?: (
    requestId: string
  ) => void;

  onViewDetails?: (
    request: ServiceRequest
  ) => void;

  onViewInterests?: (
    request: ServiceRequest
  ) => void;

}

function MarketplaceCard({

  request,

  showInterestedButton = false,

  showCancelButton = false,

  onInterested,

  onCancel,

  onViewDetails,

  onViewInterests,

}: MarketplaceCardProps) {

  const fundingUnavailable =
  request.exchangeMode !== "DIRECT_BARTER" &&
  request.requesterHasSufficientTokens === false;

  const statusVariant = ():
    | "default"
    | "secondary"
    | "destructive"
    | "outline" => {

    switch (request.status) {

      case ServiceRequestStatus.OPEN:
        return "default";

      case ServiceRequestStatus.ACCEPTED:
        return "secondary";

      case ServiceRequestStatus.COMPLETED:
        return "secondary";

      case ServiceRequestStatus.CANCELLED:
        return "destructive";

      case ServiceRequestStatus.DISPUTED:
        return "destructive";

      default:
        return "outline";

    }

  };

  const exchangeModeColor = () => {

    switch (request.exchangeMode) {

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

    switch (request.exchangeMode) {

      case "DIRECT_BARTER":
        return "Direct Barter";

      case "TOKEN_BASED":
        return "Token Based";

      case "HYBRID":
        return "Hybrid";

      default:
        return request.exchangeMode;

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

      <CardContent className="flex h-full flex-col space-y-4 p-5">

        {/* Header */}

        <div className="flex items-start justify-between gap-5">

          <div className="flex-1">

            <h2 className="line-clamp-2 text-xl font-bold">

              {request.title}

            </h2>

            <div className="mt-4 flex flex-wrap items-center gap-3">

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
                {request.status}
              </Badge>

            </div>

          </div>

        </div>

        {/* Basic Information */}

        <div className="grid gap-4 md:grid-cols-2">

          <div className="flex items-center gap-2 text-sm">

            <Clock
              size={18}
            />

            <span>

              {request.estimatedHours} Hours

            </span>

          </div>

          <div className="flex items-center justify-between gap-3">

            <div className="flex items-center gap-2 text-sm">

              <User size={18} />

              <span>

                {request.requesterName}

              </span>

            </div>

            <TrustBadge
              trustAssessment={request.trustAssessment}
            />

          </div>

        </div>

        {/* Providing */}

        <div className="rounded-lg border p-3">

          <h3 className="mb-2 font-semibold text-emerald-600">

            🎁 Providing

          </h3>

          <p className="line-clamp-2 text-sm font-medium">

            {request.offerTitle}

          </p>

        </div>

        {/* Looking For */}

        <div className="rounded-lg border p-3">

          <h3 className="mb-2 font-semibold text-sky-600">

            🔍 Looking For

          </h3>

          <p className="line-clamp-2 text-sm font-medium">

            {request.requestTitle}

          </p>

        </div>

        {/* Required Skills */}

        <div className="flex-1">

          <h3 className="mb-3 font-semibold">

            Required Skills

          </h3>

          <div className="flex flex-wrap gap-2">

            {request.requiredSkills.length > 0 ? (

              request.requiredSkills.map(skill => (

                <Badge
                  key={skill}
                  variant="outline"
                >

                  {skill}

                </Badge>

              ))

            ) : (

              <span className="text-sm text-muted-foreground">

                No specific skills mentioned

              </span>

            )}

          </div>

        </div>

        {/* Footer */}

        <div
          className="
          mt-auto
          flex
          flex-wrap
          items-center
          justify-between
          gap-4
          border-t
          pt-5
          "
        >

          <div className="text-sm text-muted-foreground">

            Posted on{" "}

            {new Date(
              request.createdAt
            ).toLocaleDateString()}

          </div>

          <div className="flex w-full flex-col gap-2 md:w-auto">

            {showInterestedButton && (
              <>
                {fundingUnavailable && (
                  <p className="text-sm text-destructive">
                    The requester currently has insufficient available BLT.
                    Interest is unavailable.
                  </p>
                )}

                <Button
                  className="w-full"
                  disabled={fundingUnavailable}
                  onClick={() => onInterested?.(request)}
                >
                  <Heart className="mr-2 h-4 w-4" />
                  Interested
                </Button>
              </>
            )}

            {showCancelButton && (
  <Button className="w-full"
    variant="secondary"
    onClick={() =>
      onViewInterests?.(
        request
      )
    }
  >
    <User className="mr-2 h-4 w-4" />

    Interested Providers
  </Button>
)}

            {showCancelButton && (
              <Button className="w-full"
                variant="destructive"
                onClick={() =>
                  onCancel?.(
                    request.requestId
                  )
                }
              >
                <XCircle className="mr-2 h-4 w-4" />

                Cancel
              </Button>
            )}

            <Button className="w-full"
              variant="outline"
              onClick={() =>
                onViewDetails?.(
                  request
                )
              }
            >
              <Eye className="mr-2 h-4 w-4" />

              View Details
            </Button>

          </div>

        </div>

      </CardContent>

    </Card>

  );

}

export default MarketplaceCard;
