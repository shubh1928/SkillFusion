
export type SessionProofType =
  | "GITHUB"
  | "GOOGLE_DRIVE"
  | "WEBSITE"
  | "PDF"
  | "ZIP"
  | "IMAGE"
  | "OTHER";

export interface SessionProof {
  proofId: string;
  uploadedById: string;
  uploadedByName: string;
  proofType: SessionProofType;
  proofUrl: string;
  fileAttachment: boolean;
  fileName: string | null;
  contentType: string | null;
  fileSize: number | null;
  sha256: string | null;
  description: string | null;
  createdAt: string;
}

export interface UploadSessionProofRequest {
  proofType: SessionProofType;
  proofUrl?: string;
  description?: string;
}
