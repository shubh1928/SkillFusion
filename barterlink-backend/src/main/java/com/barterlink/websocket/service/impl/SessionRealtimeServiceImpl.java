package com.barterlink.websocket.service.impl;

import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.websocket.constants.WebSocketDestinationConstants;
import com.barterlink.websocket.dto.SessionUpdateDto;
import com.barterlink.websocket.service.SessionRealtimeService;
import com.barterlink.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SessionRealtimeServiceImpl
        implements SessionRealtimeService {

    private final WebSocketService webSocketService;

    @Override
    public void broadcastSessionScheduled(
            ServiceSession session) {

        broadcast(
                session,
                "SESSION_SCHEDULED",
                "Session has been scheduled.");
    }

    @Override
    public void broadcastSessionRescheduled(
            ServiceSession session) {

        broadcast(
                session,
                "SESSION_RESCHEDULED",
                "Session has been rescheduled.");
    }

    @Override
    public void broadcastScheduleAccepted(
            ServiceSession session) {

        broadcast(
                session,
                "SCHEDULE_ACCEPTED",
                "Session schedule accepted.");
    }

    @Override
    public void broadcastSessionStarted(
            ServiceSession session) {

        broadcast(
                session,
                "SESSION_STARTED",
                "Session started.");
    }

    @Override
    public void broadcastWorkCompleted(
            ServiceSession session) {

        broadcast(
                session,
                "WORK_COMPLETED",
                "Work marked as completed.");
    }

    @Override
    public void broadcastSessionCompleted(
            ServiceSession session) {

        broadcast(
                session,
                "SESSION_COMPLETED",
                "Session completed.");
    }

    @Override
    public void broadcastSessionCancelled(
            ServiceSession session) {

        broadcast(
                session,
                "SESSION_CANCELLED",
                "Session cancelled.");
    }

    @Override
    public void broadcastSessionDisputed(
            ServiceSession session) {

        broadcast(
                session,
                "SESSION_DISPUTED",
                "Session disputed.");
    }

    private void broadcast(
            ServiceSession session,
            String event,
            String message) {

        SessionUpdateDto dto =
                SessionUpdateDto.builder()
                        .sessionId(session.getId())
                        .agreementId(session.getAgreement().getId())
                        .requesterId(session.getAgreement().getRequester().getId())
                        .providerId(session.getAgreement().getProvider().getId())
                        .status(session.getStatus())
                        .event(event)
                        .message(message)
                        .eventTime(Instant.now())
                        .build();


        System.out.println("================================");
        System.out.println("SESSION EVENT : " + event);
        System.out.println("SESSION ID    : " + session.getId());
        System.out.println("DESTINATION   : "
                + WebSocketDestinationConstants.SESSION_TOPIC
                + session.getId());
        System.out.println("================================");

        webSocketService.sendSessionUpdate(
                WebSocketDestinationConstants.SESSION_TOPIC
                        + session.getId(),
                dto);
    }
}