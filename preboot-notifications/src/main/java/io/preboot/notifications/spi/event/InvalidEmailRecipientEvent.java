package io.preboot.notifications.spi.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class InvalidEmailRecipientEvent {

    private final String receiver;
    private final String subject;
}
