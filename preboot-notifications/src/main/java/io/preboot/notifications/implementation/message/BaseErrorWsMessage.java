package io.preboot.notifications.implementation.message;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseErrorWsMessage extends BaseWsMessage {
    boolean isErrorMessage = true;

    public BaseErrorWsMessage(String message) {
        super(message);
    }
}
