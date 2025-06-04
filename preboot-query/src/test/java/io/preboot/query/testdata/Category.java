package io.preboot.query.testdata;

import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("categories")
public class Category {
    @Id
    private Long id;

    private UUID uuid;
    private String name;
    private String description;
}
