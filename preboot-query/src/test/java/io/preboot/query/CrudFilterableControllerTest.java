package io.preboot.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.preboot.query.web.CrudFilterableController;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
class CrudFilterableControllerTest {

    @Mock
    private FilterableRepository<TestEntity, Long> repository;

    private TestController controller;
    private TestEntity existingEntity;

    @BeforeEach
    void setUp() {
        controller = new TestController(repository);
        existingEntity = createTestEntity();
    }

    @Nested
    class PatchTests {

        @Test
        void shouldPartiallyUpdateSimpleFields() {
            // Arrange
            TestEntity partialUpdate = TestEntity.builder()
                    .name("Updated Name")
                    .amount(BigDecimal.valueOf(200))
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(existingEntity));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ResponseEntity<TestEntity> response = controller.patch(1L, partialUpdate);

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

            when(repository.findById(1L)).thenReturn(Optional.of(existingEntity));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ResponseEntity<TestEntity> response = controller.patch(1L, partialUpdate);

            // Assert
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            TestEntity result = response.getBody();
            assertThat(result).isNotNull();
            assertThat(result.getAddress().getStreet()).isEqualTo("New Street");
            assertThat(result.getAddress().getCity()).isEqualTo("Original City"); // Preserved
            assertThat(result.getAddress().getCountry()).isEqualTo("Original Country"); // Preserved
        }

        @Test
        void shouldUpdateArrays() {
            // Arrange
            TestEntity partialUpdate =
                    TestEntity.builder().tags(List.of("new-tag-1", "new-tag-2")).build();

            when(repository.findById(1L)).thenReturn(Optional.of(existingEntity));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ResponseEntity<TestEntity> response = controller.patch(1L, partialUpdate);

            // Assert
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            TestEntity result = response.getBody();
            assertThat(result).isNotNull();
            assertThat(result.getTags())
                    .containsExactly("new-tag-1", "new-tag-2")
                    .doesNotContain("tag1", "tag2");
        }

        @Test
        void shouldUpdateNestedCollections() {
            // Arrange
            TestEntity.OrderItem newItem =
                    TestEntity.OrderItem.builder().productId(3L).quantity(5).build();

            TestEntity partialUpdate =
                    TestEntity.builder().items(List.of(newItem)).build();

            when(repository.findById(1L)).thenReturn(Optional.of(existingEntity));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ResponseEntity<TestEntity> response = controller.patch(1L, partialUpdate);

            // Assert
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            TestEntity result = response.getBody();
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductId()).isEqualTo(3L);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
        }

        @Test
        void shouldIgnoreNullValues() {
            // Arrange
            TestEntity partialUpdate = TestEntity.builder()
                    .name(null)
                    .amount(BigDecimal.valueOf(200))
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(existingEntity));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ResponseEntity<TestEntity> response = controller.patch(1L, partialUpdate);

            // Assert
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            TestEntity result = response.getBody();
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Original Name"); // Preserved despite null in update
            assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        }

        @Test
        void shouldReturn404WhenEntityNotFound() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.empty());

            // Act
            ResponseEntity<TestEntity> response = controller.patch(1L, new TestEntity());

            // Assert
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }

    private TestEntity createTestEntity() {
        return TestEntity.builder()
                .id(1L)
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

    static class TestController extends CrudFilterableController<TestEntity, Long> {
        TestController(FilterableRepository<TestEntity, Long> repository) {
            super(repository);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class TestEntity {
        private Long id;
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
