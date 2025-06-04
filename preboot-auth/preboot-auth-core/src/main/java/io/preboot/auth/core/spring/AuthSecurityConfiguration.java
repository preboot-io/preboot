package io.preboot.auth.core.spring;

import io.preboot.auth.core.service.JwtTokenService;
import io.preboot.auth.core.service.SessionService;
import io.preboot.auth.core.usecase.GetUserAccountUseCase;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthSecurityProperties.class)
public class AuthSecurityConfiguration {

    public static final String[] DEFAULT_PERMIT_ALL = new String[] {
        "/api/auth/login",
        "/api/auth/password/reset-request",
        "/api/auth/password/reset",
        "/api/auth/activation",
        "/api/auth/registration"
    };
    private final AuthSecurityProperties securityProperties;

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            SessionService sessionService,
            GetUserAccountUseCase getUserAccountUseCase) {
        // Combine default endpoints with user-configured endpoints
        String[] permitAllEndpoints = Stream.concat(
                        Stream.of(DEFAULT_PERMIT_ALL), securityProperties.getPublicEndpoints().stream())
                .distinct()
                .toArray(String[]::new);
        return new JwtAuthenticationFilter(
                jwtTokenService, sessionService, getUserAccountUseCase, List.of(permitAllEndpoints));
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource)
            throws Exception {

        // Combine default endpoints with user-configured endpoints
        String[] permitAllEndpoints = Stream.concat(
                        Stream.of(DEFAULT_PERMIT_ALL), securityProperties.getPublicEndpoints().stream())
                .distinct()
                .toArray(String[]::new);

        return http.cors(httpSecurityCorsConfigurer ->
                        httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource))
                .csrf(csrf -> {
                    if (!securityProperties.isEnableCsrf()) {
                        csrf.disable();
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("Unauthorized");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("Access Denied");
                        }))
                .authorizeHttpRequests(auth -> auth.requestMatchers(permitAllEndpoints)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Primary
    CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addExposedHeader(HttpHeaders.AUTHORIZATION);
        corsConfiguration.addExposedHeader(HttpHeaders.CONTENT_DISPOSITION);
        corsConfiguration.addExposedHeader("X-Forwarded-For");
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.applyPermitDefaultValues();
        corsConfiguration.setAllowedOrigins(
                securityProperties.getCorsAllowedOrigins().isEmpty()
                        ? List.of("*")
                        : securityProperties.getCorsAllowedOrigins());
        corsConfiguration.addAllowedMethod(HttpMethod.GET);
        corsConfiguration.addAllowedMethod(HttpMethod.POST);
        corsConfiguration.addAllowedMethod(HttpMethod.OPTIONS);
        corsConfiguration.addAllowedMethod(HttpMethod.DELETE);
        corsConfiguration.addAllowedMethod(HttpMethod.PUT);
        corsConfiguration.addAllowedMethod(HttpMethod.PATCH);
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
