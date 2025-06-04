package io.preboot.notifications.spi;

import io.preboot.notifications.spi.param.EmailParam;
import io.preboot.notifications.spi.param.WebSocketMessage;

public interface NotificationApi {

    void sendEmail(EmailParam emailParam);

    void sendWebSocketMessage(WebSocketMessage webSocketMessage);
}
