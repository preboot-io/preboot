package io.preboot.auth.core.usecase;

import io.preboot.auth.api.dto.ResetPasswordRequest;
import io.preboot.auth.core.model.UserAccount;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public void execute(ResetPasswordRequest request) {
        UserAccount userAccount = passwordResetService.validateAndProcessToken(request.token());

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        userAccount.setEncodedPassword(encodedPassword);

        userAccountRepository.save(userAccount);
    }
}
