package io.preboot.auth.test.eventhandler;

import io.preboot.auth.api.event.UserAccountPasswordResetTokenGeneratedEvent;
import io.preboot.eventbus.EventHandler;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PasswordResetEventHandler {
    private String capturedToken;

    @EventHandler
    public void handlePasswordResetTokenGenerated(UserAccountPasswordResetTokenGeneratedEvent event) {
        this.capturedToken = event.token();
    }

    public void reset() {
        capturedToken = null;
    }
}
