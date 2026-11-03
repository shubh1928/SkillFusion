package com.barterlink.websocket.service;

public interface WebSocketService {

    void sendNegotiationMessage(
            String destination,
            Object payload);

    void sendNotification(
            String destination,
            Object payload);

    void sendSessionUpdate(
            String destination,
            Object payload);

    void sendCollaborationSignal(
            String destination,
            Object payload);
}