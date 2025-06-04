package io.preboot.files.inmemory;

import io.preboot.eventbus.EventHandler;
import io.preboot.files.events.FileDeletedEvent;
import io.preboot.files.events.FileStoredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileEventHandler {
    private static final Logger log = LoggerFactory.getLogger(FileEventHandler.class);

    @EventHandler
    public void handleFileStored(FileStoredEvent event) {
        log.info(
                "File stored: {} by user: {} in tenant: {}",
                event.metadata().fileName(),
                event.metadata().authorId(),
                event.metadata().tenantId());
    }

    @EventHandler
    public void handleFileDeleted(FileDeletedEvent event) {
        log.info("File deleted: {} by user: {} in tenant: {}", event.fileId(), event.authorId(), event.tenantId());
    }
}
