package io.preboot.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.preboot.query.config.TestContainersConfig;
import io.preboot.query.testdata.TestOrder;
import io.preboot.query.testdata.TestOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/** Test cases specifically for date/time filtering and conversion. */
@SpringBootTest
@Import(TestContainersConfig.class)
@Transactional
@Sql("/test-data.sql")
class DateTimeConversionTest {

    @Autowired
    private TestOrderRepository orderRepository;

    private LocalDateTime testDate1;
    private LocalDateTime testDate2;
    private String testDate1Iso;
    private String testDate2Iso;

    @BeforeEach
    void setUp() {
        // For reference, orders in test-data.sql have these dates:
        // ORD001: '2024-01-01 10:00:00'
        // ORD002: '2024-01-02 11:00:00'
        // ORD003: '2024-01-03 12:00:00'
        // ORD004: '2024-01-04 13:00:00'
        // ORD005: '2024-01-05 14:00:00'

        testDate1 = LocalDateTime.of(2024, 1, 2, 0, 0, 0);
        testDate2 = LocalDateTime.of(2024, 1, 4, 0, 0, 0);

        testDate1Iso = testDate1.toString(); // "2024-01-02T00:00:00"
        testDate2Iso = testDate2.toString(); // "2024-01-04T00:00:00"
    }

    @Test
    void testDateEquals_UsingLocalDateTime() {
        // Test equals with LocalDateTime object
        SearchParams params = SearchParams.builder()
                .filters(List.of(FilterCriteria.eq("createdAt", LocalDateTime.of(2024, 1, 1, 10, 0, 0))))
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent())
                .hasSize(1)
                .extracting(TestOrder::getOrderNumber)
                .containsExactly("ORD001");
    }

    @Test
    void testDateEquals_UsingIsoString() {
        // Test equals with ISO string - should be converted properly
        SearchParams params = SearchParams.builder()
                .filters(List.of(FilterCriteria.eq("createdAt", "2024-01-01T10:00:00")))
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent())
                .hasSize(1)
                .extracting(TestOrder::getOrderNumber)
                .containsExactly("ORD001");
    }

    @Test
    void testDateGreaterThan_UsingIsoString() {
        // Test greater than with ISO string
        SearchParams params = SearchParams.builder()
                .filters(List.of(FilterCriteria.gt("createdAt", testDate1Iso)))
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent())
                .hasSize(4)
                .extracting(TestOrder::getOrderNumber)
                .containsExactlyInAnyOrder("ORD002", "ORD003", "ORD004", "ORD005");
    }

    @Test
    void testDateLessThan_UsingIsoString() {
        // Test less than with ISO string
        SearchParams params = SearchParams.builder()
                .filters(List.of(FilterCriteria.lt("createdAt", testDate2Iso)))
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent())
                .hasSize(3)
                .extracting(TestOrder::getOrderNumber)
                .containsExactlyInAnyOrder("ORD001", "ORD002", "ORD003");
    }

    @Test
    void testDateBetween_UsingIsoStrings() {
        // Test between with ISO strings
        SearchParams params = SearchParams.builder()
                .filters(List.of(FilterCriteria.between("createdAt", testDate1Iso, testDate2Iso)))
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent())
                .hasSize(2)
                .extracting(TestOrder::getOrderNumber)
                .containsExactlyInAnyOrder("ORD002", "ORD003");
    }

    @Test
    void testCombinedDateAndOtherFilters() {
        // Combine date filter with other criteria
        SearchParams params = SearchParams.builder()
                .filters(
                        List.of(FilterCriteria.gt("createdAt", testDate1Iso), FilterCriteria.eq("status", "COMPLETED")))
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent())
                .hasSize(1)
                .extracting(TestOrder::getOrderNumber)
                .containsExactly("ORD003");
    }

    @Test
    void testDateSorting_Ascending() {
        // Test sorting by date ascending
        SearchParams params = SearchParams.builder()
                .sortField("createdAt")
                .sortDirection(Sort.Direction.ASC)
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent())
                .extracting(TestOrder::getOrderNumber)
                .containsExactly("ORD001", "ORD002", "ORD003", "ORD004", "ORD005");
    }

    @Test
    void testDateSorting_Descending() {
        // Test sorting by date descending
        SearchParams params = SearchParams.builder()
                .sortField("createdAt")
                .sortDirection(Sort.Direction.DESC)
                .build();

        Page<TestOrder> result = orderRepository.findAll(params);

        assertThat(result.getContent())
                .extracting(TestOrder::getOrderNumber)
                .containsExactly("ORD005", "ORD004", "ORD003", "ORD002", "ORD001");
    }
}
