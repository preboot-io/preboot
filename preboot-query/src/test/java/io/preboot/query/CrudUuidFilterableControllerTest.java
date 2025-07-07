package io.preboot.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.preboot.query.web.CrudUuidFilterableController;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CrudUuidFilterableControllerTest {

    @Mock
    private FilterableUuidRepository<TestEntity, Long> repository;

    private TestController controller;
    private TestEntity existingEntity;
    private UUID testUuid;

    @BeforeEach
    void setUp() {
        controller = new TestController(repository);
        testUuid = UUID.randomUUID();
        existingEntity = createTestEntity();
    }

    @Nested
    class CreateOperations {
        @Test
        void shouldCreateEntityWithGeneratedUuid() {
            // Arrange
            TestEntity newEntity = TestEntity.builder()
                    .name("New Entity")
                    .amount(BigDecimal.valueOf(100))
                    .build();

            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            TestEntity result = controller.create(newEntity);

            // Assert
            assertThat(result.getUuid()).isNotNull();
            assertThat(result.getName()).isEqualTo("New Entity");
        }

        @Test
        void shouldPreserveProvidedUuid() {
            // Arrange
            UUID providedUuid = UUID.randomUUID();
            TestEntity newEntity =
                    TestEntity.builder().uuid(providedUuid).name("New Entity").build();

            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            TestEntity result = controller.create(newEntity);

            // Assert
            assertThat(result.getUuid()).isEqualTo(providedUuid);
        }
    }

    @Nested
    class ReadOperations {
        @Test
        void shouldReturnEntityByUuid() {
            // Arrange
            when(repository.findByUuid(testUuid)).thenReturn(Optional.of(existingEntity));

            // Act
            ResponseEntity<TestEntity> response = controller.getByUuid(testUuid);

            // Assert
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUuid()).isEqualTo(testUuid);
        }

        @Test
        void shouldReturn404WhenEntityNotFound() {
            // Arrange
            when(repository.findByUuid(testUuid)).thenReturn(Optional.empty());

            // Act
            ResponseEntity<TestEntity> response = controller.getByUuid(testUuid);

            // Assert
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }

    @Nested
    class UpdateOperations {
        @Test
        void shouldUpdateExistingEntity() {
            // Arrange
            TestEntity updateEntity = TestEntity.builder()
                    .uuid(testUuid)
                    .name("Updated Name")
                    .amount(BigDecimal.valueOf(200))
                    .build();

            when(repository.existsByUuid(testUuid)).thenReturn(true);
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ResponseEntity<TestEntity> response = controller.update(testUuid, updateEntity);

            // Assert
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getName()).isEqualTo("Updated Name");
            assertThat(response.getBody().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        }

        @Test
        void shouldReturn404WhenUpdatingNonExistentEntity() {
            // Arrange
            when(repository.existsByUuid(testUuid)).thenReturn(false);

            // Act
            ResponseEntity<TestEntity> response = controller.update(testUuid, new TestEntity());

            // Assert
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }

        @Test
        void shouldEnforceUuidMatchBetweenPathAndEntity() {
            // Arrange
            UUID wrongUuid = UUID.randomUUID();
            TestEntity entityWithWrongUuid = TestEntity.builder()
                    .uuid(wrongUuid)  // Different UUID than path parameter
                    .name("Updated Name")
                    .amount(BigDecimal.valueOf(200))
                    .build();

            when(repository.existsByUuid(testUuid)).thenReturn(true);
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ResponseEntity<TestEntity> response = controller.update(testUuid, entityWithWrongUuid);

            // Assert
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            TestEntity result = response.getBody();
            assertThat(result).isNotNull();
            assertThat(result.getUuid()).isEqualTo(testUuid); // Should match path parameter, not entity's original UUID
            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));

            // Verify the entity saved has the correct UUID
            verify(repository).save(any(TestEntity.class));
        }
    }

    @Nested
    class PatchOperations {
        @Test
        void shouldPartiallyUpdateSimpleFields() {
            // Arrange
            TestEntity partialUpdate = TestEntity.builder()
                    .name("Updated Name")
                    .amount(BigDecimal.valueOf(200))
                    .build();

            when(repository.findByUuid(testUuid)).thenReturn(Optional.of(existingEntity));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ResponseEntity<TestEntity> response = controller.patch(testUuid, partialUpdate);

            // Assert
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            TestEntity result = response.getBody();
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(result.getStatus()).isEqualTo("ACTIVE"); // Preserved
            assertThat(result.getCreatedAt()).isEqualTo(existingEntity.getCreatedAt()); // Preserved
        }

        @Test
        void shouldUpdateNestedObject() {
            // Arrange
            TestEntity.Address newAddress =
                    TestEntity.Address.builder().street("New Street").build();

            TestEntity partialUpdate = TestEntity.builder().address(newAddress).build();

            when(repository.findByUuid(testUuid)).thenReturn(Optional.of(existingEntity));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ResponseEntity<TestEntity> response = controller.patch(testUuid, partialUpdate);

            // Assert
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            TestEntity result = response.getBody();
            assertThat(result).isNotNull();
            assertThat(result.getAddress().getStreet()).isEqualTo("New Street");
            assertThat(result.getAddress().getCity()).isEqualTo("Original City"); // Preserved
            assertThat(result.getAddress().getCountry()).isEqualTo("Original Country"); // Preserved
        }

        @Test
        void shouldReturn404WhenPatchingNonExistentEntity() {
            // Arrange
            when(repository.findByUuid(testUuid)).thenReturn(Optional.empty());

            // Act
            ResponseEntity<TestEntity> response = controller.patch(testUuid, new TestEntity());

            // Assert
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }

    @Nested
    class DeleteOperations {
        @Test
        void shouldDeleteExistingEntity() {
            // Arrange
            when(repository.existsByUuid(testUuid)).thenReturn(true);

            // Act
            ResponseEntity<Void> response = controller.delete(testUuid);

            // Assert
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }

        @Test
        void shouldReturn404WhenDeletingNonExistentEntity() {
            // Arrange
            when(repository.existsByUuid(testUuid)).thenReturn(false);

            // Act
            ResponseEntity<Void> response = controller.delete(testUuid);

            // Assert
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }

    private TestEntity createTestEntity() {
        return TestEntity.builder()
                .id(1L)
                .uuid(testUuid)
                .name("Original Name")
                .amount(BigDecimal.valueOf(100))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .address(TestEntity.Address.builder()
                        .street("Original Street")
                        .city("Original City")
                        .country("Original Country")
                        .build())
                .tags(List.of("tag1", "tag2"))
                .items(List.of(
                        TestEntity.OrderItem.builder().productId(1L).quantity(2).build(),
                        TestEntity.OrderItem.builder().productId(2L).quantity(3).build()))
                .build();
    }

    static class TestController extends CrudUuidFilterableController<TestEntity, Long> {
        TestController(FilterableUuidRepository<TestEntity, Long> repository) {
            super(repository);
        }

        @Override
        protected TestEntity merge(TestEntity existingEntity, TestEntity partialEntity) {
            // Simple merge implementation for testing
            if (partialEntity.getName() != null) {
                existingEntity.setName(partialEntity.getName());
            }
            if (partialEntity.getAmount() != null) {
                existingEntity.setAmount(partialEntity.getAmount());
            }
            if (partialEntity.getAddress() != null) {
                if (partialEntity.getAddress().getStreet() != null) {
                    existingEntity
                            .getAddress()
                            .setStreet(partialEntity.getAddress().getStreet());
                }
            }
            return existingEntity;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class TestEntity implements HasUuid {
        private Long id;
        private UUID uuid;
        private String name;
        private BigDecimal amount;
        private String status;
        private LocalDateTime createdAt;
        private Address address;
        private List<String> tags;
        private List<OrderItem> items;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        static class Address {
            private String street;
            private String city;
            private String country;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        static class OrderItem {
            private Long productId;
            private Integer quantity;
        }
    }
}
