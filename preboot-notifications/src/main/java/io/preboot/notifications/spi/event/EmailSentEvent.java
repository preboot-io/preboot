package io.preboot.notifications.spi.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public class EmailSentEvent {

    private final String from;
    private final String copyToEmail;
    private final String receiver;
    private final String subject;
    private final String content;
}
