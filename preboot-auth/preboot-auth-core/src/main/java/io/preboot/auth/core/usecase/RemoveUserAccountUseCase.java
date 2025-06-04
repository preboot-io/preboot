package io.preboot.auth.core.usecase;

import io.preboot.auth.api.event.UserAccountRemovedEvent;
import io.preboot.auth.api.event.UserAccountRemovedFromTenantEvent;
import io.preboot.auth.core.model.UserAccount;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.repository.UserAccountSessionRepository;
import io.preboot.auth.core.repository.UserAccountTenantRepository;
import io.preboot.eventbus.EventPublisher;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveUserAccountUseCase {
    private final UserAccountRepository userAccountRepository;
    private final UserAccountSessionRepository userAccountSessionRepository;
    private final UserAccountTenantRepository userAccountTenantRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void execute(final UUID userUuid, final UUID tenantId) {
        userAccountRepository.findByUuid(userUuid).ifPresent(userAccount -> removeUserAccount(userAccount, tenantId));
    }

    private void removeUserAccount(final UserAccount userAccount, final UUID tenantId) {
        userAccount.clearTenantRolesAndPermissions(tenantId);
        userAccountTenantRepository.deleteAllByUserAccountUuidAndTenantUuid(userAccount.getUuid(), tenantId);
        userAccountSessionRepository.deleteByUserAccountIdAndTenantId(userAccount.getUuid(), tenantId);
        if (userAccount.getTenantIds().isEmpty()) {
            userAccountRepository.delete(userAccount);
            eventPublisher.publish(new UserAccountRemovedEvent(userAccount.getEmail(), userAccount.getUsername()));
        } else {
            userAccountRepository.save(userAccount);
        }
        eventPublisher.publish(new UserAccountRemovedFromTenantEvent(userAccount.getUuid(), tenantId));
    }
}
