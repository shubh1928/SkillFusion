import { useCallback, useEffect, useRef, useState } from "react";

interface ActiveRecording {
    recorder: MediaRecorder;
    canvasStream: MediaStream;
    recordingStream: MediaStream;
    audioContext: AudioContext;
    audioSources: MediaStreamAudioSourceNode[];
    videoElements: HTMLVideoElement[];
    animationFrameId: number;
    chunks: Blob[];
    startedAt: number;
    resolveStop: ((blob: Blob) => void) | null;
}

interface SessionMediaRecorder {
    isRecording: boolean;
    elapsedSeconds: number;
    recordingError: string | null;
    startRecording: (
        localStream: MediaStream,
        remoteStream: MediaStream,
        localVideoStream?: MediaStream
    ) => Promise<void>;
    stopRecording: () => Promise<Blob | null>;
}

const CANVAS_WIDTH = 1280;
const CANVAS_HEIGHT = 720;

const SUPPORTED_MIME_TYPES = [
    "video/webm;codecs=vp9,opus",
    "video/webm;codecs=vp8,opus",
    "video/webm",
    "video/mp4",
];

function chooseMimeType(): string {
    if (typeof MediaRecorder === "undefined") {
        throw new Error(
            "This browser does not support MediaRecorder."
        );
    }

    return (
        SUPPORTED_MIME_TYPES.find((type) =>
            MediaRecorder.isTypeSupported(type)
        ) ?? ""
    );
}

function createVideoElement(
    stream: MediaStream
): HTMLVideoElement {
    const video = document.createElement("video");

    video.muted = true;
    video.autoplay = true;
    video.playsInline = true;
    video.srcObject = stream;

    // Keep the video available for canvas capture without
    // changing the visible session interface.
    video.style.position = "fixed";
    video.style.left = "-10000px";
    video.style.top = "0";
    video.style.width = "640px";
    video.style.height = "360px";
    video.style.pointerEvents = "none";

    document.body.appendChild(video);

    return video;
}

function drawVideoCover(
    context: CanvasRenderingContext2D,
    video: HTMLVideoElement,
    x: number,
    y: number,
    width: number,
    height: number
): void {
    if (
        video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA ||
        video.videoWidth === 0 ||
        video.videoHeight === 0
    ) {
        return;
    }

    const sourceRatio = video.videoWidth / video.videoHeight;
    const targetRatio = width / height;

    let sourceX = 0;
    let sourceY = 0;
    let sourceWidth = video.videoWidth;
    let sourceHeight = video.videoHeight;

    if (sourceRatio > targetRatio) {
        sourceWidth = video.videoHeight * targetRatio;
        sourceX = (video.videoWidth - sourceWidth) / 2;
    } else {
        sourceHeight = video.videoWidth / targetRatio;
        sourceY = (video.videoHeight - sourceHeight) / 2;
    }

    context.drawImage(
        video,
        sourceX,
        sourceY,
        sourceWidth,
        sourceHeight,
        x,
        y,
        width,
        height
    );
}

function releaseResources(recording: ActiveRecording): void {
    window.cancelAnimationFrame(recording.animationFrameId);

    recording.videoElements.forEach((video) => {
        video.pause();
        video.srcObject = null;
        video.remove();
    });

    // Stop the generated recording tracks only.
    // Do not stop the original camera/microphone streams.
    recording.recordingStream
        .getTracks()
        .forEach((track) => track.stop());

    recording.canvasStream
        .getTracks()
        .forEach((track) => track.stop());

    recording.audioSources.forEach((source) => {
        try {
            source.disconnect();
        } catch {
            // The source may already be disconnected.
        }
    });

    if (recording.audioContext.state !== "closed") {
        void recording.audioContext.close();
    }
}

export default function useSessionMediaRecorder():
    SessionMediaRecorder {

    const activeRecordingRef =
        useRef<ActiveRecording | null>(null);

    const [isRecording, setIsRecording] = useState(false);

    const [elapsedSeconds, setElapsedSeconds] = useState(0);

    const [recordingError, setRecordingError] =
        useState<string | null>(null);

    const cleanupRecording = useCallback(
        (recording: ActiveRecording) => {
            releaseResources(recording);

            if (activeRecordingRef.current === recording) {
                activeRecordingRef.current = null;
            }

            setIsRecording(false);
        },
        []
    );

    const startRecording = useCallback(
        async (
            localStream: MediaStream,
            remoteStream: MediaStream,
            localVideoStream: MediaStream = localStream
        ): Promise<void> => {
            if (activeRecordingRef.current) {
                throw new Error(
                    "A session recording is already active."
                );
            }

            if (
                !localStream ||
                !remoteStream ||
                !localVideoStream
            ) {
                throw new Error(
                    "Local and remote media streams are required."
                );
            }

            if (
                localVideoStream.getVideoTracks().length === 0 ||
                remoteStream.getVideoTracks().length === 0
            ) {
                throw new Error(
                    "Both participants must have an available video track."
                );
            }

            if (
                localStream.getAudioTracks().length === 0 ||
                remoteStream.getAudioTracks().length === 0
            ) {
                throw new Error(
                    "Audio tracks from both participants are required."
                );
            }

            setRecordingError(null);
            setElapsedSeconds(0);

            const mimeType = chooseMimeType();

            const canvas = document.createElement("canvas");
            canvas.width = CANVAS_WIDTH;
            canvas.height = CANVAS_HEIGHT;

            const context = canvas.getContext("2d");

            if (!context) {
                throw new Error(
                    "Unable to initialize the recording canvas."
                );
            }

            const canvasStream = canvas.captureStream(30);
            const audioContext = new AudioContext();
            const audioDestination =
                audioContext.createMediaStreamDestination();

            const audioSources: MediaStreamAudioSourceNode[] = [];
            const videoElements: HTMLVideoElement[] = [];

            let recordingStream: MediaStream | null = null;

            try {
                const localVideo =
                    createVideoElement(localVideoStream);

                const remoteVideo =
                    createVideoElement(remoteStream);

                videoElements.push(localVideo, remoteVideo);

                await Promise.all([
                    localVideo.play(),
                    remoteVideo.play(),
                ]);

                for (const stream of [localStream, remoteStream]) {
                    const audioTracks = stream.getAudioTracks();

                    if (audioTracks.length === 0) {
                        continue;
                    }

                    const audioOnlyStream =
                        new MediaStream(audioTracks);

                    const source =
                        audioContext.createMediaStreamSource(
                            audioOnlyStream
                        );

                    source.connect(audioDestination);
                    audioSources.push(source);
                }

                await audioContext.resume();

                recordingStream = new MediaStream([
                    ...canvasStream.getVideoTracks(),
                    ...audioDestination.stream.getAudioTracks(),
                ]);

                const recorder = new MediaRecorder(
                    recordingStream,
                    mimeType ? { mimeType } : undefined
                );

                const recording: ActiveRecording = {
                    recorder,
                    canvasStream,
                    recordingStream,
                    audioContext,
                    audioSources,
                    videoElements,
                    animationFrameId: 0,
                    chunks: [],
                    startedAt: Date.now(),
                    resolveStop: null,
                };

                activeRecordingRef.current = recording;

                const drawFrame = () => {
                    if (
                        activeRecordingRef.current !== recording
                    ) {
                        return;
                    }

                    const halfWidth = CANVAS_WIDTH / 2;

                    context.fillStyle = "#111111";
                    context.fillRect(
                        0,
                        0,
                        CANVAS_WIDTH,
                        CANVAS_HEIGHT
                    );

                    drawVideoCover(
                        context,
                        localVideo,
                        0,
                        0,
                        halfWidth,
                        CANVAS_HEIGHT
                    );

                    drawVideoCover(
                        context,
                        remoteVideo,
                        halfWidth,
                        0,
                        halfWidth,
                        CANVAS_HEIGHT
                    );

                    context.fillStyle = "#FFFFFF";
                    context.font = "20px sans-serif";
                    context.fillText(
                        "You",
                        24,
                        CANVAS_HEIGHT - 24
                    );
                    context.fillText(
                        "Participant",
                        halfWidth + 24,
                        CANVAS_HEIGHT - 24
                    );

                    context.fillStyle = "#FFFFFF";
                    context.fillRect(
                        halfWidth - 1,
                        0,
                        2,
                        CANVAS_HEIGHT
                    );

                    recording.animationFrameId =
                        window.requestAnimationFrame(drawFrame);
                };

                drawFrame();

                recorder.ondataavailable = (event) => {
                    if (event.data.size > 0) {
                        recording.chunks.push(event.data);
                    }
                };

                recorder.onerror = () => {
                    setRecordingError(
                        "The browser encountered an error while recording."
                    );

                    if (recorder.state !== "inactive") {
                        recorder.stop();
                    }
                };

                recorder.onstop = () => {
                    const blob = new Blob(recording.chunks, {
                        type: recorder.mimeType || "video/webm",
                    });

                    cleanupRecording(recording);

                    const resolveStop = recording.resolveStop;
                    recording.resolveStop = null;

                    resolveStop?.(blob);
                };

                recorder.start(1000);

                setIsRecording(true);

            } catch (error) {
                if (recordingStream) {
                    recordingStream
                        .getTracks()
                        .forEach((track) => track.stop());
                }

                canvasStream
                    .getTracks()
                    .forEach((track) => track.stop());

                videoElements.forEach((video) => {
                    video.pause();
                    video.srcObject = null;
                    video.remove();
                });

                audioSources.forEach((source) => {
                    try {
                        source.disconnect();
                    } catch {
                        // The source may already be disconnected.
                    }
                });

                if (audioContext.state !== "closed") {
                    await audioContext.close();
                }

                throw error;
            }
        },
        [cleanupRecording]
    );

    const stopRecording = useCallback(
        (): Promise<Blob | null> => {
            const recording = activeRecordingRef.current;

            if (!recording) {
                return Promise.resolve(null);
            }

            if (recording.recorder.state === "inactive") {
                return Promise.resolve(null);
            }

            return new Promise<Blob>((resolve, reject) => {
                recording.resolveStop = resolve;

                try {
                    recording.recorder.stop();
                } catch (error) {
                    recording.resolveStop = null;
                    reject(error);
                }
            });
        },
        []
    );

    useEffect(() => {
        if (!isRecording) {
            return;
        }

        const recording = activeRecordingRef.current;

        if (!recording) {
            return;
        }

        const intervalId = window.setInterval(() => {
            setElapsedSeconds(
                Math.floor(
                    (Date.now() - recording.startedAt) / 1000
                )
            );
        }, 1000);

        return () => {
            window.clearInterval(intervalId);
        };
    }, [isRecording]);

    useEffect(() => {
        return () => {
            const recording = activeRecordingRef.current;

            if (!recording) {
                return;
            }

            // Last-resort cleanup on unmount. Normal session
            // completion must call stopRecording() explicitly
            // and upload the returned Blob before leaving.
            recording.resolveStop = null;

            if (recording.recorder.state !== "inactive") {
                recording.recorder.stop();
            } else {
                cleanupRecording(recording);
            }
        };
    }, [cleanupRecording]);

    return {
        isRecording,
        elapsedSeconds,
        recordingError,
        startRecording,
        stopRecording,
    };
}