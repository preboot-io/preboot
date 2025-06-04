package io.preboot.auth.api.event;

public record UserAccountRemovedEvent(String email, String username) {}
