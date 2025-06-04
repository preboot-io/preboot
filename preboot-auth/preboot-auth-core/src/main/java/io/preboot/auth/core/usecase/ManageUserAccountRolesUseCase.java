package io.preboot.auth.core.usecase;

import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.exception.UserAccountNotFoundException;
import io.preboot.auth.core.model.UserAccount;
import io.preboot.auth.core.repository.UserAccountRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageUserAccountRolesUseCase {
    private final UserAccountRepository userAccountRepository;
    private final GetUserAccountUseCase getUserAccountUseCase;

    @Transactional
    public UserAccountInfo addRole(final UUID userId, final UUID tenantId, final String roleName) {
        final UserAccount userAccount = userAccountRepository
                .findByUuid(userId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found: " + userId));
        userAccount.addRole(roleName, tenantId);
        userAccountRepository.save(userAccount);
        return getUserAccountUseCase.execute(userId, tenantId);
    }

    @Transactional
    public UserAccountInfo removeRole(final UUID userId, final UUID tenantId, final String roleName) {
        final UserAccount userAccount = userAccountRepository
                .findByUuid(userId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found: " + userId));
        userAccount.removeRole(roleName, tenantId);
        userAccountRepository.save(userAccount);
        return getUserAccountUseCase.execute(userId, tenantId);
    }
}
