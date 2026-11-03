export type CollaborationRoomStatus =
  | "CREATED"
  | "ACTIVE"
  | "CLOSED";

export default interface CollaborationRoom {

  roomId: string;

  sessionId: string;

  status: CollaborationRoomStatus;

  createdAt: string;

  activatedAt: string | null;

  closedAt: string | null;

}