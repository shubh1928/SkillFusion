import { useEffect, useState } from "react";
import toast from "react-hot-toast";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import type Skill from "@/models/skills/Skill";
import type CreateServiceRequest from "@/models/marketplace/CreateServiceRequest";

import { getAllSkills } from "@/services/SkillService";
import { createRequest } from "@/services/MarketplaceService";
import { getWallet } from "@/services/WalletService";

interface CreateRequestDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onRequestCreated: () => void;
}

const initialForm: CreateServiceRequest = {

  title: "",

  exchangeMode: "" as any,

  offerTitle: "",

  offerDescription: "",

  requestTitle: "",

  requestDescription: "",

  estimatedHours: 1,

  tokenAmount: undefined,

  skillIds: [],

  selectiveRequest: false,

};

function CreateRequestDialog({

  open,

  onOpenChange,

  onRequestCreated,

}: CreateRequestDialogProps) {

  const [loading, setLoading] =
    useState(false);

  const [availableSkills, setAvailableSkills] =
    useState<Skill[]>([]);

  const [form, setForm] =
    useState<CreateServiceRequest>(
      initialForm
    );

    const [availableTokens, setAvailableTokens] =
  useState<number | null>(null);

const [walletLoading, setWalletLoading] =
  useState(false);

const [walletBalanceError, setWalletBalanceError] =
  useState(false);

  const handleChange = (

    field: keyof CreateServiceRequest,

    value: any

  ) => {

    setForm(previous => ({

      ...previous,

      [field]: value,

    }));

  };

  const toggleSkill = (

    skillId: string

  ) => {

    setForm(previous => {

      const exists =
        previous.skillIds.includes(skillId);

      return {

        ...previous,

        skillIds: exists

          ? previous.skillIds.filter(
              id => id !== skillId
            )

          : [
              ...previous.skillIds,
              skillId,
            ],

      };

    });

  };

  const loadSkills = async () => {

    try {

      const skills =
        await getAllSkills();

      setAvailableSkills(skills);

    } catch {

      toast.error(
        "Failed to load skills."
      );

    }

  };

  useEffect(() => {

    if (open) {

      loadSkills();

    }

  }, [open]);


useEffect(() => {
  const requiresTokenBalance =
    form.exchangeMode === "TOKEN_BASED" ||
    form.exchangeMode === "HYBRID";

  if (!open || !requiresTokenBalance) {
    setAvailableTokens(null);
    setWalletLoading(false);
    setWalletBalanceError(false);
    return;
  }

  let cancelled = false;

  const loadWalletBalance = async () => {
    setWalletLoading(true);
    setWalletBalanceError(false);
    setAvailableTokens(null);

    try {
      const wallet = await getWallet();
      const balance = wallet.availableTokens;

      if (
        typeof balance !== "number" ||
        !Number.isFinite(balance) ||
        balance < 0
      ) {
        throw new Error("Invalid available balance");
      }

      if (!cancelled) {
        setAvailableTokens(balance);
      }
    } catch {
      if (!cancelled) {
        setAvailableTokens(null);
        setWalletBalanceError(true);
      }
    } finally {
      if (!cancelled) {
        setWalletLoading(false);
      }
    }
  };

  void loadWalletBalance();

  return () => {
    cancelled = true;
  };
}, [open, form.exchangeMode]);


  const handleDialogChange = (
    isOpen: boolean
  ) => {

    if (!isOpen) {

      setForm(initialForm);

    }

    onOpenChange(isOpen);

  };

  const requiresTokenBalance =
  form.exchangeMode === "TOKEN_BASED" ||
  form.exchangeMode === "HYBRID";

const tokenAmountInvalid =
  typeof form.tokenAmount !== "number" ||
  !Number.isInteger(form.tokenAmount) ||
  form.tokenAmount < 1;

const fundingUnavailable =
  requiresTokenBalance &&
  (
    tokenAmountInvalid ||
    walletLoading ||
    walletBalanceError ||
    availableTokens === null ||
    (
      typeof form.tokenAmount === "number" &&
      availableTokens < form.tokenAmount
    )
  );

  const handleSubmit = async () => {

    if (!form.title.trim()) {

      toast.error("Title is required");

      return;

    }

    if (!form.exchangeMode) {

      toast.error(
        "Please select an exchange mode."
      );

      return;

    }

    if (

      form.exchangeMode !== "TOKEN_BASED"

    ) {

      if (!form.offerTitle.trim()) {

        toast.error(
          "Offer title is required."
        );

        return;

      }

      if (!form.offerDescription.trim()) {

        toast.error(
          "Offer description is required."
        );

        return;

      }

    }

    if (!form.requestTitle.trim()) {

      toast.error(
        "Request title is required."
      );

      return;

    }

    if (!form.requestDescription.trim()) {

      toast.error(
        "Request description is required."
      );

      return;

    }

    if (form.estimatedHours < 1) {

      toast.error(
        "Estimated hours must be at least 1."
      );

      return;

    }

if (requiresTokenBalance && tokenAmountInvalid) {
  toast.error(
    "Token amount must be a positive whole number of BLT."
  );
  return;
}

if (requiresTokenBalance) {
  if (
    walletLoading ||
    walletBalanceError ||
    availableTokens === null
  ) {
    toast.error(
      "Unable to verify your available BLT balance. Please close and reopen the form to retry."
    );
    return;
  }

  if (availableTokens < (form.tokenAmount ?? 0)) {
    toast.error(
      "Insufficient available BLT balance."
    );
    return;
  }
}

    try {

      setLoading(true);

      await createRequest(form);

      toast.success(
        "Marketplace request created."
      );

      handleDialogChange(false);

      onRequestCreated();

    } catch (error: any) {

      toast.error(

        error?.response?.data?.message ??

        "Failed to create request."

      );

    } finally {

      setLoading(false);

    }

  };

  return (

    <Dialog
      open={open}
      onOpenChange={handleDialogChange}
    >

      <DialogContent
  className="
    w-[95vw]
    max-w-4xl
    max-h-[90vh]
    overflow-y-auto
  "
>

        <DialogHeader>

          <DialogTitle>
            Create Marketplace Request
          </DialogTitle>

          <DialogDescription>
            Describe what you need and what you are willing to exchange.
          </DialogDescription>

        </DialogHeader>

        <div className="space-y-6">

          {/* Title */}

          <div className="space-y-2">

            <Label>
              Request Title
            </Label>

            <Input
              placeholder="Need help building a Spring Boot REST API"
              value={form.title}
              onChange={(e) =>
                handleChange(
                  "title",
                  e.target.value
                )
              }
            />

          </div>

          {/* Exchange Mode */}

          <div className="space-y-2">

            <Label>
              Exchange Mode
            </Label>

            <select
              value={form.exchangeMode}
              onChange={(e) =>
                handleChange(
                  "exchangeMode",
                  e.target.value
                )
              }
              className="
                w-full
                rounded-md
                border
                bg-background
                px-3
                py-2
              "
            >

              <option value="">
                Select Exchange Mode
              </option>

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

          {/* Offer Section */}

          {form.exchangeMode !== "TOKEN_BASED" && (

            <div className="space-y-5 rounded-lg border p-5">

              <h3 className="font-semibold">

                What are you offering?

              </h3>

              <div className="space-y-2">

                <Label>
                  Offer Title
                </Label>

                <Input
                  value={form.offerTitle}
                  onChange={(e) =>
                    handleChange(
                      "offerTitle",
                      e.target.value
                    )
                  }
                />

              </div>

              <div className="space-y-2">

                <Label>
                  Offer Description
                </Label>

                <textarea
                  rows={4}
                  className="
                    w-full
                    rounded-md
                    border
                    bg-transparent
                    p-3
                  "
                  value={form.offerDescription}
                  onChange={(e) =>
                    handleChange(
                      "offerDescription",
                      e.target.value
                    )
                  }
                />

              </div>

            </div>

          )}

          {/* Request Section */}

          <div className="space-y-5 rounded-lg border p-5">

            <h3 className="font-semibold">

              What do you need?

            </h3>

            <div className="space-y-2">

              <Label>
                Request Title
              </Label>

              <Input
                value={form.requestTitle}
                onChange={(e) =>
                  handleChange(
                    "requestTitle",
                    e.target.value
                  )
                }
              />

            </div>

            <div className="space-y-2">

              <Label>
                Request Description
              </Label>

              <textarea
                rows={4}
                className="
                  w-full
                  rounded-md
                  border
                  bg-transparent
                  p-3
                "
                value={form.requestDescription}
                onChange={(e) =>
                  handleChange(
                    "requestDescription",
                    e.target.value
                  )
                }
              />

            </div>

          </div>

          {/* Required Skills */}

          <div className="space-y-3">

            <Label>

              Required Skills

            </Label>

            <div className="grid grid-cols-2 gap-3 md:grid-cols-3">

              {availableSkills.map((skill) => {

                const selected =
                  form.skillIds.includes(skill.id);

                return (

                  <button
                    key={skill.id}
                    type="button"
                    onClick={() =>
                      toggleSkill(skill.id)
                    }
                    className={`
                      rounded-lg
                      border
                      px-4
                      py-2
                      text-sm
                      transition
                      ${
                        selected
                          ? "border-primary bg-primary text-primary-foreground"
                          : "hover:border-primary"
                      }
                    `}
                  >

                    {skill.name}

                  </button>

                );

              })}

            </div>

          </div>
          {/* Estimated Hours */}

          <div className="space-y-2">

            <Label>
              Estimated Hours
            </Label>

            <Input
              type="number"
              min={1}
              value={form.estimatedHours}
              onChange={(e) =>
                handleChange(
                  "estimatedHours",
                  Number(e.target.value)
                )
              }
            />

          </div>

          {/* Token Amount */}

          {form.exchangeMode !== "DIRECT_BARTER" && (

            <div className="space-y-2">

              <Label>
                Token Amount
              </Label>

              <Input
                type="number"
                min={1}
                step={1}
                value={form.tokenAmount ?? ""}
                onChange={(e) =>
                  handleChange(
                    "tokenAmount",
                    Number(e.target.value)
                  )
                }
              />

              {walletLoading && (
  <p className="text-sm text-muted-foreground">
    Checking your available BLT balance...
  </p>
)}

{!walletLoading && walletBalanceError && (
  <p className="text-sm text-destructive" role="alert">
    Your available BLT balance could not be verified.
    Close and reopen this form to retry.
  </p>
)}

{!walletLoading &&
  !walletBalanceError &&
  availableTokens !== null && (
    <p className="text-sm text-muted-foreground">
      Available balance:{" "}
      <span className="font-semibold">
        {availableTokens} BLT
      </span>
    </p>
  )}

{!tokenAmountInvalid &&
  !walletLoading &&
  !walletBalanceError &&
  availableTokens !== null &&
  typeof form.tokenAmount === "number" &&
  availableTokens < form.tokenAmount && (
    <p className="text-sm text-destructive" role="alert">
      Insufficient available BLT for this request.
    </p>
  )}

            </div>

          )}

          {/* Selective Request */}

          <div className="flex items-center gap-3">

            <input
              id="selectiveRequest"
              type="checkbox"
              checked={form.selectiveRequest}
              onChange={(e) =>
                handleChange(
                  "selectiveRequest",
                  e.target.checked
                )
              }
            />

            <Label htmlFor="selectiveRequest">

              Selective Request

            </Label>

          </div>

        </div>

        <DialogFooter>

          <Button
            variant="outline"
            onClick={() =>
              handleDialogChange(false)
            }
          >

            Cancel

          </Button>

          <Button
            onClick={handleSubmit}
            disabled={loading || fundingUnavailable}
          >

            {loading
              ? "Creating..."
              : "Create Request"}

          </Button>

        </DialogFooter>

      </DialogContent>

    </Dialog>

  );

}

export default CreateRequestDialog;