package io.preboot.auth.emails.spring;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthEmailsProperties.class)
public class AuthEmailsConfiguration {}
