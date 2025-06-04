package io.preboot.auth.test.eventhandler;

import io.preboot.auth.api.event.UserAccountActivationTokenGeneratedEvent;
import io.preboot.eventbus.EventHandler;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AccountActivationEventHandler {
    private String capturedToken;

    @EventHandler
    public void handleActivationTokenGenerated(UserAccountActivationTokenGeneratedEvent event) {
        this.capturedToken = event.token();
    }

    public void reset() {
        capturedToken = null;
    }
}
