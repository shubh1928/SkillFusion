import type SignalType from "./SignalType";

export default interface CollaborationSignalMessage {

    roomId: string;

    senderId?: string;         // <-- NEW

    receiverId: string;

    signalType: SignalType;

    payload: string;

    createdAt?: string;

}