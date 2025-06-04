package io.preboot.notifications.implementation;

import java.util.Map;
import org.springframework.core.io.InputStreamSource;

public interface MailSenderServiceApi {
    void send(String from, String to, String subject, String content, Map<String, InputStreamSource> attachments);
}
