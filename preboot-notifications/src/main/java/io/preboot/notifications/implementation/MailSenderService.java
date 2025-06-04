package io.preboot.notifications.implementation;

import io.preboot.notifications.spi.exception.MailSendFailureException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@RequiredArgsConstructor
@Service
class MailSenderService implements MailSenderServiceApi {

    private static final String CHARSET_UTF_8 = "UTF-8";
    private final JavaMailSender mailSender;

    @Override
    public void send(
            String from, String to, String subject, String content, Map<String, InputStreamSource> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            message.setSubject(subject, CHARSET_UTF_8);
            MimeMessageHelper helper = new MimeMessageHelper(message, true, CHARSET_UTF_8);
            helper.setFrom(from);
            helper.setTo(modifyToMultipleReceivers(to));
            helper.setText(content, true);
            if (attachments != null && !CollectionUtils.isEmpty(attachments)) {
                attachments.forEach((attachmentFilename, attachmentContent) -> {
                    try {
                        helper.addAttachment(attachmentFilename, attachmentContent);
                    } catch (MessagingException e) {
                        log.error("Error adding attachment to email", e);
                        throw new MailSendFailureException(e.getMessage());
                    }
                });
            }
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Error sending email", ex);
            throw new MailSendFailureException(ex.getMessage());
        }
    }

    private String[] modifyToMultipleReceivers(String copyToEmail) {
        return Arrays.stream(copyToEmail.split("[,;]")).map(String::trim).toArray(String[]::new);
    }
}
