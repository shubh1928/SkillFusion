import { useCallback, useEffect, useRef, useState } from "react";
import { useParams } from "react-router";

import toast from "react-hot-toast";
import useAuth from "@/auth/store";
import SessionInfoPanel from "@/components/session-workspace/SessionInfoPanel";

import SessionWebSocketService
    from "@/services/SessionWebSocketService";

import {
  getCollaborationRoom,
} from "@/services/CollaborationService";

import WorkspaceView from "@/models/collaboration/WorkspaceView";
import WorkspaceHeader from "@/components/session-workspace/WorkspaceHeader";
import ChatView from "@/components/session-workspace/chat/ChatView";
import useSessionMediaRecorder from "@/hooks/useSessionMediaRecorder";

import {
    uploadSessionRecording,
} from "@/services/SessionRecordingFileService";
import CollaborationWebSocketService
  from "@/services/CollaborationWebSocketService";

import type CollaborationRoom
  from "@/models/collaboration/CollaborationRoom";

import SignalType
  from "@/models/collaboration/SignalType";

import type CollaborationSignalMessage
  from "@/models/collaboration/CollaborationSignalMessage";

import useCollaborationCall from "@/hooks/useCollaborationCall";

import VideoView from "@/components/session-workspace/video/VideoView";
import SessionProofPanel from "@/components/session-workspace/SessionProofPanel";
import SessionModeDialog from "@/components/session-workspace/SessionModeDialog";
import SessionMode from "@/models/sessions/SessionMode";
import SessionRecordingConsentDialog
  from "@/components/session-workspace/SessionRecordingConsentDialog";

import OpenDisputeDialog
  from "@/components/session-workspace/OpenDisputeDialog";

import type SessionRecordingConsentStatus
  from "@/models/sessions/SessionRecordingConsentStatus";
import SubmitRatingDialog
from "@/components/ratings/SubmitRatingDialog";

import {
    hasCurrentUserRated,
} from "@/services/RatingService";

import {
    getSession,
    startSession,
    selectSessionMode,
    submitRecordingConsent,
    getRecordingConsentStatus,
    reportRecorderReady,
    providerConfirm,
    requesterConfirm,
    cancelSession,
    disputeSession,
    getRecordingReadiness,
} from "@/services/SessionService";

import { getAgreement } from "@/services/AgreementService";

import type AgreementVersion from
  "@/models/agreement/AgreementVersion";

import {
  getAgreementVersions,
} from "@/services/AgreementVersionService";

import {
  confirmMilestoneCompletion,
  retryNextMilestoneFunding,
} from "@/services/MilestoneService";

import type {
  MilestoneCompletionResult,
} from "@/services/MilestoneService";

import {
    getMessages,
} from "@/services/NegotiationService";

import type { IMessage } from "@stomp/stompjs";

import NegotiationWebSocketService
    from "@/services/NegotiationWebSocketService";

import type Session from "@/models/sessions/Session";
import type Agreement from "@/models/agreement/Agreement";
import type NegotiationMessage from "@/models/negotiation/NegotiationMessage";

function SessionWorkspacePage() {

  const [elapsedTime, setElapsedTime] = useState("00:00:00");

    const { sessionId } = useParams();

    const user =
    useAuth((state) => state.user);

    const messagesEndRef =
    useRef<HTMLDivElement>(null);

const consentDialogPromptedForRef =
    useRef<string | null>(null);

const [session, setSession] =
useState<Session | null>(null);

const [agreement, setAgreement] =
useState<Agreement | null>(null);

const [activeVersion, setActiveVersion] =
  useState<AgreementVersion | null>(null);

const [messages, setMessages] =
useState<NegotiationMessage[]>([]);

const [message, setMessage] =
useState("");

const [loading, setLoading] =
useState(true);

const [collaborationRoom, setCollaborationRoom] =
  useState<CollaborationRoom | null>(null);


const [currentWorkspaceView, setCurrentWorkspaceView] =
    useState(WorkspaceView.CHAT);

const [isInfoPanelOpen, setIsInfoPanelOpen] =
    useState(true);

const [showRatingDialog, setShowRatingDialog] =
    useState(false);

const [showSessionModeDialog, setShowSessionModeDialog] =
    useState(false);

const [isModeSubmitting, setIsModeSubmitting] =
    useState(false);

const [showOpenDisputeDialog, setShowOpenDisputeDialog] =
    useState(false);

const [isDisputeSubmitting, setIsDisputeSubmitting] =
    useState(false);

const [
    showSessionRecordingConsentDialog,
    setShowSessionRecordingConsentDialog,
] = useState(false);

const [
    isRecordingConsentSubmitting,
    setIsRecordingConsentSubmitting,
] = useState(false);

const [
    recordingConsentStatus,
    setRecordingConsentStatus,
] = useState<SessionRecordingConsentStatus | null>(null);

const [
    hasLoadedRecordingConsentStatus,
    setHasLoadedRecordingConsentStatus,
] = useState(false);

const [, setHasRated] =
    useState(false);


const [isStartingLiveSession, setIsStartingLiveSession] =
    useState(false);

const recorderStartAttemptForSessionRef =
    useRef<string | null>(null);

const uploadedRecordingForSessionRef =
    useRef<string | null>(null);

const leavingCallRef = useRef(false);

const pendingRecordingUploadRef = useRef<{
    sessionId: string;
    blob: Blob;
    durationSeconds: number;
} | null>(null);


const {

    localStream,

    previewStream,

    remoteStream,

    connectionState,

    startLocalMedia,

    isMuted,

    toggleMicrophone,

    isCameraOff,

    toggleCamera,

    isScreenSharing,

    toggleScreenShare,

    stopLocalMedia,

    initializePeerConnection,

    attachLocalTracks,

    setOnIceCandidate,

    addIceCandidate,

    createOffer,

    createAnswer,

    setRemoteAnswer,

} = useCollaborationCall();

const {
    isRecording,
    elapsedSeconds: recordingElapsedSeconds,
    startRecording,
    stopRecording,
} = useSessionMediaRecorder();

const currentUserRecordingConsent: boolean | null = (() => {
    if (
        !session ||
        !recordingConsentStatus ||
        recordingConsentStatus.sessionId !== session.sessionId ||
        !user?.id
    ) {
        return null;
    }

    if (user.id === session.requesterId) {
        return recordingConsentStatus.requesterRecordingConsent;
    }

    if (user.id === session.providerId) {
        return recordingConsentStatus.providerRecordingConsent;
    }

    return null;
})();

const serverSessionHasStarted = Boolean(
    session?.status === "IN_PROGRESS" &&
    session?.startedAt &&
    !session?.completedAt
);

const canRecordSession = Boolean(
    serverSessionHasStarted &&
    session?.status !== "PROVIDER_CONFIRMED" &&
    session?.status !== "COMPLETED" &&
    session?.status !== "DISPUTED" &&
    session?.status !== "CANCELLED" &&
    !session?.completedAt &&
    session?.agreedSessionMode === SessionMode.LIVE_VIDEO_SESSION &&
    recordingConsentStatus?.sessionId === session?.sessionId &&
    recordingConsentStatus?.bothParticipantsConsented === true &&
    currentUserRecordingConsent === true
);

const handleIncomingChatMessage = useCallback(

    (stompMessage: IMessage) => {

        try {

            const incomingMessage =
                JSON.parse(
                    stompMessage.body
                ) as NegotiationMessage;


            if (!incomingMessage.messageId) {

                console.warn(
                    "Session negotiation message has no messageId."
                );

                return;

            }


            setMessages((previous) => {

                const messageMap =
                    new Map<string, NegotiationMessage>();


                for (const existingMessage of previous) {

                    messageMap.set(
                        existingMessage.messageId,
                        existingMessage
                    );

                }


                if (
                    !messageMap.has(
                        incomingMessage.messageId
                    )
                ) {

                    messageMap.set(
                        incomingMessage.messageId,
                        incomingMessage
                    );

                }


                return Array.from(
                    messageMap.values()
                ).sort(

                    (a, b) =>

                        new Date(
                            a.createdAt
                        ).getTime()

                        -

                        new Date(
                            b.createdAt
                        ).getTime()

                );

            });

        } catch (error) {

            console.error(
                "Failed to process session chat WebSocket message.",
                error
            );

        }

    },

    []

);



const loadWorkspace = useCallback(async () => {
    if (!sessionId) return;

    try {
        setLoading(true);

        const sessionResponse =
            await getSession(sessionId);

        setSession(sessionResponse);

        const collaborationRoomResponse =
            await getCollaborationRoom(
                sessionResponse.sessionId
            );

        setCollaborationRoom(
            collaborationRoomResponse
        );

        const agreementResponse =
            await getAgreement(
                sessionResponse.agreementId
            );

        setAgreement(agreementResponse);

        try {
  const agreementVersions = await getAgreementVersions(
    sessionResponse.negotiationId,
  );

  const selectedActiveVersion =
    agreementVersions.find(
      (version) => version.status === "ACTIVE",
    ) ?? null;

  setActiveVersion(selectedActiveVersion);
} catch (versionError) {
  setActiveVersion(null);

  console.error(
    "Failed to load active agreement version.",
    versionError,
  );
}

        const messageResponse =
            await getMessages(
                sessionResponse.negotiationId
            );

        setMessages((previous) => {
            const messageMap =
                new Map<string, NegotiationMessage>();

            for (const existingMessage of previous) {
                messageMap.set(
                    existingMessage.messageId,
                    existingMessage
                );
            }

            for (const historyMessage of messageResponse) {
                messageMap.set(
                    historyMessage.messageId,
                    historyMessage
                );
            }

            return Array.from(
                messageMap.values()
            ).sort(
                (a, b) =>
                    new Date(a.createdAt).getTime() -
                    new Date(b.createdAt).getTime()
            );
        });

        const alreadyRated =
            await hasCurrentUserRated(
                sessionResponse.sessionId
            );

        setHasRated(alreadyRated);

        if (
            sessionResponse.status === "COMPLETED" &&
            !alreadyRated
        ) {
            setShowRatingDialog(true);
        }

    } catch (error: any) {
        toast.error(
            error?.response?.data?.message ??
            "Unable to load session."
        );
    } finally {
        setLoading(false);
    }
}, [sessionId]);

useEffect(() => {

    loadWorkspace();

}, [loadWorkspace]);



useEffect(() => {
    if (
        !session ||
        (session.status !== "CREATED" &&
            session.status !== "SCHEDULED")
    ) {
        return;
    }

    const currentSessionId = session.sessionId;

    const intervalId = window.setInterval(async () => {
        try {
            const updatedSession = await getSession(
                currentSessionId
            );

            setSession(updatedSession);

            // When the other participant starts the session,
            // refresh the room as well as the session.
            if (updatedSession.status === "IN_PROGRESS") {
                const updatedRoom = await getCollaborationRoom(
                    currentSessionId
                );

                setCollaborationRoom(updatedRoom);
            }
        } catch (error) {
            console.error(
                "Unable to refresh session or collaboration-room status.",
                error
            );
        }
    }, 3000);

    return () => {
        window.clearInterval(intervalId);
    };
}, [session?.sessionId, session?.status]);




useEffect(() => {
    let cancelled = false;

    setRecordingConsentStatus(null);
    setHasLoadedRecordingConsentStatus(false);
    setShowSessionRecordingConsentDialog(false);

    if (
        !session ||
        session.agreedSessionMode !==
            SessionMode.LIVE_VIDEO_SESSION ||
        !session.modeAgreedAt
    ) {
        return;
    }

    const refreshConsentStatus = async () => {
        try {
            const status = await getRecordingConsentStatus(
                session.sessionId
            );

            if (cancelled) {
                return;
            }

            setRecordingConsentStatus(status);
            setHasLoadedRecordingConsentStatus(true);
        } catch (error) {
            if (!cancelled) {
                console.error(
                    "Unable to refresh recording-consent status.",
                    error
                );
            }
        }
    };

    void refreshConsentStatus();

    const intervalId = window.setInterval(() => {
        void refreshConsentStatus();
    }, 3000);

    return () => {
        cancelled = true;
        window.clearInterval(intervalId);
    };
}, [
    session?.sessionId,
    session?.agreedSessionMode,
    session?.modeAgreedAt,
]);


useEffect(() => {
    if (
        !session ||
        session.agreedSessionMode !==
            SessionMode.LIVE_VIDEO_SESSION ||
        !hasLoadedRecordingConsentStatus ||
        !recordingConsentStatus ||
        !user?.id
    ) {
        setShowSessionRecordingConsentDialog(false);
        return;
    }

    let currentUserConsent: boolean | null | undefined;

    if (user.id === session.requesterId) {
        currentUserConsent =
            recordingConsentStatus.requesterRecordingConsent;
    } else if (user.id === session.providerId) {
        currentUserConsent =
            recordingConsentStatus.providerRecordingConsent;
    } else {
        setShowSessionRecordingConsentDialog(false);
        return;
    }

    const agreementKey =
        `${session.sessionId}:${session.modeAgreedAt}`;

    if (currentUserConsent == null) {
        if (
            consentDialogPromptedForRef.current !==
            agreementKey
        ) {
            consentDialogPromptedForRef.current =
                agreementKey;

            setShowSessionRecordingConsentDialog(true);
        }
    } else {
        consentDialogPromptedForRef.current =
            agreementKey;

        setShowSessionRecordingConsentDialog(false);
    }
}, [
    session,
    user?.id,
    recordingConsentStatus,
    hasLoadedRecordingConsentStatus,
]);

useEffect(() => {
    const negotiationId = session?.negotiationId;

    if (!negotiationId) {
        return;
    }

    let cancelled = false;

    const connectNegotiationChat = async () => {
        try {
            await NegotiationWebSocketService.subscribeToNegotiation(
                negotiationId,
                handleIncomingChatMessage
            );
        } catch (error) {
            if (cancelled) {
                return;
            }

            console.error(
                "Session negotiation WebSocket connection failed.",
                error
            );
        }
    };

    connectNegotiationChat();

    return () => {
        cancelled = true;
        NegotiationWebSocketService.disconnect();
    };
}, [
    session?.negotiationId,
    handleIncomingChatMessage,
]);

useEffect(() => {

    if (!session?.startedAt) return;

    const startedAt =
        new Date(session.startedAt).getTime();

    const updateTimer = () => {

        // If session is completed,
        // freeze timer at completedAt
        const endTime = session.completedAt
            ? new Date(session.completedAt).getTime()
            : Date.now();

        const diff =
            Math.floor((endTime - startedAt) / 1000);

        const hours =
            String(Math.floor(diff / 3600)).padStart(2, "0");

        const minutes =
            String(Math.floor((diff % 3600) / 60)).padStart(2, "0");

        const seconds =
            String(diff % 60).padStart(2, "0");

        setElapsedTime(
            `${hours}:${minutes}:${seconds}`
        );

    };

    // Run once immediately
    updateTimer();

    // If completed, don't keep updating
    if (session.completedAt) {
        return;
    }

    const interval = setInterval(updateTimer, 1000);

    return () => clearInterval(interval);

}, [session]);



useEffect(() => {

    messagesEndRef.current?.scrollIntoView({

        behavior: "smooth",

    });

}, [messages]);

useEffect(() => {

     if (!serverSessionHasStarted) {
    return;
}

if (
    session?.agreedSessionMode !==
    SessionMode.LIVE_VIDEO_SESSION
) {
    return;
}

if (
    recordingConsentStatus?.bothParticipantsConsented !== true ||
    currentUserRecordingConsent !== true
) {
    return;
}

    if (!collaborationRoom) {
        return;
    }

    if (collaborationRoom.status !== "ACTIVE") {
        return;
    }

    console.log("========== CONNECT EFFECT START ==========");

    CollaborationWebSocketService.connect(async () => {

        console.log(
                    "Connected to collaboration room:",
                    collaborationRoom.roomId
                );


                if (!session) {
                    return;
                }

                const receiverId =
                    user?.id === session.requesterId
                        ? session.providerId
                        : session.requesterId;

                const isRequester = user?.id === session.requesterId;

                console.log("========== SUBSCRIBING ==========");

                CollaborationWebSocketService.subscribeToRoom(

                    collaborationRoom.roomId,

                   async (message) => {

const signal: CollaborationSignalMessage = JSON.parse(message.body);

console.log(
    "Received Signal:",
    signal
);

console.log("After received log");

console.log("Before ignore", signal.senderId, user?.id);

if (signal.senderId === user?.id) {
    console.log("Ignored my own signal");
    return;
}

console.log("Not ignored");



console.log("Before switch", signal.signalType);

switch (signal.signalType) {


      case SignalType.JOIN: {

    console.log("JOIN-1");

    try {

        console.log("JOIN-2");

        await startLocalMedia();

        console.log("JOIN-3");

        initializePeerConnection();

        console.log("JOIN-4");

        attachLocalTracks();

        console.log("JOIN-5");

        if (!isRequester) {

                    console.log("JOIN-6 Provider sending READY");

                    const readySignal: CollaborationSignalMessage = {

                        roomId: collaborationRoom.roomId,

                        receiverId: signal.senderId!,

                        signalType: SignalType.READY,

                        payload: "",

                    };

                    CollaborationWebSocketService.sendSignal(readySignal);

                    break;
                }


    } catch (error) {

        console.error(
            "Failed to process JOIN.",
            error
        );

    }

    break;
}



case SignalType.READY: {

    console.log("READY received");

    try {

        await startLocalMedia();

        initializePeerConnection();

        attachLocalTracks();

        const offer = await createOffer();

        const offerSignal: CollaborationSignalMessage = {

            roomId: collaborationRoom.roomId,

            receiverId: signal.senderId!,

            signalType: SignalType.OFFER,

            payload: JSON.stringify(offer),

        };

        CollaborationWebSocketService.sendSignal(
            offerSignal
        );

    } catch (error) {

        console.error(
            "Failed to create offer after READY.",
            error
        );

    }

    break;
}


        case SignalType.OFFER: {

            try {

                await startLocalMedia();

                initializePeerConnection();

                attachLocalTracks();

                const answer =
                    await createAnswer(
                        JSON.parse(signal.payload)
                    );

                const answerSignal: CollaborationSignalMessage = {

                    roomId: collaborationRoom.roomId,

                    receiverId: signal.senderId!,

                    signalType: SignalType.ANSWER,

                    payload: JSON.stringify(answer),

                };

                CollaborationWebSocketService.sendSignal(
                    answerSignal
                );

            } catch (error) {

                console.error(
                    "Failed to process incoming offer.",
                    error
                );

                toast.error(
                    "Unable to establish the video call."
                );

                handleLeaveCall();

            }

            break;

}



        case SignalType.ANSWER: {

            try {

                await setRemoteAnswer(
                    JSON.parse(signal.payload)
                );

            } catch (error) {

                console.error(
                    "Failed to apply remote answer.",
                    error
                );

                toast.error(
                    "Unable to establish the video call."
                );

                handleLeaveCall();

            }

            break;

        }


        case SignalType.ICE_CANDIDATE:

            try {

                await addIceCandidate(
                    JSON.parse(signal.payload)
                );

            } catch (error) {

                console.error(
                    "Failed to add ICE candidate.",
                    error
                );

            }

            break;

        default:

            break;

    }

}

                );

                console.log("========== SUBSCRIBED ==========");

                console.log("Subscribed at:", new Date().toISOString(), "User:", user?.id);

                
                if (!receiverId) {
                    return;
                }

                setOnIceCandidate((candidate) => {

    const iceSignal: CollaborationSignalMessage = {

        roomId: collaborationRoom.roomId,

        receiverId,

        signalType: SignalType.ICE_CANDIDATE,

        payload: JSON.stringify(candidate),

    };

    CollaborationWebSocketService.sendSignal(
        iceSignal
    );

});

                if (isRequester) {

                    const joinSignal: CollaborationSignalMessage = {
                        roomId: collaborationRoom.roomId,
                        receiverId,
                        signalType: SignalType.JOIN,
                        payload: "",
                    };

                    console.log("========== REQUESTER SENDING JOIN ==========");

                        console.log(
                            "JOIN sent at:",
                            new Date().toISOString(),
                            "Requester:",
                            user?.id
                        );

                        CollaborationWebSocketService.sendSignal(joinSignal);

                        console.log("JOIN SENT");

                }

    });

    return () => {

    setOnIceCandidate(null);

    CollaborationWebSocketService.disconnect();

};

}, [
    collaborationRoom,
    serverSessionHasStarted,
    session?.sessionId,
    session?.agreedSessionMode,
    recordingConsentStatus?.bothParticipantsConsented,
    currentUserRecordingConsent,
    user?.id,
]);

useEffect(() => {
    if (
        !session ||
        !canRecordSession ||
        connectionState !== "connected" ||
        !localStream ||
        !remoteStream
    ) {
        return;
    }

    if (
        recorderStartAttemptForSessionRef.current ===
        session.sessionId
    ) {
        return;
    }

    recorderStartAttemptForSessionRef.current =
        session.sessionId;

    void startRecording(
        localStream,
        remoteStream,
        previewStream ?? localStream
    ).then(() => {
        console.info(
            "Actual session recording started:",
            session.sessionId
        );
    }).catch((error: unknown) => {
        recorderStartAttemptForSessionRef.current = null;

        console.error(
            "Failed to start actual session recording.",
            error
        );

        toast.error(
            "The call is connected, but recording could not start. No recording indicator will be shown."
        );
    });
}, [
    session?.sessionId,
    canRecordSession,
    connectionState,
    localStream,
    remoteStream,
    previewStream,
    startRecording,
]);


useEffect(() => {

    if (!sessionId) {
        return;
    }

    SessionWebSocketService.connect(() => {

    console.log("SESSION SOCKET CONNECTED");

    SessionWebSocketService.subscribeToSession(

        sessionId,

        (message) => {

            console.log(
                "SESSION UPDATE:",
                message.body
            );

        }

    );

});

    return () => {

        SessionWebSocketService.disconnect();

    };

}, [sessionId]);


const handleSendMessage = () => {

    if (
        !session ||
        message.trim() === ""
    ) {

        return;

    }


    try {

        NegotiationWebSocketService.sendMessage(

            session.negotiationId,

            message

        );


        setMessage("");

    } catch (error: any) {

        toast.error(

            error?.message ??

            "Unable to send message."

        );

    }

};

const openConsentedVideoSession = () => {
    if (!session) {
        return;
    }

    if (!session.agreedSessionMode) {
        setShowSessionModeDialog(true);
        return;
    }

    if (
        session.agreedSessionMode !==
        SessionMode.LIVE_VIDEO_SESSION
    ) {
        toast("This session is configured for non-live collaboration.");
        return;
    }

    if (
        !hasLoadedRecordingConsentStatus ||
        !recordingConsentStatus ||
        recordingConsentStatus.sessionId !== session.sessionId
    ) {
        toast("Checking recording-consent status.");
        return;
    }

    if (currentUserRecordingConsent !== true) {
        consentDialogPromptedForRef.current = null;
        setShowSessionRecordingConsentDialog(true);
        return;
    }

    if (!recordingConsentStatus.bothParticipantsConsented) {
        toast("Waiting for the other participant's recording consent.");
        return;
    }

    if (!serverSessionHasStarted) {
        toast("Click Start Session to prepare your media and begin the live session.");
        return;
    }

    setCurrentWorkspaceView(WorkspaceView.VIDEO);
};

const handleStartSession = async () => {
    if (!session) {
        return;
    }

    if (!session.agreedSessionMode) {
        setShowSessionModeDialog(true);
        return;
    }

    if (
        session.agreedSessionMode ===
        SessionMode.LIVE_VIDEO_SESSION
    ) {
        if (isStartingLiveSession) {
            return;
        }

        if (
            !hasLoadedRecordingConsentStatus ||
            !recordingConsentStatus ||
            recordingConsentStatus.sessionId !== session.sessionId
        ) {
            toast("Please wait while recording-consent status is checked.");
            return;
        }

        if (currentUserRecordingConsent !== true) {
            consentDialogPromptedForRef.current = null;
            setShowSessionRecordingConsentDialog(true);
            return;
        }

        if (!recordingConsentStatus.bothParticipantsConsented) {
            toast("Both participants must consent before starting live video.");
            return;
        }

        if (serverSessionHasStarted) {
            setCurrentWorkspaceView(WorkspaceView.VIDEO);
            return;
        }

        setIsStartingLiveSession(true);

        try {
            // Request real camera and microphone access first.
            const mediaStream = await startLocalMedia();

            const hasLiveAudio = mediaStream
                .getAudioTracks()
                .some((track) => track.readyState === "live");

            const hasLiveVideo = mediaStream
                .getVideoTracks()
                .some((track) => track.readyState === "live");

            if (!hasLiveAudio || !hasLiveVideo) {
                throw new Error(
                    "A live camera and microphone stream is required."
                );
            }

            // Confirm the browser can create a supported recording format.
            if (typeof MediaRecorder === "undefined") {
                throw new Error(
                    "This browser does not support session recording."
                );
            }

            const supportedRecordingTypes = [
                "video/webm;codecs=vp9,opus",
                "video/webm;codecs=vp8,opus",
                "video/webm",
                "video/mp4",
            ];

            const hasSupportedRecordingType =
                supportedRecordingTypes.some((type) =>
                    MediaRecorder.isTypeSupported(type)
                );

            if (!hasSupportedRecordingType) {
                throw new Error(
                    "This browser does not support a compatible recording format."
                );
            }

           // Read the current readiness status first.
            // Do not report readiness a second time if already ready.
            let readiness = await getRecordingReadiness(
                session.sessionId
            );

            if (!readiness.currentUserRecorderReady) {
                readiness = await reportRecorderReady(
                    session.sessionId
                );
            }

            setCurrentWorkspaceView(WorkspaceView.VIDEO);

            if (!readiness.bothParticipantsRecorderReady) {
                toast.success(
                    "Your camera, microphone, and recorder are ready. " +
                    "Wait for the other participant to prepare, then click Start Session again."
                );
                return;
            }

            // The backend must still validate consent and readiness.
            const updatedSession = await startSession(
                session.sessionId
            );

            setSession(updatedSession);

            await loadWorkspace();

            if (updatedSession.status === "IN_PROGRESS") {
                toast.success(
                    "Session started. Connecting the participants and starting recording."
                );
            } else {
                toast.success(
                    "Your start has been recorded. Waiting for the other participant to start the session."
                );
            }
        } catch (error: any) {
            toast.error(
                error?.response?.data?.message ??
                error?.message ??
                "Unable to prepare or start the live session."
            );
        } finally {
            setIsStartingLiveSession(false);
        }

        return;
    }

    // Preserve the existing non-live session workflow.
    if (
        session.agreedSessionMode !==
        SessionMode.NON_LIVE_COLLABORATION
    ) {
        toast.error("The agreed session mode is invalid.");
        return;
    }

    try {
        await startSession(session.sessionId);

        toast.success("Session updated.");

        await loadWorkspace();
    } catch (error: any) {
        toast.error(
            error?.response?.data?.message ??
            "Unable to start session."
        );
    }
};


const handleSelectSessionMode = async (
    selectedMode: SessionMode
) => {
    if (!session || isModeSubmitting) {
        return;
    }

    setIsModeSubmitting(true);

    try {
        const updatedSession = await selectSessionMode(
            session.sessionId,
            selectedMode
        );

        setSession(updatedSession);
        setShowSessionModeDialog(false);

        if (!updatedSession.agreedSessionMode) {
            toast.success(
                "Your selection is saved. Waiting for the other participant to choose the same mode."
            );
        } else if (
            updatedSession.agreedSessionMode ===
            SessionMode.NON_LIVE_COLLABORATION
        ) {
            toast.success(
                "Both participants agreed to non-live collaboration. Select Start Session again when ready."
            );
        } else {
            toast.success(
                "Both participants agreed to live video. Recording consent must be completed before live media can start."
            );
        }
    } catch (error: any) {
        toast.error(
            error?.response?.data?.message ??
                "Unable to save session mode."
        );
    } finally {
        setIsModeSubmitting(false);
    }
};


const handleSubmitRecordingConsent = async (
    recordingConsent: boolean,
    educationalReuseConsent: boolean
) => {
    if (!session || isRecordingConsentSubmitting) {
        return;
    }

    setIsRecordingConsentSubmitting(true);

    try {
        const updatedStatus = await submitRecordingConsent(
            session.sessionId,
            recordingConsent,
            educationalReuseConsent
        );

        setRecordingConsentStatus(updatedStatus);
        setHasLoadedRecordingConsentStatus(true);
        setShowSessionRecordingConsentDialog(false);

        if (!recordingConsent) {
            toast(
                "Your refusal has been recorded. Live recording cannot start. Both participants must explicitly agree to any alternative before the session can proceed."
            );
        } else if (updatedStatus.bothParticipantsConsented) {
            toast.success(
                "Both participants have consented. Prepare your camera, microphone, and recorder before starting the session."
            );
        } else {
            toast.success(
                "Your recording consent has been saved. Waiting for the other participant."
            );
        }
    } catch (error: any) {
        toast.error(
            error?.response?.data?.message ??
                "Unable to submit recording consent."
        );
    } finally {
        setIsRecordingConsentSubmitting(false);
    }
};


const handleProviderComplete = async () => {

    if (!session) return;

    try {

        await providerConfirm(
            session.sessionId
        );

        toast.success(
            "Work marked completed."
        );

        await loadWorkspace();

    } catch (error: any) {

        toast.error(

            error?.response?.data?.message ??

            "Unable to complete."

        );

    }

};

const handleRequesterComplete = async () => {

    if (!session) return;

    try {

        await requesterConfirm(
            session.sessionId
        );

        toast.success(
            "Session completed."
        );

        await loadWorkspace();

    } catch (error: any) {

        toast.error(

            error?.response?.data?.message ??

            "Unable to confirm."

        );

    }

};



const handleConfirmMilestoneCompletion = async (
  milestoneId: string,
): Promise<MilestoneCompletionResult | null> => {
  try {
    const result = await confirmMilestoneCompletion(
      milestoneId,
    );

    if (result.settled) {
      toast.success(
        result.message ||
          `Milestone ${result.milestoneNumber} release was confirmed.`,
      );
    } else {
      toast(
        result.message ||
          "Confirmation recorded. Settlement may still be pending.",
      );
    }

    await loadWorkspace();

    return result;
  } catch (error: any) {
    toast.error(
      error?.response?.data?.message ??
        "Unable to confirm or reconcile milestone completion.",
    );

    return null;
  }
};


const handleRetryNextMilestoneFunding = async (
  agreementId: string,
): Promise<void> => {
  try {
    const result = await retryNextMilestoneFunding(
      agreementId,
    );

    if (result.allMilestonesReleased) {
      toast.success(
        result.message ||
          "All milestone releases were checked. Refreshing the session.",
      );
    } else if (result.milestoneFunded) {
      toast.success(
        result.message ||
          "Next milestone funding was confirmed.",
      );
    } else {
      toast(
        result.message ||
          "Funding remains unresolved and may require reconciliation.",
      );
    }

    await loadWorkspace();
  } catch (error: any) {
    toast.error(
      error?.response?.data?.message ??
        "Unable to resolve milestone funding.",
    );
  }
};


const handleCancelSession = async () => {

    if (!session) return;

    try {

        await cancelSession(
            session.sessionId
        );

        toast.success(
            "Session cancelled."
        );

        await loadWorkspace();

    } catch (error: any) {

        toast.error(

            error?.response?.data?.message ??

            "Unable to cancel."

        );

    }

};

const handleDisputeSession = () => {
    if (!session) {
        return;
    }

    setShowOpenDisputeDialog(true);
};

const handleSubmitDispute = async (reason: string) => {
    if (!session || isDisputeSubmitting) {
        return;
    }

    try {
        setIsDisputeSubmitting(true);

        const updatedSession = await disputeSession(
            session.sessionId,
            reason,
        );

        // Avoid leaving the form open if the server accepted the request
        // but did not return the persistent case ID.
        setShowOpenDisputeDialog(false);

        await loadWorkspace();

        if (!updatedSession.disputeId) {
            toast.error(
                "The backend did not return the dispute case reference. Verify the session response contract before trying to open another dispute.",
            );
            return;
        }

        toast.success(
            `Dispute case ${updatedSession.disputeId} opened successfully.`,
        );
    } catch (error: any) {
        toast.error(
            error?.response?.data?.message ??
                "Unable to open the dispute.",
        );
    } finally {
        setIsDisputeSubmitting(false);
    }
};

const handleLeaveCall = async () => {
    if (leavingCallRef.current) {
        return;
    }

    leavingCallRef.current = true;

    let uploadFailed = false;

    try {
        const stoppedBlob = await stopRecording();

        const isRequester =
            Boolean(session && user?.id === session.requesterId);

        if (
            stoppedBlob &&
            session &&
            isRequester
        ) {
            pendingRecordingUploadRef.current = {
                sessionId: session.sessionId,
                blob: stoppedBlob,
                durationSeconds: recordingElapsedSeconds,
            };
        }

        const pendingUpload = pendingRecordingUploadRef.current;

        if (
            session &&
            isRequester &&
            pendingUpload?.sessionId === session.sessionId &&
            uploadedRecordingForSessionRef.current !== session.sessionId
        ) {
            try {
                await uploadSessionRecording(
                    pendingUpload.sessionId,
                    pendingUpload.blob,
                    pendingUpload.durationSeconds
                );

                uploadedRecordingForSessionRef.current =
                    pendingUpload.sessionId;

                pendingRecordingUploadRef.current = null;

                toast.success(
                    "The session recording was uploaded successfully."
                );
            } catch (error: any) {
                uploadFailed = true;

                console.error(
                    "Session recording upload failed.",
                    error
                );

                toast.error(
                    error?.response?.data?.message ??
                    "Recording upload failed. The recording remains in this page's memory; click Leave Call again to retry."
                );
            }
        }
    } finally {
        if (!uploadFailed) {
            setOnIceCandidate(null);

            CollaborationWebSocketService.disconnect();

            stopLocalMedia();

            recorderStartAttemptForSessionRef.current = null;

            setCurrentWorkspaceView(WorkspaceView.CHAT);
        }

        leavingCallRef.current = false;
    }
};

if (loading) {

  return <div>Loading...</div>;

}

return (

<div className="flex h-[calc(100vh-8rem)] min-h-0 flex-col overflow-hidden p-5">

     <WorkspaceHeader
            currentView={currentWorkspaceView}
           onViewChange={(nextView) => {
    if (nextView === WorkspaceView.VIDEO) {
        openConsentedVideoSession();
        return;
    }

    if (nextView === WorkspaceView.SCREEN_SHARE) {
        stopLocalMedia();
        setCurrentWorkspaceView(WorkspaceView.CHAT);

        toast(
            "Screen sharing remains disabled until real media readiness is verified."
        );

        return;
    }

    setCurrentWorkspaceView(nextView);
}}
            onToggleInfo={() =>
                setIsInfoPanelOpen(previous => !previous)
            }
        />

  <div className="flex min-h-0 flex-1 gap-5 overflow-hidden">

      {/* LEFT PANEL */}

<div className="min-h-0 min-w-0 flex-1 overflow-hidden">

            {currentWorkspaceView === WorkspaceView.CHAT && (

            <ChatView
                messages={messages}
                currentUserId={user?.id}
                message={message}
                onMessageChange={setMessage}
                onSendMessage={handleSendMessage}
                messagesEndRef={messagesEndRef}
            />

        )}



{currentWorkspaceView === WorkspaceView.RESOURCES && (
    session ? (
        <SessionProofPanel
            sessionId={session.sessionId}
            currentUserId={user?.id}
        />
    ) : (
        <div className="flex h-full items-center justify-center rounded-xl border">
            <p className="text-muted-foreground">
                Session information is unavailable.
            </p>
        </div>
    )
)}


{currentWorkspaceView === WorkspaceView.VIDEO && (
    <VideoView
        previewStream={previewStream}
        remoteStream={remoteStream}
        connectionState={connectionState}
        isMuted={isMuted}
        onToggleMicrophone={toggleMicrophone}
        isCameraOff={isCameraOff}
        onToggleCamera={toggleCamera}
        isScreenSharing={isScreenSharing}
        onToggleScreenShare={toggleScreenShare}
        onLeaveCall={handleLeaveCall}
        recordingAllowed={isRecording}
recordingElapsedSeconds={recordingElapsedSeconds}
    />
)}

{currentWorkspaceView === WorkspaceView.SCREEN_SHARE && (

    <div className="flex h-full items-center justify-center rounded-xl border">
        <div className="text-center">
            <h2 className="text-2xl font-bold">
                Screen Sharing
            </h2>

            <p className="mt-2 text-muted-foreground">
                Coming Soon
            </p>
        </div>
    </div>

)}

{currentWorkspaceView === WorkspaceView.DETAILS && (

    <div className="flex h-full items-center justify-center rounded-xl border">
        <div className="text-center">
            <h2 className="text-2xl font-bold">
                Details
            </h2>

            <p className="mt-2 text-muted-foreground">
                Coming Soon
            </p>
        </div>
    </div>

)}

</div>

{/* RIGHT PANEL */}

<div
    className={`
    h-full
    min-h-0
    shrink-0
    overflow-hidden
    transition-all
    duration-300
    ${
        isInfoPanelOpen
            ? "w-[360px]"
            : "w-0 border-0"
    }
`}
>
 <SessionInfoPanel
    session={session}
    agreement={agreement}
    collaborationRoom={collaborationRoom}
    elapsedTime={elapsedTime}
    userId={user?.id}
    onStartSession={handleStartSession}
    onProviderComplete={handleProviderComplete}
    onRequesterComplete={handleRequesterComplete}
    onCancelSession={handleCancelSession}
    onDisputeSession={handleDisputeSession}
    activeVersion={activeVersion}
onConfirmMilestoneCompletion={handleConfirmMilestoneCompletion}
onRetryNextMilestoneFunding={handleRetryNextMilestoneFunding}
/>
      </div>

      <SessionModeDialog
    open={showSessionModeDialog}
    isSubmitting={isModeSubmitting}
    onClose={() => setShowSessionModeDialog(false)}
    onSelect={handleSelectSessionMode}
/>

<SessionRecordingConsentDialog
    open={showSessionRecordingConsentDialog}
    isSubmitting={isRecordingConsentSubmitting}
    onClose={() =>
        setShowSessionRecordingConsentDialog(false)
    }
    onSubmit={handleSubmitRecordingConsent}
/>

<OpenDisputeDialog
    open={showOpenDisputeDialog}
    isSubmitting={isDisputeSubmitting}
    sessionId={session?.sessionId ?? ""}
    onClose={() => setShowOpenDisputeDialog(false)}
    onSubmit={handleSubmitDispute}
/>

      <SubmitRatingDialog
    open={showRatingDialog}
    onOpenChange={setShowRatingDialog}
    sessionId={session?.sessionId ?? ""}
    ratedUserName={
        user?.id === session?.requesterId
            ? session?.providerName ?? ""
            : session?.requesterName ?? ""
    }
    onSubmitted={async () => {

        setHasRated(true);

        setShowRatingDialog(false);

        await loadWorkspace();

    }}
/>

    </div>
  </div>
);
}

export default SessionWorkspacePage;