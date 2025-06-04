package io.preboot.notifications.implementation.message;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseWsMessage implements Serializable {
    String id;
    String message;
    Instant timestamp;

    public BaseWsMessage(String message) {
        this.id = UUID.randomUUID().toString();
        this.message = message;
        this.timestamp = Instant.now();
    }
}
