export default interface SessionRecordingReadinessStatus {
  sessionId: string;

  modeAgreedAt: string | null;

  requesterRecorderReadyAt: string | null;
  providerRecorderReadyAt: string | null;

  requesterRecorderReady: boolean;
  providerRecorderReady: boolean;
  currentUserRecorderReady: boolean;

  bothParticipantsConsented: boolean;
  bothParticipantsRecorderReady: boolean;
}