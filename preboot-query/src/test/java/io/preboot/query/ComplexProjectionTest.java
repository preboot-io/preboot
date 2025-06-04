package io.preboot.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.preboot.query.config.TestContainersConfig;
import io.preboot.query.testdata.complex.CategoryInfo;
import io.preboot.query.testdata.complex.TransactionRepository;
import io.preboot.query.testdata.complex.TransactionWithCategories;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestContainersConfig.class)
@Transactional
@Sql("/complex-projection-test.sql")
@ActiveProfiles("test")
class ComplexProjectionTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void findOne_WithCategoriesProjection_ShouldReturnTransactionWithCategoryDetails() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.eq("name", "Grocery Shopping"))
                .build();

        // Act
        var result = transactionRepository.findOneProjectedBy(params, TransactionWithCategories.class);

        // Assert
        assertThat(result).isPresent();
        TransactionWithCategories transaction = result.get();

        assertThat(transaction.getName()).isEqualTo("Grocery Shopping");
        assertThat(transaction.getType()).isEqualTo("EXPENSE");
        assertThat(transaction.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));

        assertThat(transaction.getCategories())
                .isNotEmpty()
                .hasSize(2)
                .anySatisfy(category -> {
                    assertThat(category.getName()).isEqualTo("Food");
                    assertThat(category.getColor()).isEqualTo("#FF0000");
                })
                .anySatisfy(category -> {
                    assertThat(category.getName()).isEqualTo("Household");
                    assertThat(category.getColor()).isEqualTo("#00FF00");
                });
    }

    @Test
    void findAll_WithCategoriesProjection_ShouldReturnAllTransactionsWithCategories() {
        // Arrange
        SearchParams params = SearchParams.empty();

        // Act
        var result = transactionRepository.findAllProjectedBy(params, TransactionWithCategories.class);

        // Assert
        assertThat(result.getContent()).isNotEmpty().allSatisfy(transaction -> {
            assertThat(transaction.getUuid()).isNotNull();
            assertThat(transaction.getName()).isNotEmpty();
            // If transaction has categories, verify their properties
            if (!transaction.getCategories().isEmpty()) {
                assertThat(transaction.getCategories()).allSatisfy(category -> {
                    assertThat(category.getName()).isNotEmpty();
                    assertThat(category.getColor()).isNotEmpty();
                });
            }
        });
    }

    @Test
    void findAll_WithCategoryFilter_ShouldReturnMatchingTransactions() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.eq("categories.category.name", "Food"))
                .build();

        // Act
        var result = transactionRepository.findAllProjectedBy(params, TransactionWithCategories.class);

        // Assert
        assertThat(result.getContent()).isNotEmpty().allSatisfy(transaction -> assertThat(transaction.getCategories())
                .extracting(CategoryInfo::getName)
                .contains("Food"));
    }

    @Test
    void findAll_WithFilterOnMultipleCategoryProperties_ShouldReturnMatchingTransactions() {
        // Test filtering on multiple category properties
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.eq("categories.category.name", "Food"),
                        FilterCriteria.eq("categories.category.color", "#FF0000"))
                .build();

        var result = transactionRepository.findAllProjectedBy(params, TransactionWithCategories.class);

        assertThat(result.getContent()).isNotEmpty().allSatisfy(transaction -> {
            assertThat(transaction.getCategories())
                    .anyMatch(category -> category.getName().equals("Food")
                            && category.getColor().equals("#FF0000"));
        });
    }

    @Test
    void findAll_WithOrFilterOnCategories_ShouldReturnMatchingTransactions() {
        // Test OR condition on categories
        SearchParams params = SearchParams.criteria(FilterCriteria.or(Arrays.asList(
                        FilterCriteria.eq("categories.category.name", "Food"),
                        FilterCriteria.eq("categories.category.name", "Entertainment"))))
                .build();

        var result = transactionRepository.findAllProjectedBy(params, TransactionWithCategories.class);

        assertThat(result.getContent()).isNotEmpty().allSatisfy(transaction -> {
            assertThat(transaction.getCategories())
                    .anyMatch(category -> category.getName().equals("Food")
                            || category.getName().equals("Entertainment"));
        });
    }

    @Test
    void findAll_WithComplexCalculatedProperties_ShouldWork() {
        interface TransactionWithCalculations {
            UUID getUuid();

            String getName();

            BigDecimal getAmount();

            @Value("#{target.categories}")
            List<CategoryInfo> getCategories();

            @Value("#{target.amount > 1000 ? 'High' : 'Normal'}")
            String getAmountCategory();

            @Value("#{target.name + (target.categories.size() > 0 ? ' (' + target.categories[0].name + ')' : '')}")
            String getDisplayName();
        }

        SearchParams params = SearchParams.empty();

        var result = transactionRepository.findAllProjectedBy(params, TransactionWithCalculations.class);

        assertThat(result.getContent()).isNotEmpty().allSatisfy(transaction -> {
            // Check amount category (High for amounts > 1000, Normal otherwise)
            assertThat(transaction.getAmountCategory())
                    .isEqualTo(transaction.getAmount().compareTo(new BigDecimal("1000")) > 0 ? "High" : "Normal");

            // Check display name
            assertThat(transaction.getDisplayName()).contains(transaction.getName());

            // If transaction has categories, verify the first category name is included
            if (!transaction.getCategories().isEmpty()) {
                assertThat(transaction.getDisplayName())
                        .contains(transaction.getCategories().get(0).getName());
            }
        });
    }

    @Test
    void findAll_WithDateBasedFiltering_ShouldReturnMatchingTransactions() {
        // Test filtering based on transaction date
        LocalDate testDate = LocalDate.of(2024, 2, 15);

        SearchParams params = SearchParams.criteria(
                        FilterCriteria.eq("transactionDate", testDate),
                        FilterCriteria.eq("categories.category.name", "Food"))
                .build();

        var result = transactionRepository.findAllProjectedBy(params, TransactionWithCategories.class);

        assertThat(result.getContent()).isNotEmpty().allSatisfy(transaction -> {
            assertThat(transaction.getTransactionDate()).isEqualTo(testDate);
            assertThat(transaction.getCategories())
                    .anyMatch(category -> category.getName().equals("Food"));
        });
    }

    @Test
    void findAll_WithPaginationAndSorting_ShouldReturnCorrectResults() {
        SearchParams params = SearchParams.builder()
                .page(0)
                .size(2)
                .sortField("amount")
                .sortDirection(Sort.Direction.DESC)
                .filters(List.of(
                        FilterCriteria.gt("amount", new BigDecimal("100")),
                        FilterCriteria.eq("categories.category.name", "Food")))
                .build();

        var result = transactionRepository.findAllProjectedBy(params, TransactionWithCategories.class);

        assertThat(result.getContent()).hasSize(1).allSatisfy(transaction -> {
            assertThat(transaction.getAmount()).isGreaterThan(new BigDecimal("100"));
            assertThat(transaction.getCategories())
                    .anyMatch(category -> category.getName().equals("Food"));
        });
        assertThat(result.getContent())
                .extracting(TransactionWithCategories::getAmount)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }
}
