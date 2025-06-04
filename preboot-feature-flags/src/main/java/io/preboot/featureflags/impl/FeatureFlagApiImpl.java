package io.preboot.featureflags.impl;

import io.preboot.featureflags.FeatureFlagApi;
import java.util.HashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FeatureFlagApiImpl implements FeatureFlagApi {

    private final FeatureFlagRepository featureFlagRepository;
    private final TenantFeatureFlagRepository tenantFeatureFlagRepository;

    @Override
    public void setFlag(UUID tenantId, String flagName, boolean enabled) {
        TenantFeatureFlag flag = tenantFeatureFlagRepository
                .findByName(flagName)
                .orElseGet(() -> {
                    TenantFeatureFlag newFlag = new TenantFeatureFlag();
                    newFlag.setName(flagName);
                    newFlag.setTenantBindings(new HashSet<>());
                    return newFlag;
                });
        if (enabled) {
            flag.getTenantBindings().add(new TenantFeatureFlagBinding(tenantId));
        } else {
            flag.getTenantBindings().removeIf(b -> b.getTenantId().equals(tenantId));
        }
        tenantFeatureFlagRepository.save(flag);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled(UUID tenantId, String flagName) {
        return tenantFeatureFlagRepository
                .findByName(flagName)
                .map(flag -> {
                    if (flag.getTenantBindings().stream()
                            .anyMatch(b -> b.getTenantId().equals(tenantId))) {
                        return flag.isActive();
                    } else {
                        return false;
                    }
                })
                .orElse(false);
    }

    @Override
    public void setGlobalFlag(String flagName, boolean enabled) {
        FeatureFlag flag = featureFlagRepository.findByName(flagName).orElseGet(() -> {
            FeatureFlag newFlag = new FeatureFlag();
            newFlag.setName(flagName);
            return newFlag;
        });
        flag.setActive(enabled);
        featureFlagRepository.save(flag);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isGlobalFlagEnabled(String flagName) {
        return featureFlagRepository
                .findByName(flagName)
                .map(FeatureFlag::isActive)
                .orElse(false);
    }
}
