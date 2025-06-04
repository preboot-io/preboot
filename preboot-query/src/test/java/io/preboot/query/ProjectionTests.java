package io.preboot.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.preboot.query.config.TestContainersConfig;
import io.preboot.query.testdata.TestOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestContainersConfig.class)
@Transactional
@Sql("/test-data.sql")
class ProjectionTests {

    @Autowired
    private TestOrderRepository orderRepository;

    // Simple projection with just one field
    public interface OrderNumberOnly {
        String getOrderNumber();
    }

    // Projection with SpEL expressions
    public interface OrderWithStatus {
        Long getId();

        String getOrderNumber();

        String getStatus();

        BigDecimal getAmount();

        @Value("#{target.orderNumber + ' - ' + target.status}")
        String getOrderSummary();

        @Value("#{target.amount > 150 ? 'High Value' : 'Standard'}")
        String getValueCategory();
    }

    // Projection with collection
    public interface OrderItemProjection {
        String getProductCode();

        Integer getQuantity();

        BigDecimal getTotalPrice();
    }

    public interface OrderWithItems {
        String getOrderNumber();

        String getStatus();

        @Value("#{target.orderItems}")
        List<OrderItemProjection> getOrderItems();
    }

    @Test
    void findOne_WithSimpleProjection_ShouldWork() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.eq("orderNumber", "ORD001"))
                .unpaged(true)
                .sortField(null) // "id" is default sort field and we don't have this in projection
                .build();

        // Act
        Optional<OrderNumberOnly> result = orderRepository.findOneProjectedBy(params, OrderNumberOnly.class);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getOrderNumber()).isEqualTo("ORD001");
    }

    @Test
    void findOne_WithCalculatedFieldsProjection_ShouldWork() {
        // Test with regular order
        testOrderWithStatus("ORD001", "COMPLETED", new BigDecimal("100.00"), "Standard");

        // Test with high value order
        testOrderWithStatus("ORD003", "COMPLETED", new BigDecimal("300.00"), "High Value");
    }

    private void testOrderWithStatus(
            String orderNumber, String expectedStatus, BigDecimal expectedAmount, String expectedCategory) {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.eq("orderNumber", orderNumber))
                .build();

        // Act
        Optional<OrderWithStatus> result = orderRepository.findOneProjectedBy(params, OrderWithStatus.class);

        // Assert
        assertThat(result).isPresent();
        OrderWithStatus order = result.get();
        assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(order.getStatus()).isEqualTo(expectedStatus);
        assertThat(order.getAmount()).isEqualByComparingTo(expectedAmount);
        assertThat(order.getOrderSummary()).isEqualTo(orderNumber + " - " + expectedStatus);
        assertThat(order.getValueCategory()).isEqualTo(expectedCategory);
    }

    @Test
    void findOne_WithCollectionProjection_ShouldWork() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.eq("orderNumber", "ORD001"))
                .build();

        // Act
        Optional<OrderWithItems> result = orderRepository.findOneProjectedBy(params, OrderWithItems.class);

        // Assert
        assertThat(result).isPresent();
        OrderWithItems order = result.get();
        assertThat(order.getOrderNumber()).isEqualTo("ORD001");
        assertThat(order.getStatus()).isEqualTo("COMPLETED");

        // Verify order items
        List<OrderItemProjection> items = order.getOrderItems();
        assertThat(items).hasSize(2).satisfies(itemsList -> {
            OrderItemProjection firstItem = itemsList.get(0);
            assertThat(firstItem.getProductCode()).isEqualTo("PROD-A");
            assertThat(firstItem.getQuantity()).isEqualTo(2);
            assertThat(firstItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("100.00"));

            OrderItemProjection secondItem = itemsList.get(1);
            assertThat(secondItem.getProductCode()).isEqualTo("PROD-B");
            assertThat(secondItem.getQuantity()).isEqualTo(1);
            assertThat(secondItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        });
    }

    @Test
    void findAll_WithProjectionShouldReturnMultipleResults() {
        // Arrange
        SearchParams params =
                SearchParams.criteria(FilterCriteria.eq("status", "COMPLETED")).build();

        // Act
        Page<OrderWithStatus> result = orderRepository.findAllProjectedBy(params, OrderWithStatus.class);

        // Assert
        assertThat(result.getContent()).hasSize(2).allSatisfy(order -> {
            assertThat(order.getStatus()).isEqualTo("COMPLETED");
            assertThat(order.getOrderSummary()).endsWith("- COMPLETED");
            assertThat(order.getValueCategory()).isIn("Standard", "High Value");
        });
    }
}
