import type NegotiationMessageType
from "./NegotiationMessageType";

export default interface NegotiationMessage {

  messageId: string;

  senderId: string;

  senderName: string;

  senderImage?: string;

  messageType: NegotiationMessageType;

  message: string;

  createdAt: string;

}