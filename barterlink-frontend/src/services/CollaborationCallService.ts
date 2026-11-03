class CollaborationCallService {

    private localStream: MediaStream | null = null;

    private isMicrophoneMuted = false;

    private isCameraDisabled = false;

    private displayStream: MediaStream | null = null;

    private isScreenSharing = false;

    private screenShareStateListener: ((sharing: boolean) => void) | null = null;

    private peerConnection: RTCPeerConnection | null = null;

    private remoteStream: MediaStream | null = null;

    private onIceCandidateCallback:
    ((candidate: RTCIceCandidate) => void)
    | null = null;

    private onRemoteStreamCallback:
    ((stream: MediaStream | null) => void)
    | null = null;

    private onConnectionStateCallback:
    ((state: RTCPeerConnectionState) => void)
    | null = null;

    private onRemoteDisconnectedCallback:
    (() => void)
    | null = null;

            async startLocalMedia(): Promise<MediaStream> {

                console.log("startLocalMedia()");
                console.log("Existing localStream:", this.localStream);

                if (this.localStream) {

                    return this.localStream;

                }

                try {

                    this.localStream =
                        await navigator.mediaDevices.getUserMedia({

                            video: true,

                            audio: true,

                        });

                    console.log(
                        "Local media started."
                    );

                    return this.localStream;

                } catch (error) {

                    console.error(
                        "Failed to access camera/microphone.",
                        error
                    );

                    throw error;

                }

            }

    getLocalStream(): MediaStream | null {

        return this.localStream;

    }


    getPreviewStream(): MediaStream | null {

    if (this.isScreenSharing && this.displayStream) {

        return this.displayStream;

    }

    return this.localStream;

}



        initializePeerConnection(): RTCPeerConnection {

        if (this.peerConnection) {

            return this.peerConnection;

        }

        this.peerConnection =
            new RTCPeerConnection({

                iceServers: [

                    {
                        urls: "stun:stun.l.google.com:19302",
                    },

                ],

            });

            this.peerConnection.onicecandidate = (event) => {

                    if (!event.candidate) {

                        return;

                    }

                    console.log(
                        "ICE Candidate:",
                        event.candidate
                    );

                    this.onIceCandidateCallback?.(
                        event.candidate
                    );

                };

                this.peerConnection.ontrack = (event) => {

                        this.remoteStream = event.streams[0];

                        console.log(
                            "Remote stream received.",
                            this.remoteStream
                        );

                        this.onRemoteStreamCallback?.(
                            this.remoteStream
                        );

                    };

        this.peerConnection.onconnectionstatechange = () => {

            const state =
                this.peerConnection?.connectionState;

            console.log(
                "Connection State:",
                state
            );

            if (state) {

                this.onConnectionStateCallback?.(state);

            }

            if (
                state === "disconnected" ||
                state === "failed" ||
                state === "closed"
            ) {

                this.remoteStream = null;

                this.onRemoteStreamCallback?.(null);

                this.onRemoteDisconnectedCallback?.();

            }

        };


        this.peerConnection.oniceconnectionstatechange = () => {

    console.log(
        "ICE State:",
        this.peerConnection?.iceConnectionState
    );

};

        console.log(
            "Peer connection initialized."
        );

        return this.peerConnection;

    }


    getPeerConnection(): RTCPeerConnection | null {

            return this.peerConnection;

        }

    
    setOnIceCandidate(

    callback:
        ((candidate: RTCIceCandidate) => void)
        | null

) {

    this.onIceCandidateCallback = callback;

}



                setOnRemoteStream(

            callback:
                ((stream: MediaStream | null) => void)
                | null

        ){

            this.onRemoteStreamCallback = callback;

        }


        setOnConnectionState(

            callback:
                ((state: RTCPeerConnectionState) => void)
                | null

        ) {

            this.onConnectionStateCallback = callback;

        }


        setOnRemoteDisconnected(
            callback: (() => void) | null
        ) {

            this.onRemoteDisconnectedCallback = callback;

        }


    attachLocalTracks() {

    if (!this.peerConnection) {

        throw new Error(
            "Peer connection has not been initialized."
        );

    }

    if (!this.localStream) {

        throw new Error(
            "Local media has not been started."
        );

    }

    this.localStream
        .getTracks()
        .forEach(track => {

            this.peerConnection!.addTrack(
                track,
                this.localStream!
            );

        });

    console.log(
        "Local media tracks attached."
    );

}




async createOffer(): Promise<RTCSessionDescriptionInit> {

    if (!this.peerConnection) {

        throw new Error(
            "Peer connection has not been initialized."
        );

    }

    try {

        const offer =
            await this.peerConnection.createOffer();

        await this.peerConnection.setLocalDescription(
            offer
        );

        console.log(
            "Offer created.",
            offer
        );

        return offer;

    } catch (error) {

        console.error(
            "Failed to create WebRTC offer.",
            error
        );

        throw error;

    }

}



async createAnswer(
    offer: RTCSessionDescriptionInit
): Promise<RTCSessionDescriptionInit> {

    if (!this.peerConnection) {

        throw new Error(
            "Peer connection has not been initialized."
        );

    }

    try {

        await this.peerConnection.setRemoteDescription(
            new RTCSessionDescription(offer)
        );

        const answer =
            await this.peerConnection.createAnswer();

        await this.peerConnection.setLocalDescription(
            answer
        );

        console.log(
            "Answer created.",
            answer
        );

        return answer;

    } catch (error) {

        console.error(
            "Failed to create WebRTC answer.",
            error
        );

        throw error;

    }

}



async setRemoteAnswer(
    answer: RTCSessionDescriptionInit
): Promise<void> {

    if (!this.peerConnection) {

        throw new Error(
            "Peer connection has not been initialized."
        );

    }

    try {

        await this.peerConnection.setRemoteDescription(
            new RTCSessionDescription(answer)
        );

        console.log(
            "Remote answer applied."
        );

    } catch (error) {

        console.error(
            "Failed to apply remote answer.",
            error
        );

        throw error;

    }

}




async addIceCandidate(
    candidate: RTCIceCandidateInit
): Promise<void> {

    if (!this.peerConnection) {

        throw new Error(
            "Peer connection has not been initialized."
        );

    }

    try {

        await this.peerConnection.addIceCandidate(
            new RTCIceCandidate(candidate)
        );

        console.log(
            "Remote ICE candidate added.",
            candidate
        );

    } catch (error) {

        console.error(
            "Failed to add ICE candidate.",
            error
        );

        throw error;

    }

}


toggleMicrophone(): boolean {

    if (!this.localStream) {

        throw new Error(
            "Local media has not been started."
        );

    }

    const audioTrack =
        this.localStream.getAudioTracks()[0];

    if (!audioTrack) {

        throw new Error(
            "No microphone track found."
        );

    }

    this.isMicrophoneMuted =
        !this.isMicrophoneMuted;

    audioTrack.enabled =
        !this.isMicrophoneMuted;

    console.log(
        this.isMicrophoneMuted
            ? "Microphone muted."
            : "Microphone unmuted."
    );

    return this.isMicrophoneMuted;

}



toggleCamera(): boolean {

    if (!this.localStream) {
        return this.isCameraDisabled;
    }

    const videoTrack =
        this.localStream.getVideoTracks()[0];

    if (!videoTrack) {
        return this.isCameraDisabled;
    }

    this.isCameraDisabled = !this.isCameraDisabled;

    videoTrack.enabled = !this.isCameraDisabled;

    return this.isCameraDisabled;

}


setScreenShareStateListener(
    listener: (sharing: boolean) => void
) {
    this.screenShareStateListener = listener;
}


async startScreenShare(): Promise<boolean> {

    if (!this.peerConnection || !this.localStream) {
        return false;
    }

    try {

        this.displayStream =
            await navigator.mediaDevices.getDisplayMedia({

                video: true,

                audio: false,

            });

        const displayTrack =
            this.displayStream.getVideoTracks()[0];

        if (!displayTrack) {
            return false;
        }

        const sender =
            this.peerConnection
                .getSenders()
                .find(sender => sender.track?.kind === "video");

        if (!sender) {
            return false;
        }

        await sender.replaceTrack(displayTrack);

        displayTrack.onended = async () => {

            if (this.isScreenSharing) {

                await this.stopScreenShare();

            }

        };

        this.isScreenSharing = true;

        this.screenShareStateListener?.(true);

        return true;

    } catch (error) {

        console.error(
            "Failed to start screen sharing.",
            error
        );

        return false;

    }

}


async stopScreenShare(): Promise<boolean> {

    if (
        !this.peerConnection ||
        !this.localStream
    ) {
        return false;
    }

    try {

        const cameraTrack =
            this.localStream.getVideoTracks()[0];

        if (!cameraTrack) {
            return false;
        }

        const sender =
            this.peerConnection
                .getSenders()
                .find(sender => sender.track?.kind === "video");

        if (!sender) {
            return false;
        }

        await sender.replaceTrack(cameraTrack);

        this.displayStream
            ?.getTracks()
            .forEach(track => track.stop());

        this.displayStream = null;

        this.isScreenSharing = false;

        this.screenShareStateListener?.(false);

        return true;

    } catch (error) {

        console.error(
            "Failed to stop screen sharing.",
            error
        );

        return false;

    }

}



isMuted(): boolean {

    return this.isMicrophoneMuted;

}



    stopLocalMedia() {

        /*
        * Stop screen sharing first.
        */
        this.displayStream
            ?.getTracks()
            .forEach(track => track.stop());

        this.displayStream = null;

        this.isScreenSharing = false;

        this.screenShareStateListener?.(false);

        /*
        * Stop camera & microphone.
        */
        this.localStream
            ?.getTracks()
            .forEach(track => track.stop());

        this.localStream = null;

        /*
        * Reset UI state.
        */
        this.isMicrophoneMuted = false;

        this.isCameraDisabled = false;

        /*
        * Clear remote stream.
        */
        this.remoteStream = null;

        /*
        * Remove PeerConnection callbacks.
        */
        if (this.peerConnection) {

            this.peerConnection.onicecandidate = null;

            this.peerConnection.ontrack = null;

            this.peerConnection.onconnectionstatechange = null;

            this.peerConnection.oniceconnectionstatechange = null;

            this.peerConnection.close();

        }

        this.peerConnection = null;

        /*
        * Remove service callbacks.
        */
        this.onIceCandidateCallback = null;

        this.onRemoteStreamCallback = null;

        console.log(
            "Local media stopped."
        );

    }

}

export default new CollaborationCallService();