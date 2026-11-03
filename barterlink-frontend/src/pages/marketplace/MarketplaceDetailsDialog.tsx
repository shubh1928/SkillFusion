import { useEffect, useState } from "react";
import toast from "react-hot-toast";

import {
  Clock,
  User,
  Handshake,
  Coins,
} from "lucide-react";

import {
  getRequestById,
} from "@/services/MarketplaceService";

import {
  expressInterest,
} from "@/services/InterestService";


import type ServiceRequest
from "@/models/marketplace/ServiceRequest";

import { Badge } from "@/components/ui/badge";

import { Button } from "@/components/ui/button";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

interface MarketplaceDetailsDialogProps {

  requestId: string | null;

  open: boolean;

  onOpenChange: (
    open: boolean
  ) => void;

}

function MarketplaceDetailsDialog({

  requestId,

  open,

  onOpenChange,

}: MarketplaceDetailsDialogProps) {

  const [loading, setLoading] =
    useState(false);

  const [request, setRequest] =
    useState<ServiceRequest | null>(null);

  const handleInterested = async () => {

    if (!request) return;

    try {

      await expressInterest(
        request.requestId
      );

      toast.success(
        "Interest sent successfully."
      );

} catch (error: any) {

  const message = error?.response?.data?.message;

  const normalizedMessage =
    typeof message === "string"
      ? message.toLowerCase()
      : "";

  if (
    normalizedMessage.includes("insufficient available blt") ||
    normalizedMessage.includes("requester does not have a wallet")
  ) {
    setRequest(current =>
      current
        ? {
            ...current,
            requesterHasSufficientTokens: false,
          }
        : current
    );
  }

  toast.error(
    message ?? "Unable to express interest."
  );

}

  };

  useEffect(() => {

    if (!open || !requestId) {

      setRequest(null);

      return;

    }

    const loadRequest =
      async () => {

        try {

          setLoading(true);

          const response =
            await getRequestById(
              requestId
            );

          setRequest(response);

        } catch {

          toast.error(
            "Failed to load request."
          );

        } finally {

          setLoading(false);

        }

      };

    loadRequest();

  }, [open, requestId]);

  return (

    <Dialog
      open={open}
      onOpenChange={onOpenChange}
    >

      <DialogContent className="max-w-3xl">

        <DialogHeader>

          <DialogTitle>

            {request?.title}

          </DialogTitle>

          <DialogDescription>

            Complete marketplace request details.

          </DialogDescription>

        </DialogHeader>

        {loading ? (

          <div className="py-16 text-center">

            Loading...

          </div>

        ) : request && (

          <div className="space-y-6">

            {/* Header */}

            <div className="flex flex-wrap items-center justify-between gap-4">

              <div className="flex gap-3">

                <Badge>

                  <Handshake
                    className="mr-1 h-3 w-3"
                  />

                  {request.exchangeMode.replace(
                    "_",
                    " "
                  )}

                </Badge>

                <Badge
                  variant="outline"
                >

                  {request.status}

                </Badge>

              </div>

              <div className="text-sm text-muted-foreground">

                Posted on{" "}

                {new Date(
                  request.createdAt
                ).toLocaleDateString()}

              </div>

            </div>

            {/* Basic Information */}

            <div className="grid gap-4 md:grid-cols-2">

              <div className="flex items-center gap-2">

                <User size={18} />

                <span>

                  {request.requesterName}

                </span>

              </div>

              <div className="flex items-center gap-2">

                <Clock size={18} />

                <span>

                  {request.estimatedHours} Hours

                </span>

              </div>

            </div>

            {/* Providing */}

            <div className="rounded-lg border p-5">

              <h3 className="mb-3 text-lg font-semibold text-emerald-600">

                🎁 I Can Provide

              </h3>

              <h4 className="font-semibold">

                {request.offerTitle}

              </h4>

              <p className="mt-2 text-muted-foreground">

                {request.offerDescription}

              </p>

            </div>
            {/* Looking For */}

            <div className="rounded-lg border p-5">

              <h3 className="mb-3 text-lg font-semibold text-sky-600">

                🔍 Looking For

              </h3>

              <h4 className="font-semibold">

                {request.requestTitle}

              </h4>

              <p className="mt-2 text-muted-foreground">

                {request.requestDescription}

              </p>

            </div>

            {/* Required Skills */}

            <div className="rounded-lg border p-5">

              <h3 className="mb-3 text-lg font-semibold">

                Required Skills

              </h3>

              <div className="flex flex-wrap gap-2">

                {request.requiredSkills.length > 0 ? (

                  request.requiredSkills.map((skill) => (

                    <Badge
                      key={skill}
                      variant="secondary"
                    >
                      {skill}
                    </Badge>

                  ))

                ) : (

                  <span className="text-muted-foreground text-sm">

                    No specific skills required.

                  </span>

                )}

              </div>

            </div>

            {/* Exchange Information */}

            <div className="grid gap-4 md:grid-cols-2">

              <div className="rounded-lg border p-4">

                <h3 className="mb-2 font-semibold">

                  Exchange Mode

                </h3>

                <Badge>

                  {request.exchangeMode.replaceAll("_", " ")}

                </Badge>

              </div>

              {(request.exchangeMode === "TOKEN_BASED" ||
                request.exchangeMode === "HYBRID") && (

                <div className="rounded-lg border p-4">

                  <h3 className="mb-2 flex items-center gap-2 font-semibold">

                    <Coins size={18} />

                    Token Amount

                  </h3>

                  <p className="text-lg font-semibold">

                    {request.tokenAmount} Tokens

                  </p>

                </div>

              )}

            </div>

          </div>

        )}

        <DialogFooter>

          {request && (
            <>
              {request.exchangeMode !== "DIRECT_BARTER" &&
                request.requesterHasSufficientTokens === false && (
                  <p className="text-sm text-destructive">
                    The requester currently has insufficient available BLT.
                    Interest is unavailable.
                  </p>
                )}

              <Button
                disabled={
                  request.exchangeMode !== "DIRECT_BARTER" &&
                  request.requesterHasSufficientTokens === false
                }
                onClick={handleInterested}
              >
                ❤️ Interested
              </Button>
            </>
          )}

          <Button
            variant="outline"
            onClick={() =>
              onOpenChange(false)
            }
          >

            Close

          </Button>

        </DialogFooter>

      </DialogContent>

    </Dialog>

  );

}

export default MarketplaceDetailsDialog;