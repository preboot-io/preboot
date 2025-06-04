package io.preboot.securedata.repository;

import io.preboot.eventbus.EventPublisher;
import io.preboot.query.FilterableFragmentContext;
import io.preboot.securedata.context.SecurityContextProvider;
import io.preboot.securedata.metadata.SecureEntityMetadataCache;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
public class SecureRepositoryContext {
    private final FilterableFragmentContext filterableContext;
    private final SecurityContextProvider securityContextProvider;
    private final SecureEntityMetadataCache metadataCache;
    private final EventPublisher eventPublisher;
}
