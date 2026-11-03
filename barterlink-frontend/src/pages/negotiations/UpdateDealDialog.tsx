import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";

import {
  useEffect,
  useState,
} from "react";

import { Input } from "@/components/ui/input";

import { Label } from "@/components/ui/label";

import {
  updateProposal,
} from "@/services/NegotiationService";

import toast
from "react-hot-toast";

import { Button } from "@/components/ui/button";

import type Negotiation
from "@/models/negotiation/Negotiation";

interface UpdateDealDialogProps {

  open: boolean;

  onOpenChange: (
    open: boolean
  ) => void;

  negotiation: Negotiation | null;

  onSuccess: () => void;

}

function UpdateDealDialog({

  open,

  onOpenChange,

  negotiation,

  onSuccess,

}: UpdateDealDialogProps) {

    const [offerTitle, setOfferTitle] =
  useState("");

const [offerDescription, setOfferDescription] =
  useState("");

const [requestTitle, setRequestTitle] =
  useState("");

const [requestDescription, setRequestDescription] =
  useState("");

const [exchangeMode, setExchangeMode] =
  useState<
    "DIRECT_BARTER"
    | "TOKEN_BASED"
    | "HYBRID"
  >("DIRECT_BARTER");

const [estimatedHours, setEstimatedHours] =
  useState(1);

const [tokenAmount, setTokenAmount] =
  useState(0);


  useEffect(() => {

  if (!negotiation) {

    return;

  }

  setOfferTitle(
    negotiation.offerTitle
  );

  setOfferDescription(
    negotiation.offerDescription
  );

  setRequestTitle(
    negotiation.requestTitle
  );

  setRequestDescription(
    negotiation.requestDescription
  );

  setExchangeMode(
    negotiation.exchangeMode
  );

  setEstimatedHours(
    negotiation.estimatedHours
  );

  setTokenAmount(
    negotiation.tokenAmount ?? 0
  );

}, [
  negotiation,
]);


const handleSave = async () => {

  if (!negotiation) {

    return;

  }

  try {

    await updateProposal(

      negotiation.negotiationId,

      {

        offerTitle,

        offerDescription,

        requestTitle,

        requestDescription,

        exchangeMode,

        estimatedHours,

        tokenAmount,

      }

    );

toast.success(
  "Deal updated successfully."
);

onOpenChange(false);

onSuccess();

  } catch (error: any) {

    toast.error(

      error?.response?.data?.message ??

      "Unable to update deal."

    );

  }

};


  return (

    <Dialog
      open={open}
      onOpenChange={onOpenChange}
    >

      <DialogContent className="sm:max-w-3xl">

        <DialogHeader>

          <DialogTitle>

            Update Current Deal

          </DialogTitle>

          <DialogDescription>

            Modify the current proposal before
            sending it to the other participant.

          </DialogDescription>

        </DialogHeader>

<div className="space-y-6">

  {/* Offering */}

  <div>

    <label className="mb-2 block text-sm font-medium">

      Offering Title

    </label>

<div className="space-y-2">

  <Label>
    Offer Title
  </Label>

  <Input
    value={offerTitle}
    onChange={(e) =>
      setOfferTitle(e.target.value)
    }
  />

</div>

  </div>

  <div>

    <label className="mb-2 block text-sm font-medium">

      Offering Description

    </label>

    <textarea
      rows={4}
      value={offerDescription}
      onChange={(e) =>
        setOfferDescription(
          e.target.value
        )
      }
      className="
        w-full
        rounded-lg
        border
        p-3
      "
    />

  </div>

  {/* Looking For */}

  <div>

    <label className="mb-2 block text-sm font-medium">

      Request Title

    </label>

<div className="space-y-2">

  <Label>
    Request Title
  </Label>

  <Input
    value={requestTitle}
    onChange={(e) =>
      setRequestTitle(e.target.value)
    }
  />

</div>

  </div>

  <div>

    <label className="mb-2 block text-sm font-medium">

      Request Description

    </label>

    <textarea
      rows={4}
      value={requestDescription}
      onChange={(e) =>
        setRequestDescription(
          e.target.value
        )
      }
      className="
        w-full
        rounded-lg
        border
        p-3
      "
    />

  </div>

  {/* Exchange Mode */}

<div>

  <label className="mb-2 block text-sm font-medium">

    Exchange Mode

  </label>

  <select
    value={exchangeMode}
    onChange={(e) =>
      setExchangeMode(
        e.target.value as
          | "DIRECT_BARTER"
          | "TOKEN_BASED"
          | "HYBRID"
      )
    }
    className="
      w-full
      rounded-lg
      border
      p-3
    "
  >

    <option value="DIRECT_BARTER">

      Direct Barter

    </option>

    <option value="TOKEN_BASED">

      Token Based

    </option>

    <option value="HYBRID">

      Hybrid

    </option>

  </select>

</div>


<div>

  <label className="mb-2 block text-sm font-medium">

    Estimated Hours

  </label>

<div className="space-y-2">

  <Label>
    Estimated Hours
  </Label>

  <Input
    type="number"
    value={estimatedHours}
    onChange={(e) =>
      setEstimatedHours(
        Number(e.target.value)
      )
    }
  />

</div>

</div>


{exchangeMode !== "DIRECT_BARTER" && (

  <div>

    <label className="mb-2 block text-sm font-medium">

      Token Amount

    </label>

<div className="space-y-2">

  <Label>
    Token Amount
  </Label>

  <Input
    type="number"
    value={tokenAmount}
    onChange={(e) =>
      setTokenAmount(
        Number(e.target.value)
      )
    }
  />

</div>

  </div>

)}

</div>

        <div
          className="
          flex
          justify-end
          gap-3
          border-t
          pt-5
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

<Button
  onClick={handleSave}
>

  Save Deal

</Button>

        </div>

      </DialogContent>

    </Dialog>

  );

}

export default UpdateDealDialog;