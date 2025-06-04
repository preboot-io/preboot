package io.preboot.auth.core.spring;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthAccountProperties.class)
class AuthAccountConfiguration {}
