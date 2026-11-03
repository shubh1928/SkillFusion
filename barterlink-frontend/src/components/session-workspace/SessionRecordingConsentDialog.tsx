import { useEffect, useState } from "react";

interface SessionRecordingConsentDialogProps {
  open: boolean;
  isSubmitting?: boolean;
  onClose: () => void;
  onSubmit: (
    recordingConsent: boolean,
    educationalReuseConsent: boolean
  ) => void;
}

function SessionRecordingConsentDialog({
  open,
  isSubmitting = false,
  onClose,
  onSubmit,
}: SessionRecordingConsentDialogProps) {
  const [recordingConsent, setRecordingConsent] =
    useState<boolean | null>(null);

  const [educationalReuseConsent, setEducationalReuseConsent] =
    useState(false);

  useEffect(() => {
    if (open) {
      setRecordingConsent(null);
      setEducationalReuseConsent(false);
    }
  }, [open]);

  if (!open) {
    return null;
  }

  const handleSubmit = () => {
    if (recordingConsent === null || isSubmitting) {
      return;
    }

    onSubmit(
      recordingConsent,
      recordingConsent && educationalReuseConsent
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="recording-consent-title"
        className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-xl border bg-background p-6 text-foreground shadow-xl"
      >
        <h2
          id="recording-consent-title"
          className="text-xl font-bold"
        >
          Live Session Recording Consent
        </h2>

        <p className="mt-3 text-sm leading-6 text-muted-foreground">
          Live video sessions require recording consent before
          recording can begin. Please read the following
          information before making your decision.
        </p>

        <section className="mt-5 space-y-3">
          <h3 className="font-semibold">
            Why is the session recorded?
          </h3>

          <p className="text-sm leading-6">
            The full, unedited recording is intended to verify
            session participation and work progress, support
            platform security, and help resolve disputes.
            Access is restricted to authorized persons.
          </p>

          <h3 className="font-semibold">
            Retention and deletion
          </h3>

          <p className="text-sm leading-6">
            When the session is completed and both participants
            have been notified, the applicable seven-calendar-day
            dispute period begins. If no dispute is raised, the
            original recording is scheduled for deletion within
            the following 24 hours. If a dispute or lawful
            preservation obligation applies, retention may continue
            according to the applicable policy.
          </p>

          <h3 className="font-semibold">
            Educational and commercial reuse
          </h3>

          <p className="text-sm leading-6">
            BarterLink may create a separate edited educational
            version only with the necessary permissions and
            rights. The original evidence recording remains
            separate. Educational reuse is optional and is not
            required for dispute-related recording.
          </p>
        </section>

        <section className="mt-6 rounded-lg border p-4">
          <h3 className="font-semibold">
            1. Required recording decision
          </h3>

          <p className="mt-1 text-sm text-muted-foreground">
            Choose one option. Recording cannot begin without
            both participants' consent.
          </p>

          <div className="mt-4 space-y-3">
            <label className="flex cursor-pointer items-start gap-3">
              <input
                type="radio"
                name="recordingConsent"
                checked={recordingConsent === true}
                disabled={isSubmitting}
                onChange={() => setRecordingConsent(true)}
                className="mt-1"
              />

              <span className="text-sm">
                <span className="block font-medium">
                  I consent to session recording.
                </span>

                <span className="block text-muted-foreground">
                  I understand the stated purposes, restricted
                  access, retention, and deletion rules.
                </span>
              </span>
            </label>

            <label className="flex cursor-pointer items-start gap-3">
              <input
                type="radio"
                name="recordingConsent"
                checked={recordingConsent === false}
                disabled={isSubmitting}
                onChange={() => {
                  setRecordingConsent(false);
                  setEducationalReuseConsent(false);
                }}
                className="mt-1"
              />

              <span className="text-sm">
                <span className="block font-medium">
                  I do not consent to session recording.
                </span>

                <span className="block text-muted-foreground">
                  Recording must not begin based on this decision.
                </span>
              </span>
            </label>
          </div>
        </section>

        <section className="mt-4 rounded-lg border p-4">
          <h3 className="font-semibold">
            2. Optional educational reuse permission
          </h3>

          <label className="mt-3 flex cursor-pointer items-start gap-3">
            <input
              type="checkbox"
              checked={educationalReuseConsent}
              disabled={
                isSubmitting || recordingConsent !== true
              }
              onChange={(event) =>
                setEducationalReuseConsent(event.target.checked)
              }
              className="mt-1"
            />

            <span className="text-sm">
              I separately permit the creation, storage, editing,
              distribution, and applicable commercial use of an
              approved educational version, subject to the
              applicable licence, necessary permissions, and laws.
            </span>
          </label>

          <p className="mt-2 text-xs text-muted-foreground">
            This permission is optional. Leaving it unchecked
            means educational reuse is not authorized by this
            decision.
          </p>
        </section>

        <p className="mt-4 text-xs leading-5 text-muted-foreground">
          Before real users are asked to consent, the Recording
          Policy, Privacy Policy, and Educational Content Licence
          must be accessible, and their approved versions must
          match the policy version recorded by the backend.
        </p>

        {recordingConsent === false && (
          <p
            role="status"
            className="mt-4 rounded-lg border border-amber-500 p-3 text-sm"
          >
            Your decision will be recorded. Recording will not
            start. Continuing without recording requires explicit
            agreement from both participants; otherwise, the
            session must not proceed as an unrecorded live session.
          </p>
        )}

        <div className="mt-6 flex flex-wrap justify-end gap-3">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={onClose}
            className="rounded-lg border px-4 py-2 disabled:opacity-50"
          >
            Cancel
          </button>

          <button
            type="button"
            disabled={
              isSubmitting || recordingConsent === null
            }
            onClick={handleSubmit}
            className="rounded-lg bg-primary px-4 py-2 text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isSubmitting
              ? "Submitting..."
              : "Submit Decision"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default SessionRecordingConsentDialog;