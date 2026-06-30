import { useState } from "react";
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

import type CreateServiceRequest from "@/models/marketplace/CreateServiceRequest";

import { createRequest } from "@/services/MarketplaceService";

interface CreateRequestDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onRequestCreated: () => void;
}

const initialForm: CreateServiceRequest = {
  title: "",
  description: "",
  estimatedHours: 1,
  tokenCost: 1,
  selectiveRequest: false,
};

function CreateRequestDialog({
  open,
  onOpenChange,
  onRequestCreated,
}: CreateRequestDialogProps) {

  const [loading, setLoading] = useState(false);

  const [form, setForm] =
    useState<CreateServiceRequest>(initialForm);

  const handleChange = (
    field: keyof CreateServiceRequest,
    value: string | number | boolean
  ) => {

    setForm((previous) => ({
      ...previous,
      [field]: value,
    }));

  };


const handleDialogChange = (isOpen: boolean) => {

  if (!isOpen) {
    setForm(initialForm);
  }

  onOpenChange(isOpen);

};

  const handleSubmit = async () => {

    if (!form.title.trim()) {
      toast.error("Title is required");
      return;
    }

    if (!form.description.trim()) {
      toast.error("Description is required");
      return;
    }

    if (form.estimatedHours < 1) {
      toast.error("Estimated hours must be at least 1");
      return;
    }

    if (form.tokenCost < 1) {
      toast.error("Token cost must be at least 1");
      return;
    }

    try {

      setLoading(true);

      await createRequest(form);

    toast.success("Request created successfully");

    handleDialogChange(false);

    onRequestCreated();

    } catch (error: any) {

      toast.error(
        error?.response?.data?.message ??
        "Failed to create request"
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

      <DialogContent className="sm:max-w-xl">

        <DialogHeader>

          <DialogTitle>
            Create Marketplace Request
          </DialogTitle>

          <DialogDescription>
            Describe the service you need from the community.
          </DialogDescription>

        </DialogHeader>

        <div className="space-y-5">

          {/* Title */}

          <div className="space-y-2">

            <Label>
              Title
            </Label>

            <Input
              placeholder="Need help with Spring Boot..."
              value={form.title}
              onChange={(e) =>
                handleChange(
                  "title",
                  e.target.value
                )
              }
            />

          </div>

          {/* Description */}

          <div className="space-y-2">

            <Label>
              Description
            </Label>

            <textarea
              className="
              w-full
              rounded-md
              border
              bg-transparent
              p-3
              text-sm
              outline-none
              focus:ring-2
              focus:ring-primary
              "
              rows={5}
              placeholder="Describe your requirement..."
              value={form.description}
              onChange={(e) =>
                handleChange(
                  "description",
                  e.target.value
                )
              }
            />

          </div>

          {/* Hours */}

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

          {/* Tokens */}

          <div className="space-y-2">

            <Label>
              Token Cost
            </Label>

            <Input
              type="number"
              min={1}
              value={form.tokenCost}
              onChange={(e) =>
                handleChange(
                  "tokenCost",
                  Number(e.target.value)
                )
              }
            />

          </div>

          {/* Selective */}

          <div className="flex items-center gap-3">

            <input
              id="selective"
              type="checkbox"
              checked={form.selectiveRequest}
              onChange={(e) =>
                handleChange(
                  "selectiveRequest",
                  e.target.checked
                )
              }
            />

            <Label htmlFor="selective">
              Selective Request
            </Label>

          </div>

        </div>

        <DialogFooter>

        <Button
        variant="outline"
        onClick={() => handleDialogChange(false)}
        >
        Cancel
        </Button>

          <Button
            onClick={handleSubmit}
            disabled={loading}
          >
            {
              loading
                ? "Creating..."
                : "Create Request"
            }
          </Button>

        </DialogFooter>

      </DialogContent>

    </Dialog>
  );
}

export default CreateRequestDialog;