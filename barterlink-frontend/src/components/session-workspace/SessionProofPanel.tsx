
import { useCallback, useEffect, useRef, useState } from "react";
import toast from "react-hot-toast";

import type {
  SessionProof,
  SessionProofType,
  UploadSessionProofRequest,
} from "@/models/sessions/SessionProof";

import {
  deleteSessionProof,
  downloadSessionProof,
  getSessionProofs,
  uploadSessionProof,
} from "@/services/SessionProofService";

interface SessionProofPanelProps {
  sessionId: string;
  currentUserId?: string;
}

const MAX_FILE_SIZE = 10 * 1024 * 1024;

const PROOF_TYPES: { value: SessionProofType; label: string }[] = [
  { value: "GITHUB", label: "GitHub repository" },
  { value: "GOOGLE_DRIVE", label: "Google Drive" },
  { value: "WEBSITE", label: "Website or project link" },
  { value: "PDF", label: "PDF document" },
  { value: "ZIP", label: "ZIP archive" },
  { value: "IMAGE", label: "Image (JPEG or PNG)" },
  { value: "OTHER", label: "Other work or evidence" },
];

const OTHER_FILE_ACCEPT =
  ".docx,.xlsx,.txt,.csv,.java,.py,.js,.ts,.html,.css,.sql";

function getFileAccept(proofType: SessionProofType): string {
  switch (proofType) {
    case "PDF":
      return ".pdf,application/pdf";
    case "ZIP":
      return ".zip,application/zip,application/x-zip-compressed";
    case "IMAGE":
      return ".jpg,.jpeg,.png,image/jpeg,image/png";
    case "OTHER":
      return OTHER_FILE_ACCEPT;
    default:
      return "";
  }
}

function formatFileSize(size: number | null): string {
  if (size === null || !Number.isFinite(size)) {
    return "";
  }

  if (size < 1024) {
    return `${size} B`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }

  return `${(size / (1024 * 1024)).toFixed(2)} MB`;
}

function isValidHttpUrl(value: string): boolean {
  try {
    const url = new URL(value);

    return (
      (url.protocol === "http:" || url.protocol === "https:") &&
      Boolean(url.hostname) &&
      !url.username &&
      !url.password
    );
  } catch {
    return false;
  }
}

export default function SessionProofPanel({
  sessionId,
  currentUserId,
}: SessionProofPanelProps) {
  const [proofs, setProofs] = useState<SessionProof[]>([]);
  const [proofType, setProofType] =
    useState<SessionProofType>("PDF");
  const [otherMode, setOtherMode] = useState<"file" | "link">("file");
  const [linkUrl, setLinkUrl] = useState("");
  const [description, setDescription] = useState("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [deletingProofId, setDeletingProofId] = useState<string | null>(null);
  const [downloadingProofId, setDownloadingProofId] =
    useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const linkProofType =
    proofType === "GITHUB" ||
    proofType === "GOOGLE_DRIVE" ||
    proofType === "WEBSITE";

  const needsLink =
    linkProofType ||
    (proofType === "OTHER" && otherMode === "link");

  const needsFile = !needsLink;

  const loadProofs = useCallback(async () => {
    if (!sessionId) {
      setProofs([]);
      setLoading(false);
      return;
    }

    setLoading(true);

    try {
      const result = await getSessionProofs(sessionId);
      setProofs(result);
    } catch (error: any) {
      toast.error(
        error?.response?.data?.message ??
          "Unable to load session proofs.",
      );
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  useEffect(() => {
    void loadProofs();
  }, [loadProofs]);

  const resetForm = () => {
    setProofType("PDF");
    setOtherMode("file");
    setLinkUrl("");
    setDescription("");
    setSelectedFile(null);

    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleProofTypeChange = (value: SessionProofType) => {
    setProofType(value);
    setLinkUrl("");
    setSelectedFile(null);

    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleFileChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0] ?? null;

    if (file && file.size > MAX_FILE_SIZE) {
      toast.error("The maximum attachment size is 10 MB.");
      event.target.value = "";
      setSelectedFile(null);
      return;
    }

    setSelectedFile(file);
  };

  const handleUpload = async (
    event: React.FormEvent<HTMLFormElement>,
  ) => {
    event.preventDefault();

    if (!sessionId) {
      toast.error("Session information is not available.");
      return;
    }

    if (needsLink) {
      if (!linkUrl.trim() || !isValidHttpUrl(linkUrl.trim())) {
        toast.error("Enter a valid HTTP or HTTPS URL.");
        return;
      }
    } else {
      if (!selectedFile) {
        toast.error("Select a file to upload.");
        return;
      }

      if (selectedFile.size > MAX_FILE_SIZE) {
        toast.error("The maximum attachment size is 10 MB.");
        return;
      }
    }

    const request: UploadSessionProofRequest = {
      proofType,
      ...(needsLink ? { proofUrl: linkUrl.trim() } : {}),
      ...(description.trim()
        ? { description: description.trim() }
        : {}),
    };

    try {
      setUploading(true);

      await uploadSessionProof(
        sessionId,
        request,
        needsFile ? selectedFile ?? undefined : undefined,
      );

      toast.success("Session proof submitted successfully.");
      resetForm();
      await loadProofs();
    } catch (error: any) {
      toast.error(
        error?.response?.data?.message ??
          "Unable to submit session proof.",
      );
    } finally {
      setUploading(false);
    }
  };

  const handleDownload = async (proof: SessionProof) => {
    try {
      setDownloadingProofId(proof.proofId);

      const blob = await downloadSessionProof(proof.proofId);
      const objectUrl = URL.createObjectURL(blob);
      const anchor = document.createElement("a");

      anchor.href = objectUrl;
      anchor.download = proof.fileName || "session-proof";
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();

      URL.revokeObjectURL(objectUrl);
    } catch (error: any) {
      toast.error(
        error?.response?.data?.message ??
          "Unable to download this attachment.",
      );
    } finally {
      setDownloadingProofId(null);
    }
  };

  const handleDelete = async (proof: SessionProof) => {
    if (
      !window.confirm(
        `Delete the proof "${proof.fileName || proof.proofType}"?`,
      )
    ) {
      return;
    }

    try {
      setDeletingProofId(proof.proofId);

      await deleteSessionProof(proof.proofId);

      setProofs((previous) =>
        previous.filter((item) => item.proofId !== proof.proofId),
      );

      toast.success("Session proof deleted.");
    } catch (error: any) {
      toast.error(
        error?.response?.data?.message ??
          "Unable to delete this proof.",
      );
    } finally {
      setDeletingProofId(null);
    }
  };

  return (
    <div className="flex h-full flex-col gap-5 overflow-y-auto rounded-xl border p-5">
      <div>
        <h2 className="text-2xl font-bold">Session Proofs</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Share your work, deliverables, documents, images, and project links
          with the other session participant.
        </p>
      </div>

      <form
        onSubmit={handleUpload}
        className="space-y-4 rounded-xl border p-4"
      >
        <h3 className="text-lg font-semibold">Submit proof or work</h3>

        <div className="space-y-1">
          <label
            htmlFor="session-proof-type"
            className="block text-sm font-medium"
          >
            Proof type
          </label>

          <select
            id="session-proof-type"
            value={proofType}
            onChange={(event) =>
              handleProofTypeChange(
                event.target.value as SessionProofType,
              )
            }
            disabled={uploading}
            className="w-full rounded-md border bg-background p-2 text-sm"
          >
            {PROOF_TYPES.map((type) => (
              <option key={type.value} value={type.value}>
                {type.label}
              </option>
            ))}
          </select>
        </div>

        {proofType === "OTHER" && (
          <fieldset className="space-y-2">
            <legend className="text-sm font-medium">
              Submit other proof as
            </legend>

            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="other-proof-mode"
                value="file"
                checked={otherMode === "file"}
                disabled={uploading}
                onChange={() => {
                  setOtherMode("file");
                  setLinkUrl("");
                  setSelectedFile(null);

                  if (fileInputRef.current) {
                    fileInputRef.current.value = "";
                  }
                }}
              />
              Upload a file
            </label>

            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="other-proof-mode"
                value="link"
                checked={otherMode === "link"}
                disabled={uploading}
                onChange={() => {
                  setOtherMode("link");
                  setSelectedFile(null);

                  if (fileInputRef.current) {
                    fileInputRef.current.value = "";
                  }
                }}
              />
              Submit an external link
            </label>
          </fieldset>
        )}

        {needsLink ? (
          <div className="space-y-1">
            <label
              htmlFor="session-proof-url"
              className="block text-sm font-medium"
            >
              Proof URL
            </label>

            <input
              id="session-proof-url"
              type="url"
              value={linkUrl}
              onChange={(event) => setLinkUrl(event.target.value)}
              placeholder="https://..."
              maxLength={2000}
              required
              disabled={uploading}
              className="w-full rounded-md border bg-background p-2 text-sm"
            />
          </div>
        ) : (
          <div className="space-y-1">
            <label
              htmlFor="session-proof-file"
              className="block text-sm font-medium"
            >
              Attachment (maximum 10 MB)
            </label>

            <input
              ref={fileInputRef}
              id="session-proof-file"
              type="file"
              accept={getFileAccept(proofType)}
              onChange={handleFileChange}
              required
              disabled={uploading}
              className="block w-full text-sm"
            />

            {selectedFile && (
              <p className="text-sm text-muted-foreground">
                {selectedFile.name} · {formatFileSize(selectedFile.size)}
              </p>
            )}

            <p className="text-xs text-muted-foreground">
              Accepted types depend on the selected proof category. The
              backend performs the final file validation.
            </p>
          </div>
        )}

        <div className="space-y-1">
          <label
            htmlFor="session-proof-description"
            className="block text-sm font-medium"
          >
            Description (optional)
          </label>

          <textarea
            id="session-proof-description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            maxLength={2000}
            rows={3}
            placeholder="Describe the work or evidence you are sharing."
            disabled={uploading}
            className="w-full resize-y rounded-md border bg-background p-2 text-sm"
          />
        </div>

        <button
          type="submit"
          disabled={uploading}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
        >
          {uploading ? "Submitting..." : "Submit proof"}
        </button>
      </form>

      <section className="space-y-3">
        <div className="flex items-center justify-between gap-3">
          <h3 className="text-lg font-semibold">Shared proofs</h3>

          <button
            type="button"
            onClick={() => void loadProofs()}
            disabled={loading}
            className="rounded-md border px-3 py-1.5 text-sm disabled:opacity-50"
          >
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        </div>

        {loading ? (
          <p className="text-sm text-muted-foreground">
            Loading session proofs...
          </p>
        ) : proofs.length === 0 ? (
          <div className="rounded-lg border border-dashed p-6 text-center">
            <p className="font-medium">No proofs shared yet</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Submitted work and links will appear here.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {proofs.map((proof) => (
              <article
                key={proof.proofId}
                className="space-y-3 rounded-lg border p-4"
              >
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="font-semibold">
                      {proof.fileAttachment
                        ? proof.fileName || proof.proofType
                        : PROOF_TYPES.find(
                            (type) => type.value === proof.proofType,
                          )?.label ?? proof.proofType}
                    </p>

                    <p className="mt-1 text-xs text-muted-foreground">
                      {proof.uploadedByName} ·{" "}
                      {new Date(proof.createdAt).toLocaleString()}
                    </p>
                  </div>

                  <span className="rounded-full border px-2 py-1 text-xs">
                    {proof.proofType}
                  </span>
                </div>

                {proof.description && (
                  <p className="whitespace-pre-wrap break-words text-sm">
                    {proof.description}
                  </p>
                )}

                {proof.fileAttachment ? (
                  <div className="space-y-2">
                    <p className="text-xs text-muted-foreground">
                      {proof.contentType || "Attachment"}
                      {proof.fileSize !== null
                        ? ` · ${formatFileSize(proof.fileSize)}`
                        : ""}
                    </p>

                    <button
                      type="button"
                      onClick={() => void handleDownload(proof)}
                      disabled={downloadingProofId === proof.proofId}
                      className="rounded-md border px-3 py-2 text-sm disabled:opacity-50"
                    >
                      {downloadingProofId === proof.proofId
                        ? "Downloading..."
                        : "Download attachment"}
                    </button>
                  </div>
                ) : proof.proofUrl ? (
                  <a
                    href={proof.proofUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="block break-all text-sm underline"
                  >
                    Open external proof
                  </a>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    No link or attachment is available.
                  </p>
                )}

                {currentUserId === proof.uploadedById && (
                  <div>
                    <button
                      type="button"
                      onClick={() => void handleDelete(proof)}
                      disabled={deletingProofId === proof.proofId}
                      className="rounded-md border border-destructive px-3 py-2 text-sm text-destructive disabled:opacity-50"
                    >
                      {deletingProofId === proof.proofId
                        ? "Deleting..."
                        : "Delete proof"}
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
