import {
  Clock,
  User,
  Gift,
  Target,
  Tag,
} from "lucide-react";

import useAuth from "@/auth/store";

import type ServiceRequest
from "@/models/marketplace/ServiceRequest";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

interface RequestDetailsDialogProps {

  open: boolean;

  onOpenChange: (open: boolean) => void;

  request: ServiceRequest | null;

  onInterested: (
    request: ServiceRequest
  ) => void;

}

function RequestDetailsDialog({
  

  open,

  onOpenChange,

  request,

  onInterested,

}: RequestDetailsDialogProps) {

  const user = useAuth((state) => state.user);

const isOwner =
  request?.requesterId === user?.id;

  const fundingUnavailable =
  request !== null &&
  request.exchangeMode !== "DIRECT_BARTER" &&
  request.requesterHasSufficientTokens === false;

  if (!request) {

    return null;

  }

  return (

    <Dialog
      open={open}
      onOpenChange={onOpenChange}
    >

      <DialogContent
        className="
        w-[95vw]
        max-w-3xl
        max-h-[90vh]
        overflow-y-auto
        "
      >

        <DialogHeader>

          <div
            className="
            flex
            flex-col
            gap-3
            md:flex-row
            md:items-center
            md:justify-between
            "
          >

            <div>

              <DialogTitle
                className="text-2xl"
              >
                {request.title}
              </DialogTitle>

              <DialogDescription
                className="mt-2"
              >
                Complete details of this
                marketplace request.
              </DialogDescription>

            </div>

            <Badge>

              {request.exchangeMode
                .replace("_", " ")}

            </Badge>

          </div>

        </DialogHeader>

        <div className="space-y-6">
            {/* Posted By + Hours */}

<div
  className="
  grid
  gap-4
  md:grid-cols-2
  "
>

  <div
    className="
    rounded-lg
    border
    p-4
    "
  >

    <div className="flex items-center gap-2">

      <User
        className="text-primary"
        size={18}
      />

      <span className="font-medium">
        Posted By
      </span>

    </div>

    <p className="mt-2 text-muted-foreground">

      {request.requesterName}

    </p>

  </div>

  <div
    className="
    rounded-lg
    border
    p-4
    "
  >

    <div className="flex items-center gap-2">

      <Clock
        className="text-primary"
        size={18}
      />

      <span className="font-medium">

        Estimated Hours

      </span>

    </div>

    <p className="mt-2 text-muted-foreground">

      {request.estimatedHours} Hours

    </p>

  </div>

</div>

{/* What I'm Offering */}

<div
  className="
  rounded-lg
  border
  p-5
  space-y-3
  "
>

  <div className="flex items-center gap-2">

    <Gift
      className="text-primary"
      size={18}
    />

    <h3 className="font-semibold">

      What I'm Offering

    </h3>

  </div>

  <h4 className="font-medium">

    {request.offerTitle}

  </h4>

  <p className="text-sm text-muted-foreground whitespace-pre-line">

    {request.offerDescription}

  </p>

</div>

{/* What I Need */}

<div
  className="
  rounded-lg
  border
  p-5
  space-y-3
  "
>

  <div className="flex items-center gap-2">

    <Target
      className="text-primary"
      size={18}
    />

    <h3 className="font-semibold">

      What I Need

    </h3>

  </div>

  <h4 className="font-medium">

    {request.requestTitle}

  </h4>

  <p className="text-sm text-muted-foreground whitespace-pre-line">

    {request.requestDescription}

  </p>

</div>

{/* Required Skills */}

<div
  className="
  rounded-lg
  border
  p-5
  "
>

  <div className="flex items-center gap-2 mb-4">

    <Tag
      className="text-primary"
      size={18}
    />

    <h3 className="font-semibold">

      Required Skills

    </h3>

  </div>

  <div className="flex flex-wrap gap-2">

    {request.requiredSkills.length === 0 ? (

      <p className="text-sm text-muted-foreground">

        No specific skills required.

      </p>

    ) : (

      request.requiredSkills.map(skill => (

        <Badge
          key={skill}
          variant="secondary"
        >

          {skill}

        </Badge>

      ))

    )}

  </div>

</div>

{/* Additional Details */}

<div
  className="
  grid
  gap-4
  md:grid-cols-2
  "
>

  <div
    className="
    rounded-lg
    border
    p-4
    "
  >

    <p className="text-sm font-medium">

      Selective Request

    </p>

    <p className="mt-2 text-muted-foreground">

      {request.selectiveRequest
        ? "Yes"
        : "No"}

    </p>

  </div>

  <div
    className="
    rounded-lg
    border
    p-4
    "
  >

    <p className="text-sm font-medium">

      Posted On

    </p>

    <p className="mt-2 text-muted-foreground">

      {new Date(
        request.createdAt
      ).toLocaleDateString()}

    </p>

  </div>

</div>

</div>

<DialogFooter
  className="
  pt-6
  border-t
  flex
  flex-col
  gap-3
  sm:flex-row
  sm:justify-end
  "
>

  <Button
    variant="outline"
    onClick={() =>
      onOpenChange(false)
    }
  >

    Close

  </Button>

{!isOwner && (
  <>
    {fundingUnavailable && (
      <p className="text-sm text-destructive">
        The requester currently has insufficient available BLT.
        Interest is unavailable.
      </p>
    )}

    <Button
      disabled={fundingUnavailable}
      onClick={() => onInterested(request)}
    >
      Interested
    </Button>
  </>
)}

</DialogFooter>

</DialogContent>

</Dialog>

);

}

export default RequestDetailsDialog;
