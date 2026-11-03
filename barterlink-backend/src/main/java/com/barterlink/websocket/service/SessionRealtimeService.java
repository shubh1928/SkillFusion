package com.barterlink.websocket.service;

import com.barterlink.sessions.entity.ServiceSession;

public interface SessionRealtimeService {

    void broadcastSessionScheduled(
            ServiceSession session);

    void broadcastSessionRescheduled(
            ServiceSession session);

    void broadcastScheduleAccepted(
            ServiceSession session);

    void broadcastSessionStarted(
            ServiceSession session);

    void broadcastWorkCompleted(
            ServiceSession session);

    void broadcastSessionCompleted(
            ServiceSession session);

    void broadcastSessionCancelled(
            ServiceSession session);

    void broadcastSessionDisputed(
            ServiceSession session);
}