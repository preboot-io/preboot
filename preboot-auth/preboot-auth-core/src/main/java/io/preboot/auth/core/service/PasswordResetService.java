package io.preboot.auth.core.service;

import io.jsonwebtoken.Claims;
import io.preboot.auth.api.event.UserAccountPasswordResetTokenGeneratedEvent;
import io.preboot.auth.api.exception.InvalidPasswordResetTokenException;
import io.preboot.auth.api.exception.PasswordResetTokenExpiredException;
import io.preboot.auth.core.model.UserAccount;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.eventbus.EventPublisher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final JwtTokenService jwtTokenService;
    private final UserAccountRepository userAccountRepository;
    private final EventPublisher eventPublisher;

    public void createResetToken(UserAccount userAccount) {
        String token = jwtTokenService.generatePasswordResetToken(
                userAccount.getUuid(), userAccount.getResetTokenVersion() // Include current version
                );
        eventPublisher.publish(new UserAccountPasswordResetTokenGeneratedEvent(
                userAccount.getUuid(), userAccount.getUsername(), userAccount.getEmail(), token));
    }

    @Transactional
    public UserAccount validateAndProcessToken(String token) {
        Claims claims = jwtTokenService.validatePasswordResetToken(token);
        UUID userId = UUID.fromString(claims.getSubject());
        int tokenVersion = claims.get("resetTokenVersion", Integer.class);

        UserAccount user = userAccountRepository
                .findByUuid(userId)
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid token"));

        if (tokenVersion != user.getResetTokenVersion()) {
            throw new PasswordResetTokenExpiredException("Token already used");
        }

        user.incrementResetTokenVersion();
        return userAccountRepository.save(user);
    }
}
