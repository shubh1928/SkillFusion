package com.barterlink.collaboration.signaling.controller;

import com.barterlink.collaboration.signaling.dto.CollaborationSignalMessageDto;
import com.barterlink.collaboration.signaling.service.SignalingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import java.security.Principal;

import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Controller
@RequiredArgsConstructor
public class CollaborationSignalingController {

    private final SignalingService signalingService;

    @MessageMapping("/collaboration.signal")
    public void relaySignal(
            CollaborationSignalMessageDto signal,
            SimpMessageHeaderAccessor headers,
            Principal principal) {

        System.out.println("Controller Principal = " + principal);
        System.out.println("Header User = " + headers.getUser());
        System.out.println("CONTROLLER RECEIVED : " + signal);

        signalingService.relaySignal(signal, principal);
    }

}