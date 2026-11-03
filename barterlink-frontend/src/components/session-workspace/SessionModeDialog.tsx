import SessionMode from "@/models/sessions/SessionMode";

interface SessionModeDialogProps {
  open: boolean;
  isSubmitting?: boolean;
  onClose: () => void;
  onSelect: (mode: SessionMode) => void;
}

function SessionModeDialog({
  open,
  isSubmitting = false,
  onClose,
  onSelect,
}: SessionModeDialogProps) {
  if (!open) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="session-mode-title"
        className="w-full max-w-lg rounded-xl border bg-background p-6 text-foreground shadow-xl"
      >
        <h2
          id="session-mode-title"
          className="text-xl font-bold"
        >
          Choose Session Mode
        </h2>

        <p className="mt-2 text-sm text-muted-foreground">
          You and the other participant must agree on the
          same mode before the session can proceed.
        </p>

        <div className="mt-6 space-y-4">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={() =>
              onSelect(SessionMode.NON_LIVE_COLLABORATION)
            }
            className="w-full rounded-lg border p-4 text-left transition hover:border-primary hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50"
          >
            <span className="block font-semibold">
              Non-live Collaboration
            </span>

            <span className="mt-1 block text-sm text-muted-foreground">
              Collaborate through chat, resources, files,
              and other non-video features. In-app live
              audio and video will remain disabled.
            </span>
          </button>

          <button
            type="button"
            disabled={isSubmitting}
            onClick={() =>
              onSelect(SessionMode.LIVE_VIDEO_SESSION)
            }
            className="w-full rounded-lg border p-4 text-left transition hover:border-primary hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50"
          >
            <span className="block font-semibold">
              Live Video Session
            </span>

            <span className="mt-1 block text-sm text-muted-foreground">
              Use in-app audio, video, and screen sharing.
              Both participants must complete the required
              recording-consent process before live media
              can be enabled.
            </span>
          </button>
        </div>

        <p className="mt-4 text-xs text-muted-foreground">
          Saving your choice does not start the session.
          Live media remains unavailable until the backend
          confirms the required mode agreement and all
          recording prerequisites are satisfied.
        </p>

        <div className="mt-6 flex justify-end">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={onClose}
            className="rounded-lg border px-4 py-2 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}

export default SessionModeDialog;