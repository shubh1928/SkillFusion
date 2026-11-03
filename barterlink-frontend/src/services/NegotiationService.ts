import apiClient from "@/config/ApiClient";

import type Negotiation
from "@/models/negotiation/Negotiation";

import type NegotiationMessage
from "@/models/negotiation/NegotiationMessage";

import type {
  AgreementReleaseMode,
} from "@/models/agreement/AgreementVersion";

/* -----------------------------
   Open Negotiation
------------------------------ */

export const openNegotiation = async (
  interestId: string
): Promise<Negotiation> => {

  const response = await apiClient.post(
    `/negotiations/interests/${interestId}`
  );

  return response.data.data;

};


export const continueChat = async (
  requestId: string
): Promise<Negotiation> => {

  const response = await apiClient.get(
    `/negotiations/request/${requestId}/continue-chat`
  );

  return response.data.data;

};



/* -----------------------------
   Get Negotiation
------------------------------ */

export const getNegotiation = async (
  negotiationId: string
): Promise<Negotiation> => {

  const response = await apiClient.get(
    `/negotiations/${negotiationId}`
  );

  return response.data.data;

};

/* -----------------------------
   Provider Negotiations
------------------------------ */

export const getMyNegotiations = async (
): Promise<Negotiation[]> => {

  const response = await apiClient.get(
    "/negotiations/provider"
  );

  return response.data.data;

};

/* -----------------------------
   Request Negotiations
------------------------------ */

export const getRequestNegotiations = async (
  requestId: string
): Promise<Negotiation[]> => {

  const response = await apiClient.get(
    `/negotiations/request/${requestId}`
  );

  return response.data.data;

};

/* -----------------------------
   Messages
------------------------------ */

export const getMessages = async (
  negotiationId: string
): Promise<NegotiationMessage[]> => {

  const response = await apiClient.get(
    `/negotiations/${negotiationId}/messages`
  );

  return response.data.data;

};

/* -----------------------------
   Send Message
------------------------------ */

export const sendMessage = async (
  negotiationId: string,
  message: string
): Promise<NegotiationMessage> => {

  const response = await apiClient.post(
    `/negotiations/${negotiationId}/messages`,
    {
      message,
    }
  );

  return response.data.data;

};

/* -----------------------------
   Update Proposal
------------------------------ */

export const updateProposal = async (
  negotiationId: string,
  proposal: {

    offerTitle: string;

    offerDescription: string;

    requestTitle: string;

    requestDescription: string;

    exchangeMode:
      | "DIRECT_BARTER"
      | "TOKEN_BASED"
      | "HYBRID";

    estimatedHours: number;

    tokenAmount?: number;

  }

): Promise<Negotiation> => {

  const response = await apiClient.put(
    `/negotiations/${negotiationId}/proposal`,
    proposal
  );

  return response.data.data;

};



/* -----------------------------
   Select Agreement Release Mode
------------------------------ */

export const selectAgreementReleaseMode = async (
  negotiationId: string,
  releaseMode: AgreementReleaseMode,
  milestoneCount?: number,
): Promise<Negotiation> => {
  const response = await apiClient.put(
    `/negotiations/${negotiationId}/release-mode`,
    {
      releaseMode,
      milestoneCount,
    },
  );

  return response.data.data;
};



/* -----------------------------
   Session Request
------------------------------ */

export const requestSession = async (
  negotiationId: string
): Promise<Negotiation> => {

  const response = await apiClient.post(
    `/negotiations/${negotiationId}/request-session`
  );

  return response.data.data;

};

/* -----------------------------
   Accept Session
------------------------------ */

export const acceptSessionRequest = async (
  negotiationId: string
): Promise<Negotiation> => {

  const response = await apiClient.post(
    `/negotiations/${negotiationId}/accept-session`
  );

  return response.data.data;

};

/* -----------------------------
   Reject Session
------------------------------ */

export const rejectSessionRequest = async (
  negotiationId: string
): Promise<Negotiation> => {

  const response = await apiClient.post(
    `/negotiations/${negotiationId}/reject-session`
  );

  return response.data.data;

};

/* -----------------------------
   Cancel Negotiation
------------------------------ */

export const cancelNegotiation = async (
  negotiationId: string
): Promise<Negotiation> => {

  const response = await apiClient.delete(
    `/negotiations/${negotiationId}`
  );

  return response.data.data;

};


