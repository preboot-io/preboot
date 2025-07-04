package io.preboot.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.preboot.query.testdata.TestEvent;
import io.preboot.query.testdata.TestEventRepository;
import io.preboot.query.config.TestContainersConfig;
import java.time.Instant;
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
@Sql("/test-data-instant.sql")
public class InstantHandlingTest {

    @Autowired
    private TestEventRepository eventRepository;

    @Test
    void testInstantEquality_WithInstantValue_ShouldWork() {
        // Given
        Instant targetTime = Instant.parse("2024-01-02T11:30:00Z");
        SearchParams params = SearchParams.criteria(
            FilterCriteria.eq("eventTimestamp", targetTime)
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEventName()).isEqualTo("Payment Processed");
        assertThat(result.getContent().get(0).getEventTimestamp()).isEqualTo(targetTime);
    }

    @Test
    void testInstantEquality_WithStringValue_ShouldWork() {
        // Given
        SearchParams params = SearchParams.criteria(
            FilterCriteria.eq("eventTimestamp", "2024-01-02T11:30:00Z")
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEventName()).isEqualTo("Payment Processed");
    }

    @Test
    void testInstantEquality_BothInstantAndString_ShouldProduceSameResults() {
        // Given
        Instant targetTime = Instant.parse("2024-01-03T14:45:00Z");
        SearchParams instantParams = SearchParams.criteria(
            FilterCriteria.eq("eventTimestamp", targetTime)
        ).build();
        
        SearchParams stringParams = SearchParams.criteria(
            FilterCriteria.eq("eventTimestamp", "2024-01-03T14:45:00Z")
        ).build();

        // When
        Page<TestEvent> instantResult = eventRepository.findAll(instantParams);
        Page<TestEvent> stringResult = eventRepository.findAll(stringParams);

        // Then
        assertThat(instantResult.getContent()).hasSize(1);
        assertThat(stringResult.getContent()).hasSize(1);
        assertThat(instantResult.getContent().get(0).getEventName())
            .isEqualTo(stringResult.getContent().get(0).getEventName());
        assertThat(instantResult.getContent().get(0).getEventName()).isEqualTo("Order Created");
    }

    @Test
    void testInstantGreaterThan_WithInstantValue_ShouldWork() {
        // Given
        Instant cutoffTime = Instant.parse("2024-01-04T12:00:00Z");
        SearchParams params = SearchParams.criteria(
            FilterCriteria.gt("eventTimestamp", cutoffTime)
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .containsExactlyInAnyOrder("Payment Failed", "Order Shipped", "User Logout");
    }

    @Test
    void testInstantLessThan_WithStringValue_ShouldWork() {
        // Given
        SearchParams params = SearchParams.criteria(
            FilterCriteria.lt("eventTimestamp", "2024-01-03T00:00:00Z")
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .containsExactlyInAnyOrder("User Registration", "Payment Processed");
    }

    @Test
    void testInstantBetween_WithInstantValues_ShouldWork() {
        // Given
        Instant startTime = Instant.parse("2024-01-02T00:00:00Z");
        Instant endTime = Instant.parse("2024-01-05T00:00:00Z");
        SearchParams params = SearchParams.criteria(
            FilterCriteria.between("eventTimestamp", startTime, endTime)
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .containsExactlyInAnyOrder("Payment Processed", "Order Created", "User Login");
    }

    @Test
    void testInstantBetween_WithStringValues_ShouldWork() {
        // Given
        SearchParams params = SearchParams.criteria(
            FilterCriteria.between("eventTimestamp", "2024-01-02T00:00:00Z", "2024-01-05T00:00:00Z")
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .containsExactlyInAnyOrder("Payment Processed", "Order Created", "User Login");
    }

    @Test
    void testInstantNotEquals_WithInstantValue_ShouldWork() {
        // Given
        Instant excludeTime = Instant.parse("2024-01-04T09:15:00Z");
        SearchParams params = SearchParams.criteria(
            FilterCriteria.neq("eventTimestamp", excludeTime)
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(6);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .doesNotContain("User Login");
    }

    @Test
    void testInstantGreaterThanOrEqual_ShouldWork() {
        // Given
        Instant cutoffTime = Instant.parse("2024-01-05T16:20:00Z");
        SearchParams params = SearchParams.criteria(
            FilterCriteria.gte("eventTimestamp", cutoffTime)
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .containsExactlyInAnyOrder("Payment Failed", "Order Shipped", "User Logout");
    }

    @Test
    void testInstantLessThanOrEqual_ShouldWork() {
        // Given
        Instant cutoffTime = Instant.parse("2024-01-03T14:45:00Z");
        SearchParams params = SearchParams.criteria(
            FilterCriteria.lte("eventTimestamp", cutoffTime)
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .containsExactlyInAnyOrder("User Registration", "Payment Processed", "Order Created");
    }

    @Test
    void testInstantWithAndCondition_ShouldWork() {
        // Given
        Instant afterTime = Instant.parse("2024-01-01T00:00:00Z");
        SearchParams params = SearchParams.criteria(
            FilterCriteria.and(List.of(
                FilterCriteria.gt("eventTimestamp", afterTime),
                FilterCriteria.eq("eventType", "PAYMENT_EVENT")
            ))
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .containsExactlyInAnyOrder("Payment Processed", "Payment Failed");
    }

    @Test
    void testCountWithInstantFilter_ShouldWork() {
        // Given
        Instant afterTime = Instant.parse("2024-01-04T00:00:00Z");
        SearchParams params = SearchParams.criteria(
            FilterCriteria.gt("eventTimestamp", afterTime)
        ).build();

        // When
        long count = eventRepository.count(params);

        // Then
        assertThat(count).isEqualTo(4);
    }

    @Test
    void testInstantWithSorting_ShouldWork() {
        // Given
        SearchParams params = SearchParams.criteria(
            FilterCriteria.eq("eventType", "USER_EVENT")
        ).sortDirection(Sort.Direction.DESC).sortField("eventTimestamp").build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .containsExactly("User Logout", "User Login", "User Registration");
    }

    @Test
    void testInstantWithNullHandling_ShouldWork() {
        // Given
        SearchParams params = SearchParams.criteria(
            FilterCriteria.isNull("updatedAt")
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEventName()).isEqualTo("Order Shipped");
        assertThat(result.getContent().get(0).getUpdatedAt()).isNull();
    }

    @Test
    void testInstantWithNotNullHandling_ShouldWork() {
        // Given
        SearchParams params = SearchParams.criteria(
            FilterCriteria.isNotNull("updatedAt")
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(6);
        assertThat(result.getContent())
            .allSatisfy(event -> assertThat(event.getUpdatedAt()).isNotNull());
    }

    @Test
    void testInstantInOperator_WithInstantValues_ShouldWork() {
        // Given
        Instant time1 = Instant.parse("2024-01-01T10:00:00Z");
        Instant time2 = Instant.parse("2024-01-03T14:45:00Z");
        SearchParams params = SearchParams.criteria(
            FilterCriteria.in("eventTimestamp", time1, time2)
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .containsExactlyInAnyOrder("User Registration", "Order Created");
    }

    @Test
    void testInstantInOperator_WithStringValues_ShouldWork() {
        // Given
        SearchParams params = SearchParams.criteria(
            FilterCriteria.in("eventTimestamp", "2024-01-02T11:30:00Z", "2024-01-05T16:20:00Z")
        ).build();

        // When
        Page<TestEvent> result = eventRepository.findAll(params);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(TestEvent::getEventName)
            .containsExactlyInAnyOrder("Payment Processed", "Payment Failed");
    }
}