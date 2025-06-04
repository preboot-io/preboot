package io.preboot.auth.core.spring;

import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.exception.SessionExpiredException;
import io.preboot.auth.core.service.JwtTokenService;
import io.preboot.auth.core.service.SessionService;
import io.preboot.auth.core.usecase.GetUserAccountUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final SessionService sessionService;
    private final GetUserAccountUseCase getUserAccountUseCase;
    private final List<String> EXCLUDED_PATHS;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXCLUDED_PATHS.stream().anyMatch(path -> request.getServletPath().equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                UUID sessionId = jwtTokenService.extractSessionId(token);
                var session = sessionService.getSession(sessionId);

                if (session.getExpiresAt().isBefore(Instant.now())) {
                    throw new SessionExpiredException("Session has expired");
                }

                final UserAccountInfo userAccountInfo =
                        getUserAccountUseCase.execute(session.getUserAccountId(), session.getTenantId());

                Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
                userAccountInfo
                        .roles()
                        .forEach(role -> grantedAuthorities.add((GrantedAuthority) () -> "ROLE_" + role));
                userAccountInfo
                        .permissions()
                        .forEach(permission -> grantedAuthorities.add((GrantedAuthority) () -> permission));
                userAccountInfo
                        .customPermissions()
                        .forEach(permission -> grantedAuthorities.add((GrantedAuthority) () -> permission));

                var authentication =
                        new SessionAwareAuthentication(userAccountInfo, session.getSessionId(), grantedAuthorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
