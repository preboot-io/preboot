package io.preboot.featureflags.impl;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Data
class FeatureFlag {
    @Id
    private Long id;

    private String name;
    private boolean active;

    @Version
    private Long version;
}
