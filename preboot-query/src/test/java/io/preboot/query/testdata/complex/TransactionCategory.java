package io.preboot.query.testdata.complex;

import io.preboot.query.AggregateReference;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("transaction_categories")
public class TransactionCategory {
    @AggregateReference(
            target = Category.class,
            targetColumn = "uuid",
            sourceColumn = "category_uuid",
            alias = "category")
    private UUID categoryUuid;
}
