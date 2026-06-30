import {
  Clock,
  Coins,
  User,
  CheckCircle,
  XCircle,
} from "lucide-react";

import type ServiceRequest from "@/models/marketplace/ServiceRequest";
import ServiceRequestStatus from "@/models/marketplace/ServiceRequestStatus";

import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

interface MarketplaceCardProps {
  request: ServiceRequest;

  showAcceptButton?: boolean;

  showCancelButton?: boolean;

  onAccept?: (requestId: string) => void;

  onCancel?: (requestId: string) => void;
}

function MarketplaceCard({
  request,
  showAcceptButton = false,
  showCancelButton = false,
  onAccept,
  onCancel,
}: MarketplaceCardProps) {
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
        return "default";

      case ServiceRequestStatus.CANCELLED:
        return "destructive";

      case ServiceRequestStatus.DISPUTED:
        return "destructive";

      default:
        return "outline";
    }
  };

  return (
    <Card className="transition-all hover:shadow-lg hover:-translate-y-1">
      <CardContent className="space-y-5 p-6">

        {/* Header */}

        <div className="flex items-start justify-between gap-4">

          <div className="flex-1">

            <h3 className="text-xl font-semibold">
              {request.title}
            </h3>

            <p className="mt-2 text-sm text-muted-foreground">
              {request.description}
            </p>

          </div>

          <Badge variant={statusVariant()}>
            {request.status}
          </Badge>

        </div>

        {/* Details */}

        <div className="grid gap-3 md:grid-cols-3">

          <div className="flex items-center gap-2 text-sm">

            <Clock size={18} />

            <span>
              {request.estimatedHours} Hours
            </span>

          </div>

          <div className="flex items-center gap-2 text-sm">

            <Coins size={18} />

            <span>
              {request.tokenCost} Tokens
            </span>

          </div>

          <div className="flex items-center gap-2 text-sm">

            <User size={18} />

            <span>
              {request.requesterName}
            </span>

          </div>

        </div>

        {/* Provider */}

        {request.selectedProviderName && (
          <div className="rounded-lg border bg-muted/40 p-3 text-sm">

            <span className="font-medium">
              Provider:
            </span>{" "}
            {request.selectedProviderName}

          </div>
        )}

        {/* Footer */}

        <div className="flex flex-wrap items-center justify-between gap-3 border-t pt-4">

          <div className="text-xs text-muted-foreground">

            Created{" "}
                {request.createdAt
                ? new Date(request.createdAt).toLocaleDateString()
                : "-"}

          </div>

          <div className="flex gap-3">

            {showAcceptButton &&
              request.status === ServiceRequestStatus.OPEN && (
                <Button
                  onClick={() => onAccept?.(request.requestId)}
                >
                  <CheckCircle className="mr-2 h-4 w-4" />
                  Accept
                </Button>
              )}

            {showCancelButton &&
              request.status === ServiceRequestStatus.OPEN && (
                <Button
                  variant="destructive"
                  onClick={() => onCancel?.(request.requestId)}
                >
                  <XCircle className="mr-2 h-4 w-4" />
                  Cancel
                </Button>
              )}

          </div>

        </div>

      </CardContent>
    </Card>
  );
}

export default MarketplaceCard;