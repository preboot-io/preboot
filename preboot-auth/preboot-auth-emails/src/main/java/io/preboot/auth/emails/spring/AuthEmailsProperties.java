package io.preboot.auth.emails.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "preboot.auth-emails")
public class AuthEmailsProperties {
    private String passwordResetUrl;
    private String passwordActivationUrl;
    private String senderEmail;
    private String accountActivationEmailTemplate = "account-activation.email";
    private String accountActivationEmailSubject = "Account Activation";
    private String forgottenPasswordResetEmailTemplate = "account-reset-password.email";
    private String forgottenPasswordResetEmailSubject = "Password Reset";
    private String logoPath = "svg/logo.svg";
}
