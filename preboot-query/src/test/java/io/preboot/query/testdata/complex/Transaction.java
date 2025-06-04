package io.preboot.query.testdata.complex;

import io.preboot.query.HasUuid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("transactions")
public class Transaction implements HasUuid {
    @Id
    private Long id;

    private UUID uuid;
    private String name;
    private BigDecimal amount;

    @Column("type")
    private String type;

    private LocalDate transactionDate;

    @MappedCollection(idColumn = "transaction_id")
    private Set<TransactionCategory> categories;
}
