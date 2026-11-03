import {
  MessageCircle,
  Zap,
} from "lucide-react";

import type ServiceRequest
from "@/models/marketplace/ServiceRequest";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import { Card } from "@/components/ui/card";

import { Button } from "@/components/ui/button";

interface InterestActionDialogProps {

  open: boolean;

  onOpenChange: (
    open: boolean
  ) => void;

  request: ServiceRequest | null;

onChat: () => void;

onDirectInterest: () => void;

}

function InterestActionDialog({

  open,

  onOpenChange,

  request,

  onChat,

  onDirectInterest,

}: InterestActionDialogProps) {

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
        sm:max-w-2xl
        max-h-[90vh]
        overflow-y-auto
        "
      >

        <DialogHeader>

          <DialogTitle>

            Interested in this Request

          </DialogTitle>

          <DialogDescription>

            Choose how you would like to continue with

            <span className="font-medium">

              {" "}{request.title}

            </span>

          </DialogDescription>

        </DialogHeader>

        <div className="space-y-6">
            
            {/* Chat with Requester */}

<Card
  className="
  p-6
  transition-all
  hover:shadow-md
  "
>

  <div className="flex items-start gap-4">

    <div
      className="
      flex
      h-12
      w-12
      items-center
      justify-center
      rounded-full
      bg-primary/10
      "
    >

      <MessageCircle
        className="text-primary"
        size={24}
      />

    </div>

    <div className="flex-1 space-y-3">

      <div>

        <h3 className="text-lg font-semibold">

          Chat with Requester

        </h3>

        <p className="mt-1 text-sm text-muted-foreground">

          Start a conversation before committing.

          Discuss the requirements, clarify doubts,

          negotiate exchange terms and decide

          whether both of you are a good match.

        </p>

      </div>

      <Button
        onClick={onChat}
      >

        <MessageCircle
          className="mr-2 h-4 w-4"
        />

        Continue to Chat

      </Button>

    </div>

  </div>

</Card>

{/* Direct Interest */}

<Card
  className="
  p-6
  transition-all
  hover:shadow-md
  "
>

  <div className="flex items-start gap-4">

    <div
      className="
      flex
      h-12
      w-12
      items-center
      justify-center
      rounded-full
      bg-emerald-100
      dark:bg-emerald-900/30
      "
    >

      <Zap
        className="text-emerald-600"
        size={24}
      />

    </div>

    <div className="flex-1 space-y-3">

      <div>

        <h3 className="text-lg font-semibold">

          Continue Without Chat

        </h3>

        <p className="mt-1 text-sm text-muted-foreground">

          You're confident you can help immediately.

          Your interest will be sent directly to the requester.

          They can review your profile and decide whether
          to continue.

        </p>

      </div>

      <Button
        variant="secondary"
        onClick={onDirectInterest}
      >

        <Zap
          className="mr-2 h-4 w-4"
        />

        Continue

      </Button>

    </div>

  </div>

</Card>

        </div>

        <div
          className="
          flex
          justify-end
          border-t
          pt-6
          "
        >

          <Button
            variant="outline"
            onClick={() =>
              onOpenChange(false)
            }
          >

            Cancel

          </Button>

        </div>

      </DialogContent>

    </Dialog>

  );

}

export default InterestActionDialog;