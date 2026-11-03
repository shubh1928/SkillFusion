import { useEffect, useRef } from "react";

import {
    Mic,
    MicOff,
    Video,
    VideoOff,
    Monitor,
    PhoneOff,
    UserRound,
} from "lucide-react";
import { Button } from "@/components/ui/button";

interface VideoViewProps {

    previewStream: MediaStream | null;

    remoteStream: MediaStream | null;

    isMuted: boolean;

    onToggleMicrophone: () => void;

    isCameraOff: boolean;

    onToggleCamera: () => void;

    isScreenSharing: boolean;

    onToggleScreenShare: () => void;

    connectionState: RTCPeerConnectionState;

    onLeaveCall: () => void;

        // UI-only preview; does not capture actual media.
    recordingAllowed?: boolean;
    recordingElapsedSeconds?: number;

}

export default function VideoView({

    previewStream,

    remoteStream,

    isMuted,

    onToggleMicrophone,

    isCameraOff,

    onToggleCamera,

    isScreenSharing,

    onToggleScreenShare,

    connectionState,

    onLeaveCall,

    recordingAllowed = false,
    recordingElapsedSeconds = 0,

}: VideoViewProps) {

    const localVideoRef = useRef<HTMLVideoElement>(null);
    const remoteVideoRef = useRef<HTMLVideoElement>(null);

    useEffect(() => {

        if (localVideoRef.current) {

            localVideoRef.current.srcObject = previewStream;

        }

    }, [previewStream]);

    useEffect(() => {
        if (remoteVideoRef.current) {
            remoteVideoRef.current.srcObject = remoteStream;
        }
    }, [remoteStream]);


    useEffect(() => {

    console.log("VideoView mounted");

    return () => {

        console.trace("VideoView unmounted");

    };

}, []);




        return (

            <div className="flex h-full flex-col rounded-xl border">

                {/* Header */}

                <div className="flex items-center justify-between">

                    <h2 className="text-xl font-bold">
                        Video Collaboration
                    </h2>

                    <span
                        className={`
                            rounded-full
                            px-3
                            py-1
                            text-xs
                            font-medium
                            ${
                                connectionState === "connected"
                                    ? "bg-green-100 text-green-700"

                                : connectionState === "connecting"
                                    ? "bg-yellow-100 text-yellow-700"

                                : connectionState === "failed"
                                    ? "bg-red-100 text-red-700"

                                : connectionState === "disconnected"
                                    ? "bg-red-100 text-red-700"

                                : "bg-gray-100 text-gray-700"
                            }
                        `}
                    >
                        {connectionState}
                    </span>

                </div>

                {/* Video Area */}

                <div className="relative flex-1 bg-black">

                    {/* Floating recording indicator — preserves the existing call UI */}
{recordingAllowed && (
    <div
        className="
            pointer-events-none
            absolute
            right-4
            top-4
            z-30
            flex
            items-center
            gap-2
            rounded-full
            bg-red-600
            px-3
            py-2
            text-xs
            font-semibold
            text-white
            shadow-lg
        "
        role="status"
        aria-label="Live session recording"
    >
        <span className="h-2 w-2 animate-pulse rounded-full bg-white" />

        <span>REC · LIVE</span>

        <span className="tabular-nums">
            {[
                Math.floor(recordingElapsedSeconds / 3600),
                Math.floor((recordingElapsedSeconds % 3600) / 60),
                recordingElapsedSeconds % 60,
            ]
                .map((value) => String(value).padStart(2, "0"))
                .join(":")}
        </span>
    </div>
)}

                    {/* Remote Video */}

                    <video
                        ref={remoteVideoRef}
                        autoPlay
                        playsInline
                        className="h-full w-full object-cover"
                    />

                    {!remoteStream && (

                        <div
                            className="
                                absolute
                                inset-0
                                flex
                                flex-col
                                items-center
                                justify-center
                                gap-4
                                text-white
                            "
                        >

                            <div
                                className="
                                    flex
                                    h-24
                                    w-24
                                    items-center
                                    justify-center
                                    rounded-full
                                    bg-white/10
                                "
                            >

                                <UserRound size={48} />

                            </div>

                            <h2 className="text-2xl font-semibold">

                                Waiting for participant

                            </h2>

                            <p className="text-white/70">

                                Another user hasn't joined yet.

                            </p>

                        </div>

                    )}

                    {/* Local Preview */}

                    <div
                        className="
                            absolute
                            bottom-6
                            right-6
                            h-44
                            w-72
                            overflow-hidden
                            rounded-2xl
                            border
                            border-white/20
                            bg-zinc-950
                            shadow-2xl
                            backdrop-blur-sm
                        "
                    >

                        <div
                            className="
                                absolute
                                left-3
                                top-3
                                z-10
                                rounded-full
                                bg-black/60
                                px-3
                                py-1
                                text-xs
                                font-medium
                                text-white
                                backdrop-blur
                            "
                        >
                            You
                        </div>

                        <video
                            ref={localVideoRef}
                            autoPlay
                            muted
                            playsInline
                            className="h-full w-full object-cover"
                        />

                        {!previewStream && (

                            <div
                                className="
                                    absolute
                                    inset-0
                                    flex
                                    flex-col
                                    items-center
                                    justify-center
                                    gap-3
                                    bg-black/70
                                    text-white
                                "
                            >

                                <Video size={32} />

                                <p className="text-sm font-medium">
                                    Starting camera...
                                </p>

                            </div>

                        )}

                    </div>

                </div>

                {/* Controls */}

                <div className="border-t bg-background/80 px-6 py-5">

                    <div className="flex items-center justify-center gap-10">

                        {/* Mic */}

                        <button
                        onClick={onToggleMicrophone}
                            className="
                                flex
                                flex-col
                                items-center
                                gap-2
                                transition
                                hover:scale-105
                            "
                        >

                            <div
                               className={`
                                    flex
                                    h-12
                                    w-12
                                    items-center
                                    justify-center
                                    rounded-full
                                    shadow-sm
                                    transition-all
                                    duration-200
                                    ${
                                        isMuted
                                            ? "bg-red-600 text-white"
                                            : "border bg-card hover:bg-muted"
                                    }
                                `}
                            >
                                {isMuted ? (
                                    <MicOff size={20} />
                                ) : (
                                    <Mic size={20} />
                                )}
                            </div>

                            <span
                                className={`
                                    text-xs
                                    ${
                                        isMuted
                                            ? "text-red-600"
                                            : ""
                                    }
                                `}
                            >
                                {isMuted ? "Unmute" : "Mic"}
                            </span>

                        </button>

                        {/* Camera */}

                        <button
                        onClick={onToggleCamera}
                            className="
                                flex
                                flex-col
                                items-center
                                gap-2
                                transition
                                hover:scale-105
                            "
                        >

                            <div
                                className={`
                                    flex
                                    h-12
                                    w-12
                                    items-center
                                    justify-center
                                    rounded-full
                                    shadow-sm
                                    transition-all
                                    duration-200
                                    ${
                                        isCameraOff
                                            ? "bg-red-600 text-white"
                                            : "border bg-card hover:bg-muted"
                                    }
                                `}
                            >
                                {isCameraOff ? (
                                    <VideoOff size={20} />
                                ) : (
                                    <Video size={20} />
                                )}
                            </div>

                            <span
                                className={`
                                    text-xs
                                    ${
                                        isCameraOff
                                            ? "text-red-600"
                                            : ""
                                    }
                                `}
                            >
                                {isCameraOff ? "Camera Off" : "Camera"}
                            </span>

                        </button>

                        {/* Screen Share */}

                        <Button
                            variant="outline"
                            size="lg"
                            onClick={onToggleScreenShare}
                            className={`
                                flex flex-col items-center gap-2 h-16 min-w-20
                                ${
                                    isScreenSharing
                                        ? "bg-blue-600 text-white hover:bg-blue-700"
                                        : "border bg-card hover:bg-muted"
                                }
                            `}
                        >
                            <Monitor size={20} />

                            <span
                                className={`text-xs ${
                                    isScreenSharing ? "text-white" : ""
                                }`}
                            >
                                {isScreenSharing
                                    ? "Stop Share"
                                    : "Share Screen"}
                            </span>
                        </Button>

                        {/* Leave */}

                        <button
                         onClick={onLeaveCall}
                            className="
                                flex
                                flex-col
                                items-center
                                gap-2
                                transition
                                hover:scale-105
                            "
                        >

                            <div
                                className="
                                    flex
                                    h-12
                                    w-12
                                    items-center
                                    justify-center
                                    rounded-full
                                    bg-red-600
                                    text-white
                                    shadow-sm
                                    transition
                                    hover:bg-red-700
                                "
                            >
                                <PhoneOff size={20} />
                            </div>

                            <span className="text-xs text-red-600">
                                Leave
                            </span>

                        </button>

                    </div>

                </div>

            </div>

        );
}