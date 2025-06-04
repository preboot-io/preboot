package io.preboot.securedata;

import io.preboot.eventbus.EventHandler;
import io.preboot.securedata.event.SecureRepositoryEvent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentEventCollector {
    private final List<SecureRepositoryEvent<?>> events = new ArrayList<>();

    @EventHandler(typeParameter = TestDocument.class)
    public void handleBeforeCreate(SecureRepositoryEvent.BeforeCreateEvent<TestDocument> event) {
        events.add(event);
    }

    @EventHandler(typeParameter = TestDocument.class)
    public void handleAfterCreate(SecureRepositoryEvent.AfterCreateEvent<TestDocument> event) {
        events.add(event);
    }

    @EventHandler(typeParameter = TestDocument.class)
    public void handleBeforeUpdate(SecureRepositoryEvent.BeforeUpdateEvent<TestDocument> event) {
        events.add(event);
    }

    @EventHandler(typeParameter = TestDocument.class)
    public void handleAfterUpdate(SecureRepositoryEvent.AfterUpdateEvent<TestDocument> event) {
        events.add(event);
    }

    @EventHandler(typeParameter = TestDocument.class)
    public void handleBeforeDelete(SecureRepositoryEvent.BeforeDeleteEvent<TestDocument> event) {
        events.add(event);
    }

    @EventHandler(typeParameter = TestDocument.class)
    public void handleAfterDelete(SecureRepositoryEvent.AfterDeleteEvent<TestDocument> event) {
        events.add(event);
    }

    public List<SecureRepositoryEvent<?>> getEvents() {
        return new ArrayList<>(events);
    }

    public void clear() {
        events.clear();
    }
}
