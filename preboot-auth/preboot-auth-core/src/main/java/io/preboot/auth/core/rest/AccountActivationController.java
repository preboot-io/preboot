package io.preboot.auth.core.rest;

import io.preboot.auth.api.dto.ActivateAccountRequest;
import io.preboot.auth.api.exception.InvalidActivationTokenException;
import io.preboot.auth.api.exception.PasswordResetTokenExpiredException;
import io.preboot.auth.core.spring.AccountActivationService;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AccountActivationController {
    private final AccountActivationService accountActivationService;

    @ExceptionHandler(InvalidActivationTokenException.class)
    public ResponseEntity<String> handleInvalidActivationTokenException(InvalidActivationTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    @ExceptionHandler(PasswordResetTokenExpiredException.class)
    public ResponseEntity<String> handlePasswordResetTokenExpiredException(PasswordResetTokenExpiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    @PostMapping("/activation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateAccount(@RequestBody @Valid ActivateAccountRequest request) {
        accountActivationService.validateAndActivateAccount(request.token(), request.password());
    }
}
