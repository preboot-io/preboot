package io.preboot.auth.core.usecase;

import io.preboot.auth.api.dto.UpdatePasswordCommand;
import io.preboot.auth.api.event.UserAccountPasswordUpdatedEvent;
import io.preboot.auth.api.exception.UserAccountNotFoundException;
import io.preboot.auth.core.model.UserAccount;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.eventbus.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePasswordUseCase {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    @Transactional
    public void execute(final UpdatePasswordCommand command) {
        final UserAccount userAccount = userAccountRepository
                .findByUuid(command.userId())
                .orElseThrow(() -> new UserAccountNotFoundException("User not found: " + command.userId()));
        final String currentPasswordEncoded = passwordEncoder.encode(command.currentPassword());
        final String newPasswordEncoded = passwordEncoder.encode(command.newPassword());
        userAccount.updatePassword(currentPasswordEncoded, newPasswordEncoded);
        userAccountRepository.save(userAccount);
        eventPublisher.publish(new UserAccountPasswordUpdatedEvent(
                userAccount.getUuid(), userAccount.getEmail(), userAccount.getUsername()));
    }
}
