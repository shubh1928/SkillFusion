package com.barterlink.websocket.constants;

public final class WebSocketDestinationConstants {

    private WebSocketDestinationConstants() {
    }

    /*
     * Client subscribes to these destinations.
     */

    public static final String NEGOTIATION_TOPIC =
            "/topic/negotiation/";

    public static final String NOTIFICATION_TOPIC =
            "/topic/notifications/";

    public static final String SESSION_TOPIC =
            "/topic/session/";

    public static final String COLLABORATION_TOPIC =
            "/topic/collaboration/";

}