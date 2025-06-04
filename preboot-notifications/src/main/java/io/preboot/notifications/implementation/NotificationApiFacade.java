package io.preboot.notifications.implementation;

import io.preboot.core.validation.BeanValidator;
import io.preboot.eventbus.EventPublisher;
import io.preboot.notifications.spi.NotificationApi;
import io.preboot.notifications.spi.event.EmailSentEvent;
import io.preboot.notifications.spi.param.EmailParam;
import io.preboot.notifications.spi.param.WebSocketMessage;
import io.preboot.templates.api.TemplateReader;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Slf4j
@Service
public class NotificationApiFacade implements NotificationApi {

    private final MailSenderServiceApi mailSenderServiceApi;
    private final TemplateReader templateService;
    private final EventPublisher eventPublisher;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Value("${app.configuration.disableEmails:false}")
    private Boolean disableEmails;

    @Override
    public void sendEmail(EmailParam emailParam) {
        BeanValidator.validate(emailParam);

        if (disableEmails != null && disableEmails) {
            log.info("Email sending is disabled. Email will not be sent to {}.", emailParam.getReceiverEmail());
            return;
        }

        final String content = templateService.getContent(emailParam.getTemplate(), emailParam.getTemplateParams());
        Map<String, InputStreamSource> attachments = getAttachments(emailParam);
        String toAddresses = StringUtils.hasText(emailParam.getCopyToEmail())
                ? "%s,%s".formatted(emailParam.getReceiverEmail(), emailParam.getCopyToEmail())
                : emailParam.getReceiverEmail();
        mailSenderServiceApi.send(
                emailParam.getSenderEmail(), toAddresses, emailParam.getSubject(), content, attachments);
        eventPublisher.publish(new EmailSentEvent(
                emailParam.getSenderEmail(),
                emailParam.getCopyToEmail(),
                emailParam.getReceiverEmail(),
                emailParam.getSubject(),
                content));
    }

    @Override
    public void sendWebSocketMessage(WebSocketMessage message) {
        if (StringUtils.hasText(message.getReceiverId())) {
            simpMessagingTemplate.convertAndSendToUser(
                    message.getReceiverId(), message.getDestination(), message.getPayload());
        } else {
            simpMessagingTemplate.convertAndSend(message.getDestination(), message.getPayload());
        }
    }

    private Map<String, InputStreamSource> getAttachments(EmailParam emailParam) {
        if (!emailParam.hasAttachments()) {
            return Collections.emptyMap();
        }
        return emailParam.getAttachments().entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> () -> new ByteArrayInputStream(entry.getValue())));
    }
}
