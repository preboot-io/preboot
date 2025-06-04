package io.preboot.query.testdata;

import io.preboot.query.AggregateReference;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("products")
@Data
public class Product {
    @Id
    private Long id;

    private UUID uuid;
    private String name;
    private BigDecimal price;

    @AggregateReference(
            target = Category.class,
            targetColumn = "uuid",
            sourceColumn = "category_uuid",
            alias = "category")
    private UUID categoryUuid;
}
