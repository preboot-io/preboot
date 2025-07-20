package io.preboot.auth.api;

import io.preboot.auth.api.dto.*;
import io.preboot.query.SearchParams;
import java.util.UUID;
import org.springframework.data.domain.Page;

public interface UserAccountManagementApi {
    UserAccountInfo getUserAccount(UUID userAccountId, final UUID tenantId);

    UserAccountInfo createUserAccountForTenant(CreateInactiveUserAccountRequest request);

    UserAccountInfo createTenantAndUserAccount(CreateTenantAndInactiveUserAccountRequest request);

    void resendActivationLink(ResentActivationLinkCommand command);

    void updatePassword(UpdatePasswordCommand command);

    UserAccountInfo addRole(UUID userId, UUID tenantId, String roleName);

    UserAccountInfo removeRole(UUID userId, UUID tenantId, String roleName);

    Page<UserAccountInfo> getUserAccountsInfo(SearchParams queryParams, final UUID tenantId);

    Page<UserAccountInfo> getAllUserAccountsInfo(SearchParams params);

    void removeUser(UUID userId, final UUID tenantId);
}
