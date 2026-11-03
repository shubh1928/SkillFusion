import apiClient from "@/config/ApiClient";

export type SessionRecordingStatus =
    | "PENDING_UPLOAD"
    | "UPLOADING"
    | "READY"
    | "FAILED"
    | "DELETE_PENDING"
    | "DELETED";

export interface SessionRecording {
    recordingId: string;
    sessionId: string;
    fileName: string;
    mimeType: string;
    status: SessionRecordingStatus;
    fileSizeBytes: number | null;
    durationSeconds: number | null;
    createdAt: string;
    uploadedAt: string | null;
    educationalReusePermitted: boolean;
}

/**
 * Upload a completed browser recording to the backend.
 *
 * The backend validates session membership and recording consent,
 * stores the video in Google Drive, and persists metadata in MySQL.
 */
export const uploadSessionRecording = async (
    sessionId: string,
    recordingBlob: Blob,
    durationSeconds: number,
): Promise<SessionRecording> => {
    // MediaRecorder may include codec parameters in the MIME type.
    const mimeType = (recordingBlob.type || "video/webm")
        .split(";")[0]
        .trim()
        .toLowerCase();

    if (mimeType !== "video/webm" && mimeType !== "video/mp4") {
        throw new Error(
            `Unsupported recording format: ${recordingBlob.type || "unknown"}`,
        );
    }

    // Ensure the uploaded file has a normalized MIME type.
    const normalizedBlob = new Blob([recordingBlob], {
        type: mimeType,
    });

    const extension = mimeType === "video/mp4" ? "mp4" : "webm";

    const fileName =
        `session-${sessionId}-recording.${extension}`;

    const formData = new FormData();

    formData.append("file", normalizedBlob, fileName);

    formData.append(
        "durationSeconds",
        String(Math.max(0, Math.floor(durationSeconds))),
    );

    const response = await apiClient.post(
        `/sessions/${sessionId}/recordings`,
        formData,
    );

    return response.data.data as SessionRecording;
};

/**
 * Retrieve recording metadata for a session.
 *
 * The backend returns only recordings the authenticated user
 * is authorized to access.
 */
export const getSessionRecordings = async (
    sessionId: string
): Promise<SessionRecording[]> => {
    if (!sessionId.trim()) {
        throw new Error("Session ID is required.");
    }

    const response = await apiClient.get(
        `/sessions/${sessionId}/recordings`
    );

    return response.data.data as SessionRecording[];
};