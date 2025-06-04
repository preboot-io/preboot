package io.preboot.query.testdata.complex;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

public interface CategoryInfo {
    UUID getCategoryUuid();

    @Value("#{target.category.name}")
    String getName();

    @Value("#{target.category.color}")
    String getColor();
}
