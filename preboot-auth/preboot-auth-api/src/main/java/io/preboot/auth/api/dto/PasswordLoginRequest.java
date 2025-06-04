package io.preboot.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordLoginRequest(@NotBlank @Email String email, @NotBlank String password, boolean rememberMe) {}
