package io.preboot.auth.core.usecase;

import io.preboot.auth.api.dto.ResentActivationLinkCommand;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.spring.AccountActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResendActivationLinkUseCase {
    private final UserAccountRepository userAccountRepository;
    private final AccountActivationService accountActivationService;

    @Transactional
    public void execute(final ResentActivationLinkCommand command) {
        userAccountRepository.findByUuid(command.userId()).ifPresent(userAccount -> {
            if (!userAccount.isActive()) {
                if (command.tenantId() != null && !userAccount.getTenantIds().contains(command.tenantId())) {
                    throw new AccessDeniedException("ACCESS DENIED");
                }
                // Generate and send activation token
                accountActivationService.createActivationToken(userAccount);
            }
        });
    }
}
