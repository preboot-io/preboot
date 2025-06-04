package io.preboot.auth.core.model;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_accounts")
@Data
@Accessors(chain = true)
public class UserAccount {
    @Id
    private Long id;

    private UUID uuid;
    private String email;
    private String username;
    private String language;
    private String timezone;
    private boolean active;
    private int resetTokenVersion;

    private Set<UserAccountCredential> credentials;
    private Set<UserAccountRole> roles;
    private Set<UserAccountPermission> permissions;
    private Set<UserAccountDevice> devices;

    @Version
    private Long version;

    public static UserAccount create(
            String username, String email, String encodedPassword, String language, String timezone) {
        UserAccount userAccount = new UserAccount();
        userAccount.setUuid(UUID.randomUUID());
        userAccount.setUsername(username);
        userAccount.setEmail(email);
        userAccount.setLanguage(language);
        userAccount.setTimezone(timezone);
        userAccount.setResetTokenVersion(0);

        UserAccountCredential credential = new UserAccountCredential().setType("PASSWORD");
        credential.setAttribute(encodedPassword);
        userAccount.getCredentials().add(credential);
        return userAccount;
    }

    public Set<UserAccountRole> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }

    public Set<UserAccountPermission> getPermissions() {
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        return permissions;
    }

    public void addRole(String roleName, UUID tenantId) {
        if (roleName == null) {
            return;
        }

        if (getRoles().stream()
                .noneMatch(r -> Objects.equals(r.getName(), roleName) && Objects.equals(r.getTenantId(), tenantId))) {
            getRoles().add(new UserAccountRole().setName(roleName).setTenantId(tenantId));
        }
    }

    public void removeRole(String roleName, UUID tenantId) {
        getRoles().removeIf(r -> Objects.equals(r.getName(), roleName) && Objects.equals(r.getTenantId(), tenantId));
    }

    public Set<UserAccountRole> getTenantRoles(UUID tenantId) {
        return getRoles().stream()
                .filter(r -> Objects.equals(r.getTenantId(), tenantId))
                .collect(toSet());
    }

    public void addPermission(String permissionName, UUID tenantId) {
        if (permissionName == null) {
            return;
        }

        if (getPermissions().stream()
                .noneMatch(r ->
                        Objects.equals(r.getName(), permissionName) && Objects.equals(r.getTenantId(), tenantId))) {
            getPermissions()
                    .add(new UserAccountPermission().setName(permissionName).setTenantId(tenantId));
        }
    }

    public void removePermission(String permissionName, UUID tenantId) {
        getPermissions()
                .removeIf(
                        r -> Objects.equals(r.getName(), permissionName) && Objects.equals(r.getTenantId(), tenantId));
    }

    public Set<UserAccountPermission> getTenantPermissions(UUID tenantId) {
        return getPermissions().stream()
                .filter(r -> Objects.equals(r.getTenantId(), tenantId))
                .collect(toSet());
    }

    public Set<UUID> getTenantIds() {
        return getRoles().stream().map(UserAccountRole::getTenantId).collect(toSet());
    }

    public void updatePassword(String currentPasswordEncoded, final String newPasswordEncoded) {
        final List<UserAccountCredential> password = getCredentials().stream()
                .filter(c -> c.getType().equals("PASSWORD"))
                .toList();

        if (password.isEmpty()) {
            UserAccountCredential credential = new UserAccountCredential().setType("PASSWORD");
            credential.setAttribute(newPasswordEncoded);
            getCredentials().add(credential);
            return;
        }

        password.stream()
                .filter(c -> c.getAttribute().equals(currentPasswordEncoded))
                .forEach(c -> c.setAttribute(newPasswordEncoded));
    }

    public Set<UserAccountCredential> getCredentials() {
        if (credentials == null) {
            credentials = new HashSet<>();
        }
        return credentials;
    }

    public Optional<String> getEncodedPassword() {
        return getCredentials().stream()
                .filter(c -> c.getType().equals("PASSWORD"))
                .map(UserAccountCredential::getAttribute)
                .findAny();
    }

    public void incrementResetTokenVersion() {
        this.resetTokenVersion++;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final UserAccount userAccount = (UserAccount) o;
        return Objects.equals(id, userAccount.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public void setEncodedPassword(final String newPasswordEncoded) {
        final List<UserAccountCredential> password = getCredentials().stream()
                .filter(c -> c.getType().equals("PASSWORD"))
                .toList();

        if (password.isEmpty()) {
            UserAccountCredential credential = new UserAccountCredential().setType("PASSWORD");
            credential.setAttribute(newPasswordEncoded);
            getCredentials().add(credential);
            return;
        }

        password.forEach(c -> c.setAttribute(newPasswordEncoded));
    }

    public void clearTenantRolesAndPermissions(final UUID tenantId) {
        getRoles().removeIf(r -> Objects.equals(r.getTenantId(), tenantId));
        getPermissions().removeIf(p -> Objects.equals(p.getTenantId(), tenantId));
    }

    public boolean isTechnicalAdmin() {
        return getRoles().stream().anyMatch(r -> Objects.equals(r.getName(), "super-admin"));
    }
}
