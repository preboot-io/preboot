package io.preboot.auth.core.usecase;

import io.preboot.auth.api.dto.RequestPasswordResetRequest;
import io.preboot.auth.api.exception.UserAccountNotFoundException;
import io.preboot.auth.core.model.UserAccount;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestPasswordResetUseCase {
    private final UserAccountRepository userAccountRepository;
    private final PasswordResetService passwordResetService;

    @Transactional
    public void execute(RequestPasswordResetRequest request) {
        UserAccount userAccount = userAccountRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));

        passwordResetService.createResetToken(userAccount);
    }
}
