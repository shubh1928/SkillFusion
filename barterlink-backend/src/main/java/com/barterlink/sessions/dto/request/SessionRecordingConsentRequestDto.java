package com.barterlink.sessions.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionRecordingConsentRequestDto {

    private Boolean recordingConsent;

    private Boolean educationalReuseConsent;
}