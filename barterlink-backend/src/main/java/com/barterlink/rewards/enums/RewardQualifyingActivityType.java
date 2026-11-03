package com.barterlink.rewards.enums;

/**
 * Backend-controlled allowlist of meaningful, successful business actions
 * that may qualify a user for a daily activity streak day.
 *
 * Do not accept these event types directly from the frontend.
 */
public enum RewardQualifyingActivityType {

    MARKETPLACE_REQUEST_CREATED,

    NEGOTIATION_MESSAGE_SENT,

    SESSION_REQUEST_SUBMITTED,
    SESSION_REQUEST_ACCEPTED,

    SESSION_SCHEDULED,
    SESSION_SCHEDULE_ACCEPTED,

    SESSION_PROOF_SUBMITTED,

    SESSION_WORK_MARKED_COMPLETE,
    SESSION_COMPLETION_CONFIRMED,

    NEGOTIATION_PROPOSAL_UPDATED,
    NEGOTIATION_RELEASE_MODE_SELECTED,
    NEGOTIATION_CREATED
}