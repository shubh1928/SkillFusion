export default interface SessionRecordingConsentStatus {
  sessionId: string;

  // null means the participant has not submitted a decision
  requesterRecordingConsent: boolean | null;
  providerRecordingConsent: boolean | null;

  // Educational reuse is a separate, optional permission
  requesterEducationalReuseConsent: boolean | null;
  providerEducationalReuseConsent: boolean | null;

  requesterConsentAt: string | null;
  providerConsentAt: string | null;

  policyVersion: string;
  modeAgreedAt: string;

  // Calculated by the backend
  bothParticipantsConsented: boolean;
}