package io.preboot.featureflags.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.preboot.featureflags.FeatureFlagApi;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureFlagApiImplTest {

    private FeatureFlagApi featureFlagApi;

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @Mock
    private TenantFeatureFlagRepository tenantFeatureFlagRepository;

    @BeforeEach
    void setUp() {
        featureFlagApi = new FeatureFlagApiImpl(featureFlagRepository, tenantFeatureFlagRepository);
    }

    @Test
    void setFlag_WhenFlagDoesNotExist_ShouldCreateNewFlag() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String flagName = "test-flag";
        boolean enabled = true;

        when(tenantFeatureFlagRepository.findByName(flagName)).thenReturn(Optional.empty());

        // Act
        featureFlagApi.setFlag(tenantId, flagName, enabled);

        // Assert
        ArgumentCaptor<TenantFeatureFlag> flagCaptor = ArgumentCaptor.forClass(TenantFeatureFlag.class);
        verify(tenantFeatureFlagRepository).save(flagCaptor.capture());

        TenantFeatureFlag savedFlag = flagCaptor.getValue();
        assertThat(savedFlag.getName()).isEqualTo(flagName);
        assertThat(savedFlag.getTenantBindings()).hasSize(1);
        assertThat(savedFlag.getTenantBindings().iterator().next().getTenantId())
                .isEqualTo(tenantId);
    }

    @Test
    void setFlag_WhenFlagExistsAndEnabledIsTrue_ShouldAddTenantBinding() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String flagName = "test-flag";
        boolean enabled = true;

        TenantFeatureFlag existingFlag = new TenantFeatureFlag();
        existingFlag.setName(flagName);
        existingFlag.setTenantBindings(new HashSet<>());

        when(tenantFeatureFlagRepository.findByName(flagName)).thenReturn(Optional.of(existingFlag));

        // Act
        featureFlagApi.setFlag(tenantId, flagName, enabled);

        // Assert
        ArgumentCaptor<TenantFeatureFlag> flagCaptor = ArgumentCaptor.forClass(TenantFeatureFlag.class);
        verify(tenantFeatureFlagRepository).save(flagCaptor.capture());

        TenantFeatureFlag savedFlag = flagCaptor.getValue();
        assertThat(savedFlag.getName()).isEqualTo(flagName);
        assertThat(savedFlag.getTenantBindings()).hasSize(1);
        assertThat(savedFlag.getTenantBindings().iterator().next().getTenantId())
                .isEqualTo(tenantId);
    }

    @Test
    void setFlag_WhenFlagExistsAndEnabledIsFalse_ShouldRemoveTenantBinding() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String flagName = "test-flag";
        boolean enabled = false;

        TenantFeatureFlag existingFlag = new TenantFeatureFlag();
        existingFlag.setName(flagName);
        Set<TenantFeatureFlagBinding> bindings = new HashSet<>();
        bindings.add(new TenantFeatureFlagBinding(tenantId));
        existingFlag.setTenantBindings(bindings);

        when(tenantFeatureFlagRepository.findByName(flagName)).thenReturn(Optional.of(existingFlag));

        // Act
        featureFlagApi.setFlag(tenantId, flagName, enabled);

        // Assert
        ArgumentCaptor<TenantFeatureFlag> flagCaptor = ArgumentCaptor.forClass(TenantFeatureFlag.class);
        verify(tenantFeatureFlagRepository).save(flagCaptor.capture());

        TenantFeatureFlag savedFlag = flagCaptor.getValue();
        assertThat(savedFlag.getName()).isEqualTo(flagName);
        assertThat(savedFlag.getTenantBindings()).isEmpty();
    }

    @Test
    void isEnabled_WhenFlagExistsAndTenantHasBinding_ShouldReturnFlagActiveStatus() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String flagName = "test-flag";

        TenantFeatureFlag flag = new TenantFeatureFlag();
        flag.setName(flagName);
        flag.setActive(true);
        Set<TenantFeatureFlagBinding> bindings = new HashSet<>();
        bindings.add(new TenantFeatureFlagBinding(tenantId));
        flag.setTenantBindings(bindings);

        when(tenantFeatureFlagRepository.findByName(flagName)).thenReturn(Optional.of(flag));

        // Act
        boolean result = featureFlagApi.isEnabled(tenantId, flagName);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isEnabled_WhenFlagExistsButTenantHasNoBinding_ShouldReturnFalse() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String flagName = "test-flag";

        TenantFeatureFlag flag = new TenantFeatureFlag();
        flag.setName(flagName);
        flag.setActive(true);
        flag.setTenantBindings(new HashSet<>());

        when(tenantFeatureFlagRepository.findByName(flagName)).thenReturn(Optional.of(flag));

        // Act
        boolean result = featureFlagApi.isEnabled(tenantId, flagName);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isEnabled_WhenFlagDoesNotExist_ShouldReturnFalse() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        String flagName = "test-flag";

        when(tenantFeatureFlagRepository.findByName(flagName)).thenReturn(Optional.empty());

        // Act
        boolean result = featureFlagApi.isEnabled(tenantId, flagName);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void setGlobalFlag_WhenFlagDoesNotExist_ShouldCreateNewFlag() {
        // Arrange
        String flagName = "test-global-flag";
        boolean enabled = true;

        when(featureFlagRepository.findByName(flagName)).thenReturn(Optional.empty());

        // Act
        featureFlagApi.setGlobalFlag(flagName, enabled);

        // Assert
        ArgumentCaptor<FeatureFlag> flagCaptor = ArgumentCaptor.forClass(FeatureFlag.class);
        verify(featureFlagRepository).save(flagCaptor.capture());

        FeatureFlag savedFlag = flagCaptor.getValue();
        assertThat(savedFlag.getName()).isEqualTo(flagName);
        assertThat(savedFlag.isActive()).isEqualTo(enabled);
    }

    @Test
    void setGlobalFlag_WhenFlagExists_ShouldUpdateFlag() {
        // Arrange
        String flagName = "test-global-flag";
        boolean enabled = true;

        FeatureFlag existingFlag = new FeatureFlag();
        existingFlag.setName(flagName);
        existingFlag.setActive(false);

        when(featureFlagRepository.findByName(flagName)).thenReturn(Optional.of(existingFlag));

        // Act
        featureFlagApi.setGlobalFlag(flagName, enabled);

        // Assert
        ArgumentCaptor<FeatureFlag> flagCaptor = ArgumentCaptor.forClass(FeatureFlag.class);
        verify(featureFlagRepository).save(flagCaptor.capture());

        FeatureFlag savedFlag = flagCaptor.getValue();
        assertThat(savedFlag.getName()).isEqualTo(flagName);
        assertThat(savedFlag.isActive()).isEqualTo(enabled);
    }

    @Test
    void isGlobalFlagEnabled_WhenFlagExistsAndActive_ShouldReturnTrue() {
        // Arrange
        String flagName = "test-global-flag";

        FeatureFlag flag = new FeatureFlag();
        flag.setName(flagName);
        flag.setActive(true);

        when(featureFlagRepository.findByName(flagName)).thenReturn(Optional.of(flag));

        // Act
        boolean result = featureFlagApi.isGlobalFlagEnabled(flagName);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isGlobalFlagEnabled_WhenFlagExistsButNotActive_ShouldReturnFalse() {
        // Arrange
        String flagName = "test-global-flag";

        FeatureFlag flag = new FeatureFlag();
        flag.setName(flagName);
        flag.setActive(false);

        when(featureFlagRepository.findByName(flagName)).thenReturn(Optional.of(flag));

        // Act
        boolean result = featureFlagApi.isGlobalFlagEnabled(flagName);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isGlobalFlagEnabled_WhenFlagDoesNotExist_ShouldReturnFalse() {
        // Arrange
        String flagName = "test-global-flag";

        when(featureFlagRepository.findByName(flagName)).thenReturn(Optional.empty());

        // Act
        boolean result = featureFlagApi.isGlobalFlagEnabled(flagName);

        // Assert
        assertThat(result).isFalse();
    }
}
