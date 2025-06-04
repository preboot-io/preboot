package io.preboot.securedata;

import io.preboot.eventbus.EventHandler;
import io.preboot.securedata.event.SecureRepositoryEvent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NoteEventCollector {
    private final List<SecureRepositoryEvent<?>> events = new ArrayList<>();

    @EventHandler(typeParameter = TestNote.class)
    public void handleBeforeCreate(SecureRepositoryEvent.BeforeCreateEvent<TestNote> event) {
        events.add(event);
    }

    @EventHandler(typeParameter = TestNote.class)
    public void handleAfterCreate(SecureRepositoryEvent.AfterCreateEvent<TestNote> event) {
        events.add(event);
    }

    @EventHandler(typeParameter = TestNote.class)
    public void handleBeforeUpdate(SecureRepositoryEvent.BeforeUpdateEvent<TestNote> event) {
        events.add(event);
    }

    @EventHandler(typeParameter = TestNote.class)
    public void handleAfterUpdate(SecureRepositoryEvent.AfterUpdateEvent<TestNote> event) {
        events.add(event);
    }

    @EventHandler(typeParameter = TestNote.class)
    public void handleBeforeDelete(SecureRepositoryEvent.BeforeDeleteEvent<TestNote> event) {
        events.add(event);
    }

    @EventHandler(typeParameter = TestNote.class)
    public void handleAfterDelete(SecureRepositoryEvent.AfterDeleteEvent<TestNote> event) {
        events.add(event);
    }

    public List<SecureRepositoryEvent<?>> getEvents() {
        return new ArrayList<>(events);
    }

    public void clear() {
        events.clear();
    }
}
