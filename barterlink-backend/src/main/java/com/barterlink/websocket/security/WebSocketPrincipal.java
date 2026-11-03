package com.barterlink.websocket.security;

import lombok.Getter;

import java.security.Principal;
import java.util.UUID;

@Getter
public class WebSocketPrincipal implements Principal {

    private final UUID userId;

    public WebSocketPrincipal(UUID userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}