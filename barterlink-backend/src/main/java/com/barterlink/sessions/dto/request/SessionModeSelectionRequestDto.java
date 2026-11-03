package com.barterlink.sessions.dto.request;

import com.barterlink.sessions.enums.SessionMode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionModeSelectionRequestDto {

    private SessionMode selectedMode;

}