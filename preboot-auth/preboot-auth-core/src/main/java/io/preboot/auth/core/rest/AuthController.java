package io.preboot.auth.core.rest;

import io.preboot.auth.api.AuthApi;
import io.preboot.auth.api.dto.AuthResponse;
import io.preboot.auth.api.dto.PasswordLoginRequest;
import io.preboot.auth.api.dto.TenantInfo;
import io.preboot.auth.api.dto.UseTenantRequest;
import io.preboot.auth.api.dto.UserAccountInfo;
import io.preboot.auth.api.exception.PasswordInvalidException;
import io.preboot.auth.api.exception.SessionExpiredException;
import io.preboot.auth.api.exception.SessionFingerprintException;
import io.preboot.auth.api.exception.SessionNotFoundException;
import io.preboot.auth.api.exception.TenantAccessDeniedException;
import io.preboot.auth.api.exception.UserAccountNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthApi authApi;

    @ExceptionHandler(PasswordInvalidException.class)
    public ResponseEntity<String> handlePasswordInvalidException(PasswordInvalidException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @ExceptionHandler(UserAccountNotFoundException.class)
    public ResponseEntity<String> handleUserAccountNotFoundException(UserAccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @ExceptionHandler(SessionFingerprintException.class)
    public ResponseEntity<String> handleSessionFingerprintException(SessionFingerprintException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Device fingerprint does not match");
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<String> handleSessionExpiredException(SessionExpiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session expired");
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<String> handleSessionNotFoundException(SessionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session not found");
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<String> handleTenantAccessDeniedException(TenantAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid PasswordLoginRequest request, HttpServletRequest servletRequest) {
        return authApi.login(request, servletRequest);
    }

    @GetMapping("/me")
    public UserAccountInfo getCurrentUserAccount(HttpServletRequest servletRequest) {
        return authApi.getCurrentUserAccount(servletRequest);
    }

    @GetMapping("/my-tenants")
    public List<TenantInfo> getCurrentUserTenants(HttpServletRequest servletRequest) {
        return authApi.getCurrentUserTenants(servletRequest);
    }

    @PostMapping("/use-tenant")
    public AuthResponse setCurrentUserTenant(
            @RequestBody @Valid UseTenantRequest useTenantRequest, HttpServletRequest servletRequest) {
        return authApi.setCurrentUserTenant(useTenantRequest.tenantId(), servletRequest);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest servletRequest) {
        return authApi.refreshSession(servletRequest);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest servletRequest) {
        authApi.logout(servletRequest);
    }
}
