package io.preboot.notifications.spi.param;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class WebSocketMessage {
    String receiverId;

    @NotEmpty
    String destination;

    Object payload;
}
