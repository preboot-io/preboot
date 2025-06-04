package io.preboot.auth.core.rest;

import io.jsonwebtoken.JwtException;
import io.preboot.auth.api.dto.RequestPasswordResetRequest;
import io.preboot.auth.api.dto.ResetPasswordRequest;
import io.preboot.auth.api.exception.InvalidPasswordResetTokenException;
import io.preboot.auth.api.exception.PasswordResetTokenExpiredException;
import io.preboot.auth.api.exception.UserAccountNotFoundException;
import io.preboot.auth.core.usecase.RequestPasswordResetUseCase;
import io.preboot.auth.core.usecase.ResetPasswordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @ExceptionHandler(UserAccountNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserAccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler({
        InvalidPasswordResetTokenException.class,
        PasswordResetTokenExpiredException.class,
        JwtException.class
    })
    public ResponseEntity<String> handleTokenException(Exception e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
    }

    @PostMapping("/reset-request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestReset(@RequestBody @Valid RequestPasswordResetRequest request) {
        requestPasswordResetUseCase.execute(request);
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);
    }
}
