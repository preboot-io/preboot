package io.preboot.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.preboot.query.config.TestContainersConfig;
import io.preboot.query.testdata.TestOrder;
import io.preboot.query.testdata.TestOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestContainersConfig.class)
@Transactional
@Sql("/test-data.sql")
class NestedFilterableRepositoryIntegrationTest {

    @Autowired
    private TestOrderRepository orderRepository;

    @Test
    void findAll_WithOrderItemProductCode_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.eq("orderItems.productCode", "PROD-A"))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .hasSize(2) // Orders containing PROD-A
                .allMatch(order ->
                        order.getOrderItems().stream().anyMatch(item -> "PROD-A".equals(item.getProductCode())));
    }

    @Test
    void findAll_WithOrderItemQuantityGreaterThan_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.gt("orderItems.quantity", 3))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .hasSize(2) // Orders with items quantity > 3
                .allMatch(order -> order.getOrderItems().stream().anyMatch(item -> item.getQuantity() > 3));
    }

    @Test
    void findAll_WithCombinedOrderAndItemFilters_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.eq("status", "COMPLETED"),
                        FilterCriteria.gt("orderItems.unitPrice", new BigDecimal("75")))
                .sortField("amount")
                .sortDirection(Sort.Direction.DESC)
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .hasSize(1) // Only completed orders with high-priced items
                .allMatch(order -> "COMPLETED".equals(order.getStatus())
                        && order.getOrderItems().stream()
                                .anyMatch(item -> item.getUnitPrice().compareTo(new BigDecimal("75")) > 0));
    }

    @Test
    void findAll_WithMultipleItemCriteria_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.gt("orderItems.quantity", 2),
                        FilterCriteria.gt("orderItems.totalPrice", new BigDecimal("200")))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).allMatch(order -> order.getOrderItems().stream()
                .anyMatch(item -> item.getQuantity() > 2 && item.getTotalPrice().compareTo(new BigDecimal("200")) > 0));
    }

    @Test
    void findAll_WithOrderItemsAndPagination_ShouldReturnCorrectPage() {
        // Arrange
        SearchParams params = SearchParams.builder()
                .filters(List.of(
                        FilterCriteria.like("orderItems.productCode", "PROD-%"),
                        FilterCriteria.gt("orderItems.quantity", 1)))
                .page(0)
                .size(2)
                .sortField("amount")
                .sortDirection(Sort.Direction.DESC)
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2).allMatch(order -> order.getOrderItems().stream()
                .anyMatch(item -> item.getProductCode().startsWith("PROD-") && item.getQuantity() > 1));
        assertThat(result.getContent()).extracting(TestOrder::getAmount).isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    void findOne_WithOrderItemCriteria_ShouldReturnMatchingOrder() {
        // Arrange
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.eq("orderItems.productCode", "PROD-A"),
                        FilterCriteria.eq("orderItems.quantity", 3))
                .build();

        // Act
        var result = orderRepository.findOne(params);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getOrderItems())
                .anyMatch(item -> "PROD-A".equals(item.getProductCode()) && item.getQuantity() == 3);
    }

    @Test
    void count_WithOrderItemsCriteria_ShouldReturnCorrectCount() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.gt("orderItems.totalPrice", new BigDecimal("300")))
                .build();

        // Act
        long count = orderRepository.count(params);

        // Assert
        assertThat(count).isEqualTo(2); // Orders with items total price > 300
    }
}
