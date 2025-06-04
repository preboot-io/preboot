package io.preboot.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivateAccountRequest(@NotBlank String token, @NotBlank String password) {}
