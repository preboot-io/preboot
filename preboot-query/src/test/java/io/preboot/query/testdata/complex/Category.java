package io.preboot.query.testdata.complex;

import io.preboot.query.HasUuid;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("categories")
public class Category implements HasUuid {
    @Id
    private Long id;

    private UUID uuid;
    private String name;
    private String color;
}
