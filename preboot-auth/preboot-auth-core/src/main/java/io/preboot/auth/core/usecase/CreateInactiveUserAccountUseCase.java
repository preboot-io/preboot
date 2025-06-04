package io.preboot.auth.core.usecase;

import io.preboot.auth.api.dto.CreateInactiveUserAccountRequest;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.event.UserAccountCreatedEvent;
import io.preboot.auth.core.model.UserAccount;
import io.preboot.auth.core.model.UserAccountTenant;
import io.preboot.auth.core.repository.UserAccountRepository;
import io.preboot.auth.core.repository.UserAccountTenantRepository;
import io.preboot.auth.core.spring.AccountActivationService;
import io.preboot.auth.core.spring.AuthAccountProperties;
import io.preboot.eventbus.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateInactiveUserAccountUseCase {
    private final UserAccountRepository userAccountRepository;
    private final AccountActivationService accountActivationService;
    private final EventPublisher eventPublisher;
    private final AuthAccountProperties newAccountProperties;
    private final GetUserAccountUseCase getUserAccountUseCase;
    private final UserAccountTenantRepository userAccountTenantRepository;

    @Transactional
    public UserAccountInfo execute(final CreateInactiveUserAccountRequest request) {
        final UserAccount userAccount = userAccountRepository
                .findByEmail(request.email())
                .orElseGet(() -> {
                    // Create user account without password
                    final UserAccount account = UserAccount.create(
                            request.username(),
                            request.email(),
                            null, // no password yet
                            request.language() != null ? request.language() : newAccountProperties.getDefaultLanguage(),
                            request.timezone() != null
                                    ? request.timezone()
                                    : newAccountProperties.getDefaultTimezone());
                    account.setActive(false);
                    return account;
                });

        // if user is already part of the tenant, do nothing
        if (userAccount.getTenantIds().contains(request.tenantId())) {
            return getUserAccountUseCase.execute(userAccount.getUuid(), request.tenantId());
        }

        if (request.roles() != null) {
            request.roles().forEach(role -> userAccount.addRole(role, request.tenantId()));
        }
        if (request.permissions() != null) {
            request.permissions().forEach(permission -> userAccount.addPermission(permission, request.tenantId()));
        }

        userAccountRepository.save(userAccount);
        userAccountTenantRepository.save(new UserAccountTenant()
                .setUserAccountUuid(userAccount.getUuid())
                .setTenantUuid(request.tenantId()));

        if (!userAccount.isActive()) {
            // Generate and send activation token
            accountActivationService.createActivationToken(userAccount);
        }

        eventPublisher.publish(new UserAccountCreatedEvent(
                request.tenantId(), userAccount.getUuid(), userAccount.getEmail(), userAccount.getUsername()));

        return getUserAccountUseCase.execute(userAccount.getUuid(), request.tenantId());
    }
}
