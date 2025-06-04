package io.preboot.query.testdata.complex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

public interface TransactionWithCategories {
    Long getId();

    UUID getUuid();

    String getName();

    BigDecimal getAmount();

    String getType();

    LocalDate getTransactionDate();

    @Value("#{target.categories}")
    List<CategoryInfo> getCategories();
}
