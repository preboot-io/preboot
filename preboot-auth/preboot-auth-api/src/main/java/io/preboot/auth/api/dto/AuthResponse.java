package io.preboot.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthResponse(@NotBlank String token) {}
