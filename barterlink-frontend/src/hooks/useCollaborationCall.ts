import { useCallback, useEffect, useState } from "react";

import CollaborationCallService from "@/services/CollaborationCallService";

import CollaborationWebSocketService from "@/services/CollaborationWebSocketService";

export default function useCollaborationCall() {

    const [localStream, setLocalStream] =
        useState<MediaStream | null>(null);

        const [remoteStream, setRemoteStream] =
    useState<MediaStream | null>(null);

    const [previewStream, setPreviewStream] =
    useState<MediaStream | null>(null);

    const [isMuted, setIsMuted] =
    useState(false);

    const [isCameraOff, setIsCameraOff] = useState(false);

    const [isScreenSharing, setIsScreenSharing] = useState(false);

    const [connectionState, setConnectionState] = useState<RTCPeerConnectionState>("new");

    const initializePeerConnection = useCallback(() => {

    return CollaborationCallService.initializePeerConnection();

}, []);

const attachLocalTracks = useCallback(() => {

    CollaborationCallService.attachLocalTracks();

}, []);

    const startLocalMedia = useCallback(async () => {

        const stream =
            await CollaborationCallService.startLocalMedia();

        setLocalStream(stream);

        setPreviewStream(
            CollaborationCallService.getPreviewStream()
        );

        console.log(
    "Hook previewStream:",
    CollaborationCallService.getPreviewStream()
);

        return stream;

    }, []);



const stopLocalMedia = useCallback(() => {

    console.log("stopLocalMedia() CALLED");
console.trace();

    /*
     * Disconnect signaling first.
     */
    CollaborationWebSocketService.disconnect();

    /*
     * Stop WebRTC.
     */
    CollaborationCallService.stopLocalMedia();

    /*
     * Reset React state.
     */
    setLocalStream(null);

    setPreviewStream(null);

    setRemoteStream(null);

    setIsMuted(false);

    setIsCameraOff(false);

    setIsScreenSharing(false);

}, []);

    

        const toggleMicrophone = useCallback(() => {

            const muted =
                CollaborationCallService.toggleMicrophone();

            setIsMuted(muted);

        }, []);


    const toggleCamera = useCallback(() => {

            const cameraOff =
                CollaborationCallService.toggleCamera();

            setIsCameraOff(cameraOff);

        }, []);

    
        const toggleScreenShare = useCallback(async () => {

            if (isScreenSharing) {

                await CollaborationCallService.stopScreenShare();

            } else {

                await CollaborationCallService.startScreenShare();

            }

            setPreviewStream(
                CollaborationCallService.getPreviewStream()
            );

        }, [isScreenSharing]);


    const createOffer = useCallback(async () => {

    return await CollaborationCallService.createOffer();

}, []);

const createAnswer = useCallback(

    async (
        offer: RTCSessionDescriptionInit
    ) => {

        return await CollaborationCallService.createAnswer(
            offer
        );

    },

    []

);

const setRemoteAnswer = useCallback(

    async (
        answer: RTCSessionDescriptionInit
    ) => {

        await CollaborationCallService.setRemoteAnswer(
            answer
        );

    },

    []

);

const addIceCandidate = useCallback(

    async (
        candidate: RTCIceCandidateInit
    ) => {

        await CollaborationCallService.addIceCandidate(
            candidate
        );

    },

    []

);

const setOnIceCandidate = useCallback(

    (
        callback:
            ((candidate: RTCIceCandidate) => void)
            | null
    ) => {

        CollaborationCallService.setOnIceCandidate(
            callback
        );

    },

    []

);

    useEffect(() => {

        CollaborationCallService.setOnRemoteStream(
            setRemoteStream
        );

        return () => {

            console.log("useCollaborationCall cleanup");

              console.trace("HOOK CLEANUP");

    CollaborationCallService.setOnRemoteStream(null);

    CollaborationCallService.setOnIceCandidate(null);

    CollaborationCallService.setScreenShareStateListener(() => {});

    CollaborationWebSocketService.disconnect();

    CollaborationCallService.stopLocalMedia();

};

    }, []);

    useEffect(() => {

    CollaborationCallService.setScreenShareStateListener(
        (sharing) => {

            setIsScreenSharing(sharing);

        }
    );

    return () => {

        CollaborationCallService.setScreenShareStateListener(
            () => {}
        );

    };

}, []);


useEffect(() => {

    CollaborationCallService.setOnConnectionState(
        setConnectionState
    );

    return () => {

        CollaborationCallService.setOnConnectionState(
            null
        );

    };

}, []);

return {

    localStream,

    previewStream,

    remoteStream,

    connectionState,

    isMuted,

    toggleMicrophone,

    isCameraOff,

    toggleCamera,

    isScreenSharing,

    toggleScreenShare,

    startLocalMedia,

    stopLocalMedia,

    initializePeerConnection,

    attachLocalTracks,

    createOffer,

    createAnswer,

    setRemoteAnswer,

    addIceCandidate,

    setOnIceCandidate,

};

}