import { useEffect, useState, type FormEvent } from "react";
import {
  AlertTriangle,
  FileText,
  ShieldCheck,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";

interface OpenDisputeDialogProps {
  open: boolean;
  isSubmitting?: boolean;
  sessionId: string;
  onClose: () => void;
  onSubmit: (reason: string) => Promise<void> | void;
}

const MAX_REASON_LENGTH = 5000;

export default function OpenDisputeDialog({
  open,
  isSubmitting = false,
  sessionId,
  onClose,
  onSubmit,
}: OpenDisputeDialogProps) {
  const [reason, setReason] = useState("");
  const [acknowledgedSharing, setAcknowledgedSharing] = useState(false);

  useEffect(() => {
    if (open) {
      setReason("");
      setAcknowledgedSharing(false);
    }
  }, [open]);

  if (!open) {
    return null;
  }

  const trimmedReason = reason.trim();

  const canSubmit =
    trimmedReason.length > 0 &&
    trimmedReason.length <= MAX_REASON_LENGTH &&
    acknowledgedSharing &&
    !isSubmitting;

  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>,
  ) => {
    event.preventDefault();

    if (!canSubmit) {
      return;
    }

    await onSubmit(trimmedReason);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <form
        role="dialog"
        aria-modal="true"
        aria-labelledby="open-dispute-title"
        onSubmit={handleSubmit}
        className="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-xl border bg-background p-6 text-foreground shadow-xl"
      >
        <div className="flex items-start gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-destructive/10">
            <AlertTriangle
              className="text-destructive"
              size={22}
            />
          </div>

          <div>
            <h2
              id="open-dispute-title"
              className="text-xl font-bold"
            >
              Open a dispute
            </h2>

            <p className="mt-1 text-sm text-muted-foreground">
              Session reference:{" "}
              <span className="font-mono">{sessionId}</span>
            </p>
          </div>
        </div>

        <div className="mt-5 rounded-lg border p-4">
          <div className="flex items-start gap-2">
            <ShieldCheck
              className="mt-0.5 shrink-0 text-primary"
              size={19}
            />

            <div>
              <h3 className="font-semibold">
                What happens when you submit
              </h3>

              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                The backend opens a persistent dispute case, marks the
                session as disputed, and assigns the review panel. Based on
                the current backend implementation, existing session proofs,
                negotiation messages, and agreement-version history are
                associated with the case. The assigned panel can review the
                reason and the evidence references returned by the backend.
              </p>
            </div>
          </div>
        </div>

        <div className="mt-5 space-y-2">
          <label
            htmlFor="dispute-reason"
            className="text-sm font-semibold"
          >
            Explain the dispute{" "}
            <span className="text-destructive">*</span>
          </label>

          <Textarea
            id="dispute-reason"
            value={reason}
            onChange={(event) =>
              setReason(event.target.value.slice(0, MAX_REASON_LENGTH))
            }
            placeholder="Describe what happened, what was agreed, what remains unresolved, and why you are raising this dispute."
            rows={7}
            maxLength={MAX_REASON_LENGTH}
            disabled={isSubmitting}
            required
            aria-required="true"
            className="min-h-36 resize-y"
          />

          <p className="text-right text-xs text-muted-foreground">
            {reason.length}/{MAX_REASON_LENGTH} characters
          </p>
        </div>

        <div className="mt-4 rounded-lg border border-amber-500/40 bg-amber-500/5 p-4">
          <div className="flex items-start gap-2">
            <FileText
              className="mt-0.5 shrink-0"
              size={18}
            />

            <div className="text-sm leading-6">
              <p className="font-medium">
                Evidence and finality
              </p>

              <p className="mt-1 text-muted-foreground">
                The current dispute-opening API accepts a written reason,
                not file uploads. If you need to provide additional files
                or links, close this dialog and submit them through the
                existing Session Proofs panel before opening the dispute.
                Existing session proofs are associated with the case when
                the dispute is created. The panel decision has no appeal
                action in the current workflow.
              </p>
            </div>
          </div>
        </div>

        <label className="mt-5 flex cursor-pointer items-start gap-3 text-sm">
          <input
            type="checkbox"
            className="mt-1"
            checked={acknowledgedSharing}
            onChange={(event) =>
              setAcknowledgedSharing(event.target.checked)
            }
            disabled={isSubmitting}
          />

          <span>
            I understand that my reason and relevant existing session,
            negotiation, proof, and agreement-version records may be
            shared with the assigned dispute panel for review.
          </span>
        </label>

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Cancel
          </Button>

          <Button
            type="submit"
            disabled={!canSubmit}
          >
            {isSubmitting ? "Submitting dispute…" : "Submit dispute"}
          </Button>
        </div>
      </form>
    </div>
  );
}