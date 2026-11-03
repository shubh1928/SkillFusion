package com.barterlink.websocket.service.impl;

import com.barterlink.collaboration.signaling.dto.CollaborationSignalMessageDto;
import com.barterlink.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl
        implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendNegotiationMessage(
            String destination,
            Object payload) {

        messagingTemplate.convertAndSend(
                destination,
                payload);
    }

    @Override
    public void sendNotification(
            String destination,
            Object payload) {

        messagingTemplate.convertAndSend(
                destination,
                payload);
    }

    @Override
    public void sendSessionUpdate(
            String destination,
            Object payload) {

        messagingTemplate.convertAndSend(
                destination,
                payload);
    }


    @Override
    public void sendCollaborationSignal(
            String destination,
            Object payload) {

        System.out.println("========== SEND COLLABORATION SIGNAL ==========");
        System.out.println("Destination : " + destination);

        if (payload instanceof CollaborationSignalMessageDto signal) {

            System.out.println("Type       : " + signal.getSignalType());
            System.out.println("Sender     : " + signal.getSenderId());
            System.out.println("Receiver   : " + signal.getReceiverId());
            System.out.println("Room       : " + signal.getRoomId());

        }

        messagingTemplate.convertAndSend(
                destination,
                payload);

        System.out.println("========== SIGNAL SENT ==========");
    }

}