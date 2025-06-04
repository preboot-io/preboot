package io.preboot.auth.core.spring;

import io.jsonwebtoken.Claims;
import io.preboot.auth.api.event.UserAccountActivatedEvent;
import io.preboot.auth.api.event.UserAccountActivationTokenGeneratedEvent;
import io.preboot.auth.api.exception.InvalidActivationTokenException;
import io.preboot.auth.core.model.UserAccount;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.service.JwtTokenService;
import io.preboot.eventbus.EventPublisher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountActivationService {
    private final JwtTokenService jwtTokenService;
    private final UserAccountRepository userAccountRepository;
    private final EventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    public void createActivationToken(UserAccount userAccount) {
        String token = jwtTokenService.generateActivationToken(userAccount.getUuid());
        eventPublisher.publish(new UserAccountActivationTokenGeneratedEvent(
                userAccount.getUuid(), userAccount.getEmail(), userAccount.getUsername(), token));
    }

    @Transactional
    public UserAccount validateAndActivateAccount(String token, String password) {
        Claims claims = jwtTokenService.validateActivationToken(token);
        UUID userId = UUID.fromString(claims.getSubject());

        UserAccount user = userAccountRepository
                .findByUuid(userId)
                .orElseThrow(() -> new InvalidActivationTokenException("Invalid token"));

        if (user.isActive()) {
            throw new InvalidActivationTokenException("Account already activated");
        }

        String encodedPassword = passwordEncoder.encode(password);
        user.setEncodedPassword(encodedPassword);
        user.setActive(true);

        final UserAccount saved = userAccountRepository.save(user);
        eventPublisher.publish(new UserAccountActivatedEvent(saved.getUuid(), saved.getEmail(), saved.getUsername()));
        return saved;
    }
}
