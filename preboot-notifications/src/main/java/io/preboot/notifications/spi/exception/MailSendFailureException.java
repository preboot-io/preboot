package io.preboot.notifications.spi.exception;

public class MailSendFailureException extends RuntimeException {
    public MailSendFailureException(String message) {
        super(message);
    }
}
