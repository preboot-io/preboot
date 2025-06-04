package io.preboot.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.preboot.query.config.TestContainersConfig;
import io.preboot.query.testdata.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
@Sql("/aggregate-reference-test.sql")
class AggregateReferenceTest {

    @Autowired
    private ProductRepository productRepository;

    interface ProductWithCategoryProjection {
        UUID getUuid();

        String getName();

        BigDecimal getPrice();

        @Value("#{target.category.name}")
        String getCategoryName();

        @Value("#{target.category.description}")
        String getCategoryDescription();

        @Value("#{target.name + ' - ' + target.category.name}")
        String getDisplayName();

        @Value("#{target.price > 100 ? 'Premium' : 'Standard'}")
        String getPriceCategory();
    }

    @Test
    void findAll_WithAggregateReference_ShouldReturnCorrectProjection() {
        // Arrange
        SearchParams params = SearchParams.builder()
                .filters(List.of(FilterCriteria.gt("price", new BigDecimal("50"))))
                .sortField("price")
                .build();

        // Act
        Page<ProductWithCategoryProjection> result =
                productRepository.findAllProjectedBy(params, ProductWithCategoryProjection.class);

        // Assert
        assertThat(result.getContent()).isNotEmpty().allSatisfy(product -> {
            assertThat(product.getUuid()).isNotNull();
            assertThat(product.getName()).isNotBlank();
            assertThat(product.getCategoryName()).isNotBlank();
            assertThat(product.getCategoryDescription()).isNotBlank();
            assertThat(product.getDisplayName()).isEqualTo(product.getName() + " - " + product.getCategoryName());
            assertThat(product.getPriceCategory()).isIn("Premium", "Standard");
        });
    }

    @Test
    void findAll_WithAggregateReferenceAndComplexFilters_ShouldReturnMatchingResults() {
        // Arrange
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.gt("price", new BigDecimal("100")),
                        FilterCriteria.like("category.name", "Electronics"))
                .sortField("name")
                .build();

        // Act
        Page<ProductWithCategoryProjection> result =
                productRepository.findAllProjectedBy(params, ProductWithCategoryProjection.class);

        // Assert
        assertThat(result.getContent()).isNotEmpty().allSatisfy(product -> {
            assertThat(product.getPrice()).isGreaterThan(new BigDecimal("100"));
            assertThat(product.getCategoryName()).contains("Electronics");
            assertThat(product.getPriceCategory()).isEqualTo("Premium");
        });
    }

    @Test
    void findOne_WithAggregateReference_ShouldReturnCorrectProjection() {
        // Arrange
        SearchParams params = SearchParams.criteria(FilterCriteria.eq("category.name", "Electronics"))
                .sortField("name")
                .build();

        // Act
        Optional<ProductWithCategoryProjection> result =
                productRepository.findOneProjectedBy(params, ProductWithCategoryProjection.class);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getCategoryName()).isEqualTo("Electronics");
    }

    @Test
    void findAll_WithMultipleAggregateConditions_ShouldReturnMatchingResults() {
        // Arrange
        SearchParams params = SearchParams.criteria(
                        FilterCriteria.eq("category.name", "Electronics"),
                        FilterCriteria.like("category.description", "%devices%"))
                .sortField("name")
                .build();

        // Act
        Page<ProductWithCategoryProjection> result =
                productRepository.findAllProjectedBy(params, ProductWithCategoryProjection.class);

        // Assert
        assertThat(result.getContent()).isNotEmpty().allSatisfy(product -> {
            assertThat(product.getCategoryName()).isEqualTo("Electronics");
            assertThat(product.getCategoryDescription()).contains("devices");
        });
    }
}
