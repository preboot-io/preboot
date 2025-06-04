package io.preboot.auth.emails;

import io.preboot.auth.api.event.UserAccountActivationTokenGeneratedEvent;
import io.preboot.auth.api.event.UserAccountPasswordResetTokenGeneratedEvent;
import io.preboot.auth.emails.spring.AuthEmailsProperties; // Ensure this import is present
import io.preboot.eventbus.EventHandler;
import io.preboot.notifications.spi.NotificationApi;
import io.preboot.notifications.spi.param.EmailParam;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEventsHandler {
    private final AuthEmailsProperties authEmailsProperties;
    private final NotificationApi notificationApi;

    @Value("${preboot.security.password-reset-token-timeout-in-days}")
    private Integer passwordResetTokenTimeoutInDays;

    @Value("${preboot.security.activation-token-timeout-in-days}")
    private Integer activationTokenTimeoutInDays;

    @EventHandler
    public void onUserAccountActivationTokenGeneratedEvent(UserAccountActivationTokenGeneratedEvent event) {
        notificationApi.sendEmail(EmailParam.builder(
                        authEmailsProperties.getSenderEmail(),
                        event.email(),
                        authEmailsProperties.getAccountActivationEmailSubject(),
                        authEmailsProperties.getAccountActivationEmailTemplate())
                .templateParams(Map.of(
                        "userName",
                        event.username(),
                        "activationLink",
                        "%s?token=%s".formatted(authEmailsProperties.getPasswordActivationUrl(), event.token()),
                        "tokenExpirationInDays",
                        activationTokenTimeoutInDays,
                        "logoBase64",
                        getLogoAsBase64()))
                .build());
    }

    @EventHandler
    public void onPasswordResetTokenGenerated(UserAccountPasswordResetTokenGeneratedEvent event) {
        notificationApi.sendEmail(EmailParam.builder(
                        authEmailsProperties.getSenderEmail(),
                        event.email(),
                        authEmailsProperties.getForgottenPasswordResetEmailSubject(),
                        authEmailsProperties.getForgottenPasswordResetEmailTemplate())
                .templateParams(Map.of(
                        "userName", event.username(),
                        "url", "%s?token=%s".formatted(authEmailsProperties.getPasswordResetUrl(), event.token()),
                        "tokenExpirationInHours", passwordResetTokenTimeoutInDays * 24,
                        "logoBase64", getLogoAsBase64()))
                .build());
    }

    private String getLogoAsBase64() {
        byte[] svgBytes = loadSvgLogo();
        if (svgBytes != null && svgBytes.length > 0) { // Added null check for svgBytes
            return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svgBytes);
        }
        return "";
    }

    private byte[] loadSvgLogo() {
        // Use the configured logo path from AuthEmailsProperties
        String logoResourcePath = authEmailsProperties.getLogoPath();
        try {
            Resource resource = new ClassPathResource(logoResourcePath);
            if (!resource.exists()) {
                log.warn("Logo resource not found at classpath: {}", logoResourcePath); // More specific log
                // Try to load the default logo as a fallback if the configured one is not found and is not the default
                // one
                if (!"svg/logo.svg".equals(logoResourcePath)) {
                    log.info("Attempting to load default logo from classpath: svg/logo.svg");
                    resource = new ClassPathResource("svg/logo.svg");
                    if (!resource.exists()) {
                        log.error("Default logo resource also not found at classpath: svg/logo.svg");
                        return new byte[0];
                    }
                } else {
                    return new byte[0]; // If default is configured and not found, return empty
                }
            }
            try (InputStream inputStream = resource.getInputStream()) { // Use try-with-resources
                return StreamUtils.copyToByteArray(inputStream);
            }
        } catch (IOException e) {
            log.error("Failed to load SVG logo from path: {}", logoResourcePath, e);
            return new byte[0];
        }
    }
}
