package com.barterlink.collaboration.signaling.service;

import com.barterlink.collaboration.signaling.dto.CollaborationSignalMessageDto;
import java.security.Principal;

public interface SignalingService {

    void relaySignal(
            CollaborationSignalMessageDto signal,
            Principal principal);

}