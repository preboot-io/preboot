package io.preboot.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.preboot.query.config.TestContainersConfig;
import io.preboot.query.testdata.TestOrder;
import io.preboot.query.testdata.TestOrderRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Sql("/test-data.sql")
@Import(TestContainersConfig.class)
class FilterableRepositoryIntegrationTest {

    @Autowired
    private TestOrderRepository orderRepository;

    @Test
    void findAll_WithNoFilters_ShouldReturnAllOrders() {
        // Arrange
        SearchParams params = SearchParams.empty();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);
    }

    @Test
    void findAll_WithPagination_ShouldReturnCorrectPage() {
        // Arrange
        SearchParams params = SearchParams.builder().page(0).size(2).build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findAll_WithSorting_ShouldReturnOrderedResults() {
        // Arrange
        SearchParams params = SearchParams.builder()
                .sortField("amount")
                .sortDirection(Sort.Direction.DESC)
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .extracting(TestOrder::getAmount)
                .isSortedAccordingTo((bigDecimal, val) -> -1 * bigDecimal.compareTo(val));
    }

    @Test
    void findAll_WithEqualsFilter_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params =
                SearchParams.criteria(FilterCriteria.eq("status", "COMPLETED")).build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).allMatch(order -> "COMPLETED".equals(order.getStatus()));
    }

    @Test
    void findAll_WithGreaterThanFilter_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.gt("amount", new BigDecimal("100")))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).allMatch(order -> order.getAmount().compareTo(new BigDecimal("100")) > 0);
    }

    @Test
    void findAll_WithLikeFilter_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params =
                SearchParams.criteria(FilterCriteria.like("orderNumber", "ORD")).build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).allMatch(order -> order.getOrderNumber().startsWith("ORD"));
    }

    @Test
    void findAll_WithMultipleFilters_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.eq("status", "COMPLETED"), FilterCriteria.gt("amount", new BigDecimal("100")))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .allMatch(order -> "COMPLETED".equals(order.getStatus())
                        && order.getAmount().compareTo(new BigDecimal("100")) > 0);
    }

    @Test
    void findOne_WithMatchingFilter_ShouldReturnOrder() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.eq("orderNumber", "ORD001"))
                .build();

        // Act
        Optional<TestOrder> result = orderRepository.findOne(params);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getOrderNumber()).isEqualTo("ORD001");
    }

    @Test
    void findOne_WithNonMatchingFilter_ShouldReturnEmpty() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.eq("orderNumber", "NONEXISTENT"))
                .build();

        // Act
        Optional<TestOrder> result = orderRepository.findOne(params);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void count_WithFilters_ShouldReturnCorrectCount() {
        // Arrange
        SearchParams params =
                SearchParams.criteria(FilterCriteria.eq("status", "COMPLETED")).build();

        // Act
        long count = orderRepository.count(params);

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    void findAll_WhenUnpaged_ShouldReturnAllResults() {
        // Arrange
        SearchParams params = SearchParams.builder().unpaged(true).build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5); // Assuming 5 total records
        assertThat(result.getPageable().isPaged()).isFalse();
    }

    @Test
    void findAll_WhenUnpagedWithSort_ShouldReturnAllResultsSorted() {
        // Arrange
        SearchParams params = SearchParams.builder()
                .unpaged(true)
                .sortField("amount")
                .sortDirection(Sort.Direction.DESC)
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .hasSize(5)
                .extracting(TestOrder::getAmount)
                .isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(result.getPageable().isPaged()).isFalse();
    }

    @Test
    void findAll_WithGreaterThanEqualsFilter_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.gte("amount", new BigDecimal("300")))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).allMatch(order -> order.getAmount().compareTo(new BigDecimal("300")) >= 0);
    }

    @Test
    void findAll_WithLessThanEqualsFilter_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.lte("amount", new BigDecimal("200")))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).allMatch(order -> order.getAmount().compareTo(new BigDecimal("200")) <= 0);
    }

    @Test
    void findAll_WithCombinedGteAndLteFilter_ShouldReturnMatchingOrders() {
        // Arrange
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.gte("amount", new BigDecimal("200")),
                        FilterCriteria.lte("amount", new BigDecimal("400")))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .extracting(TestOrder::getAmount)
                .containsExactlyInAnyOrder(
                        new BigDecimal("200.00"), new BigDecimal("300.00"), new BigDecimal("400.00"));
    }

    @Test
    void testNotEquals() {
        Page<TestOrder> result = orderRepository.findAll(
                SearchParams.criteria(FilterCriteria.neq("status", "COMPLETED")).build());

        assertThat(result.getContent()).extracting(TestOrder::getStatus).doesNotContain("COMPLETED");
    }

    @Test
    void testLike() {
        Page<TestOrder> result =
                orderRepository.findAll(SearchParams.criteria(FilterCriteria.like("orderNumber", "ORD%"))
                        .build());

        assertThat(result.getContent())
                .extracting(TestOrder::getOrderNumber)
                .allMatch(number -> number.startsWith("ORD"));
    }

    @Test
    void testIsNull() {
        // Skip this test initially as we don't have null values in test data
        Page<TestOrder> result = orderRepository.findAll(
                SearchParams.criteria(FilterCriteria.isNull("status")).build());

        assertThat(result.getContent()).isEmpty(); // We expect no results as no orders have null status
    }

    @Test
    void testIsNotNull() {
        Page<TestOrder> result = orderRepository.findAll(
                SearchParams.criteria(FilterCriteria.isNotNull("status")).build());

        assertThat(result.getContent()).extracting(TestOrder::getStatus).doesNotContainNull();
    }

    @Test
    void testIn() {
        Page<TestOrder> result =
                orderRepository.findAll(SearchParams.criteria(FilterCriteria.in("status", "COMPLETED", "PENDING"))
                        .build());

        assertThat(result.getContent()).extracting(TestOrder::getStatus).containsOnly("COMPLETED", "PENDING");
    }

    @Test
    void testArrayOverlap_ShouldReturnMatchingOrders() {
        SearchParams params = SearchParams.criteria(FilterCriteria.ao("tags", "priority", "discount"))
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result).isNotNull();
        assertThat(result.getContent())
                .hasSize(4)
                .allMatch(order ->
                        order.getTags().contains("priority") || order.getTags().contains("discount"));
    }

    @Test
    void testBetween() {
        BigDecimal from = new BigDecimal("200");
        BigDecimal to = new BigDecimal("400");

        Page<TestOrder> result =
                orderRepository.findAll(SearchParams.criteria(FilterCriteria.between("amount", from, to))
                        .build());

        assertThat(result.getContent())
                .extracting(TestOrder::getAmount)
                .allMatch(amount -> amount.compareTo(from) >= 0 && amount.compareTo(to) <= 0);
    }

    @Test
    void testCombinedOperators() {
        Page<TestOrder> result = orderRepository.findAll(SearchParams.criteria(
                        FilterCriteria.gt("amount", new BigDecimal("200")),
                        FilterCriteria.lt("amount", new BigDecimal("400")),
                        FilterCriteria.neq("status", "CANCELLED"))
                .build());

        assertThat(result.getContent())
                .allMatch(order -> order.getAmount().compareTo(new BigDecimal("200")) > 0
                        && order.getAmount().compareTo(new BigDecimal("400")) < 0
                        && !"CANCELLED".equals(order.getStatus()));
    }

    @Test
    void testOr_WithStatusConditions() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.or(Arrays.asList(
                        FilterCriteria.eq("status", "COMPLETED"), FilterCriteria.eq("status", "PENDING"))))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result.getContent())
                .extracting(TestOrder::getStatus)
                .containsOnly("COMPLETED", "PENDING")
                .doesNotContain("CANCELLED");
    }

    @Test
    void testComplex_AndWithOr() {
        // Arrange
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.gt("amount", new BigDecimal("200")),
                        FilterCriteria.or(Arrays.asList(
                                FilterCriteria.eq("status", "COMPLETED"), FilterCriteria.eq("status", "PENDING"))))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result.getContent())
                .allMatch(order -> order.getAmount().compareTo(new BigDecimal("200")) > 0
                        && (order.getStatus().equals("COMPLETED")
                                || order.getStatus().equals("PENDING")));
    }

    @Test
    void testMultipleOr_Groups() {
        // Arrange
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.or(Arrays.asList(
                                FilterCriteria.eq("status", "COMPLETED"), FilterCriteria.eq("status", "PENDING"))),
                        FilterCriteria.or(Arrays.asList(
                                FilterCriteria.lt("amount", new BigDecimal("200")),
                                FilterCriteria.gt("amount", new BigDecimal("400")))))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result.getContent())
                .allMatch(order -> (order.getStatus().equals("COMPLETED")
                                || order.getStatus().equals("PENDING"))
                        && (order.getAmount().compareTo(new BigDecimal("200")) < 0
                                || order.getAmount().compareTo(new BigDecimal("400")) > 0));
    }

    @Test
    void testNestedOr_WithAmount() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.or(Arrays.asList(
                        FilterCriteria.eq("status", "COMPLETED"), FilterCriteria.gt("amount", new BigDecimal("400")))))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result.getContent())
                .allMatch(order -> order.getStatus().equals("COMPLETED")
                        || order.getAmount().compareTo(new BigDecimal("400")) > 0);
    }

    @Test
    void testOr_WithNestedProperties() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.or(Arrays.asList(
                        FilterCriteria.eq("orderItems.productCode", "PROD-A"),
                        FilterCriteria.eq("orderItems.productCode", "PROD-B"))))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result.getContent()).allMatch(order -> order.getOrderItems().stream()
                .anyMatch(item -> item.getProductCode().equals("PROD-A")
                        || item.getProductCode().equals("PROD-B")));
    }

    @Test
    void testNestedOperators() {
        // This will create a complex condition like:
        // (status = 'COMPLETED' AND (amount > 200 OR amount < 100))
        // OR
        // (status = 'PENDING' AND amount >= 400)

        // Create the (amount > 200 OR amount < 100) sub-condition
        List<FilterCriteria> amountConditions = Arrays.asList(
                FilterCriteria.gt("amount", new BigDecimal("200")), FilterCriteria.lt("amount", new BigDecimal("100")));

        // Create first main group: status = 'COMPLETED' AND (amount > 200 OR amount < 100)
        List<FilterCriteria> firstGroup =
                Arrays.asList(FilterCriteria.eq("status", "COMPLETED"), FilterCriteria.or(amountConditions));

        // Create second main group: status = 'PENDING' AND amount >= 400
        List<FilterCriteria> secondGroup = Arrays.asList(
                FilterCriteria.eq("status", "PENDING"), FilterCriteria.gte("amount", new BigDecimal("400")));

        // Combine both main groups with OR
        SearchParams params = SearchParams.criteria(FilterCriteria.or(
                        Arrays.asList(FilterCriteria.and(firstGroup), FilterCriteria.and(secondGroup))))
                .build();

        // Act
        Page<TestOrder> result = orderRepository.findAll(params);

        // Assert
        assertThat(result.getContent())
                .isNotEmpty()
                .allMatch(order ->
                        // First condition: COMPLETED AND (amount > 200 OR amount < 100)
                        ("COMPLETED".equals(order.getStatus())
                                        && (order.getAmount().compareTo(new BigDecimal("200")) > 0
                                                || order.getAmount().compareTo(new BigDecimal("100")) < 0))
                                ||
                                // Second condition: PENDING AND amount >= 400
                                ("PENDING".equals(order.getStatus())
                                        && order.getAmount().compareTo(new BigDecimal("400")) >= 0));

        // Verify specific matching orders
        assertThat(result.getContent())
                .extracting(order -> tuple(order.getStatus(), order.getAmount()))
                .containsExactlyInAnyOrder(
                        tuple("COMPLETED", new BigDecimal("300.00")), // Matches first condition
                        tuple("PENDING", new BigDecimal("500.00")) // Matches second condition
                        );
    }

    @Test
    void testCaseInsensitiveEquals_Status() {
        // Test different case variations for status "COMPLETED"
        List<String> variations = Arrays.asList("COMPLETED", "completed", "Completed", "ComPleTeD");

        for (String statusValue : variations) {
            SearchParams params = SearchParams.criteria(FilterCriteria.eqic("status", statusValue))
                    .build();

            Page<TestOrder> result = orderRepository.findAll(params);

            assertThat(result.getContent())
                    .as("Testing with status value: " + statusValue)
                    .hasSize(2)
                    .extracting(TestOrder::getStatus)
                    .allMatch(status -> status.equalsIgnoreCase("COMPLETED"));
        }
    }

    @Test
    void testCaseInsensitiveEquals_OrderNumber() {
        // Test with different case variations for order number
        List<String> variations = Arrays.asList("ORD001", "ord001", "Ord001", "oRd001");

        for (String orderNum : variations) {
            SearchParams params = SearchParams.criteria(FilterCriteria.eqic("orderNumber", orderNum))
                    .build();

            Page<TestOrder> result = orderRepository.findAll(params);

            assertThat(result.getContent())
                    .as("Testing with order number: " + orderNum)
                    .hasSize(1)
                    .extracting(TestOrder::getOrderNumber)
                    .allMatch(number -> number.equalsIgnoreCase("ORD001"));
        }
    }

    @Test
    void testCaseInsensitiveEquals_WithNestedProperty() {
        // Test case-insensitive equals with nested property (product code)
        List<String> variations = Arrays.asList("PROD-A", "prod-a", "Prod-A", "pRoD-a");

        for (String productCode : variations) {
            SearchParams params = SearchParams.criteria(FilterCriteria.eqic("orderItems.productCode", productCode))
                    .build();

            Page<TestOrder> result = orderRepository.findAll(params);

            assertThat(result.getContent())
                    .as("Testing with product code: " + productCode)
                    .hasSize(2)
                    .allMatch(order -> order.getOrderItems().stream()
                            .anyMatch(item -> item.getProductCode().equalsIgnoreCase("PROD-A")));
        }
    }

    @Test
    void testCaseInsensitiveEquals_CombinedWithOtherCriteria() {
        // Test combining case-insensitive equals with other criteria
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.eqic("status", "completed"), FilterCriteria.gt("amount", new BigDecimal("200")))
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent())
                .hasSize(1)
                .allMatch(order -> order.getStatus().equalsIgnoreCase("COMPLETED")
                        && order.getAmount().compareTo(new BigDecimal("200")) > 0);
    }

    @Test
    void testCaseInsensitiveEquals_WithOrCondition() {
        // Test case-insensitive equals within OR condition
        SearchParams params = SearchParams.criteria(FilterCriteria.or(Arrays.asList(
                        FilterCriteria.eqic("status", "completed"), FilterCriteria.eqic("status", "PENDING"))))
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent())
                .hasSize(4)
                .allMatch(order -> order.getStatus().equalsIgnoreCase("COMPLETED")
                        || order.getStatus().equalsIgnoreCase("PENDING"));
    }

    @Test
    void testCaseInsensitiveEquals_NoMatches() {
        // Test case-insensitive equals with non-existent value
        SearchParams params = SearchParams.criteria(FilterCriteria.eqic("status", "nonexistent"))
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent()).isEmpty();
    }
}
